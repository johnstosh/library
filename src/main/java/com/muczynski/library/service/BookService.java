/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.RandomAuthor;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.BookSummaryDto;
import com.muczynski.library.dto.BulkDeleteResultDto;
import com.muczynski.library.dto.GenreLookupResultDto;
import com.muczynski.library.dto.SavedBookDto;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.BranchRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BranchRepository libraryRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private RandomAuthor randomAuthor;

    @Autowired
    private AskGrok askGrok;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    public BookDto createBook(BookDto bookDto) {
        Book book = bookMapper.toEntity(bookDto);

        // Set author - allow null for temporary books that will be enriched later
        if (bookDto.getAuthorId() != null) {
            book.setAuthor(authorRepository.findById(bookDto.getAuthorId())
                    .orElseThrow(() -> new LibraryException("Author not found: " + bookDto.getAuthorId())));
        } else {
            book.setAuthor(null);
        }

        // Set library - required field
        if (bookDto.getLibraryId() == null) {
            throw new LibraryException("Library ID is required");
        }
        book.setLibrary(libraryRepository.findById(bookDto.getLibraryId())
                .orElseThrow(() -> new LibraryException("Library not found: " + bookDto.getLibraryId())));

        // Set lastModified to current time
        book.setLastModified(LocalDateTime.now());

        Book savedBook = bookRepository.save(book);
        return bookMapper.toDto(savedBook);
    }

    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(bookMapper::toDto)
                .sorted(Comparator.comparing(BookDto::getDateAddedToLibrary,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<BookDto> getBooksWithoutLocNumber() {
        return bookRepository.findBooksWithoutLocNumber().stream()
                .map(bookMapper::toDto)
                .sorted(Comparator.comparing(BookDto::getDateAddedToLibrary,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Get books from most recent 2 days OR with temporary titles (date-pattern titles).
     * Uses efficient projection query - no N+1 queries.
     * Temporary titles match pattern: YYYY-M-D or YYYY-MM-DD at start of title.
     *
     * @return List of SavedBookDto with all fields needed by both Books page and Books from Feed page
     */
    public List<SavedBookDto> getBooksFromMostRecentDay() {
        return bookRepository.findSavedBooksWithProjection().stream()
                .map(projection -> SavedBookDto.builder()
                        .id(projection.getId())
                        .title(projection.getTitle())
                        .author(projection.getAuthorName())
                        .library(projection.getLibraryName())
                        .photoCount(projection.getPhotoCount())
                        .needsProcessing(isTemporaryTitle(projection.getTitle()))
                        .locNumber(projection.getLocNumber())
                        .status(projection.getStatus())
                        .grokipediaUrl(projection.getGrokipediaUrl())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Check if a title is a temporary title (starts with date pattern YYYY-M-D or YYYY-MM-DD).
     */
    public static boolean isTemporaryTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        return title.matches("^\\d{4}-\\d{1,2}-\\d{1,2}.*");
    }

    /**
     * Get books with temporary titles (date-pattern titles) for batch processing.
     * Uses efficient query to find IDs first, then loads full DTOs only for matching books.
     *
     * @return List of BookDto for books with temporary titles
     */
    public List<BookDto> getBooksWithTemporaryTitles() {
        List<Long> ids = bookRepository.findBookIdsWithTemporaryTitles();
        if (ids.isEmpty()) {
            return List.of();
        }
        return bookRepository.findAllById(ids).stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<BookDto> getBooksWith3LetterLocStart() {
        return bookRepository.findBooksWith3LetterLocStart().stream()
                .map(bookMapper::toDto)
                .sorted(Comparator.comparing(BookDto::getDateAddedToLibrary,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<BookDto> getBooksWithoutGrokipediaUrl() {
        return bookRepository.findBooksWithoutGrokipediaUrl().stream()
                .map(bookMapper::toDto)
                .sorted(Comparator.comparing(BookDto::getDateAddedToLibrary,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public BookDto getBookById(Long id) {
        return bookRepository.findById(id)
                .map(bookMapper::toDto)
                .orElse(null);
    }

    public List<BookDto> getBooksByAuthorId(Long authorId) {
        return bookRepository.findByAuthorIdOrderByTitleAsc(authorId).stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    public BookDto updateBook(Long id, BookDto bookDto) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new LibraryException("Book not found: " + id));
        book.setTitle(bookDto.getTitle());
        book.setPublicationYear(bookDto.getPublicationYear());
        book.setPublisher(bookDto.getPublisher());
        book.setPlotSummary(bookDto.getPlotSummary());
        book.setRelatedWorks(bookDto.getRelatedWorks());
        book.setDetailedDescription(bookDto.getDetailedDescription());
        book.setGrokipediaUrl(bookDto.getGrokipediaUrl());
        book.setFreeTextUrl(bookDto.getFreeTextUrl());
        // Preserve dateAddedToLibrary if not provided in DTO (don't overwrite with null)
        if (bookDto.getDateAddedToLibrary() != null) {
            book.setDateAddedToLibrary(bookDto.getDateAddedToLibrary());
        }
        if (bookDto.getStatus() != null) {
            book.setStatus(bookDto.getStatus());
        }
        book.setLocNumber(bookDto.getLocNumber());
        book.setStatusReason(bookDto.getStatusReason());
        if (bookDto.getAuthorId() != null) {
            book.setAuthor(authorRepository.findById(bookDto.getAuthorId()).orElseThrow(() -> new LibraryException("Author not found: " + bookDto.getAuthorId())));
        }
        if (bookDto.getLibraryId() != null) {
            book.setLibrary(libraryRepository.findById(bookDto.getLibraryId()).orElseThrow(() -> new LibraryException("Library not found: " + bookDto.getLibraryId())));
        }
        // Update tags list - normalize to lowercase with only allowed characters
        if (bookDto.getTagsList() != null) {
            book.setTagsList(bookDto.getTagsList().stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.toLowerCase().replaceAll("[^a-z0-9-]", ""))
                    .filter(tag -> !tag.isEmpty())
                    .distinct()
                    .collect(Collectors.toList()));
        } else {
            book.setTagsList(new ArrayList<>());
        }
        // Update lastModified to current time
        book.setLastModified(LocalDateTime.now());
        Book savedBook = bookRepository.save(book);
        return bookMapper.toDto(savedBook);
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new LibraryException("Book not found: " + id);
        }
        long loanCount = loanRepository.countByBookId(id);
        if (loanCount > 0) {
            throw new LibraryException("Cannot delete book because it is currently checked out with " + loanCount + " loan(s).");
        }
        bookRepository.deleteById(id);
    }

    /**
     * Delete multiple books with partial success handling.
     * Books that can be deleted are deleted; books with active loans are skipped.
     * Returns a result DTO with counts and error details for failed deletions.
     */
    @Transactional
    public BulkDeleteResultDto deleteBulkBooks(List<Long> bookIds) {
        List<Long> deletedIds = new ArrayList<>();
        List<BulkDeleteResultDto.BulkDeleteFailureDto> failures = new ArrayList<>();

        for (Long id : bookIds) {
            try {
                deleteBook(id);
                deletedIds.add(id);
            } catch (LibraryException e) {
                // Get book title for error message
                String title = bookRepository.findById(id)
                        .map(Book::getTitle)
                        .orElse("Unknown");
                failures.add(BulkDeleteResultDto.BulkDeleteFailureDto.builder()
                        .id(id)
                        .title(title)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return BulkDeleteResultDto.builder()
                .deletedCount(deletedIds.size())
                .failedCount(failures.size())
                .deletedIds(deletedIds)
                .failures(failures)
                .build();
    }

    public BookDto cloneBook(Long id) {
        Book original = bookRepository.findById(id)
                .orElseThrow(() -> new LibraryException("Book not found: " + id));

        // Generate new title with copy number
        String newTitle = generateCloneTitle(original.getTitle());

        // Create new book with same data but new title
        Book clone = new Book();
        clone.setTitle(newTitle);
        clone.setPublicationYear(original.getPublicationYear());
        clone.setPublisher(original.getPublisher());
        clone.setPlotSummary(original.getPlotSummary());
        clone.setRelatedWorks(original.getRelatedWorks());
        clone.setDetailedDescription(original.getDetailedDescription());
        clone.setDateAddedToLibrary(LocalDateTime.now());
        clone.setLastModified(LocalDateTime.now());
        clone.setStatus(original.getStatus());
        clone.setLocNumber(original.getLocNumber());
        clone.setStatusReason(original.getStatusReason());
        clone.setAuthor(original.getAuthor());
        clone.setLibrary(original.getLibrary());

        Book savedClone = bookRepository.save(clone);
        logger.info("Cloned book ID {} to new book ID {} with title '{}'", id, savedClone.getId(), newTitle);
        return bookMapper.toDto(savedClone);
    }

    private String generateCloneTitle(String originalTitle) {
        // Extract base title by removing any existing ", c. N" suffix
        String baseTitle = originalTitle;
        int copyIndex = 1;

        // Check if title already ends with ", c. N" pattern (case insensitive)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(.+),\\s*[cC]\\.\\s*(\\d+)$");
        java.util.regex.Matcher matcher = pattern.matcher(originalTitle);
        if (matcher.matches()) {
            baseTitle = matcher.group(1);
        }

        // Find all existing books with the same base title
        List<Book> allBooks = bookRepository.findAll();
        int maxCopyNumber = 0;

        for (Book book : allBooks) {
            String bookTitle = book.getTitle();

            // Check if this book's title matches the base title exactly (the original)
            if (bookTitle.equals(baseTitle)) {
                maxCopyNumber = Math.max(maxCopyNumber, 0);
            }

            // Check if this book's title matches the pattern "baseTitle, c. N"
            java.util.regex.Matcher bookMatcher = java.util.regex.Pattern
                    .compile("^" + java.util.regex.Pattern.quote(baseTitle) + ",\\s*[cC]\\.\\s*(\\d+)$")
                    .matcher(bookTitle);
            if (bookMatcher.matches()) {
                int num = Integer.parseInt(bookMatcher.group(1));
                maxCopyNumber = Math.max(maxCopyNumber, num);
            }
        }

        // Return the next copy number
        return baseTitle + ", c. " + (maxCopyNumber + 1);
    }

    private void handleRandomAuthor(BookDto dto) {
        Author randomAuthorEntity = randomAuthor.create();

        Pageable singlePage = PageRequest.of(0, 1);
        Page<Author> existingAuthors = authorRepository.findByNameContainingIgnoreCase(randomAuthorEntity.getName(), singlePage);

        Long selectedAuthorId;
        if (!existingAuthors.isEmpty()) {
            selectedAuthorId = existingAuthors.getContent().get(0).getId();
        } else {
            Author savedAuthor = authorRepository.save(randomAuthorEntity);
            selectedAuthorId = savedAuthor.getId();
        }

        dto.setAuthorId(selectedAuthorId);
        dto.setTitle("temporary title");
    }

    private Map<String, Object> extractJsonFromResponse(String response) {
        // Trim whitespace from the entire response first
        String trimmedResponse = response.trim();

        // Find the start of the JSON
        int startIndex = trimmedResponse.indexOf('{');
        if (startIndex == -1) {
            logger.debug("No opening brace found in AI response: {}", trimmedResponse);
            throw new LibraryException("No valid JSON found in response - no opening brace");
        }

        // Find the end by balancing braces
        int braceCount = 0;
        int endIndex = -1;
        for (int i = startIndex; i < trimmedResponse.length(); i++) {
            char c = trimmedResponse.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIndex = i;
                    break;
                }
            }
        }

        if (endIndex == -1) {
            logger.debug("No closing brace found in AI response: {}", trimmedResponse);
            throw new LibraryException("No valid JSON found in response - unbalanced braces");
        }

        String jsonSubstring = trimmedResponse.substring(startIndex, endIndex + 1);
        String beforeJson = trimmedResponse.substring(0, startIndex).trim();
        String afterJson = trimmedResponse.substring(endIndex + 1).trim();

        if (!beforeJson.isEmpty() && !isAllWhitespace(beforeJson)) {
            logger.warn("Extraneous text before JSON: '{}'", beforeJson);
        }
        if (!afterJson.isEmpty() && !isAllWhitespace(afterJson)) {
            logger.warn("Extraneous text after JSON: '{}'", afterJson);
        }

        try {
            return objectMapper.readValue(jsonSubstring, Map.class);
        } catch (Exception e) {
            logger.debug("Full AI response that failed to parse: {}", trimmedResponse, e);
            logger.debug("Failed to parse JSON from AI response substring: {}", jsonSubstring, e);
            throw new LibraryException("Failed to parse JSON from response: " + e.getMessage(), e);
        }
    }

    private boolean isAllWhitespace(String str) {
        return str.trim().isEmpty();
    }

    public BookDto generateTempBook(Long id) {
        BookDto dto = getBookById(id);
        if (dto == null) {
            throw new LibraryException("Book not found: " + id);
        }

        dto.setStatus(BookStatus.ACTIVE);
        if (dto.getDateAddedToLibrary() == null) {
            dto.setDateAddedToLibrary(LocalDateTime.now());
        }

        List<Photo> photos = photoRepository.findByBookIdOrderByPhotoOrder(id);
        if (!photos.isEmpty()) {
            // Prepare all photos for AI analysis
            List<Map<String, Object>> photoDataList = new ArrayList<>();
            for (Photo photo : photos) {
                Map<String, Object> photoData = new HashMap<>();
                photoData.put("imageBytes", photo.getImage());
                photoData.put("contentType", photo.getContentType());
                photoDataList.add(photoData);
            }

            String question = """
                Based on these book images, infer the book and author. The images may include the book cover, back cover, spine, table of contents, or other pages.
                Research details from a Catholic perspective. Be frank in your assessments, without providing a balanced view.
                Emphasize Catholic teachings, saints, and doctrine where applicable.
                If the content aligns with Catholic faith, praise it; if not, politely critique it with charity and wisdom.

                Provide details for the author:
                - name: the full name of the author. First name first and last name last.
                - dateOfBirth: birth date in YYYY-MM-DD format, or null if unknown
                - dateOfDeath: death date in YYYY-MM-DD format, or null if alive or unknown
                - religiousAffiliation: the author's religious affiliation; be frank if they were heretics or lapsed
                - birthCountry: the author's country of birth
                - nationality: the author's nationality, or nationalities
                - briefBiography: a frank Catholic biography, highlighting virtues, public sins, conversion. Don't provide a balanced viewpoint. Be frank, but polite and charitable.

                For the book:
                - title: title of the book. If there's ambiguity, explain in plotSummary.
                - publicationYear: publication year, if known. If there's any uncertainty, leave null.
                - publisher: Name of the book's publisher, if known. If there's any ambiguity, leave null.
                - plotSummary: a frank Catholic summary and critique of the plot. Don't provide a balanced viewpoint. Be frank, but polite and charitable.
                - relatedWorks: only include here other works by the same author. Important closely related works can be described in the detailedDescription.
                - detailedDescription: a detailed description from a Catholic point of view. Don't provide a balanced viewpoint. Be frank, but polite and charitable.

                Respond only with a JSON object in this exact format:
                {"author": {"name": "[author name]", "dateOfBirth": "[YYYY-MM-DD or null]", "dateOfDeath": "[YYYY-MM-DD or null]",
                "religiousAffiliation": "[affiliation]", "birthCountry": "[country]", "nationality": "[nationality]",
                "briefBiography": "[biography text]"},

                "book": {"title": "[title]", "publicationYear": [year], "publisher": "[publisher]", "plotSummary": "[summary]",
                "relatedWorks": "[related]", "detailedDescription": "[description]"}}
                Do not include any other text before or after the JSON. Dig deep for helpful information.""";

            int maxRetries = 3;
            RuntimeException lastException = null;
            for (int retry = 0; retry < maxRetries; retry++) {
                try {
                    String response = askGrok.analyzePhotos(photoDataList, question, AskGrok.MODEL_GROK_4);
                    Map<String, Object> jsonData = extractJsonFromResponse(response);

                    Map<String, Object> authorMap = (Map<String, Object>) jsonData.get("author");
                    if (authorMap != null) {
                        String authorName = (String) authorMap.get("name");
                        if (authorName != null && !authorName.trim().isEmpty()) {
                            authorName = authorName.trim();

                            Pageable singlePage = PageRequest.of(0, 1);
                            Page<Author> existingAuthors = authorRepository.findByNameContainingIgnoreCase(authorName, singlePage);

                            Author authorEntity;
                            if (!existingAuthors.isEmpty()) {
                                authorEntity = existingAuthors.getContent().get(0);
                                // Update existing author with new details
                                String dobStr = (String) authorMap.get("dateOfBirth");
                                authorEntity.setDateOfBirth(dobStr != null && !dobStr.isEmpty() ? LocalDate.parse(dobStr) : null);

                                String dodStr = (String) authorMap.get("dateOfDeath");
                                authorEntity.setDateOfDeath(dodStr != null && !dodStr.isEmpty() ? LocalDate.parse(dodStr) : null);

                                String religiousAffiliation = (String) authorMap.get("religiousAffiliation");
                                authorEntity.setReligiousAffiliation(religiousAffiliation != null ? religiousAffiliation.trim() : "AI-generated");

                                String birthCountry = (String) authorMap.get("birthCountry");
                                authorEntity.setBirthCountry(birthCountry != null ? birthCountry.trim() : null);

                                String nationality = (String) authorMap.get("nationality");
                                authorEntity.setNationality(nationality != null ? nationality.trim() : null);

                                String briefBiography = (String) authorMap.get("briefBiography");
                                authorEntity.setBriefBiography(briefBiography != null ? briefBiography.trim() : null);

                                authorEntity = authorRepository.save(authorEntity);
                            } else {
                                authorEntity = new Author();
                                authorEntity.setName(authorName);
                                String dobStr = (String) authorMap.get("dateOfBirth");
                                authorEntity.setDateOfBirth(dobStr != null && !dobStr.isEmpty() ? LocalDate.parse(dobStr) : null);

                                String dodStr = (String) authorMap.get("dateOfDeath");
                                authorEntity.setDateOfDeath(dodStr != null && !dodStr.isEmpty() ? LocalDate.parse(dodStr) : null);

                                String religiousAffiliation = (String) authorMap.get("religiousAffiliation");
                                authorEntity.setReligiousAffiliation(religiousAffiliation != null ? religiousAffiliation.trim() : "AI-generated");

                                String birthCountry = (String) authorMap.get("birthCountry");
                                authorEntity.setBirthCountry(birthCountry != null ? birthCountry.trim() : null);

                                String nationality = (String) authorMap.get("nationality");
                                authorEntity.setNationality(nationality != null ? nationality.trim() : null);

                                String briefBiography = (String) authorMap.get("briefBiography");
                                authorEntity.setBriefBiography(briefBiography != null ? briefBiography.trim() : null);

                                authorEntity = authorRepository.save(authorEntity);
                            }

                            Long authorId = authorEntity.getId();
                            dto.setAuthorId(authorId);

                            // Handle author image
                            String authorImageUrl = (String) authorMap.get("imageUrl");
                            if (authorImageUrl != null && !authorImageUrl.trim().isEmpty()) {
                                try {
                                    ResponseEntity<byte[]> imageResp = restTemplate.getForEntity(authorImageUrl, byte[].class);
                                    if (imageResp.getStatusCode().is2xxSuccessful() && imageResp.getBody() != null && imageResp.getBody().length > 0) {
                                        String ct = imageResp.getHeaders().getContentType() != null ?
                                                imageResp.getHeaders().getContentType().toString() : MediaType.IMAGE_JPEG_VALUE;
                                        Photo authorPhoto = new Photo();
                                        authorPhoto.setAuthor(authorEntity);
                                        authorPhoto.setImage(imageResp.getBody());
                                        authorPhoto.setContentType(ct);

                                        List<Photo> existingAuthorPhotos = photoRepository.findByAuthorId(authorId);
                                        int maxOrder = existingAuthorPhotos.stream().mapToInt(Photo::getPhotoOrder).max().orElse(-1);
                                        authorPhoto.setPhotoOrder(maxOrder + 1);
                                        photoRepository.save(authorPhoto);

                                        logger.debug("Added author image for author ID {}", authorId);
                                    }
                                } catch (Exception e) {
                                    logger.warn("Failed to download and add author image from URL {}: {}", authorImageUrl, e.getMessage(), e);
                                }
                            }
                        }
                    }

                    Map<String, Object> bookMap = (Map<String, Object>) jsonData.get("book");
                    if (bookMap != null) {
                        String title = (String) bookMap.get("title");
                        if (title != null && !title.trim().isEmpty()) {
                            dto.setTitle(title.trim());
                        }

                        Number yearNum = (Number) bookMap.get("publicationYear");
                        if (yearNum != null) {
                            dto.setPublicationYear(yearNum.intValue());
                        }

                        String publisher = (String) bookMap.get("publisher");
                        if (publisher != null && !publisher.trim().isEmpty()) {
                            dto.setPublisher(publisher.trim());
                        }

                        String locNumber = (String) bookMap.get("locNumber");
                        if (locNumber != null && !locNumber.trim().isEmpty()) {
                            dto.setLocNumber(locNumber.trim());
                        }

                        String plotSummary = (String) bookMap.get("plotSummary");
                        if (plotSummary != null && !plotSummary.trim().isEmpty()) {
                            dto.setPlotSummary(plotSummary.trim());
                        }

                        String relatedWorks = (String) bookMap.get("relatedWorks");
                        if (relatedWorks != null && !relatedWorks.trim().isEmpty()) {
                            dto.setRelatedWorks(relatedWorks.trim());
                        }

                        String detailedDescription = (String) bookMap.get("detailedDescription");
                        if (detailedDescription != null && !detailedDescription.trim().isEmpty()) {
                            dto.setDetailedDescription(detailedDescription.trim());
                        }

                        // Handle book cover image
                        String coverImageUrl = (String) bookMap.get("coverImageUrl");
                        if (coverImageUrl != null && !coverImageUrl.trim().isEmpty()) {
                            try {
                                ResponseEntity<byte[]> imageResp = restTemplate.getForEntity(coverImageUrl, byte[].class);
                                if (imageResp.getStatusCode().is2xxSuccessful() && imageResp.getBody() != null && imageResp.getBody().length > 0) {
                                    String ct = imageResp.getHeaders().getContentType() != null ?
                                            imageResp.getHeaders().getContentType().toString() : MediaType.IMAGE_JPEG_VALUE;
                                    Book book = bookRepository.findById(id).orElseThrow(() -> new LibraryException("Book not found: " + id));
                                    Photo coverPhoto = new Photo();
                                    coverPhoto.setBook(book);
                                    coverPhoto.setImage(imageResp.getBody());
                                    coverPhoto.setContentType(ct);

                                    List<Photo> existingPhotos = photoRepository.findByBookIdOrderByPhotoOrder(id);
                                    for (int i = 0; i < existingPhotos.size(); i++) {
                                        existingPhotos.get(i).setPhotoOrder(i + 1);
                                    }
                                    coverPhoto.setPhotoOrder(0);
                                    existingPhotos.add(0, coverPhoto);
                                    photoRepository.saveAll(existingPhotos);

                                    logger.debug("Added book cover image for book ID {}", id);
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to download and add book cover image from URL {}: {}", coverImageUrl, e.getMessage(), e);
                            }
                        }
                    }
                    // If we reach here, success
                    break;
                } catch (RuntimeException e) {
                    lastException = e;
                    if (e.getMessage().contains("unbalanced braces") && retry < maxRetries - 1) {
                        logger.warn("Incomplete AI response detected (unbalanced braces), retrying {}/{} for book ID {} using {} photos", retry + 1, maxRetries, id, photos.size());
                        // Optional: add a small delay before retry
                        try {
                            Thread.sleep(2000); // 2 seconds delay
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new LibraryException("Retry interrupted", ie);
                        }
                        continue;
                    } else {
                        throw e;
                    }
                }
            }
            if (lastException != null) {
                // If all retries failed, rethrow the last exception
                throw lastException;
            }
        } else {
            handleRandomAuthor(dto);
        }

        if (dto.getLibraryId() == null) {
            List<Library> libraries = libraryRepository.findAll();
            if (!libraries.isEmpty()) {
                dto.setLibraryId(libraries.get(0).getId());
            }
        }

        return updateBook(id, dto);
    }

    public BookDto getTitleAuthorFromPhoto(Long id) {
        BookDto dto = getBookById(id);
        if (dto == null) {
            throw new LibraryException("Book not found: " + id);
        }

        List<Photo> photos = photoRepository.findByBookIdOrderByPhotoOrder(id);
        if (photos.isEmpty()) {
            throw new LibraryException("No photos found for book ID: " + id);
        }

        Photo photo = photos.get(0);
        String question = """
            Based on this book cover image, identify only the book title and author name.

            Respond only with a JSON object in this exact format:
            {"title": "[book title]", "authorName": "[author full name, first name first]"}

            Do not include any other text before or after the JSON.""";

        String response = askGrok.analyzePhoto(photo.getImage(), photo.getContentType(), question, AskGrok.MODEL_GROK_4);
        Map<String, Object> jsonData = extractJsonFromResponse(response);

        String title = (String) jsonData.get("title");
        if (title != null && !title.trim().isEmpty()) {
            dto.setTitle(title.trim());
        }

        String authorName = (String) jsonData.get("authorName");
        if (authorName != null && !authorName.trim().isEmpty()) {
            authorName = authorName.trim();

            Pageable singlePage = PageRequest.of(0, 1);
            Page<Author> existingAuthors = authorRepository.findByNameContainingIgnoreCase(authorName, singlePage);

            Author authorEntity;
            if (!existingAuthors.isEmpty()) {
                authorEntity = existingAuthors.getContent().get(0);
            } else {
                authorEntity = new Author();
                authorEntity.setName(authorName);
                authorEntity = authorRepository.save(authorEntity);
            }
            dto.setAuthorId(authorEntity.getId());
        }

        return updateBook(id, dto);
    }

    public BookDto getBookFromTitleAuthor(Long id, String title, String authorName) {
        BookDto dto = getBookById(id);
        if (dto == null) {
            throw new LibraryException("Book not found: " + id);
        }

        if (title == null || title.trim().isEmpty()) {
            throw new LibraryException("Title is required");
        }

        String authorPart = (authorName != null && !authorName.trim().isEmpty())
            ? "by author \"" + authorName.trim() + "\""
            : "(author unknown)";

        String question = String.format("""
            Research the book titled "%s" %s. Provide a card catalog entry from a Catholic perspective.
            Be frank in your assessments, without providing a balanced view. Emphasize Catholic teachings,
            saints, and doctrine where applicable.

            Provide details for the author:
            - name: the full name of the author. First name first and last name last.
            - dateOfBirth: birth date in YYYY-MM-DD format, or null if unknown
            - dateOfDeath: death date in YYYY-MM-DD format, or null if alive or unknown
            - religiousAffiliation: the author's religious affiliation; be frank if they were heretics or lapsed
            - birthCountry: the author's country of birth
            - nationality: the author's nationality, or nationalities
            - briefBiography: a frank Catholic biography, highlighting virtues, public sins, conversion.

            For the book:
            - title: title of the book
            - publicationYear: publication year, if known
            - publisher: Name of the book's publisher, if known
            - locNumber: Library of Congress call number, if known
            - plotSummary: a frank Catholic summary and critique of the plot
            - relatedWorks: other works by the same author
            - detailedDescription: a detailed description from a Catholic point of view

            Respond only with a JSON object in this exact format:
            {"author": {"name": "[author name]", "dateOfBirth": "[YYYY-MM-DD or null]", "dateOfDeath": "[YYYY-MM-DD or null]",
            "religiousAffiliation": "[affiliation]", "birthCountry": "[country]", "nationality": "[nationality]",
            "briefBiography": "[biography text]"},
            "book": {"title": "[title]", "publicationYear": [year or null], "publisher": "[publisher or null]",
            "locNumber": "[LOC or null]", "plotSummary": "[summary]", "relatedWorks": "[related]",
            "detailedDescription": "[description]"}}
            Do not include any other text before or after the JSON.""", title.trim(), authorPart);

        String response = askGrok.askQuestion(question);
        Map<String, Object> jsonData = extractJsonFromResponse(response);

        @SuppressWarnings("unchecked")
        Map<String, Object> authorMap = (Map<String, Object>) jsonData.get("author");
        if (authorMap != null) {
            String responseAuthorName = (String) authorMap.get("name");
            if (responseAuthorName != null && !responseAuthorName.trim().isEmpty()) {
                responseAuthorName = responseAuthorName.trim();

                Pageable singlePage = PageRequest.of(0, 1);
                Page<Author> existingAuthors = authorRepository.findByNameContainingIgnoreCase(responseAuthorName, singlePage);

                Author authorEntity;
                if (!existingAuthors.isEmpty()) {
                    authorEntity = existingAuthors.getContent().get(0);
                    // Update existing author with new details
                    String dobStr = (String) authorMap.get("dateOfBirth");
                    if (dobStr != null && !dobStr.isEmpty() && !dobStr.equals("null")) {
                        try {
                            authorEntity.setDateOfBirth(LocalDate.parse(dobStr));
                        } catch (Exception e) {
                            logger.debug("Could not parse date of birth: {}", dobStr);
                        }
                    }

                    String dodStr = (String) authorMap.get("dateOfDeath");
                    if (dodStr != null && !dodStr.isEmpty() && !dodStr.equals("null")) {
                        try {
                            authorEntity.setDateOfDeath(LocalDate.parse(dodStr));
                        } catch (Exception e) {
                            logger.debug("Could not parse date of death: {}", dodStr);
                        }
                    }

                    String religiousAffiliation = (String) authorMap.get("religiousAffiliation");
                    if (religiousAffiliation != null) {
                        authorEntity.setReligiousAffiliation(religiousAffiliation.trim());
                    }

                    String birthCountry = (String) authorMap.get("birthCountry");
                    if (birthCountry != null) {
                        authorEntity.setBirthCountry(birthCountry.trim());
                    }

                    String nationality = (String) authorMap.get("nationality");
                    if (nationality != null) {
                        authorEntity.setNationality(nationality.trim());
                    }

                    String briefBiography = (String) authorMap.get("briefBiography");
                    if (briefBiography != null) {
                        authorEntity.setBriefBiography(briefBiography.trim());
                    }

                    authorEntity = authorRepository.save(authorEntity);
                } else {
                    authorEntity = new Author();
                    authorEntity.setName(responseAuthorName);
                    String dobStr = (String) authorMap.get("dateOfBirth");
                    if (dobStr != null && !dobStr.isEmpty() && !dobStr.equals("null")) {
                        try {
                            authorEntity.setDateOfBirth(LocalDate.parse(dobStr));
                        } catch (Exception e) {
                            logger.debug("Could not parse date of birth: {}", dobStr);
                        }
                    }

                    String dodStr = (String) authorMap.get("dateOfDeath");
                    if (dodStr != null && !dodStr.isEmpty() && !dodStr.equals("null")) {
                        try {
                            authorEntity.setDateOfDeath(LocalDate.parse(dodStr));
                        } catch (Exception e) {
                            logger.debug("Could not parse date of death: {}", dodStr);
                        }
                    }

                    String religiousAffiliation = (String) authorMap.get("religiousAffiliation");
                    authorEntity.setReligiousAffiliation(religiousAffiliation != null ? religiousAffiliation.trim() : "AI-generated");

                    String birthCountry = (String) authorMap.get("birthCountry");
                    authorEntity.setBirthCountry(birthCountry != null ? birthCountry.trim() : null);

                    String nationality = (String) authorMap.get("nationality");
                    authorEntity.setNationality(nationality != null ? nationality.trim() : null);

                    String briefBiography = (String) authorMap.get("briefBiography");
                    authorEntity.setBriefBiography(briefBiography != null ? briefBiography.trim() : null);

                    authorEntity = authorRepository.save(authorEntity);
                }
                dto.setAuthorId(authorEntity.getId());
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> bookMap = (Map<String, Object>) jsonData.get("book");
        if (bookMap != null) {
            String bookTitle = (String) bookMap.get("title");
            if (bookTitle != null && !bookTitle.trim().isEmpty()) {
                dto.setTitle(bookTitle.trim());
            }

            Object yearObj = bookMap.get("publicationYear");
            if (yearObj != null) {
                if (yearObj instanceof Number) {
                    dto.setPublicationYear(((Number) yearObj).intValue());
                }
            }

            String publisher = (String) bookMap.get("publisher");
            if (publisher != null && !publisher.trim().isEmpty() && !publisher.equals("null")) {
                dto.setPublisher(publisher.trim());
            }

            String locNumber = (String) bookMap.get("locNumber");
            if (locNumber != null && !locNumber.trim().isEmpty() && !locNumber.equals("null")) {
                dto.setLocNumber(locNumber.trim());
            }

            String plotSummary = (String) bookMap.get("plotSummary");
            if (plotSummary != null && !plotSummary.trim().isEmpty()) {
                dto.setPlotSummary(plotSummary.trim());
            }

            String relatedWorks = (String) bookMap.get("relatedWorks");
            if (relatedWorks != null && !relatedWorks.trim().isEmpty()) {
                dto.setRelatedWorks(relatedWorks.trim());
            }

            String detailedDescription = (String) bookMap.get("detailedDescription");
            if (detailedDescription != null && !detailedDescription.trim().isEmpty()) {
                dto.setDetailedDescription(detailedDescription.trim());
            }
        }

        return updateBook(id, dto);
    }

    public List<BookSummaryDto> getAllBookSummaries() {
        return bookRepository.findAll().stream()
                .map(bookMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get summaries (id + lastModified) for books without LOC number.
     * Used for cache validation in frontend.
     */
    public List<BookSummaryDto> getSummariesWithoutLocNumber() {
        return bookRepository.findSummariesWithoutLocNumber().stream()
                .map(this::projectionToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get summaries (id + lastModified) for books from most recent 2 days OR with temporary titles.
     * Used for cache validation in frontend.
     */
    public List<BookSummaryDto> getSummariesFromMostRecentDay() {
        return bookRepository.findSummariesFromMostRecentDay().stream()
                .map(this::projectionToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get summaries (id + lastModified) for books with 3-letter LOC start.
     * Used for cache validation in frontend.
     */
    public List<BookSummaryDto> getSummariesWith3LetterLocStart() {
        return bookRepository.findSummariesWith3LetterLocStart().stream()
                .map(this::projectionToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get summaries (id + lastModified) for books without Grokipedia URL.
     * Used for cache validation in frontend.
     */
    public List<BookSummaryDto> getSummariesWithoutGrokipediaUrl() {
        return bookRepository.findSummariesWithoutGrokipediaUrl().stream()
                .map(this::projectionToSummaryDto)
                .collect(Collectors.toList());
    }

    private BookSummaryDto projectionToSummaryDto(BookRepository.BookSummaryProjection projection) {
        BookSummaryDto dto = new BookSummaryDto();
        dto.setId(projection.getId());
        dto.setLastModified(projection.getLastModified());
        return dto;
    }

    public List<BookDto> getBooksByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return bookRepository.findAllById(ids).stream()
                .map(bookMapper::toDto)
                .sorted(Comparator.comparing(BookDto::getDateAddedToLibrary,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Lookup genres for a single book using Grok AI.
     * Skips books with blank descriptions.
     *
     * @param bookId The book ID to look up genres for
     * @return GenreLookupResultDto with suggested genres
     */
    public GenreLookupResultDto lookupGenresForBook(Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return GenreLookupResultDto.builder()
                    .bookId(bookId)
                    .success(false)
                    .errorMessage("Book not found")
                    .build();
        }

        // Skip books with blank descriptions
        String description = book.getDetailedDescription();
        if (description == null || description.isBlank()) {
            return GenreLookupResultDto.builder()
                    .bookId(bookId)
                    .title(book.getTitle())
                    .success(false)
                    .errorMessage("Book has no description - cannot suggest genres")
                    .build();
        }

        try {
            // Serialize book to JSON
            BookDto bookDto = bookMapper.toDto(book);
            String bookJson = objectMapper.writeValueAsString(bookDto);

            // Serialize author to JSON if present
            String authorJson = null;
            if (book.getAuthor() != null) {
                Map<String, Object> authorMap = new HashMap<>();
                authorMap.put("name", book.getAuthor().getName());
                authorMap.put("religiousAffiliation", book.getAuthor().getReligiousAffiliation());
                authorMap.put("birthCountry", book.getAuthor().getBirthCountry());
                authorMap.put("nationality", book.getAuthor().getNationality());
                authorMap.put("briefBiography", book.getAuthor().getBriefBiography());
                authorJson = objectMapper.writeValueAsString(authorMap);
            }

            // Call Grok AI for genre suggestions
            String response = askGrok.suggestGenres(bookJson, authorJson);

            // Parse the comma-separated response into a list
            List<String> genres = parseGenreResponse(response);

            // Merge suggested genres into book's existing tags and save
            if (!genres.isEmpty()) {
                List<String> existingTags = book.getTagsList() != null ? book.getTagsList() : new ArrayList<>();
                java.util.Set<String> mergedTags = new java.util.LinkedHashSet<>(existingTags);
                mergedTags.addAll(genres);
                book.setTagsList(new ArrayList<>(mergedTags));
                bookRepository.save(book);
                logger.info("Saved {} genre tags to book '{}' (ID: {})", genres.size(), book.getTitle(), bookId);
            }

            return GenreLookupResultDto.builder()
                    .bookId(bookId)
                    .title(book.getTitle())
                    .success(true)
                    .suggestedGenres(genres)
                    .build();

        } catch (Exception e) {
            logger.error("Failed to lookup genres for book ID {}: {}", bookId, e.getMessage(), e);
            return GenreLookupResultDto.builder()
                    .bookId(bookId)
                    .title(book.getTitle())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Lookup genres for multiple books using Grok AI.
     * Skips books with blank descriptions.
     *
     * @param bookIds List of book IDs to look up genres for
     * @return List of GenreLookupResultDto with suggested genres
     */
    public List<GenreLookupResultDto> lookupGenresForBooks(List<Long> bookIds) {
        List<GenreLookupResultDto> results = new ArrayList<>();
        for (Long bookId : bookIds) {
            results.add(lookupGenresForBook(bookId));
        }
        return results;
    }

    /**
     * Parse the comma-separated genre response from Grok AI.
     * Normalizes tags to lowercase with only letters, numbers, and dashes.
     */
    private List<String> parseGenreResponse(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(response.split(","))
                .map(String::trim)
                .map(tag -> tag.toLowerCase().replaceAll("[^a-z0-9-]", ""))
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

}
