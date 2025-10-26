// (c) Copyright 2025 by Muczynski
package com.muczynski.library.service;

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
    }

    public String askAboutPhoto(byte[] imageBytes, String contentType, String question) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        String username = authentication.getName();
        UserDto userDto = userSettingsService.getUserSettings(username);
        String apiKey = userDto.getXaiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("xAI API key not configured for user: " + username);
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
        request.put("max_tokens", 300);
        request.put("temperature", 0.7);

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
            throw new RuntimeException("Unexpected response format from xAI API");
        } else {
            throw new RuntimeException("xAI API call failed: " + response.getStatusCode() + " - " + response.getBody());
        }
    }
}
