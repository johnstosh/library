/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Shared service for AI-based book photo processing operations
 * Used by both BooksFromFeedService and BookService
 */
@Service
public class BookPhotoProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(BookPhotoProcessingService.class);

    @Autowired
    private AskGrok askGrok;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Detect if a photo is a book cover using AI
     * @param photoBytes The image bytes
     * @param mimeType The image MIME type
     * @return true if AI confirms it's a book photo
     */
    public boolean isBookPhoto(byte[] photoBytes, String mimeType) {
        logger.debug("Asking AI if photo is a book cover");
        String detectionQuestion = "Is this image a photo of a book or book cover? Respond with only 'YES' or 'NO'.";
        String detectionResponse = askGrok.askAboutPhoto(photoBytes, mimeType, detectionQuestion);
        boolean isBook = detectionResponse.trim().toUpperCase().contains("YES");
        logger.debug("AI book detection result: {}", isBook);
        return isBook;
    }

    /**
     * Extract basic book metadata (title and author) from a photo using AI
     * @param photoBytes The image bytes
     * @param mimeType The image MIME type
     * @return Map with "title" and "author" keys, or null if extraction fails
     */
    public Map<String, Object> extractBasicMetadata(byte[] photoBytes, String mimeType) {
        logger.debug("Extracting basic book metadata from photo using AI");
        String metadataQuestion = "This is a photo of a book. Please extract the book information and respond ONLY with valid JSON in this exact format:\n" +
                "{\"title\": \"book title\", \"author\": \"author name\"}\n" +
                "Do not include any other text, explanation, or markdown formatting. Only the JSON object.";

        String metadataResponse = askGrok.askAboutPhoto(photoBytes, mimeType, metadataQuestion);
        return parseBasicMetadata(metadataResponse);
    }

    /**
     * Parse JSON response containing title and author
     * Handles both simple JSON parsing and Jackson ObjectMapper fallback
     * @param response AI response containing JSON
     * @return Map with "title" and "author" keys, or null if parsing fails
     */
    public Map<String, Object> parseBasicMetadata(String response) {
        logger.debug("Parsing basic metadata from AI response");

        try {
            // First try: Extract JSON and use ObjectMapper (more robust)
            String jsonString = extractJsonString(response);
            if (jsonString != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(jsonString, Map.class);
                    logger.debug("Successfully parsed metadata using ObjectMapper: title={}, author={}",
                            result.get("title"), result.get("author"));
                    return result;
                } catch (Exception e) {
                    logger.debug("ObjectMapper parsing failed, trying manual parsing: {}", e.getMessage());
                }
            }

            // Fallback: Manual string parsing for simple cases
            return parseBasicMetadataManual(response);

        } catch (Exception e) {
            logger.error("Failed to parse book metadata from response", e);
            logger.error("Response was: {}", response);
        }
        return null;
    }

    /**
     * Extract JSON string from AI response (removes markdown, extra text)
     * This is shared logic from BookService.extractJsonFromResponse
     */
    private String extractJsonString(String response) {
        String trimmedResponse = response.trim();

        // Find the start of the JSON
        int startIndex = trimmedResponse.indexOf('{');
        if (startIndex == -1) {
            logger.debug("No opening brace found in AI response");
            return null;
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
            logger.debug("No closing brace found in AI response");
            return null;
        }

        String jsonString = trimmedResponse.substring(startIndex, endIndex + 1);
        logger.debug("Extracted JSON string: {}", jsonString);
        return jsonString;
    }

    /**
     * Manual parsing for simple title/author JSON
     * Fallback when ObjectMapper fails
     */
    private Map<String, Object> parseBasicMetadataManual(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            logger.debug("Removing markdown code blocks from response");
            cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
        }

        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            java.util.Map<String, Object> result = new java.util.HashMap<>();

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

            logger.debug("Successfully parsed metadata manually: title={}, author={}", result.get("title"), result.get("author"));
            return result;
        }

        logger.warn("Response is not valid JSON format");
        return null;
    }
}
