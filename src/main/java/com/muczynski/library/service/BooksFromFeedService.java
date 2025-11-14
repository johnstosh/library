/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
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
    private com.muczynski.library.repository.LibraryRepository libraryRepository;

    @Autowired
    private BookPhotoProcessingService bookPhotoProcessingService;

    /**
     * Process photos from Google Photos feed to create books
     * @return Map containing results of the processing
     */
    public Map<String, Object> processPhotosFromFeed() {
        logger.info("===== Starting Books-from-Feed processing =====");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to process photos without authentication");
            throw new RuntimeException("No authenticated user found");
        }
        String username = authentication.getName();
        logger.info("Processing photos for user: {}", username);

        UserDto userDto = userSettingsService.getUserSettings(username);

        // Get the last photo timestamp or default to yesterday
        String lastTimestamp = userDto.getLastPhotoTimestamp();
        if (lastTimestamp == null || lastTimestamp.trim().isEmpty()) {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            lastTimestamp = yesterday.format(DateTimeFormatter.ISO_DATE_TIME);
            logger.info("No last photo timestamp found. Using yesterday as default: {}", lastTimestamp);
        } else {
            logger.info("Using last photo timestamp: {}", lastTimestamp);
        }

        // Fetch photos from Google Photos
        logger.info("Fetching photos from Google Photos since: {}", lastTimestamp);
        List<Map<String, Object>> photos = googlePhotosService.fetchPhotos(lastTimestamp);
        logger.info("Fetched {} photos to process", photos.size());

        List<Map<String, Object>> processedBooks = new ArrayList<>();
        List<Map<String, Object>> skippedPhotos = new ArrayList<>();
        String latestTimestamp = lastTimestamp;

        int photoIndex = 0;
        for (Map<String, Object> photo : photos) {
            photoIndex++;
            String photoId = (String) photo.get("id");
            logger.info("Processing photo {}/{}: ID={}", photoIndex, photos.size(), photoId);

            try {
                // Check if photo already has book metadata in description
                String description = (String) photo.get("description");
                if (description != null && (description.contains("Title:") || description.contains("Author:"))) {
                    logger.info("Skipping photo {} - already processed (has book metadata in description)", photoId);
                    skippedPhotos.add(Map.of(
                            "id", photoId,
                            "reason", "Already processed (has book metadata in description)"
                    ));
                    continue;
                }

                // Get photo creation time
                Map<String, Object> mediaMetadata = (Map<String, Object>) photo.get("mediaMetadata");
                String creationTime = (String) mediaMetadata.get("creationTime");
                logger.debug("Photo {} creation time: {}", photoId, creationTime);
                if (creationTime != null && creationTime.compareTo(latestTimestamp) > 0) {
                    latestTimestamp = creationTime;
                }

                // Download the photo
                String baseUrl = (String) photo.get("baseUrl");
                logger.debug("Downloading photo {} from: {}", photoId, baseUrl);
                byte[] photoBytes = googlePhotosService.downloadPhoto(baseUrl);
                logger.info("Downloaded photo {} ({} bytes)", photoId, photoBytes.length);

                // Determine if this is a book photo using AI
                logger.info("Asking AI if photo {} is a book photo...", photoId);
                if (!bookPhotoProcessingService.isBookPhoto(photoBytes, "image/jpeg")) {
                    logger.info("Skipping photo {} - not a book photo", photoId);
                    skippedPhotos.add(Map.of(
                            "id", photoId,
                            "reason", "Not a book photo"
                    ));
                    continue;
                }

                // Create temporary book with timestamp-based name
                // This matches the books-from-photo workflow
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
                String tempTitle = "Book_" + timestamp;

                // Find or create a placeholder author (will be updated by AI extraction)
                com.muczynski.library.domain.Author placeholderAuthor = authorService.findOrCreateAuthor("John Doe");
                logger.debug("Using placeholder author: {} (ID: {})", placeholderAuthor.getName(), placeholderAuthor.getId());

                // Get default library (first library in database)
                java.util.List<com.muczynski.library.domain.Library> libraries = libraryRepository.findAll();
                if (libraries.isEmpty()) {
                    throw new RuntimeException("No library found in database. Please create a library first.");
                }
                Long libraryId = libraries.get(0).getId();
                logger.debug("Using library ID: {}", libraryId);

                // Create temporary book entry
                logger.info("Creating temporary book entry for photo {}", photoId);
                com.muczynski.library.dto.BookDto tempBook = new com.muczynski.library.dto.BookDto();
                tempBook.setTitle(tempTitle);
                tempBook.setAuthorId(placeholderAuthor.getId());
                tempBook.setLibraryId(libraryId);
                tempBook.setStatus(com.muczynski.library.domain.BookStatus.ACTIVE);
                tempBook.setDateAddedToLibrary(java.time.LocalDate.now());

                // Create the book
                com.muczynski.library.dto.BookDto savedBook = bookService.createBook(tempBook);
                Long bookId = savedBook.getId();
                logger.info("Created temporary book with ID: {} and title: '{}'", bookId, tempTitle);

                // Attach photo to book (need to save to database)
                // TODO: Save photo to book's photo collection
                // For now, we'll just use generateTempBook with the downloaded bytes

                // Use generateTempBook to extract all metadata with AI
                // This is the shared workflow that provides comprehensive Catholic analysis
                logger.info("Generating full book details from photo using AI for book ID: {}", bookId);
                try {
                    // Note: generateTempBook expects photo to be in database
                    // We need to save photoBytes first - for now skipping this step
                    // TODO: Add photo save step before calling generateTempBook

                    // com.muczynski.library.dto.BookDto fullBook = bookService.generateTempBook(bookId);
                    // logger.info("Successfully generated book details: title='{}', author='{}'",
                    //         fullBook.getTitle(), fullBook.getAuthorName());

                    // For now, just extract basic metadata since we need photo DB integration
                    Map<String, Object> bookMetadata = bookPhotoProcessingService.extractBasicMetadata(photoBytes, "image/jpeg");
                    if (bookMetadata != null && bookMetadata.containsKey("title")) {
                        String extractedTitle = (String) bookMetadata.get("title");
                        String extractedAuthor = (String) bookMetadata.get("author");

                        // Update book with extracted title
                        savedBook.setTitle(extractedTitle);
                        com.muczynski.library.domain.Author author = authorService.findOrCreateAuthor(extractedAuthor);
                        savedBook.setAuthorId(author.getId());
                        savedBook = bookService.updateBook(bookId, savedBook);

                        logger.info("Updated book {} with title='{}', author='{}'", bookId, extractedTitle, extractedAuthor);

                        processedBooks.add(Map.of(
                                "photoId", photoId,
                                "title", extractedTitle,
                                "author", extractedAuthor,
                                "bookId", bookId
                        ));
                    } else {
                        logger.warn("Failed to extract metadata for photo {}, keeping temporary book", photoId);
                        processedBooks.add(Map.of(
                                "photoId", photoId,
                                "title", tempTitle,
                                "author", "Unknown",
                                "bookId", bookId
                        ));
                    }
                } catch (Exception e) {
                    logger.error("Error generating book details for book {}: {}", bookId, e.getMessage());
                    // Keep the temporary book even if generation fails
                    processedBooks.add(Map.of(
                            "photoId", photoId,
                            "title", tempTitle,
                            "author", "Unknown",
                            "bookId", bookId
                    ));
                }

                logger.info("Successfully processed photo {} as book (ID: {})", photoId, bookId);

            } catch (Exception e) {
                logger.error("Error processing photo {}: {}", photoId, e.getMessage(), e);
                skippedPhotos.add(Map.of(
                        "id", photoId,
                        "reason", "Error: " + e.getMessage()
                ));
            }
        }

        logger.info("Completed processing {} photos. Processed: {}, Skipped: {}",
                photos.size(), processedBooks.size(), skippedPhotos.size());

        // Update the last photo timestamp in user settings
        logger.info("Updating last photo timestamp to: {}", latestTimestamp);
        UserSettingsDto settingsUpdate = new UserSettingsDto();
        settingsUpdate.setLastPhotoTimestamp(latestTimestamp);
        userSettingsService.updateUserSettings(username, settingsUpdate);
        logger.info("Successfully updated last photo timestamp for user: {}", username);

        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", processedBooks.size());
        result.put("skippedCount", skippedPhotos.size());
        result.put("totalPhotos", photos.size());
        result.put("processedBooks", processedBooks);
        result.put("skippedPhotos", skippedPhotos);
        result.put("lastTimestamp", latestTimestamp);

        logger.info("===== Books-from-Feed processing complete =====");
        logger.info("Summary: {} total photos, {} books created, {} photos skipped",
                photos.size(), processedBooks.size(), skippedPhotos.size());

        return result;
    }

    /**
     * Process photos selected via Google Photos Picker API
     * @param photos List of photo metadata from the Picker
     * @return Map containing results of the processing
     */
    public Map<String, Object> processPhotosFromPicker(List<Map<String, Object>> photos) {
        logger.info("===== Starting Books-from-Feed processing (Picker) =====");
        logger.info("Processing {} photos selected from Picker", photos.size());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to process photos without authentication");
            throw new RuntimeException("No authenticated user found");
        }
        String username = authentication.getName();
        logger.info("Processing photos for user: {}", username);

        // Get valid access token for downloading photos from Picker baseUrl
        // Picker API requires OAuth bearer token in Authorization header when downloading
        String accessToken = googlePhotosService.getValidAccessToken(username);

        List<Map<String, Object>> processedBooks = new ArrayList<>();
        List<Map<String, Object>> skippedPhotos = new ArrayList<>();

        int photoIndex = 0;
        for (Map<String, Object> photo : photos) {
            photoIndex++;
            String photoId = (String) photo.get("id");
            String photoName = (String) photo.get("name");
            String photoUrl = (String) photo.get("url");

            logger.info("Processing photo {}/{}: name={}, id={}", photoIndex, photos.size(), photoName, photoId);

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
                // Picker API requires OAuth authentication for baseUrl downloads
                logger.debug("Downloading photo {} from URL: {}", photoName, photoUrl);
                byte[] photoBytes = downloadPhotoFromUrl(photoUrl, accessToken);
                logger.info("Downloaded photo {} ({} bytes)", photoName, photoBytes.length);

                // Determine if this is a book photo using AI (shared service)
                logger.info("Asking AI if photo {} is a book photo...", photoName);
                if (!bookPhotoProcessingService.isBookPhoto(photoBytes, "image/jpeg")) {
                    logger.info("Skipping photo {} - not a book photo", photoName);
                    skippedPhotos.add(Map.of(
                            "id", photoId,
                            "name", photoName,
                            "reason", "Not a book photo"
                    ));
                    continue;
                }

                // Create temporary book with timestamp-based name (shared workflow)
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
                String tempTitle = "Book_" + timestamp;

                // Find or create a placeholder author (will be updated by AI extraction)
                com.muczynski.library.domain.Author placeholderAuthor = authorService.findOrCreateAuthor("John Doe");
                logger.debug("Using placeholder author: {} (ID: {})", placeholderAuthor.getName(), placeholderAuthor.getId());

                // Get default library (first library in database)
                java.util.List<com.muczynski.library.domain.Library> libraries = libraryRepository.findAll();
                if (libraries.isEmpty()) {
                    throw new RuntimeException("No library found in database. Please create a library first.");
                }
                Long libraryId = libraries.get(0).getId();
                logger.debug("Using library ID: {}", libraryId);

                // Create temporary book entry
                logger.info("Creating temporary book entry for photo {}", photoName);
                com.muczynski.library.dto.BookDto tempBook = new com.muczynski.library.dto.BookDto();
                tempBook.setTitle(tempTitle);
                tempBook.setAuthorId(placeholderAuthor.getId());
                tempBook.setLibraryId(libraryId);
                tempBook.setStatus(com.muczynski.library.domain.BookStatus.ACTIVE);
                tempBook.setDateAddedToLibrary(java.time.LocalDate.now());

                // Create the book
                com.muczynski.library.dto.BookDto savedBook = bookService.createBook(tempBook);
                Long bookId = savedBook.getId();
                logger.info("Created temporary book with ID: {} and title: '{}'", bookId, tempTitle);

                // Extract metadata and update book (shared workflow)
                logger.info("Extracting book metadata from photo {}...", photoName);
                try {
                    // TODO: Save photo to database and use generateTempBook for full extraction
                    // For now, extract basic metadata with shared service
                    Map<String, Object> bookMetadata = bookPhotoProcessingService.extractBasicMetadata(photoBytes, "image/jpeg");

                    if (bookMetadata != null && bookMetadata.containsKey("title")) {
                        String extractedTitle = (String) bookMetadata.get("title");
                        String extractedAuthor = (String) bookMetadata.get("author");

                        // Update book with extracted metadata
                        savedBook.setTitle(extractedTitle);
                        com.muczynski.library.domain.Author author = authorService.findOrCreateAuthor(extractedAuthor);
                        savedBook.setAuthorId(author.getId());
                        savedBook = bookService.updateBook(bookId, savedBook);

                        logger.info("Updated book {} with title='{}', author='{}'", bookId, extractedTitle, extractedAuthor);

                        processedBooks.add(Map.of(
                                "photoId", photoId,
                                "photoName", photoName,
                                "title", extractedTitle,
                                "author", extractedAuthor,
                                "bookId", bookId
                        ));
                    } else {
                        logger.warn("Failed to extract metadata for photo {}, keeping temporary book", photoName);
                        processedBooks.add(Map.of(
                                "photoId", photoId,
                                "photoName", photoName,
                                "title", tempTitle,
                                "author", "Unknown",
                                "bookId", bookId
                        ));
                    }
                } catch (Exception e) {
                    logger.error("Error extracting metadata for photo {}: {}", photoName, e.getMessage());
                    // Keep the temporary book even if extraction fails
                    processedBooks.add(Map.of(
                            "photoId", photoId,
                            "photoName", photoName,
                            "title", tempTitle,
                            "author", "Unknown",
                            "bookId", bookId
                    ));
                }

                logger.info("Successfully processed photo {} as book (ID: {})", photoName, bookId);

            } catch (Exception e) {
                logger.error("Error processing photo {}: {}", photoName, e.getMessage(), e);
                skippedPhotos.add(Map.of(
                        "id", photoId,
                        "name", photoName,
                        "reason", "Error: " + e.getMessage()
                ));
            }
        }

        logger.info("Completed processing {} photos. Processed: {}, Skipped: {}",
                photos.size(), processedBooks.size(), skippedPhotos.size());

        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", processedBooks.size());
        result.put("skippedCount", skippedPhotos.size());
        result.put("totalPhotos", photos.size());
        result.put("processedBooks", processedBooks);
        result.put("skippedPhotos", skippedPhotos);

        logger.info("===== Books-from-Feed processing (Picker) complete =====");
        logger.info("Summary: {} total photos, {} books created, {} photos skipped",
                photos.size(), processedBooks.size(), skippedPhotos.size());

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
                throw new RuntimeException("Failed to download photo: HTTP " + responseCode);
            }
        } catch (Exception e) {
            logger.error("Error downloading photo from URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download photo: " + e.getMessage(), e);
        }
    }
}
