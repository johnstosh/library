/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.LibraryRepository;
import com.muczynski.library.repository.PhotoRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class BooksFromFeedService {

    private static final Logger logger = LoggerFactory.getLogger(BooksFromFeedService.class);

    @Autowired
    private GooglePhotosService googlePhotosService;

    @Autowired
    private AskGrok askGrok;

    @Autowired
    private UserSettingsService userSettingsService;

    @Autowired
    private BookService bookService;

    @Autowired
    private AuthorService authorService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private LibraryService libraryService;

    /**
     * Compute SHA-256 checksum of image bytes
     */
    private String computeChecksum(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imageBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            return null;
        }
    }


    /**
     * Get saved books: books from most recent datetime plus books with "FromFeed_" prefix,
     * sorted by datetime (most recent first)
     * @return List of books with basic info (id, title, firstPhotoId)
     */
    public List<Map<String, Object>> getSavedBooks() {
        logger.info("Getting saved books: most recent datetime plus FromFeed prefix");

        List<BookDto> allBooks = bookService.getAllBooks();
        List<Map<String, Object>> savedBooks = new ArrayList<>();

        // Find the most recent datetime
        LocalDateTime mostRecentDateTime = null;
        for (BookDto book : allBooks) {
            if (book.getDateAddedToLibrary() != null) {
                if (mostRecentDateTime == null || book.getDateAddedToLibrary().isAfter(mostRecentDateTime)) {
                    mostRecentDateTime = book.getDateAddedToLibrary();
                }
            }
        }

        // Get the date part of most recent datetime for comparison
        java.time.LocalDate mostRecentDate = mostRecentDateTime != null ? mostRecentDateTime.toLocalDate() : null;

        // Filter books: FromFeed prefix OR from most recent date
        for (BookDto book : allBooks) {
            boolean isFromFeed = book.getTitle() != null && book.getTitle().startsWith("FromFeed_");
            boolean isFromMostRecentDate = mostRecentDate != null
                    && book.getDateAddedToLibrary() != null
                    && book.getDateAddedToLibrary().toLocalDate().equals(mostRecentDate);

            if (isFromFeed || isFromMostRecentDate) {
                Map<String, Object> bookInfo = new HashMap<>();
                bookInfo.put("id", book.getId());
                bookInfo.put("title", book.getTitle());
                bookInfo.put("author", book.getAuthor()); // Author name for display
                bookInfo.put("firstPhotoId", book.getFirstPhotoId());
                bookInfo.put("firstPhotoChecksum", book.getFirstPhotoChecksum()); // For thumbnail caching
                bookInfo.put("locNumber", book.getLocNumber()); // LOC number for display
                bookInfo.put("status", isFromFeed ? "pending" : "processed");
                bookInfo.put("dateAdded", book.getDateAddedToLibrary() != null
                        ? book.getDateAddedToLibrary().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : null);
                savedBooks.add(bookInfo);
            }
        }

        logger.info("Found {} books (most recent date + FromFeed prefix)", savedBooks.size());
        return savedBooks;
    }

    /**
     * Process a single saved book using AI book-by-photo workflow
     * @param bookId The ID of the book to process
     * @return Map containing result of the processing
     */
    public Map<String, Object> processSingleBook(Long bookId) {
        logger.info("Processing single book: {}", bookId);

        try {
            // Get the book to check if it's a temp book
            BookDto tempBook = bookService.getBookById(bookId);
            if (tempBook == null) {
                throw new LibraryException("Book not found: " + bookId);
            }

            String tempTitle = tempBook.getTitle();
            if (!tempTitle.startsWith("FromFeed_")) {
                throw new LibraryException("Book is not a temporary FromFeed book");
            }

            // Use books-from-photo workflow: generateTempBook does comprehensive AI extraction
            logger.info("Generating full book details using AI for book {}", bookId);
            BookDto fullBook = bookService.generateTempBook(bookId);

            // Get author name from Author entity
            Author bookAuthor = null;
            if (fullBook.getAuthorId() != null) {
                bookAuthor = authorRepository.findById(fullBook.getAuthorId()).orElse(null);
            }
            String authorName = bookAuthor != null ? bookAuthor.getName() : "Unknown";

            logger.info("Successfully generated book: title='{}', author='{}'", fullBook.getTitle(), authorName);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("bookId", bookId);
            result.put("title", fullBook.getTitle());
            result.put("author", authorName);
            result.put("originalTitle", tempTitle);

            return result;

        } catch (Exception e) {
            logger.error("Error processing book {}: {}", bookId, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("bookId", bookId);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Phase 2: Process saved photos using the book-by-photo AI workflow
     * This can run independently after photos are saved to the database
     * @return Map containing results of the processing
     */
    public Map<String, Object> processSavedPhotos() {
        logger.info("===== Phase 2: Processing saved photos with AI =====");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to process photos without authentication");
            throw new LibraryException("No authenticated user found");
        }
        String username = authentication.getName();
        logger.info("Processing photos for user: {}", username);

        // Find all temporary books from feed (those starting with "FromFeed_")
        List<BookDto> allBooks = bookService.getAllBooks();
        List<BookDto> tempBooks = allBooks.stream()
                .filter(book -> book.getTitle() != null && book.getTitle().startsWith("FromFeed_"))
                .toList();

        logger.info("Found {} temporary books to process", tempBooks.size());

        List<Map<String, Object>> processedBooks = new ArrayList<>();
        List<Map<String, Object>> failedBooks = new ArrayList<>();

        int bookIndex = 0;
        for (BookDto tempBook : tempBooks) {
            bookIndex++;
            Long bookId = tempBook.getId();
            String tempTitle = tempBook.getTitle();
            logger.info("Processing book {}/{}: ID={}, Title={}", bookIndex, tempBooks.size(), bookId, tempTitle);

            try {
                // Use books-from-photo workflow: generateTempBook does comprehensive AI extraction
                logger.info("Generating full book details using AI for book {}", bookId);
                BookDto fullBook = bookService.generateTempBook(bookId);

                // Get author name from Author entity
                Author bookAuthor = null;
                if (fullBook.getAuthorId() != null) {
                    bookAuthor = authorRepository.findById(fullBook.getAuthorId()).orElse(null);
                }
                String authorName = bookAuthor != null ? bookAuthor.getName() : "Unknown";

                logger.info("Successfully generated book: title='{}', author='{}'",
                        fullBook.getTitle(), authorName);

                processedBooks.add(Map.of(
                        "bookId", bookId,
                        "title", fullBook.getTitle(),
                        "author", authorName,
                        "originalTitle", tempTitle
                ));

                logger.info("Successfully processed book {}", bookId);

            } catch (Exception e) {
                logger.error("Error processing book {}: {}", bookId, e.getMessage(), e);
                failedBooks.add(Map.of(
                        "bookId", bookId,
                        "originalTitle", tempTitle,
                        "reason", "Error: " + e.getMessage()
                ));
            }
        }

        logger.info("Completed Phase 2. Processed: {}, Failed: {}",
                processedBooks.size(), failedBooks.size());

        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", processedBooks.size());
        result.put("failedCount", failedBooks.size());
        result.put("totalBooks", tempBooks.size());
        result.put("processedBooks", processedBooks);
        result.put("failedBooks", failedBooks);

        logger.info("===== Phase 2 Complete: {} books processed with AI =====", processedBooks.size());
        return result;
    }


    /**
     * Save photos selected via Google Photos Picker API to database (Phase 1 - No AI)
     * @param photos List of photo metadata from the Picker
     * @return Map containing results of the save operation
     */
    public Map<String, Object> savePhotosFromPicker(List<Map<String, Object>> photos) {
        logger.info("===== Phase 1: Saving photos from Picker to database =====");
        logger.info("Saving {} photos selected from Picker", photos.size());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to save photos without authentication");
            throw new LibraryException("No authenticated user found");
        }
        String username = authentication.getName();
        logger.info("Saving photos for user: {}", username);

        // Get valid access token for downloading photos from Picker baseUrl
        String accessToken = googlePhotosService.getValidAccessToken(username);

        List<Map<String, Object>> savedPhotos = new ArrayList<>();
        List<Map<String, Object>> skippedPhotos = new ArrayList<>();

        // Get default library and placeholder author once
        Library library = libraryService.getOrCreateDefaultLibrary();
        Long libraryId = library.getId();
        Author placeholderAuthor = authorService.findOrCreateAuthor("John Doe");

        int photoIndex = 0;
        for (Map<String, Object> photo : photos) {
            photoIndex++;
            String photoId = (String) photo.get("id");
            String photoName = (String) photo.get("name");
            String photoUrl = (String) photo.get("url");

            logger.info("Saving photo {}/{}: name={}, id={}", photoIndex, photos.size(), photoName, photoId);

            try {
                // Check if photo already has description with book metadata
                String description = (String) photo.get("description");
                if (description != null && (description.contains("Title:") || description.contains("Author:"))) {
                    logger.info("Skipping photo {} - already processed (has book metadata in description)", photoName);
                    skippedPhotos.add(Map.of(
                            "id", photoId,
                            "name", photoName,
                            "reason", "Already processed (has book metadata in description)"
                    ));
                    continue;
                }

                // Download the photo from the URL provided by Picker
                String mimeType = (String) photo.get("mimeType");
                if (mimeType == null || mimeType.trim().isEmpty()) {
                    mimeType = "image/jpeg";
                }
                logger.debug("Downloading photo {} from URL: {} (mimeType: {})", photoName, photoUrl, mimeType);
                byte[] photoBytes = downloadPhotoFromUrl(photoUrl, accessToken);
                logger.info("Downloaded photo {} ({} bytes, mimeType: {})", photoName, photoBytes.length, mimeType);

                // Check if this photo already exists in the database by checksum
                String checksum = computeChecksum(photoBytes);
                if (checksum != null) {
                    java.util.Optional<Photo> existingPhoto = photoRepository.findByImageChecksum(checksum);
                    if (existingPhoto.isPresent()) {
                        Photo existing = existingPhoto.get();
                        Long existingBookId = existing.getBook() != null ? existing.getBook().getId() : null;
                        String existingTitle = existing.getBook() != null ? existing.getBook().getTitle() : "Unknown";
                        String existingAuthor = existing.getBook() != null && existing.getBook().getAuthor() != null
                                ? existing.getBook().getAuthor().getName() : "Unknown";

                        logger.info("Photo {} already exists in database (book ID: {}, title: '{}')",
                                photoName, existingBookId, existingTitle);

                        savedPhotos.add(Map.of(
                                "photoId", photoId,
                                "photoName", photoName,
                                "bookId", existingBookId != null ? existingBookId : -1,
                                "title", existingTitle,
                                "author", existingAuthor,
                                "existingPhoto", true
                        ));
                        continue;
                    }
                }

                // Create temporary book with special marker for processing later
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
                String tempTitle = "FromFeed_" + timestamp;

                BookDto tempBook = new BookDto();
                tempBook.setTitle(tempTitle);
                tempBook.setAuthorId(placeholderAuthor.getId());
                tempBook.setLibraryId(libraryId);
                tempBook.setStatus(BookStatus.ACTIVE);
                tempBook.setDateAddedToLibrary(LocalDateTime.now());

                BookDto savedBook = bookService.createBook(tempBook);
                Long bookId = savedBook.getId();
                logger.info("Created temporary book with ID: {} (title: {})", bookId, tempTitle);

                // Save photo to database with actual MIME type
                photoService.addPhotoFromBytes(bookId, photoBytes, mimeType);
                logger.info("Saved photo to database for book {}", bookId);

                savedPhotos.add(Map.of(
                        "photoId", photoId,
                        "photoName", photoName,
                        "bookId", bookId,
                        "title", tempTitle
                ));

            } catch (Exception e) {
                logger.error("Error saving photo {}: {}", photoName, e.getMessage(), e);
                skippedPhotos.add(Map.of(
                        "id", photoId,
                        "name", photoName,
                        "reason", "Error: " + e.getMessage()
                ));
            }
        }

        logger.info("Completed Phase 1. Saved: {}, Skipped: {}", savedPhotos.size(), skippedPhotos.size());

        Map<String, Object> result = new HashMap<>();
        result.put("savedCount", savedPhotos.size());
        result.put("skippedCount", skippedPhotos.size());
        result.put("totalPhotos", photos.size());
        result.put("savedPhotos", savedPhotos);
        result.put("skippedPhotos", skippedPhotos);

        logger.info("===== Phase 1 Complete: {} photos saved to database =====", savedPhotos.size());
        return result;
    }


    /**
     * Download photo from a URL (from Google Photos Picker)
     * Google Photos Picker API requires:
     * 1. Append =d parameter to download the image with metadata
     * 2. Include OAuth bearer token in Authorization header
     * See: https://developers.google.com/photos/picker/guides/media-items
     */
    private byte[] downloadPhotoFromUrl(String url, String accessToken) {
        // Append =d parameter to download the image with metadata
        // (required by Google Photos API to actually download the image file)
        if (!url.contains("=")) {
            url = url + "=d";
        }

        logger.debug("Downloading photo from URL: {}", url);

        try {
            java.net.URL photoUrl = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) photoUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            // Picker API requires OAuth bearer token for downloading baseUrl
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = connection.getResponseCode();
            logger.debug("HTTP response code: {}", responseCode);

            if (responseCode == 200) {
                java.io.InputStream inputStream = connection.getInputStream();
                byte[] photoBytes = inputStream.readAllBytes();
                inputStream.close();
                logger.debug("Successfully downloaded {} bytes", photoBytes.length);
                return photoBytes;
            } else {
                logger.error("Failed to download photo. HTTP response code: {}", responseCode);
                throw new LibraryException("Failed to download photo: HTTP " + responseCode);
            }
        } catch (Exception e) {
            logger.error("Error downloading photo from URL: {}", e.getMessage(), e);
            throw new LibraryException("Failed to download photo: " + e.getMessage(), e);
        }
    }
}
