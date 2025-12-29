/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.User;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.GlobalSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/oauth/google")
public class GoogleOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthController.class);

    @Value("${google.oauth.auth-uri}")
    private String authUri;

    @Value("${google.oauth.token-uri}")
    private String tokenUri;

    @Value("${google.oauth.scope}")
    private String scope;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GlobalSettingsService globalSettingsService;

    private final RestTemplate restTemplate = new RestTemplate();

    // Store state tokens temporarily (in production, use Redis or similar)
    // Maps state token to "username:origin"
    private final Map<String, String> stateTokens = new ConcurrentHashMap<>();

    /**
     * Get authenticated user ID from security context
     * @return User ID
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LibraryException("User must be logged in to authorize Google Photos");
        }
        // The principal name is the database user ID (not username)
        return Long.parseLong(authentication.getName());
    }

    /**
     * Initiate OAuth flow - redirects user to Google consent screen
     */
    @GetMapping("/authorize")
    public RedirectView authorize(@RequestParam("origin") String origin) {
        logger.info("OAuth authorization initiated from origin: {}", origin);

        Long userId = getAuthenticatedUserId();

        // Get effective client ID from global settings
        String effectiveClientId = globalSettingsService.getEffectiveClientId();
        if (effectiveClientId == null || effectiveClientId.trim().isEmpty()) {
            logger.error("Client ID not configured. Librarian must set Client ID in Global Settings.");
            throw new LibraryException("Google Client ID not configured. Contact your librarian to configure it in Global Settings.");
        }

        // Generate state token for CSRF protection
        String state = UUID.randomUUID().toString();

        logger.info("Generating OAuth state token for user ID: {}", userId);

        // Store both userId and origin, separated by colon
        stateTokens.put(state, userId + ":" + origin);

        // Build redirect URI from origin
        String redirectUri = origin + "/api/oauth/google/callback";

        logger.info("Constructed redirect URI: {}", redirectUri);
        logger.debug("Using Client ID: {}...", effectiveClientId.substring(0, Math.min(20, effectiveClientId.length())));

        logger.info("===== REQUESTED SCOPES =====");
        logger.info("Scopes being requested: {}", scope);
        String[] requestedScopes = scope.split("\\s+");
        logger.info("Number of scopes requested: {}", requestedScopes.length);
        for (String requestedScope : requestedScopes) {
            logger.info("  - {}", requestedScope);
        }
        logger.info("===== END REQUESTED SCOPES =====");

        // Build authorization URL with proper URL encoding
        String authUrl = UriComponentsBuilder.fromHttpUrl(authUri)
                .queryParam("client_id", effectiveClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("access_type", "offline") // Request refresh token
                .queryParam("prompt", "consent") // Force consent to get refresh token
                .build()
                .toUriString();

        logger.info("Redirecting user to Google consent screen");
        logger.debug("Authorization URL (first 150 chars): {}...", authUrl.substring(0, Math.min(150, authUrl.length())));

        return new RedirectView(authUrl);
    }

    /**
     * Handle OAuth callback from Google
     */
    @GetMapping("/callback")
    public RedirectView callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @RequestParam(value = "error", required = false) String error) {

        logger.info("OAuth callback received with state: {}", state.substring(0, Math.min(8, state.length())) + "...");

        // Check for errors
        if (error != null) {
            logger.error("OAuth callback received error from Google: {}", error);
            return new RedirectView("/?oauth_error=" + error);
        }

        logger.debug("OAuth callback received authorization code: {}...", code.substring(0, Math.min(10, code.length())));

        // Verify state token and extract username and origin
        String stateData = stateTokens.remove(state);
        if (stateData == null) {
            logger.error("Invalid or expired state token: {}. Possible CSRF attack or expired session.",
                    state.substring(0, Math.min(8, state.length())) + "...");
            logger.warn("State token not found in cache. Current cache size: {}. " +
                    "This can happen if the app was restarted between authorization and callback.",
                    stateTokens.size());
            return new RedirectView("/?oauth_error=invalid_state");
        }

        // Split userId and origin
        String[] parts = stateData.split(":", 2);
        if (parts.length != 2) {
            logger.error("Invalid state data format. Expected 'userId:origin', got: {}", stateData);
            return new RedirectView("/?oauth_error=invalid_state_format");
        }
        Long userId = Long.parseLong(parts[0]);
        String origin = parts[1];

        logger.info("State token validated for user ID: {} from origin: {}", userId, origin);

        try {
            // Exchange authorization code for tokens
            logger.info("Exchanging authorization code for tokens for user ID: {}", userId);
            Map<String, Object> tokenResponse = exchangeCodeForTokens(code, userId, origin);

            // Extract tokens
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            if (accessToken == null) {
                logger.error("Token exchange succeeded but no access token in response");
                return new RedirectView("/?oauth_error=no_access_token");
            }

            logger.info("Successfully obtained access token for user ID: {}", userId);
            logger.debug("Access token length: {} characters", accessToken.length());
            logger.info("Refresh token present: {}", refreshToken != null);
            logger.info("Token expires in: {} seconds", expiresIn);

            // Calculate token expiry
            Instant expiry = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600);
            String expiryTime = expiry.toString();

            logger.debug("Token expiry timestamp: {}", expiryTime);

            // Save tokens to user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new LibraryException("User not found"));

            user.setGooglePhotosApiKey(accessToken);
            if (refreshToken != null) {
                user.setGooglePhotosRefreshToken(refreshToken);
                logger.info("Saved refresh token for user ID: {}", userId);
            } else {
                logger.warn("No refresh token received for user ID: {}. User may need to re-authorize when token expires.", userId);
            }
            user.setGooglePhotosTokenExpiry(expiryTime);
            userRepository.save(user);

            logger.info("OAuth authorization completed successfully for user ID: {}", userId);

            // Redirect back to settings with success message
            return new RedirectView("/?oauth_success=true#settings");

        } catch (Exception e) {
            logger.error("OAuth callback failed for user ID: {}", userId, e);
            logger.error("Error message: {}", e.getMessage());
            return new RedirectView("/?oauth_error=" + e.getMessage());
        }
    }

    /**
     * Revoke Google Photos access
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, String>> revoke() {
        logger.info("Revoke Google Photos access requested");

        Long userId = getAuthenticatedUserId();
        logger.info("Revoking Google Photos access for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LibraryException("User not found"));

        // Null-safe checks for token presence
        boolean hadAccessToken = user.getGooglePhotosApiKey() != null && !user.getGooglePhotosApiKey().isEmpty();
        boolean hadRefreshToken = user.getGooglePhotosRefreshToken() != null && !user.getGooglePhotosRefreshToken().isEmpty();

        // Clear OAuth tokens
        user.setGooglePhotosApiKey("");
        user.setGooglePhotosRefreshToken("");
        user.setGooglePhotosTokenExpiry("");
        userRepository.save(user);

        logger.info("Successfully revoked Google Photos access for user ID: {} (had access token: {}, had refresh token: {})",
                userId, hadAccessToken, hadRefreshToken);

        return ResponseEntity.ok(Map.of("message", "Google Photos access revoked"));
    }

    /**
     * Exchange authorization code for access and refresh tokens
     */
    private Map<String, Object> exchangeCodeForTokens(String code, Long userId, String origin) {
        logger.debug("Exchanging authorization code for tokens (user ID: {}, origin: {})", userId, origin);

        // Get Client ID and Client Secret from global settings (application-wide)
        String effectiveClientId = globalSettingsService.getEffectiveClientId();
        String effectiveClientSecret = globalSettingsService.getEffectiveClientSecret();

        if (effectiveClientId == null || effectiveClientId.trim().isEmpty()) {
            logger.error("Client ID not configured. " +
                    "Librarian must set Client ID in Global Settings.");
            throw new LibraryException("Google Client ID not configured. Contact your librarian to configure it in Global Settings.");
        }

        if (effectiveClientSecret == null || effectiveClientSecret.trim().isEmpty()) {
            logger.error("Client Secret not configured. " +
                    "Librarian must set Client Secret in Global Settings or admin must set GOOGLE_CLIENT_SECRET environment variable.");
            throw new LibraryException("Google Client Secret not configured. Contact your librarian to configure it in Global Settings.");
        }

        // Validate Client Secret format
        if (effectiveClientSecret.length() < 20) {
            logger.warn("Client Secret is suspiciously short ({} characters). Expected 30+ characters.",
                    effectiveClientSecret.length());
        }

        if (!effectiveClientSecret.startsWith("GOCSPX-")) {
            logger.warn("Client Secret does not start with 'GOCSPX-'. " +
                    "This may not be a valid Google OAuth client secret.");
        }

        logger.debug("Using Client Secret from global settings");
        if (effectiveClientSecret.length() >= 4) {
            logger.debug("Client Secret last 4 chars: ...{}", effectiveClientSecret.substring(effectiveClientSecret.length() - 4));
        }

        // Build redirect URI from origin (must match what was sent to Google)
        String redirectUri = origin + "/api/oauth/google/callback";

        logger.info("Token exchange using redirect URI: {}", redirectUri);
        logger.debug("Token URI: {}", tokenUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", effectiveClientId);
        body.add("client_secret", effectiveClientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        logger.debug("Token exchange request prepared with grant_type=authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            logger.info("Sending token exchange request to Google...");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUri,
                    request,
                    Map.class
            );

            logger.info("Token exchange response status: {}", response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Token exchange failed with status: {} (expected 2xx)", response.getStatusCode());
                throw new LibraryException("Failed to exchange code for tokens");
            }

            Map<String, Object> responseBody = response.getBody();
            logger.debug("Token exchange response contains keys: {}", responseBody.keySet());

            // Log the granted scopes
            if (responseBody.containsKey("scope")) {
                String grantedScopes = (String) responseBody.get("scope");
                logger.info("===== GRANTED SCOPES =====");
                logger.info("Scopes returned by Google: {}", grantedScopes);

                // Check each required scope
                String[] requiredScopes = {
                    "https://www.googleapis.com/auth/photoslibrary",
                    "https://www.googleapis.com/auth/photoslibrary.readonly",
                    "https://www.googleapis.com/auth/photoslibrary.readonly.originals",
                    "https://www.googleapis.com/auth/photoslibrary.edit.appcreateddata",
                    "https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata",
                    "https://www.googleapis.com/auth/photospicker.mediaitems.readonly",
                    "https://www.googleapis.com/auth/photoslibrary.appendonly",
                    "https://www.googleapis.com/auth/photoslibrary.sharing"
                };

                logger.info("Checking for required scopes:");
                for (String requiredScope : requiredScopes) {
                    boolean hasScope = grantedScopes != null && grantedScopes.contains(requiredScope);
                    logger.info("  {} {}", hasScope ? "✓" : "✗", requiredScope);
                }
                logger.info("===== END GRANTED SCOPES =====");
            } else {
                logger.warn("Token response does not contain 'scope' field!");
            }

            if (responseBody.containsKey("error")) {
                String error = (String) responseBody.get("error");
                String errorDescription = (String) responseBody.getOrDefault("error_description", "No description");
                logger.error("Token exchange returned error: {} - {}", error, errorDescription);

                // Provide helpful diagnostics
                if ("invalid_client".equals(error)) {
                    logger.error("Invalid client error. This usually means:");
                    logger.error("  1. Client Secret is incorrect or from a different OAuth client");
                    logger.error("  2. Client ID doesn't match the Client Secret");
                    logger.error("  3. OAuth client was deleted or disabled in Google Cloud Console");
                }
                if ("redirect_uri_mismatch".equals(error)) {
                    logger.error("Redirect URI mismatch. The URI '{}' is not authorized in Google Cloud Console.", redirectUri);
                    logger.error("Add this exact URI to 'Authorized redirect URIs' in your OAuth client configuration.");
                }

                throw new LibraryException(error + ": " + errorDescription);
            }

            logger.info("Token exchange successful for user ID: {}", userId);
            return responseBody;

        } catch (Exception e) {
            logger.error("Exception during token exchange for user ID: {}", userId, e);
            logger.error("Error type: {}", e.getClass().getSimpleName());
            logger.error("Error message: {}", e.getMessage());

            // Check for common network errors
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Connection refused")) {
                    logger.error("Connection refused to Google token endpoint. Check network connectivity and firewall rules.");
                } else if (e.getMessage().contains("timeout")) {
                    logger.error("Timeout connecting to Google token endpoint. Check network connectivity.");
                } else if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
                    logger.error("Unauthorized (401). Client Secret is likely incorrect.");
                } else if (e.getMessage().contains("400") || e.getMessage().contains("Bad Request")) {
                    logger.error("Bad Request (400). Check that all required parameters are correct.");
                }
            }

            throw new LibraryException("Token exchange failed: " + e.getMessage(), e);
        }
    }
}
