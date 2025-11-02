/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GooglePhotosService {

    @Autowired
    private UserSettingsService userSettingsService;

    @Autowired
    private UserRepository userRepository;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${google.oauth.token-uri}")
    private String tokenUri;

    private final RestTemplate restTemplate;

    public GooglePhotosService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetch photos from Google Photos starting from a given timestamp
     * @param startTimestamp ISO 8601 timestamp to start from
     * @return List of photo metadata including URL, description, and timestamp
     */
    public List<Map<String, Object>> fetchPhotos(String startTimestamp) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        String username = authentication.getName();

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(username);

        // Build request to search for photos after the given timestamp
        Map<String, Object> filters = new HashMap<>();
        Map<String, Object> dateFilter = new HashMap<>();

        // Parse the startTimestamp and create date filter
        if (startTimestamp != null && !startTimestamp.trim().isEmpty()) {
            dateFilter.put("startDate", parseTimestamp(startTimestamp));
        }

        if (!dateFilter.isEmpty()) {
            filters.put("dateFilter", dateFilter);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("pageSize", 100); // Fetch up to 100 photos
        request.put("filters", filters);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://photoslibrary.googleapis.com/v1/mediaItems:search",
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("mediaItems")) {
                    return (List<Map<String, Object>>) body.get("mediaItems");
                }
                return new ArrayList<>();
            } else {
                throw new RuntimeException("Google Photos API call failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch photos from Google Photos: " + e.getMessage(), e);
        }
    }

    /**
     * Download a photo's content from Google Photos
     * @param baseUrl The base URL of the photo
     * @return The photo bytes
     */
    public byte[] downloadPhoto(String baseUrl) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        String username = authentication.getName();

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(username);

        // Add parameters to download full resolution photo
        String downloadUrl = baseUrl + "=d";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to download photo: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to download photo from Google Photos: " + e.getMessage(), e);
        }
    }

    /**
     * Update a photo's description in Google Photos
     * @param photoId The ID of the photo to update
     * @param description The new description
     */
    public void updatePhotoDescription(String photoId, String description) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        String username = authentication.getName();

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(username);

        Map<String, Object> request = new HashMap<>();
        request.put("description", description);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            String url = "https://photoslibrary.googleapis.com/v1/mediaItems/" + photoId + "?updateMask=description";
            restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to update photo description: " + e.getMessage(), e);
        }
    }

    /**
     * Parse timestamp string into date components for Google Photos API
     */
    private Map<String, Object> parseTimestamp(String timestamp) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
            Map<String, Object> date = new HashMap<>();
            date.put("year", dateTime.getYear());
            date.put("month", dateTime.getMonthValue());
            date.put("day", dateTime.getDayOfMonth());
            return date;
        } catch (Exception e) {
            // Default to yesterday if parsing fails
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            Map<String, Object> date = new HashMap<>();
            date.put("year", yesterday.getYear());
            date.put("month", yesterday.getMonthValue());
            date.put("day", yesterday.getDayOfMonth());
            return date;
        }
    }

    /**
     * Get valid access token, refreshing if necessary
     */
    private String getValidAccessToken(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = user.getGooglePhotosApiKey();
        String tokenExpiry = user.getGooglePhotosTokenExpiry();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new RuntimeException("Google Photos not authorized. Please authorize in Settings.");
        }

        // Check if token is expired or will expire soon (within 5 minutes)
        if (tokenExpiry != null && !tokenExpiry.trim().isEmpty()) {
            try {
                Instant expiry = Instant.parse(tokenExpiry);
                if (Instant.now().plusSeconds(300).isAfter(expiry)) {
                    // Token is expired or will expire soon, refresh it
                    return refreshAccessToken(user);
                }
            } catch (Exception e) {
                // If we can't parse expiry, try to refresh
                return refreshAccessToken(user);
            }
        }

        return accessToken;
    }

    /**
     * Refresh the access token using the refresh token
     */
    private String refreshAccessToken(User user) {
        String refreshToken = user.getGooglePhotosRefreshToken();

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new RuntimeException("No refresh token available. Please re-authorize Google Photos in Settings.");
        }

        String userClientSecret = user.getGoogleClientSecret();

        // Fall back to environment variable if user hasn't set their own
        String effectiveClientSecret = (userClientSecret != null && !userClientSecret.trim().isEmpty())
                ? userClientSecret
                : clientSecret;

        if (effectiveClientSecret == null || effectiveClientSecret.trim().isEmpty()) {
            throw new RuntimeException("Google Client Secret not configured. Please set it in Settings or contact administrator.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", effectiveClientSecret);
            body.add("refresh_token", refreshToken);
            body.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUri,
                    request,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to refresh access token");
            }

            Map<String, Object> tokenResponse = response.getBody();
            String newAccessToken = (String) tokenResponse.get("access_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            if (newAccessToken == null) {
                throw new RuntimeException("No access token in refresh response");
            }

            // Update user with new token
            user.setGooglePhotosApiKey(newAccessToken);
            Instant expiry = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600);
            user.setGooglePhotosTokenExpiry(expiry.toString());
            userRepository.save(user);

            return newAccessToken;

        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh access token: " + e.getMessage(), e);
        }
    }
}
