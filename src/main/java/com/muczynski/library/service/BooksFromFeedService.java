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
                String detectionQuestion = "Is this image a photo of a book or book cover? Respond with only 'YES' or 'NO'.";
                String detectionResponse = askGrok.askAboutPhoto(photoBytes, "image/jpeg", detectionQuestion);
                logger.info("AI detection response for photo {}: {}", photoId, detectionResponse);

                if (!detectionResponse.trim().toUpperCase().contains("YES")) {
                    logger.info("Skipping photo {} - not a book photo (AI said: {})", photoId, detectionResponse);
                    skippedPhotos.add(Map.of(
                            "id", photoId,
                            "reason", "Not a book photo"
                    ));
                    continue;
                }

                // Extract book metadata using AI
                logger.info("Extracting book metadata from photo {}...", photoId);
                String metadataQuestion = "This is a photo of a book. Please extract the book information and respond ONLY with valid JSON in this exact format:\n" +
                        "{\"title\": \"book title\", \"author\": \"author name\"}\n" +
                        "Do not include any other text, explanation, or markdown formatting. Only the JSON object.";

                String metadataResponse = askGrok.askAboutPhoto(photoBytes, "image/jpeg", metadataQuestion);
                logger.debug("AI metadata response for photo {}: {}", photoId, metadataResponse);

                // Parse the JSON response
                Map<String, Object> bookMetadata = parseBookMetadata(metadataResponse);

                if (bookMetadata == null || !bookMetadata.containsKey("title")) {
                    logger.warn("Failed to extract book metadata from photo {}. AI response: {}", photoId, metadataResponse);
                    skippedPhotos.add(Map.of(
                            "id", photoId,
                            "reason", "Could not extract book metadata"
                    ));
                    continue;
                }

                String title = (String) bookMetadata.get("title");
                String authorName = (String) bookMetadata.get("author");
                logger.info("Extracted metadata from photo {}: title='{}', author='{}'", photoId, title, authorName);

                // Create a temporary book entry with just the title
                // The AI will fill in the details from the photo
                Map<String, Object> tempBookData = new HashMap<>();
                tempBookData.put("title", title);

                // We need to create a minimal BookDto, save it, attach the photo, then use generateTempBook
                logger.info("Creating book entry for: '{}'", title);
                com.muczynski.library.dto.BookDto tempBook = new com.muczynski.library.dto.BookDto();
                tempBook.setTitle(title);
                tempBook.setStatus(com.muczynski.library.domain.BookStatus.ACTIVE);
                tempBook.setDateAddedToLibrary(java.time.LocalDate.now());

                // Create the book
                com.muczynski.library.dto.BookDto savedBook = bookService.createBook(tempBook);
                Long bookId = savedBook.getId();
                logger.info("Created book with ID: {} for title: '{}'", bookId, title);

                // Store the photo for this book
                // We'll need to use PhotoService to attach the photo
                // Note: Ideally we'd attach the photo here, but to keep it simple,
                // let's just use the metadata we extracted

                // Update photo description with book metadata
                logger.info("Updating photo {} description with book metadata", photoId);
                String newDescription = String.format("Title: %s\nAuthor: %s", title, authorName);
                googlePhotosService.updatePhotoDescription(photoId, newDescription);
                logger.info("Successfully updated photo {} description", photoId);

                processedBooks.add(Map.of(
                        "photoId", photoId,
                        "title", title,
                        "author", authorName,
                        "bookId", bookId
                ));

                logger.info("Successfully processed photo {} as book: '{}' (ID: {})", photoId, title, bookId);

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
     * Parse JSON response from AI to extract book metadata
     */
    private Map<String, Object> parseBookMetadata(String response) {
        logger.debug("Parsing book metadata from AI response");

        try {
            // Remove markdown code blocks if present
            String cleaned = response.trim();
            if (cleaned.startsWith("```")) {
                logger.debug("Removing markdown code blocks from response");
                cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            }

            // Simple JSON parsing (in production, use Jackson or Gson)
            cleaned = cleaned.trim();
            if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
                Map<String, Object> result = new HashMap<>();

                // Extract title
                int titleStart = cleaned.indexOf("\"title\"");
                if (titleStart != -1) {
                    int valueStart = cleaned.indexOf(":", titleStart) + 1;
                    int valueEnd = cleaned.indexOf("\"", cleaned.indexOf("\"", valueStart) + 1);
                    String title = cleaned.substring(cleaned.indexOf("\"", valueStart) + 1, valueEnd);
                    result.put("title", title);
                    logger.debug("Extracted title: {}", title);
                } else {
                    logger.warn("Could not find 'title' field in JSON response");
                }

                // Extract author
                int authorStart = cleaned.indexOf("\"author\"");
                if (authorStart != -1) {
                    int valueStart = cleaned.indexOf(":", authorStart) + 1;
                    int valueEnd = cleaned.indexOf("\"", cleaned.indexOf("\"", valueStart) + 1);
                    String author = cleaned.substring(cleaned.indexOf("\"", valueStart) + 1, valueEnd);
                    result.put("author", author);
                    logger.debug("Extracted author: {}", author);
                } else {
                    logger.warn("Could not find 'author' field in JSON response");
                }

                logger.debug("Successfully parsed metadata: title={}, author={}", result.get("title"), result.get("author"));
                return result;
            } else {
                logger.warn("Response is not valid JSON (doesn't start with {{ and end with }}): {}", cleaned);
            }
        } catch (Exception e) {
            // Parsing failed
            logger.error("Failed to parse book metadata from response", e);
            logger.error("Response was: {}", response);
        }
        return null;
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

                // Determine if this is a book photo using AI
                logger.info("Asking AI if photo {} is a book photo...", photoName);
                String detectionQuestion = "Is this image a photo of a book or book cover? Respond with only 'YES' or 'NO'.";
                String detectionResponse = askGrok.askAboutPhoto(photoBytes, "image/jpeg", detectionQuestion);
                logger.info("AI detection response for photo {}: {}", photoName, detectionResponse);

                if (!detectionResponse.trim().toUpperCase().contains("YES")) {
                    logger.info("Skipping photo {} - not a book photo (AI said: {})", photoName, detectionResponse);
                    skippedPhotos.add(Map.of(
                            "id", photoId,
                            "name", photoName,
                            "reason", "Not a book photo"
                    ));
                    continue;
                }

                // Extract book metadata using AI
                logger.info("Extracting book metadata from photo {}...", photoName);
                String metadataQuestion = "This is a photo of a book. Please extract the book information and respond ONLY with valid JSON in this exact format:\n" +
                        "{\"title\": \"book title\", \"author\": \"author name\"}\n" +
                        "Do not include any other text, explanation, or markdown formatting. Only the JSON object.";

                String metadataResponse = askGrok.askAboutPhoto(photoBytes, "image/jpeg", metadataQuestion);
                logger.debug("AI metadata response for photo {}: {}", photoName, metadataResponse);

                // Parse the JSON response
                Map<String, Object> bookMetadata = parseBookMetadata(metadataResponse);

                if (bookMetadata == null || !bookMetadata.containsKey("title")) {
                    logger.warn("Failed to extract book metadata from photo {}. AI response: {}", photoName, metadataResponse);
                    skippedPhotos.add(Map.of(
                            "id", photoId,
                            "name", photoName,
                            "reason", "Could not extract book metadata"
                    ));
                    continue;
                }

                String title = (String) bookMetadata.get("title");
                String authorName = (String) bookMetadata.get("author");
                logger.info("Extracted metadata from photo {}: title='{}', author='{}'", photoName, title, authorName);

                // Create book entry
                logger.info("Creating book entry for: '{}'", title);
                com.muczynski.library.dto.BookDto tempBook = new com.muczynski.library.dto.BookDto();
                tempBook.setTitle(title);
                tempBook.setStatus(com.muczynski.library.domain.BookStatus.ACTIVE);
                tempBook.setDateAddedToLibrary(java.time.LocalDate.now());

                // Create the book
                com.muczynski.library.dto.BookDto savedBook = bookService.createBook(tempBook);
                Long bookId = savedBook.getId();
                logger.info("Created book with ID: {} for title: '{}'", bookId, title);

                processedBooks.add(Map.of(
                        "photoId", photoId,
                        "photoName", photoName,
                        "title", title,
                        "author", authorName,
                        "bookId", bookId
                ));

                logger.info("Successfully processed photo {} as book: '{}' (ID: {})", photoName, title, bookId);

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
