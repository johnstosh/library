/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.Library;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.LibraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
                String mimeType = (String) photo.get("mimeType");
                if (mimeType == null || mimeType.trim().isEmpty()) {
                    mimeType = "image/jpeg"; // Default fallback
                }
                logger.debug("Downloading photo {} from: {} (mimeType: {})", photoId, baseUrl, mimeType);
                byte[] photoBytes = googlePhotosService.downloadPhoto(baseUrl);
                logger.info("Downloaded photo {} ({} bytes, mimeType: {})", photoId, photoBytes.length, mimeType);

                // Create temporary book with timestamp-based name (shared workflow)
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
                String tempTitle = "Book_" + timestamp;

                // Find or create placeholder author
                Author placeholderAuthor = authorService.findOrCreateAuthor("John Doe");
                logger.debug("Using placeholder author: {} (ID: {})", placeholderAuthor.getName(), placeholderAuthor.getId());

                // Get default library
                List<Library> libraries = libraryRepository.findAll();
                if (libraries.isEmpty()) {
                    throw new RuntimeException("No library found in database. Please create a library first.");
                }
                Long libraryId = libraries.get(0).getId();

                // Create temporary book entry
                logger.info("Creating temporary book entry for photo {}", photoId);
                BookDto tempBook = new BookDto();
                tempBook.setTitle(tempTitle);
                tempBook.setAuthorId(placeholderAuthor.getId());
                tempBook.setLibraryId(libraryId);
                tempBook.setStatus(BookStatus.ACTIVE);
                tempBook.setDateAddedToLibrary(LocalDate.now());

                BookDto savedBook = bookService.createBook(tempBook);
                Long bookId = savedBook.getId();
                logger.info("Created temporary book with ID: {}", bookId);

                // Save photo to database with actual MIME type
                logger.info("Saving photo to book {} (mimeType: {})", bookId, mimeType);
                photoService.addPhotoFromBytes(bookId, photoBytes, mimeType);

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
                        "photoId", photoId,
                        "title", fullBook.getTitle(),
                        "author", authorName,
                        "bookId", bookId
                ));

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
                String mimeType = (String) photo.get("mimeType");
                if (mimeType == null || mimeType.trim().isEmpty()) {
                    mimeType = "image/jpeg"; // Default fallback
                }
                logger.debug("Downloading photo {} from URL: {} (mimeType: {})", photoName, photoUrl, mimeType);
                byte[] photoBytes = downloadPhotoFromUrl(photoUrl, accessToken);
                logger.info("Downloaded photo {} ({} bytes, mimeType: {})", photoName, photoBytes.length, mimeType);

                // Create temporary book with timestamp-based name (shared workflow)
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
                String tempTitle = "Book_" + timestamp;

                // Find or create placeholder author
                Author placeholderAuthor = authorService.findOrCreateAuthor("John Doe");
                logger.debug("Using placeholder author: {} (ID: {})", placeholderAuthor.getName(), placeholderAuthor.getId());

                // Get default library
                List<Library> libraries = libraryRepository.findAll();
                if (libraries.isEmpty()) {
                    throw new RuntimeException("No library found in database. Please create a library first.");
                }
                Long libraryId = libraries.get(0).getId();

                // Create temporary book entry
                logger.info("Creating temporary book entry for photo {}", photoName);
                BookDto tempBook = new BookDto();
                tempBook.setTitle(tempTitle);
                tempBook.setAuthorId(placeholderAuthor.getId());
                tempBook.setLibraryId(libraryId);
                tempBook.setStatus(BookStatus.ACTIVE);
                tempBook.setDateAddedToLibrary(LocalDate.now());

                BookDto savedBook = bookService.createBook(tempBook);
                Long bookId = savedBook.getId();
                logger.info("Created temporary book with ID: {}", bookId);

                // Save photo to database with actual MIME type
                logger.info("Saving photo to book {} (mimeType: {})", bookId, mimeType);
                photoService.addPhotoFromBytes(bookId, photoBytes, mimeType);

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
                        "photoId", photoId,
                        "photoName", photoName,
                        "title", fullBook.getTitle(),
                        "author", authorName,
                        "bookId", bookId
                ));

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
