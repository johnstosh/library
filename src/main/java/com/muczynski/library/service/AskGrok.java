/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AskGrok {

    private static final Logger log = LoggerFactory.getLogger(AskGrok.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private UserSettingsService userSettingsService;

    private final RestTemplate restTemplate;

    public AskGrok() {
        this.restTemplate = new RestTemplate();
        // Configure long timeout for xAI API calls (10 minutes)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(600000); // 10 minutes
        factory.setReadTimeout(600000); // 10 minutes
        this.restTemplate.setRequestFactory(factory);
    }

    // Model constants.
    // xAI does not currently offer a rolling flagship alias (grok-latest / grok-default 404).
    // grok-4 and grok-4-latest resolve to grok-4.3. Pin the current flagship explicitly.
    public static final String MODEL_GROK_FLAGSHIP = "grok-4.6";
    public static final String MODEL_GROK_4 = MODEL_GROK_FLAGSHIP;
    public static final String MODEL_GROK_4_FAST = "grok-4-1-fast-reasoning";
    public static final String IMAGE_DETAIL_HIGH = "high";

    private static final int CATALOG_MAX_COMPLETION_TOKENS = 8000;
    private static final int FAST_MAX_COMPLETION_TOKENS = 500;

    /**
     * Analyze a single photo using Grok AI vision model with grok-4-fast.
     * @param imageBytes Photo bytes
     * @param contentType Image content type (e.g., "image/jpeg")
     * @param prompt The analysis prompt/question for the AI
     * @return AI response as String
     */
    public String analyzePhoto(byte[] imageBytes, String contentType, String prompt) {
        return analyzePhoto(imageBytes, contentType, prompt, MODEL_GROK_4_FAST);
    }

    /**
     * Analyze a single photo using Grok AI vision model with specified model.
     * @param imageBytes Photo bytes
     * @param contentType Image content type (e.g., "image/jpeg")
     * @param prompt The analysis prompt/question for the AI
     * @param model The Grok model to use
     * @return AI response as String
     */
    public String analyzePhoto(byte[] imageBytes, String contentType, String prompt, String model) {
        return analyzePhoto(imageBytes, contentType, prompt, model, null, null);
    }

    /**
     * Analyze a single photo using Grok AI vision model with specified model and optional system prompt.
     */
    public String analyzePhoto(byte[] imageBytes, String contentType, String prompt, String model, String systemPrompt) {
        return analyzePhoto(imageBytes, contentType, prompt, model, systemPrompt, null);
    }

    /**
     * Analyze a single photo using Grok AI vision model with specified model, optional system prompt,
     * and optional image detail level ({@code auto}, {@code low}, or {@code high}).
     */
    public String analyzePhoto(byte[] imageBytes, String contentType, String prompt, String model,
                               String systemPrompt, String imageDetail) {
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        String base64Image = encoder.encodeToString(imageBytes);

        Map<String, Object> imageUrlPart = new HashMap<>();
        imageUrlPart.put("url", "data:" + contentType + ";base64," + base64Image);
        if (imageDetail != null && !imageDetail.isBlank()) {
            imageUrlPart.put("detail", imageDetail);
        }

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlPart);

        List<Object> content = Arrays.asList(textPart, imagePart);
        return complete(model, chatMessages(systemPrompt, content), CATALOG_MAX_COMPLETION_TOKENS, 0.7);
    }

    /**
     * Analyze multiple photos using Grok AI vision model with grok-4-fast.
     * @param photoDataList List of maps containing "imageBytes" (byte[]) and "contentType" (String)
     * @param prompt The analysis prompt/question for the AI
     * @return AI response as String
     */
    public String analyzePhotos(List<Map<String, Object>> photoDataList, String prompt) {
        return analyzePhotos(photoDataList, prompt, MODEL_GROK_4_FAST);
    }

    /**
     * Analyze multiple photos using Grok AI vision model with specified model.
     * @param photoDataList List of maps containing "imageBytes" (byte[]) and "contentType" (String)
     * @param prompt The analysis prompt/question for the AI
     * @param model The Grok model to use
     * @return AI response as String
     */
    public String analyzePhotos(List<Map<String, Object>> photoDataList, String prompt, String model) {
        return analyzePhotos(photoDataList, prompt, model, null);
    }

    /**
     * Analyze multiple photos using Grok AI vision model with specified model and optional system prompt.
     */
    public String analyzePhotos(List<Map<String, Object>> photoDataList, String prompt, String model, String systemPrompt) {
        // Build content array: first the text question, then all images
        List<Object> content = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);
        content.add(textPart);

        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        for (Map<String, Object> photoData : photoDataList) {
            byte[] imageBytes = (byte[]) photoData.get("imageBytes");
            String contentType = (String) photoData.get("contentType");

            if (imageBytes == null || imageBytes.length == 0) {
                continue;
            }

            String base64Image = encoder.encodeToString(imageBytes);

            Map<String, Object> imageUrlPart = new HashMap<>();
            imageUrlPart.put("url", "data:" + contentType + ";base64," + base64Image);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrlPart);

            content.add(imagePart);
        }

        return complete(model, chatMessages(systemPrompt, content), CATALOG_MAX_COMPLETION_TOKENS, 0.7);
    }

    /**
     * Ask a text-only question to Grok AI (no images).
     * @param question The question to ask
     * @return AI response as String
     */
    public String askQuestion(String question) {
        return askQuestion(question, null);
    }

    /**
     * Ask a text-only question to Grok AI with an optional system prompt.
     */
    public String askQuestion(String question, String systemPrompt) {
        return complete(MODEL_GROK_FLAGSHIP, chatMessages(systemPrompt, question), CATALOG_MAX_COMPLETION_TOKENS, 0.7);
    }

    /**
     * Get Library of Congress call number suggestion for a book
     * @param title Book title
     * @param author Author name
     * @return Suggested LOC call number
     */
    public String suggestLocNumber(String title, String author) {
        String authorLabel = author != null && !author.isEmpty() ? author : "Unknown Author";
        String prompt = String.format(
                "Return a JSON array of Library of Congress Classification call numbers for the book \"%s\" by %s.\n" +
                "Each distinct call number must be its own string. Do not concatenate two numbers into one string.\n" +
                "Sort the array alphabetically. If uncertain, still list each estimate as a separate string.\n" +
                "Respond with only the JSON array, no other text. Example: [\"BX4700.T4\",\"PS3511.I9 G7\"]",
                title,
                authorLabel
        );
        String response = askQuestion(prompt);
        List<String> callNumbers = parseCallNumberArray(response);
        if (callNumbers.isEmpty()) {
            log.warn("Could not parse LOC call number array for \"{}\" by {}: {}", title, authorLabel, response);
            return null;
        }
        log.info("LOC call number array for \"{}\" by {}: {}", title, authorLabel, callNumbers);
        return callNumbers.get(0);
    }

    /**
     * Ask Grok for Grokipedia article URLs for a book, using the author as context.
     * The model must reply with a JSON array of URL strings and nothing else.
     */
    public List<String> suggestGrokipediaUrlsForBook(String title, String authorName) {
        String authorLabel = authorName != null && !authorName.isBlank() ? authorName : "unknown author";
        String prompt = String.format(
                "This is in regard to authors and books.\n" +
                "Find Grokipedia article URLs for the book \"%s\" by %s.\n" +
                "Disambiguation in the URL (for example a parenthetical like (novel)) may or may not be necessary.\n" +
                "Respond in JSON format with nothing before or after the JSON.\n" +
                "Return a JSON array of URL strings. Example: [\"https://grokipedia.com/page/The_Song_of_Bernadette_(novel)\"]",
                title,
                authorLabel
        );
        String response = askQuestion(prompt);
        List<String> urls = parseJsonStringArray(response);
        if (urls.isEmpty()) {
            log.warn("Could not parse Grokipedia URL array for book \"{}\" by {}: {}", title, authorLabel, response);
        }
        return urls;
    }

    /**
     * Ask Grok for Grokipedia article URLs for an author, using a book as context.
     * The model must reply with a JSON array of URL strings and nothing else.
     */
    public List<String> suggestGrokipediaUrlsForAuthor(String authorName, String bookTitle) {
        String bookContext = bookTitle != null && !bookTitle.isBlank()
                ? " known for the book \"" + bookTitle + "\""
                : "";
        String prompt = String.format(
                "This is in regard to authors and books.\n" +
                "Find Grokipedia article URLs for the author %s%s.\n" +
                "Disambiguation in the URL (for example a parenthetical like (writer)) may or may not be necessary.\n" +
                "Respond in JSON format with nothing before or after the JSON.\n" +
                "Return a JSON array of URL strings. Example: [\"https://grokipedia.com/page/Louisa_May_Alcott_(writer)\"]",
                authorName,
                bookContext
        );
        String response = askQuestion(prompt);
        List<String> urls = parseJsonStringArray(response);
        if (urls.isEmpty()) {
            log.warn("Could not parse Grokipedia URL array for author {}: {}", authorName, response);
        }
        return urls;
    }

    /**
     * Parses a JSON array of strings from a model reply, ignoring blank entries.
     * Does not sort; callers that need a specific order must sort themselves.
     */
    static List<String> parseJsonStringArray(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        String trimmed = response.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        try {
            List<String> parsed = OBJECT_MAPPER.readValue(
                    trimmed.substring(start, end + 1),
                    new TypeReference<List<String>>() {});
            List<String> cleaned = new ArrayList<>();
            if (parsed != null) {
                for (String item : parsed) {
                    if (item != null && !item.isBlank()) {
                        cleaned.add(item.trim());
                    }
                }
            }
            return cleaned;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Parses a JSON array of call-number strings from a model reply and sorts
     * them alphabetically, ignoring blank entries.
     */
    static List<String> parseCallNumberArray(String response) {
        List<String> cleaned = new ArrayList<>(parseJsonStringArray(response));
        cleaned.sort(String.CASE_INSENSITIVE_ORDER);
        return cleaned;
    }

    /**
     * Suggest genres/tags for a book based on its catalog card information.
     * Uses grok-4-fast model for faster responses.
     *
     * @param bookJson JSON string containing book information
     * @param authorJson JSON string containing author information (can be null)
     * @return Comma-separated list of suggested genre tags
     */
    public String suggestGenres(String bookJson, String authorJson) {
        String prompt = String.format(
                "Based on the following book and author information from a library card catalog, " +
                "suggest genre/category tags for this book. " +
                "Use ONLY these predefined tags where applicable: " +
                "fiction, slice-of-life, hagiography, saint, fantasy, family, childrens, adult, " +
                "philosophy, theology, discernment, talking-animals, biography, history, " +
                "prayer, classic, poetry, science, music, " +
                "mystery, adventure, romance, humor. " +
                "Note: science-fiction should be categorized under 'fantasy'. " +
                "Return ONLY a comma-separated list of applicable tags, nothing else. " +
                "For example: fiction, fantasy, childrens\nIf hagiography is used, don't use saint nor biography nor history. Classic should only be used for classic books as in classic literature. Use discernment for the discernment of spirits in the sense of Ignatius.\n" +
                "Book Information:\n%s\n\n" +
                "%s",
                bookJson,
                authorJson != null && !authorJson.isBlank() ? "Author Information:\n" + authorJson : ""
        );
        return askQuestionFast(prompt);
    }

    /**
     * Ask a text-only question to Grok AI using grok-4-fast model.
     * @param question The question to ask
     * @return AI response as String
     */
    public String askQuestionFast(String question) {
        return complete(MODEL_GROK_4_FAST, chatMessages(null, question), FAST_MAX_COMPLETION_TOKENS, 0.3);
    }

    private List<Map<String, Object>> chatMessages(String systemPrompt, Object userContent) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, Object> system = new HashMap<>();
            system.put("role", "system");
            system.put("content", systemPrompt);
            messages.add(system);
        }
        Map<String, Object> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userContent);
        messages.add(user);
        return messages;
    }

    private String requireApiKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("No authenticated user found");
        }
        Long userId = Long.parseLong(authentication.getName());
        UserDto userDto = userSettingsService.getUserSettings(userId);
        String apiKey = userDto.getXaiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new LibraryException("xAI API key not configured for user ID: " + userId);
        }
        return apiKey;
    }

    private String complete(String model, List<Map<String, Object>> messages, int maxCompletionTokens, double temperature) {
        String apiKey = requireApiKey();

        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("messages", messages);
        request.put("max_completion_tokens", maxCompletionTokens);
        request.put("temperature", temperature);
        request.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.x.ai/v1/chat/completions",
                entity,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("choices")) {
                List<Map> choices = (List<Map>) body.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    if (choice.containsKey("message")) {
                        Map<String, Object> messageResponse = (Map<String, Object>) choice.get("message");
                        return (String) messageResponse.get("content");
                    }
                }
            }
            throw new LibraryException("Unexpected response format from xAI API");
        } else {
            throw new LibraryException("xAI API call failed: " + response.getStatusCode() + " - " + response.getBody());
        }
    }
}
