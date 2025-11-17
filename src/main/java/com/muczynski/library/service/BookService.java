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
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LibraryRepository;
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
import java.util.Comparator;
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
    private LibraryRepository libraryRepository;

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
        book.setAuthor(authorRepository.findById(bookDto.getAuthorId()).orElseThrow(() -> new LibraryException("Author not found: " + bookDto.getAuthorId())));
        book.setLibrary(libraryRepository.findById(bookDto.getLibraryId()).orElseThrow(() -> new LibraryException("Library not found: " + bookDto.getLibraryId())));
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

    public BookDto getBookById(Long id) {
        return bookRepository.findById(id)
                .map(bookMapper::toDto)
                .orElse(null);
    }

    public BookDto updateBook(Long id, BookDto bookDto) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new LibraryException("Book not found: " + id));
        book.setTitle(bookDto.getTitle());
        book.setPublicationYear(bookDto.getPublicationYear());
        book.setPublisher(bookDto.getPublisher());
        book.setPlotSummary(bookDto.getPlotSummary());
        book.setRelatedWorks(bookDto.getRelatedWorks());
        book.setDetailedDescription(bookDto.getDetailedDescription());
        book.setDateAddedToLibrary(bookDto.getDateAddedToLibrary());
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
            dto.setDateAddedToLibrary(LocalDate.now());
        }

        List<Photo> photos = photoRepository.findByBookIdOrderByPhotoOrder(id);
        if (!photos.isEmpty()) {
            Photo photo = photos.get(0);
            String question = """
                Based on this book cover image, infer the book and author. Research details from a strictly Catholic perspective. Be frank in your assessments, 
                without providing a balanced view. Emphasize Catholic teachings, saints, and doctrine where applicable. If the content aligns with 
                Catholic faith, praise it; if not, critique it with wisdom.

                Provide details for the author:
                - name: the full name of the author. First name first and last name last.
                - dateOfBirth: birth date in YYYY-MM-DD format, or null if unknown
                - dateOfDeath: death date in YYYY-MM-DD format, or null if alive or unknown
                - religiousAffiliation: the author's religious affiliation; be frank if they were heretics or lapsed
                - birthCountry: the author's country of birth
                - nationality: the author's nationality, or nationalities
                - briefBiography: a frank Catholic biography, highlighting virtues, public sins, conversion. If a grokipedia.com article is available, 
                  then summarize the summary of that article and end with a URL to the article for more details.

                For the book:
                - title: title of the book. If there's ambiguity, explain in plotSummary.
                - publicationYear: publication year, if known. If there's any uncertainty, leave null.
                - publisher: Name of the book's publisher, if known. If there's any ambiguity, leave  null.
                - locNumber: Library of Congress LC call number, if known. If unknown or uncertain, leave null.
                - plotSummary: a frank Catholic summary and critique of the plot. Don't provide a balanced viewpoint. Be frank, but polite. If a grokipedia.com article is available, 
                  then summarize the summary of that article and end with a URL to the article for more details.
                - relatedWorks: only include here other works by the same author. Important closely related works can be described in the detailedDescription.
                - detailedDescription: a detailed description from a Catholic point of view. Don't provide a balanced viewpoint. Be frank, but polite.

                URL for a book cover image - leave null.
                URL for an author portrait image - leave null.

                Respond only with a JSON object in this exact format:
                {"author": {"name": "[author name]", "dateOfBirth": "[YYYY-MM-DD or null]", "dateOfDeath": "[YYYY-MM-DD or null]", 
                "religiousAffiliation": "[affiliation]", "birthCountry": "[country]", "nationality": "[nationality]", 
                "briefBiography": "[biography text]", "imageUrl": "[url]", "preferredContentType": "image/jpeg"}, 
                "book": {"title": "[title]", "publicationYear": [year], "publisher": "[publisher]", "locNumber": "[LOC or null]", "plotSummary": "[summary]", 
                "relatedWorks": "[related]", "detailedDescription": "[description]", "coverImageUrl": "[url]", 
                "preferredContentType": "image/jpeg"}}
                Do not include any other text before or after the JSON. Dig deep for helpful information.""";

            int maxRetries = 3;
            RuntimeException lastException = null;
            for (int retry = 0; retry < maxRetries; retry++) {
                try {
                    String response = askGrok.askAboutPhoto(photo.getImage(), photo.getContentType(), question);
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
                        logger.warn("Incomplete AI response detected (unbalanced braces), retrying {}/{} for book ID {} using photo ID {}", retry + 1, maxRetries, id, photo.getId());
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

}
