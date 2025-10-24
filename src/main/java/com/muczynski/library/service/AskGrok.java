package com.muczynski.library.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AskGrok {

    private final RestTemplate restTemplate;

    public AskGrok() {
        this.restTemplate = new RestTemplate();
    }

    public String askAboutPhoto(String photoUrl, String question, String apiKey) {
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", question);

        Map<String, Object> imageUrlPart = new HashMap<>();
        imageUrlPart.put("url", photoUrl);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlPart);

        List<Object> content = Arrays.asList(textPart, imagePart);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "grok-vision-beta");
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
