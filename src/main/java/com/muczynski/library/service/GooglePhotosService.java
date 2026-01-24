/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;
import com.muczynski.library.exception.LibraryException;

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
        // Configure 60-second timeouts for all HTTP connections
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000); // 60 seconds
        factory.setReadTimeout(60000); // 60 seconds
        this.restTemplate.setRequestFactory(factory);
    }

    /**
     * Get authenticated user from security context
     * @return User entity
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("No authenticated user found");
        }

        // The principal name is the database user ID (not username)
        Long userId = Long.parseLong(authentication.getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));
    }


    /**
     * Download a photo's content from Google Photos
     * @param baseUrl The base URL of the photo
     * @return The photo bytes
     */
    public byte[] downloadPhoto(String baseUrl) {
        logger.debug("Downloading photo from base URL: {}", baseUrl);

        User user = getAuthenticatedUser();

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(user);

        // Add parameters to download full resolution photo
        String downloadUrl = baseUrl + "=d";

        logger.debug("Download URL: {}", downloadUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            logger.debug("Downloading photo for user ID: {}", user.getId());

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
                    throw new LibraryException("Photo download returned null");
                }
            } else {
                logger.error("Failed to download photo with status: {}", response.getStatusCode());
                throw new LibraryException("Failed to download photo: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to download photo from Google Photos for user ID: {}", user.getId(), e);
            logger.error("Error message: {}", e.getMessage());
            throw new LibraryException("Failed to download photo from Google Photos: " + e.getMessage(), e);
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

        User user = getAuthenticatedUser();

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(user);

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
            logger.error("Failed to update photo description for photo ID: {} (user ID: {})", photoId, user.getId(), e);
            logger.error("Error message: {}", e.getMessage());
            throw new LibraryException("Failed to update photo description: " + e.getMessage(), e);
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
     * @param user The user entity
     * @return Valid access token
     */
    public String getValidAccessToken(User user) {
        logger.debug("Getting valid access token for user ID: {}", user.getId());

        String accessToken = user.getGooglePhotosApiKey();
        String tokenExpiry = user.getGooglePhotosTokenExpiry();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.error("User ID {} has not authorized Google Photos. Access token is empty.", user.getId());
            throw new LibraryException("Google Photos not authorized. Please authorize in Settings.");
        }

        logger.debug("Access token found for user ID: {} (length: {} chars)", user.getId(), accessToken.length());

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
                        logger.info("Access token for user ID {} has expired. Refreshing...", user.getId());
                    } else {
                        logger.info("Access token for user ID {} will expire in {} seconds. Refreshing proactively...",
                                user.getId(), secondsUntilExpiry);
                    }
                    return refreshAccessToken(user);
                } else {
                    logger.debug("Access token is valid for user ID: {} (expires in {} seconds)", user.getId(), secondsUntilExpiry);
                }
            } catch (Exception e) {
                // If we can't parse expiry, try to refresh
                logger.warn("Failed to parse token expiry '{}' for user ID: {}. Attempting to refresh token.", tokenExpiry, user.getId());
                return refreshAccessToken(user);
            }
        } else {
            logger.warn("No token expiry timestamp for user ID: {}. Token validity unknown.", user.getId());
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

                // Check for all requested scopes (2025 API)
                boolean hasPhotoslibrary = scopes != null && scopes.contains("auth/photoslibrary");
                boolean hasReadonly = scopes != null && scopes.contains("photoslibrary.readonly");
                boolean hasOriginals = scopes != null && scopes.contains("photoslibrary.readonly.originals");
                boolean hasEditAppData = scopes != null && scopes.contains("photoslibrary.edit.appcreateddata");
                boolean hasReadAppData = scopes != null && scopes.contains("photoslibrary.readonly.appcreateddata");
                boolean hasAppendOnly = scopes != null && scopes.contains("photoslibrary.appendonly");
                boolean hasSharing = scopes != null && scopes.contains("photoslibrary.sharing");

                logger.info("  Scope check:");
                logger.info("    - photoslibrary (full access): {}", hasPhotoslibrary ? "✓" : "✗");
                logger.info("    - photoslibrary.readonly: {}", hasReadonly ? "✓" : "✗");
                logger.info("    - photoslibrary.readonly.originals: {}", hasOriginals ? "✓" : "✗");
                logger.info("    - photoslibrary.edit.appcreateddata: {}", hasEditAppData ? "✓" : "✗");
                logger.info("    - photoslibrary.readonly.appcreateddata: {}", hasReadAppData ? "✓" : "✗");
                logger.info("    - photoslibrary.appendonly: {}", hasAppendOnly ? "✓" : "✗");
                logger.info("    - photoslibrary.sharing: {}", hasSharing ? "✓" : "✗");

                // Check for 2025 required scopes for photo backup
                if (hasAppendOnly && hasReadAppData && hasEditAppData) {
                    logger.info("  ✓ Token has all required 2025 scopes for photo backup");
                } else {
                    logger.error("  ✗ Token is missing required 2025 scopes!");
                    logger.error("  Required for photo backup:");
                    logger.error("    - photoslibrary.appendonly: {} (for uploading)", hasAppendOnly ? "✓" : "✗ MISSING");
                    logger.error("    - photoslibrary.readonly.appcreateddata: {} (for reading)", hasReadAppData ? "✓" : "✗ MISSING");
                    logger.error("    - photoslibrary.edit.appcreateddata: {} (for editing)", hasEditAppData ? "✓" : "✗ MISSING");
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
     * Public method to verify token scopes (can be called from other services)
     */
    public void verifyAccessTokenScopes(String accessToken) {
        verifyTokenScopes(accessToken);
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
            throw new LibraryException("No refresh token available. Please re-authorize Google Photos in Settings.");
        }

        logger.debug("Refresh token found for user: {} (length: {} chars)", username, refreshToken.length());

        // Get Client ID and Secret from global settings (application-wide)
        String effectiveClientId = globalSettingsService.getEffectiveClientId();
        String effectiveClientSecret = globalSettingsService.getEffectiveClientSecret();

        if (effectiveClientId == null || effectiveClientId.trim().isEmpty()) {
            logger.error("Client ID not configured. Cannot refresh token for user: {}.", username);
            throw new LibraryException("Google Client ID not configured. Contact your librarian to configure it in Global Settings.");
        }

        if (effectiveClientSecret == null || effectiveClientSecret.trim().isEmpty()) {
            logger.error("Client Secret not configured. Cannot refresh token for user: {}.", username);
            throw new LibraryException("Google Client Secret not configured. Contact your librarian to configure it in Global Settings.");
        }

        logger.debug("Using Client ID and Secret from global settings");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", effectiveClientId);
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
                throw new LibraryException("Failed to refresh access token");
            }

            Map<String, Object> tokenResponse = response.getBody();
            logger.debug("Token refresh response contains keys: {}", tokenResponse.keySet());

            String newAccessToken = (String) tokenResponse.get("access_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            if (newAccessToken == null) {
                logger.error("Token refresh succeeded but no access_token in response");
                throw new LibraryException("No access token in refresh response");
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

            throw new LibraryException("Failed to refresh access token: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch media items from a Google Photos Picker session
     * @param sessionId The session ID from the Picker API
     * @return List of media items with pagination handling
     */
    public List<Map<String, Object>> fetchPickerMediaItems(String sessionId) {
        logger.info("Fetching media items from Picker session: {}", sessionId);

        User user = getAuthenticatedUser();

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(user);

        List<Map<String, Object>> allMediaItems = new ArrayList<>();
        String pageToken = null;

        try {
            do {
                // Per official API docs: GET /v1/mediaItems?sessionId={sessionId}
                // https://developers.google.com/photos/picker/reference/rest/v1/mediaItems/list
                String url = "https://photospicker.googleapis.com/v1/mediaItems?sessionId=" + sessionId;
                if (pageToken != null) {
                    url += "&pageToken=" + pageToken;
                }

                logger.info("Fetching media items from: {}", url);

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(apiKey);

                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> responseBody = response.getBody();

                    logger.info("Response from Picker API: {}", responseBody);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> mediaItems = (List<Map<String, Object>>) responseBody.get("mediaItems");

                    if (mediaItems != null && !mediaItems.isEmpty()) {
                        allMediaItems.addAll(mediaItems);
                        logger.info("Fetched {} media items in this page", mediaItems.size());
                    } else {
                        logger.warn("No media items found in response. Response keys: {}", responseBody.keySet());
                    }

                    pageToken = (String) responseBody.get("nextPageToken");
                    logger.debug("Next page token: {}", pageToken != null ? pageToken : "none");

                } else {
                    logger.error("Failed to fetch media items with status: {}", response.getStatusCode());
                    throw new LibraryException("Failed to fetch media items: " + response.getStatusCode());
                }

            } while (pageToken != null);

            logger.info("Successfully fetched {} total media items from Picker session", allMediaItems.size());
            return allMediaItems;

        } catch (Exception e) {
            logger.error("Failed to fetch media items from Picker session for user ID: {}", user.getId(), e);
            logger.error("Error message: {}", e.getMessage());
            throw new LibraryException("Failed to fetch media items from Picker: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new Google Photos Picker session
     * This routes through the backend to ensure fresh access tokens with automatic refresh
     * @return Session info map with id and pickerUri
     */
    public Map<String, Object> createPickerSession() {
        User user = getAuthenticatedUser();

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(user);

        logger.info("Creating Picker session for user ID: {}", user.getId());

        try {
            String url = "https://photospicker.googleapis.com/v1/sessions";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            // Empty JSON body as per API spec
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> session = response.getBody();
                logger.info("Successfully created Picker session: {}", session.get("id"));
                return session;
            } else {
                logger.error("Failed to create Picker session with status: {}", response.getStatusCode());
                throw new LibraryException("Failed to create Picker session: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Failed to create Picker session for user ID: {}", user.getId(), e);
            logger.error("Error message: {}", e.getMessage());
            throw new LibraryException("Failed to create Picker session: " + e.getMessage(), e);
        }
    }

    /**
     * Get the status of a Google Photos Picker session
     * @param sessionId The session ID to check
     * @return Session status map with mediaItemsSet field
     */
    public Map<String, Object> getPickerSessionStatus(String sessionId) {
        User user = getAuthenticatedUser();

        // Get valid access token (will auto-refresh if needed)
        String apiKey = getValidAccessToken(user);

        logger.debug("Checking Picker session status for user ID: {} session: {}", user.getId(), sessionId);

        try {
            String url = "https://photospicker.googleapis.com/v1/sessions/" + sessionId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                logger.error("Failed to get Picker session status with status: {}", response.getStatusCode());
                throw new LibraryException("Failed to get Picker session status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Failed to get Picker session status for user ID: {}", user.getId(), e);
            logger.error("Error message: {}", e.getMessage());
            throw new LibraryException("Failed to get Picker session status: " + e.getMessage(), e);
        }
    }

    /**
     * Download photo from a URL (from Google Photos Picker)
     * Google Photos Picker API requires:
     * 1. Append =d parameter to download the image with metadata
     * 2. Include OAuth bearer token in Authorization header
     * See: https://developers.google.com/photos/picker/guides/media-items
     */
    public byte[] downloadPhotoFromUrl(String url, String accessToken) {
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
