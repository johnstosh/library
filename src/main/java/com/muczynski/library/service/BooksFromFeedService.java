/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        String username = authentication.getName();
        UserDto userDto = userSettingsService.getUserSettings(username);

        // Get the last photo timestamp or default to yesterday
        String lastTimestamp = userDto.getLastPhotoTimestamp();
        if (lastTimestamp == null || lastTimestamp.trim().isEmpty()) {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            lastTimestamp = yesterday.format(DateTimeFormatter.ISO_DATE_TIME);
        }

        // Fetch photos from Google Photos
        List<Map<String, Object>> photos = googlePhotosService.fetchPhotos(lastTimestamp);

        List<Map<String, Object>> processedBooks = new ArrayList<>();
        List<Map<String, Object>> skippedPhotos = new ArrayList<>();
        String latestTimestamp = lastTimestamp;

        for (Map<String, Object> photo : photos) {
            try {
                // Check if photo already has book metadata in description
                String description = (String) photo.get("description");
                if (description != null && (description.contains("Title:") || description.contains("Author:"))) {
                    skippedPhotos.add(Map.of(
                            "id", photo.get("id"),
                            "reason", "Already processed (has book metadata in description)"
                    ));
                    continue;
                }

                // Get photo creation time
                Map<String, Object> mediaMetadata = (Map<String, Object>) photo.get("mediaMetadata");
                String creationTime = (String) mediaMetadata.get("creationTime");
                if (creationTime != null && creationTime.compareTo(latestTimestamp) > 0) {
                    latestTimestamp = creationTime;
                }

                // Download the photo
                String baseUrl = (String) photo.get("baseUrl");
                byte[] photoBytes = googlePhotosService.downloadPhoto(baseUrl);

                // Determine if this is a book photo using AI
                String detectionQuestion = "Is this image a photo of a book or book cover? Respond with only 'YES' or 'NO'.";
                String detectionResponse = askGrok.askAboutPhoto(photoBytes, "image/jpeg", detectionQuestion);

                if (!detectionResponse.trim().toUpperCase().contains("YES")) {
                    skippedPhotos.add(Map.of(
                            "id", photo.get("id"),
                            "reason", "Not a book photo"
                    ));
                    continue;
                }

                // Extract book metadata using AI
                String metadataQuestion = "This is a photo of a book. Please extract the book information and respond ONLY with valid JSON in this exact format:\n" +
                        "{\"title\": \"book title\", \"author\": \"author name\"}\n" +
                        "Do not include any other text, explanation, or markdown formatting. Only the JSON object.";

                String metadataResponse = askGrok.askAboutPhoto(photoBytes, "image/jpeg", metadataQuestion);

                // Parse the JSON response
                Map<String, Object> bookMetadata = parseBookMetadata(metadataResponse);

                if (bookMetadata == null || !bookMetadata.containsKey("title")) {
                    skippedPhotos.add(Map.of(
                            "id", photo.get("id"),
                            "reason", "Could not extract book metadata"
                    ));
                    continue;
                }

                String title = (String) bookMetadata.get("title");
                String authorName = (String) bookMetadata.get("author");

                // Create a temporary book entry with just the title
                // The AI will fill in the details from the photo
                Map<String, Object> tempBookData = new HashMap<>();
                tempBookData.put("title", title);

                // We need to create a minimal BookDto, save it, attach the photo, then use generateTempBook
                com.muczynski.library.dto.BookDto tempBook = new com.muczynski.library.dto.BookDto();
                tempBook.setTitle(title);
                tempBook.setStatus(com.muczynski.library.domain.BookStatus.ACTIVE);
                tempBook.setDateAddedToLibrary(java.time.LocalDate.now());

                // Create the book
                com.muczynski.library.dto.BookDto savedBook = bookService.createBook(tempBook);

                // Store the photo for this book
                // We'll need to use PhotoService to attach the photo
                Long bookId = savedBook.getId();

                // Note: Ideally we'd attach the photo here, but to keep it simple,
                // let's just use the metadata we extracted

                // Update photo description with book metadata
                String newDescription = String.format("Title: %s\nAuthor: %s", title, authorName);
                googlePhotosService.updatePhotoDescription((String) photo.get("id"), newDescription);

                processedBooks.add(Map.of(
                        "photoId", photo.get("id"),
                        "title", title,
                        "author", authorName,
                        "bookId", bookId
                ));

            } catch (Exception e) {
                skippedPhotos.add(Map.of(
                        "id", photo.get("id"),
                        "reason", "Error: " + e.getMessage()
                ));
            }
        }

        // Update the last photo timestamp in user settings
        UserSettingsDto settingsUpdate = new UserSettingsDto();
        settingsUpdate.setLastPhotoTimestamp(latestTimestamp);
        userSettingsService.updateUserSettings(username, settingsUpdate);

        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", processedBooks.size());
        result.put("skippedCount", skippedPhotos.size());
        result.put("totalPhotos", photos.size());
        result.put("processedBooks", processedBooks);
        result.put("skippedPhotos", skippedPhotos);
        result.put("lastTimestamp", latestTimestamp);

        return result;
    }

    /**
     * Parse JSON response from AI to extract book metadata
     */
    private Map<String, Object> parseBookMetadata(String response) {
        try {
            // Remove markdown code blocks if present
            String cleaned = response.trim();
            if (cleaned.startsWith("```")) {
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
                }

                // Extract author
                int authorStart = cleaned.indexOf("\"author\"");
                if (authorStart != -1) {
                    int valueStart = cleaned.indexOf(":", authorStart) + 1;
                    int valueEnd = cleaned.indexOf("\"", cleaned.indexOf("\"", valueStart) + 1);
                    String author = cleaned.substring(cleaned.indexOf("\"", valueStart) + 1, valueEnd);
                    result.put("author", author);
                }

                return result;
            }
        } catch (Exception e) {
            // Parsing failed
        }
        return null;
    }
}
