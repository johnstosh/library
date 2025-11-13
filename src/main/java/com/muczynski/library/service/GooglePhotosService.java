/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GooglePhotosService.class);

    @Autowired
    private UserSettingsService userSettingsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GlobalSettingsService globalSettingsService;

    @Value("${google.oauth.client-id}")
    private String clientId;

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
        logger.info("Fetching photos from Google Photos with start timestamp: {}", startTimestamp);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to fetch photos without authentication");
            throw new RuntimeException("No authenticated user found");
        }
        String username = authentication.getName();

        logger.debug("Fetching photos for user: {}", username);

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(username);

        // Diagnostic: Verify token scopes
        verifyTokenScopes(apiKey);

        // Build request to search for photos after the given timestamp
        Map<String, Object> filters = new HashMap<>();

        // Parse the startTimestamp and create date filter
        if (startTimestamp != null && !startTimestamp.trim().isEmpty()) {
            Map<String, Object> startDate = parseTimestamp(startTimestamp);

            // Create endDate as today
            LocalDateTime today = LocalDateTime.now();
            Map<String, Object> endDate = new HashMap<>();
            endDate.put("year", today.getYear());
            endDate.put("month", today.getMonthValue());
            endDate.put("day", today.getDayOfMonth());

            // Create date range with startDate and endDate
            Map<String, Object> dateRange = new HashMap<>();
            dateRange.put("startDate", startDate);
            dateRange.put("endDate", endDate);

            // Create ranges array
            List<Map<String, Object>> ranges = new ArrayList<>();
            ranges.add(dateRange);

            // Create dateFilter with ranges
            Map<String, Object> dateFilter = new HashMap<>();
            dateFilter.put("ranges", ranges);

            filters.put("dateFilter", dateFilter);
            logger.info("Using date filter with range from: year={}, month={}, day={} to: year={}, month={}, day={}",
                    startDate.get("year"), startDate.get("month"), startDate.get("day"),
                    endDate.get("year"), endDate.get("month"), endDate.get("day"));
        } else {
            logger.info("No start timestamp provided, fetching recent photos");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("pageSize", 100); // Fetch up to 100 photos
        if (!filters.isEmpty()) {
            request.put("filters", filters);
        }

        logger.debug("Google Photos API request: pageSize=100, filters={}", filters);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            logger.info("Sending request to Google Photos API...");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://photoslibrary.googleapis.com/v1/mediaItems:search",
                    entity,
                    Map.class
            );

            logger.info("Google Photos API response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("mediaItems")) {
                    List<Map<String, Object>> mediaItems = (List<Map<String, Object>>) body.get("mediaItems");
                    logger.info("Successfully fetched {} photos from Google Photos", mediaItems.size());
                    return mediaItems;
                }
                logger.info("Google Photos API returned no media items");
                return new ArrayList<>();
            } else {
                logger.error("Google Photos API call failed with status: {}", response.getStatusCode());
                throw new RuntimeException("Google Photos API call failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to fetch photos from Google Photos for user: {}", username, e);
            logger.error("Error message: {}", e.getMessage());

            // Check for specific error types
            if (e.getMessage() != null) {
                if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
                    logger.error("Unauthorized (401). Access token may be expired or invalid. Try re-authorizing Google Photos.");
                } else if (e.getMessage().contains("403") || e.getMessage().contains("Forbidden")) {
                    logger.error("Forbidden (403). This may mean:");
                    logger.error("  1. Photos Library API is not enabled in Google Cloud Console");
                    logger.error("  2. OAuth scope doesn't include photoslibrary.readonly");
                    logger.error("  3. User revoked access from their Google account");
                } else if (e.getMessage().contains("404")) {
                    logger.error("Not Found (404). The Google Photos API endpoint may be incorrect.");
                }
            }

            throw new RuntimeException("Failed to fetch photos from Google Photos: " + e.getMessage(), e);
        }
    }

    /**
     * Download a photo's content from Google Photos
     * @param baseUrl The base URL of the photo
     * @return The photo bytes
     */
    public byte[] downloadPhoto(String baseUrl) {
        logger.debug("Downloading photo from base URL: {}", baseUrl);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to download photo without authentication");
            throw new RuntimeException("No authenticated user found");
        }
        String username = authentication.getName();

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(username);

        // Add parameters to download full resolution photo
        String downloadUrl = baseUrl + "=d";

        logger.debug("Download URL: {}", downloadUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            logger.debug("Downloading photo for user: {}", username);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            logger.debug("Photo download response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful()) {
                byte[] photoBytes = response.getBody();
                if (photoBytes != null) {
                    logger.info("Successfully downloaded photo ({} bytes)", photoBytes.length);
                    return photoBytes;
                } else {
                    logger.error("Photo download succeeded but response body is null");
                    throw new RuntimeException("Photo download returned null");
                }
            } else {
                logger.error("Failed to download photo with status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to download photo: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to download photo from Google Photos for user: {}", username, e);
            logger.error("Error message: {}", e.getMessage());
            throw new RuntimeException("Failed to download photo from Google Photos: " + e.getMessage(), e);
        }
    }

    /**
     * Update a photo's description in Google Photos
     * @param photoId The ID of the photo to update
     * @param description The new description
     */
    public void updatePhotoDescription(String photoId, String description) {
        logger.info("Updating photo description for photo ID: {}", photoId);
        logger.debug("New description: {}", description);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Attempted to update photo description without authentication");
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
            logger.debug("Update photo description URL: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            logger.info("Successfully updated photo description for photo ID: {}. Response status: {}",
                    photoId, response.getStatusCode());

        } catch (Exception e) {
            logger.error("Failed to update photo description for photo ID: {} (user: {})", photoId, username, e);
            logger.error("Error message: {}", e.getMessage());
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
        logger.debug("Getting valid access token for user: {}", username);

        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = user.getGooglePhotosApiKey();
        String tokenExpiry = user.getGooglePhotosTokenExpiry();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.error("User {} has not authorized Google Photos. Access token is empty.", username);
            throw new RuntimeException("Google Photos not authorized. Please authorize in Settings.");
        }

        logger.debug("Access token found for user: {} (length: {} chars)", username, accessToken.length());

        // Check if token is expired or will expire soon (within 5 minutes)
        if (tokenExpiry != null && !tokenExpiry.trim().isEmpty()) {
            try {
                Instant expiry = Instant.parse(tokenExpiry);
                Instant now = Instant.now();
                long secondsUntilExpiry = expiry.getEpochSecond() - now.getEpochSecond();

                logger.debug("Token expiry: {}. Seconds until expiry: {}", expiry, secondsUntilExpiry);

                if (now.plusSeconds(300).isAfter(expiry)) {
                    // Token is expired or will expire soon, refresh it
                    if (now.isAfter(expiry)) {
                        logger.info("Access token for user {} has expired. Refreshing...", username);
                    } else {
                        logger.info("Access token for user {} will expire in {} seconds. Refreshing proactively...",
                                username, secondsUntilExpiry);
                    }
                    return refreshAccessToken(user);
                } else {
                    logger.debug("Access token is valid for user: {} (expires in {} seconds)", username, secondsUntilExpiry);
                }
            } catch (Exception e) {
                // If we can't parse expiry, try to refresh
                logger.warn("Failed to parse token expiry '{}' for user: {}. Attempting to refresh token.", tokenExpiry, username);
                return refreshAccessToken(user);
            }
        } else {
            logger.warn("No token expiry timestamp for user: {}. Token validity unknown.", username);
        }

        return accessToken;
    }

    /**
     * Verify token scopes using Google's tokeninfo endpoint (diagnostic)
     */
    private void verifyTokenScopes(String accessToken) {
        try {
            logger.info("=== TOKEN DIAGNOSTIC START ===");
            logger.info("Verifying access token scopes via Google tokeninfo endpoint...");

            String tokeninfoUrl = "https://oauth2.googleapis.com/tokeninfo?access_token=" + accessToken;

            ResponseEntity<Map> response = restTemplate.getForEntity(tokeninfoUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> tokenInfo = response.getBody();

                logger.info("Token info retrieved successfully:");
                logger.info("  - Scopes: {}", tokenInfo.get("scope"));
                logger.info("  - Expires in: {} seconds", tokenInfo.get("expires_in"));
                logger.info("  - Audience (client_id): {}", tokenInfo.get("aud"));

                String scopes = (String) tokenInfo.get("scope");

                // Check for all requested scopes
                boolean hasPhotoslibrary = scopes != null && scopes.contains("auth/photoslibrary ");
                boolean hasReadonly = scopes != null && scopes.contains("photoslibrary.readonly ");
                boolean hasOriginals = scopes != null && scopes.contains("photoslibrary.readonly.originals");
                boolean hasEditAppData = scopes != null && scopes.contains("photoslibrary.edit.appcreateddata");
                boolean hasReadAppData = scopes != null && scopes.contains("photoslibrary.readonly.appcreateddata");

                logger.info("  Scope check:");
                logger.info("    - photoslibrary (full access): {}", hasPhotoslibrary ? "✓" : "✗");
                logger.info("    - photoslibrary.readonly: {}", hasReadonly ? "✓" : "✗");
                logger.info("    - photoslibrary.readonly.originals: {}", hasOriginals ? "✓" : "✗");
                logger.info("    - photoslibrary.edit.appcreateddata: {}", hasEditAppData ? "✓" : "✗");
                logger.info("    - photoslibrary.readonly.appcreateddata: {}", hasReadAppData ? "✓" : "✗");

                if (hasPhotoslibrary || (hasReadonly && hasOriginals)) {
                    logger.info("  ✓ Token has sufficient scopes for Google Photos API");
                } else {
                    logger.error("  ✗ Token may be missing required scopes!");
                    logger.error("  If you get 403 errors, revoke and re-authorize Google Photos in Settings");
                }
            } else {
                logger.warn("Failed to verify token scopes. Status: {}", response.getStatusCode());
            }
            logger.info("=== TOKEN DIAGNOSTIC END ===");
        } catch (Exception e) {
            logger.warn("Failed to verify token scopes (this is diagnostic only): {}", e.getMessage());
            logger.debug("Token verification error details:", e);
        }
    }

    /**
     * Refresh the access token using the refresh token
     */
    private String refreshAccessToken(User user) {
        String username = user.getUsername();
        logger.info("Refreshing access token for user: {}", username);

        String refreshToken = user.getGooglePhotosRefreshToken();

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            logger.error("No refresh token available for user: {}. User must re-authorize.", username);
            throw new RuntimeException("No refresh token available. Please re-authorize Google Photos in Settings.");
        }

        logger.debug("Refresh token found for user: {} (length: {} chars)", username, refreshToken.length());

        // Get Client Secret from global settings (application-wide)
        String effectiveClientSecret = globalSettingsService.getEffectiveClientSecret();

        if (effectiveClientSecret == null || effectiveClientSecret.trim().isEmpty()) {
            logger.error("Client Secret not configured. Cannot refresh token for user: {}.", username);
            throw new RuntimeException("Google Client Secret not configured. Contact your librarian to configure it in Global Settings.");
        }

        logger.debug("Using Client Secret from global settings");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", effectiveClientSecret);
            body.add("refresh_token", refreshToken);
            body.add("grant_type", "refresh_token");

            logger.debug("Sending token refresh request to: {}", tokenUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUri,
                    request,
                    Map.class
            );

            logger.info("Token refresh response status: {}", response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Token refresh failed with status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to refresh access token");
            }

            Map<String, Object> tokenResponse = response.getBody();
            logger.debug("Token refresh response contains keys: {}", tokenResponse.keySet());

            String newAccessToken = (String) tokenResponse.get("access_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            if (newAccessToken == null) {
                logger.error("Token refresh succeeded but no access_token in response");
                throw new RuntimeException("No access token in refresh response");
            }

            logger.info("Successfully refreshed access token for user: {}. New token expires in: {} seconds",
                    username, expiresIn);

            // Update user with new token
            user.setGooglePhotosApiKey(newAccessToken);
            Instant expiry = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600);
            user.setGooglePhotosTokenExpiry(expiry.toString());
            userRepository.save(user);

            logger.debug("Saved new access token for user: {}. New expiry: {}", username, expiry);

            return newAccessToken;

        } catch (Exception e) {
            logger.error("Failed to refresh access token for user: {}", username, e);
            logger.error("Error message: {}", e.getMessage());

            // Provide helpful diagnostics
            if (e.getMessage() != null) {
                if (e.getMessage().contains("invalid_grant")) {
                    logger.error("Invalid grant error. This usually means:");
                    logger.error("  1. Refresh token has expired or been revoked");
                    logger.error("  2. User revoked access from their Google account");
                    logger.error("  3. User needs to re-authorize in Settings");
                } else if (e.getMessage().contains("invalid_client")) {
                    logger.error("Invalid client error. This usually means:");
                    logger.error("  1. Client Secret is incorrect");
                    logger.error("  2. Client ID doesn't match the Client Secret");
                } else if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
                    logger.error("Unauthorized (401). Client credentials are likely incorrect.");
                }
            }

            throw new RuntimeException("Failed to refresh access token: " + e.getMessage(), e);
        }
    }
}
