/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.Library;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BranchRepository;
import com.muczynski.library.repository.PhotoRepository;
import com.muczynski.library.repository.UserRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class BooksFromFeedService {

    private static final Logger logger = LoggerFactory.getLogger(BooksFromFeedService.class);
    private static final ZoneId EASTERN = ZoneId.of("America/New_York");
    private static final Pattern FILENAME_DATESTAMP = Pattern.compile("(2[01]\\d{12})");

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
    private BranchRepository branchRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private BranchService branchService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Generate a temporary title for a book from feed using the best available date.
     * Priority: filename datestamp > Google creationTime > server now().
     * Format: yyyy-MM-dd_HH:mm:ss
     *
     * @param filename Photo filename (e.g., "20260205_180822.jpg"), may be null
     * @param creationTime ISO-8601 creation time from Google Photos (e.g., "2026-02-05T23:08:22Z"), may be null
     * @return Formatted timestamp string
     */
    public static String generateTemporaryTitle(String filename, String creationTime) {
        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

        // 1. Try to parse datestamp from filename
        LocalDateTime fromFilename = parseDateFromFilename(filename);
        if (fromFilename != null) {
            return fromFilename.format(outputFormat);
        }

        // 2. Try to parse Google creationTime (UTC) and convert to Eastern
        LocalDateTime fromCreation = parseCreationTime(creationTime);
        if (fromCreation != null) {
            return fromCreation.format(outputFormat);
        }

        // 3. Fallback to server now
        return LocalDateTime.now().format(outputFormat);
    }

    /**
     * Parse a datestamp from a photo filename.
     * Strips all non-alphanumeric characters, then finds the first run of 14+ digits
     * starting with "20" or "21" and parses as yyyyMMddHHmmss.
     *
     * @param filename The photo filename (e.g., "20260205_180822.jpg")
     * @return Parsed LocalDateTime, or null if no datestamp found
     */
    static LocalDateTime parseDateFromFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        // Strip non-alphanumeric characters to normalize filenames like "IMG-2026-02-05 18_08_22.png"
        String digitsOnly = filename.replaceAll("[^0-9]", "");
        Matcher matcher = FILENAME_DATESTAMP.matcher(digitsOnly);
        if (matcher.find()) {
            try {
                return LocalDateTime.parse(matcher.group(1),
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            } catch (DateTimeParseException e) {
                logger.debug("Found 14-digit sequence but failed to parse as date: {}", matcher.group(1));
            }
        }
        return null;
    }

    /**
     * Parse an ISO-8601 creation time from Google Photos and convert from UTC to Eastern.
     *
     * @param creationTime ISO-8601 string (e.g., "2026-02-05T23:08:22Z")
     * @return LocalDateTime in US Eastern time, or null if parsing fails
     */
    static LocalDateTime parseCreationTime(String creationTime) {
        if (creationTime == null || creationTime.isEmpty()) {
            return null;
        }
        try {
            ZonedDateTime utc = ZonedDateTime.parse(creationTime);
            return utc.withZoneSameInstant(EASTERN).toLocalDateTime();
        } catch (DateTimeParseException e) {
            // Try without timezone suffix
            try {
                return LocalDateTime.parse(creationTime.replace("Z", ""));
            } catch (DateTimeParseException e2) {
                logger.debug("Could not parse creation time: {}", creationTime);
                return null;
            }
        }
    }

    /**
     * Check if a title is a temporary title from feed.
     * Delegates to BookService.isTemporaryTitle() for consistent behavior.
     */
    public static boolean isTemporaryTitle(String title) {
        return BookService.isTemporaryTitle(title);
    }

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
     * Process a single saved book using AI book-by-photo workflow.
     * Uses NOT_SUPPORTED propagation so that inner service calls (generateTempBook)
     * each run in their own transaction. If AI processing fails, the exception
     * propagates cleanly without marking an outer transaction as rollback-only.
     *
     * @param bookId The ID of the book to process
     * @return Map containing result of the processing
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> processSingleBook(Long bookId) {
        logger.info("Processing single book: {}", bookId);

        try {
            // Get the book
            BookDto tempBook = bookService.getBookById(bookId);
            if (tempBook == null) {
                throw new LibraryException("Book not found: " + bookId);
            }

            String originalTitle = tempBook.getTitle();

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
            result.put("originalTitle", originalTitle);

            return result;

        } catch (Exception e) {
            String rootMessage = getRootCauseMessage(e);
            logger.error("Error processing book {}: {}", bookId, rootMessage, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("bookId", bookId);
            result.put("error", rootMessage);
            return result;
        }
    }

    /**
     * Extract the root cause message from a potentially nested exception chain.
     * Returns the deepest cause's message for more useful error reporting.
     */
    private String getRootCauseMessage(Exception e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String rootMessage = root.getMessage();
        if (rootMessage == null || rootMessage.isBlank()) {
            rootMessage = root.getClass().getSimpleName();
        }
        // If the root cause is different from the top-level exception, include both
        if (root != e && e.getMessage() != null && !e.getMessage().equals(rootMessage)) {
            return e.getMessage() + " (caused by: " + rootMessage + ")";
        }
        return rootMessage;
    }

    /**
     * Phase 2: Process saved photos using the book-by-photo AI workflow
     * This can run independently after photos are saved to the database.
     * Uses NOT_SUPPORTED propagation so each book's AI processing runs in its own
     * transaction, preventing one failure from rolling back all successfully processed books.
     *
     * @return Map containing results of the processing
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> processSavedPhotos() {
        logger.info("===== Phase 2: Processing saved photos with AI =====");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to process photos without authentication");
            throw new LibraryException("No authenticated user found");
        }
        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        logger.info("Processing photos for user ID: {}", userId);

        // Find all temporary books from feed (those with temporary titles)
        // Uses efficient database query instead of loading all books
        List<BookDto> tempBooks = bookService.getBooksWithTemporaryTitles();

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
                String rootMessage = getRootCauseMessage(e);
                logger.error("Error processing book {}: {}", bookId, rootMessage, e);
                failedBooks.add(Map.of(
                        "bookId", bookId,
                        "originalTitle", tempTitle,
                        "reason", "Error: " + rootMessage
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
     * Save a single photo selected via Google Photos Picker API to database (Phase 1 - No AI).
     * Each call runs in its own transaction (class-level @Transactional), so failures
     * are isolated per-photo and don't corrupt the Hibernate session for other photos.
     *
     * @param photo Single photo metadata from the Picker
     * @return Map containing result: {success, photoId, photoName, bookId, title} or {success: false, skipped: true} or {success: false, error}
     */
    public Map<String, Object> saveSinglePhotoFromPicker(Map<String, Object> photo) {
        logger.info("===== Phase 1: Saving single photo from Picker to database =====");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to save photo without authentication");
            throw new LibraryException("No authenticated user found");
        }
        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));

        String accessToken = googlePhotosService.getValidAccessToken(user);
        Library branch = branchService.getOrCreateDefaultBranch();
        Long branchId = branch.getId();

        String photoId = (String) photo.get("id");
        String photoName = (String) photo.get("name");
        String photoUrl = (String) photo.get("url");

        logger.info("Saving photo: name={}, id={}", photoName, photoId);

        try {
            // Check if photo already has description with book metadata
            String description = (String) photo.get("description");
            if (description != null && (description.contains("Title:") || description.contains("Author:"))) {
                logger.info("Skipping photo {} - already processed (has book metadata in description)", photoName);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("skipped", true);
                result.put("photoId", photoId);
                result.put("photoName", photoName);
                result.put("reason", "Already processed (has book metadata in description)");
                return result;
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
                java.util.List<Photo> existingPhotos = photoRepository.findAllByImageChecksumOrderByIdAsc(checksum);
                if (!existingPhotos.isEmpty()) {
                    Photo existing = existingPhotos.get(0);
                    Long existingBookId = existing.getBook() != null ? existing.getBook().getId() : null;
                    String existingTitle = existing.getBook() != null ? existing.getBook().getTitle() : "Unknown";
                    String existingAuthor = existing.getBook() != null && existing.getBook().getAuthor() != null
                            ? existing.getBook().getAuthor().getName() : "Unknown";

                    logger.info("Photo {} already exists in database (book ID: {}, title: '{}')",
                            photoName, existingBookId, existingTitle);

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("skipped", true);
                    result.put("photoId", photoId);
                    result.put("photoName", photoName);
                    result.put("bookId", existingBookId != null ? existingBookId : -1);
                    result.put("title", existingTitle);
                    result.put("author", existingAuthor);
                    result.put("existingPhoto", true);
                    return result;
                }
            }

            // Extract creation time from photo metadata
            String creationTime = extractCreationTime(photo);

            // Create temporary book with best available date as title
            String tempTitle = generateTemporaryTitle(photoName, creationTime);

            // Use parsed date for dateAddedToLibrary (same priority as title)
            LocalDateTime dateAdded = parseDateFromFilename(photoName);
            if (dateAdded == null) {
                dateAdded = parseCreationTime(creationTime);
            }
            if (dateAdded == null) {
                dateAdded = LocalDateTime.now();
            }

            BookDto tempBook = new BookDto();
            tempBook.setTitle(tempTitle);
            tempBook.setAuthorId(null);
            tempBook.setLibraryId(branchId);
            tempBook.setStatus(BookStatus.ACTIVE);
            tempBook.setDateAddedToLibrary(dateAdded);

            BookDto savedBook = bookService.createBook(tempBook);
            Long bookId = savedBook.getId();
            logger.info("Created temporary book with ID: {} (title: {})", bookId, tempTitle);

            photoService.addPhotoFromBytes(bookId, photoBytes, mimeType, dateAdded);
            logger.info("Saved photo to database for book {} (dateTaken: {})", bookId, dateAdded);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("skipped", false);
            result.put("photoId", photoId);
            result.put("photoName", photoName);
            result.put("bookId", bookId);
            result.put("title", tempTitle);
            return result;

        } catch (Exception e) {
            logger.error("Error saving photo {}: {}", photoName, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("skipped", false);
            result.put("photoId", photoId);
            result.put("photoName", photoName);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Save photos selected via Google Photos Picker API to database (Phase 1 - No AI)
     * @deprecated Use {@link #saveSinglePhotoFromPicker(Map)} instead for per-photo transaction isolation.
     * @param photos List of photo metadata from the Picker
     * @return Map containing results of the save operation
     */
    @Deprecated
    public Map<String, Object> savePhotosFromPicker(List<Map<String, Object>> photos) {
        logger.info("===== Phase 1: Saving photos from Picker to database =====");
        logger.info("Saving {} photos selected from Picker", photos.size());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to save photos without authentication");
            throw new LibraryException("No authenticated user found");
        }
        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));
        logger.info("Saving photos for user ID: {}", userId);

        // Get valid access token for downloading photos from Picker baseUrl
        String accessToken = googlePhotosService.getValidAccessToken(user);

        List<Map<String, Object>> savedPhotos = new ArrayList<>();
        List<Map<String, Object>> skippedPhotos = new ArrayList<>();

        // Get default branch once
        Library branch = branchService.getOrCreateDefaultBranch();
        Long branchId = branch.getId();

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
                    java.util.List<Photo> existingPhotos = photoRepository.findAllByImageChecksumOrderByIdAsc(checksum);
                    if (!existingPhotos.isEmpty()) {
                        Photo existing = existingPhotos.get(0);
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

                // Extract creation time from photo metadata
                String creationTime = extractCreationTime(photo);

                // Create temporary book with best available date as title
                String tempTitle = generateTemporaryTitle(photoName, creationTime);

                // Use parsed date for dateAddedToLibrary (same priority as title)
                LocalDateTime dateAdded = parseDateFromFilename(photoName);
                if (dateAdded == null) {
                    dateAdded = parseCreationTime(creationTime);
                }
                if (dateAdded == null) {
                    dateAdded = LocalDateTime.now();
                }

                BookDto tempBook = new BookDto();
                tempBook.setTitle(tempTitle);
                tempBook.setAuthorId(null);  // Author will be set during AI processing
                tempBook.setLibraryId(branchId);
                tempBook.setStatus(BookStatus.ACTIVE);
                tempBook.setDateAddedToLibrary(dateAdded);

                BookDto savedBook = bookService.createBook(tempBook);
                Long bookId = savedBook.getId();
                logger.info("Created temporary book with ID: {} (title: {})", bookId, tempTitle);

                // Save photo to database with actual MIME type and creation time
                photoService.addPhotoFromBytes(bookId, photoBytes, mimeType, dateAdded);
                logger.info("Saved photo to database for book {} (dateTaken: {})", bookId, dateAdded);

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
     * Extract the best available creation time string from a photo metadata map.
     * Checks mediaMetadata.creationTime, then root creationTime, then lastEditedUtc.
     */
    private String extractCreationTime(Map<String, Object> photo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mediaMetadata = (Map<String, Object>) photo.get("mediaMetadata");
            if (mediaMetadata != null) {
                String ct = (String) mediaMetadata.get("creationTime");
                if (ct != null && !ct.isEmpty()) return ct;
            }
        } catch (Exception e) {
            // ignore cast errors
        }

        String ct = (String) photo.get("creationTime");
        if (ct != null && !ct.isEmpty()) return ct;

        ct = (String) photo.get("lastEditedUtc");
        if (ct != null && !ct.isEmpty()) return ct;

        return null;
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
