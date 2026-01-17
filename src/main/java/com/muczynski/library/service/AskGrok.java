/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AskGrok {

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

    public String askAboutPhoto(byte[] imageBytes, String contentType, String question) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("No authenticated user found");
        }
        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        UserDto userDto = userSettingsService.getUserSettings(userId);
        String apiKey = userDto.getXaiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new LibraryException("xAI API key not configured for user ID: " + userId);
        }

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", question);

        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        String base64Image = encoder.encodeToString(imageBytes);

        Map<String, Object> imageUrlPart = new HashMap<>();
        imageUrlPart.put("url", "data:" + contentType + ";base64," + base64Image);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlPart);

        List<Object> content = Arrays.asList(textPart, imagePart);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "grok-4"); // use grok-4 for vision
        request.put("messages", Arrays.asList(message));
        request.put("max_tokens", 2000);
        request.put("temperature", 0.7);
        request.put("stream", false); // Disable streaming to avoid complexity; use long timeouts instead

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

    /**
     * Ask a question about multiple photos
     * @param photoDataList List of maps containing "imageBytes" (byte[]) and "contentType" (String)
     * @param question The question to ask about the photos
     * @return AI response as String
     */
    public String askAboutPhotos(List<Map<String, Object>> photoDataList, String question) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("No authenticated user found");
        }
        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        UserDto userDto = userSettingsService.getUserSettings(userId);
        String apiKey = userDto.getXaiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new LibraryException("xAI API key not configured for user ID: " + userId);
        }

        // Build content array: first the text question, then all images
        List<Object> content = new ArrayList<>();

        // Add text question first
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", question);
        content.add(textPart);

        // Add all images
        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        for (Map<String, Object> photoData : photoDataList) {
            byte[] imageBytes = (byte[]) photoData.get("imageBytes");
            String contentType = (String) photoData.get("contentType");

            // Skip photos with null image data
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

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "grok-4"); // use grok-4 for vision
        request.put("messages", Arrays.asList(message));
        request.put("max_tokens", 2000);
        request.put("temperature", 0.7);
        request.put("stream", false); // Disable streaming to avoid complexity; use long timeouts instead

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

    public String askTextOnly(String question) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("No authenticated user found");
        }
        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        UserDto userDto = userSettingsService.getUserSettings(userId);
        String apiKey = userDto.getXaiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new LibraryException("xAI API key not configured for user ID: " + userId);
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", question);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "grok-3-latest");
        request.put("messages", Arrays.asList(message));
        request.put("max_tokens", 2000);
        request.put("temperature", 0.7);
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

    /**
     * Get Library of Congress call number suggestion for a book
     * @param title Book title
     * @param author Author name
     * @return Suggested LOC call number
     */
    public String suggestLocNumber(String title, String author) {
        String prompt = String.format(
                "What is the Library of Congress call number for the book \"%s\" by %s? " +
                "Please provide ONLY the call number in standard Library of Congress format. " +
                "If you're not certain, provide your best estimate based on the subject matter and author. " +
                "Do not include any explanation, just the call number.",
                title,
                author != null && !author.isEmpty() ? author : "Unknown Author"
        );
        return askTextOnly(prompt);
    }
}
