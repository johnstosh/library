/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

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

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/oauth/google")
public class GoogleOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthController.class);

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET:}")
    private String clientSecret;

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
     * Initiate OAuth flow - redirects user to Google consent screen
     */
    @GetMapping("/authorize")
    public RedirectView authorize(@RequestParam("origin") String origin) {
        logger.info("OAuth authorization initiated from origin: {}", origin);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("OAuth authorization attempted without authentication");
            throw new RuntimeException("User must be logged in to authorize Google Photos");
        }

        // Generate state token for CSRF protection
        String state = UUID.randomUUID().toString();
        String username = authentication.getName();

        logger.info("Generating OAuth state token for user: {}", username);

        // Store both username and origin, separated by colon
        stateTokens.put(state, username + ":" + origin);

        // Build redirect URI from origin
        String redirectUri = origin + "/api/oauth/google/callback";

        logger.info("Constructed redirect URI: {}", redirectUri);
        logger.debug("Using Client ID: {}...", clientId.substring(0, Math.min(20, clientId.length())));

        // Build authorization URL
        String authUrl = authUri + "?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + scope +
                "&state=" + state +
                "&access_type=offline" + // Request refresh token
                "&prompt=consent"; // Force consent to get refresh token

        logger.info("Redirecting user to Google consent screen");
        logger.debug("Authorization URL (without client_id): {}...", authUrl.substring(0, Math.min(100, authUrl.length())));

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

        // Split username and origin
        String[] parts = stateData.split(":", 2);
        if (parts.length != 2) {
            logger.error("Invalid state data format. Expected 'username:origin', got: {}", stateData);
            return new RedirectView("/?oauth_error=invalid_state_format");
        }
        String username = parts[0];
        String origin = parts[1];

        logger.info("State token validated for user: {} from origin: {}", username, origin);

        try {
            // Exchange authorization code for tokens
            logger.info("Exchanging authorization code for tokens for user: {}", username);
            Map<String, Object> tokenResponse = exchangeCodeForTokens(code, username, origin);

            // Extract tokens
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            if (accessToken == null) {
                logger.error("Token exchange succeeded but no access token in response");
                return new RedirectView("/?oauth_error=no_access_token");
            }

            logger.info("Successfully obtained access token for user: {}", username);
            logger.debug("Access token length: {} characters", accessToken.length());
            logger.info("Refresh token present: {}", refreshToken != null);
            logger.info("Token expires in: {} seconds", expiresIn);

            // Calculate token expiry
            Instant expiry = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600);
            String expiryTime = expiry.toString();

            logger.debug("Token expiry timestamp: {}", expiryTime);

            // Save tokens to user
            User user = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setGooglePhotosApiKey(accessToken);
            if (refreshToken != null) {
                user.setGooglePhotosRefreshToken(refreshToken);
                logger.info("Saved refresh token for user: {}", username);
            } else {
                logger.warn("No refresh token received for user: {}. User may need to re-authorize when token expires.", username);
            }
            user.setGooglePhotosTokenExpiry(expiryTime);
            userRepository.save(user);

            logger.info("OAuth authorization completed successfully for user: {}", username);

            // Redirect back to settings with success message
            return new RedirectView("/?oauth_success=true#settings");

        } catch (Exception e) {
            logger.error("OAuth callback failed for user: {}", username, e);
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.error("Revoke attempted without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String username = authentication.getName();
        logger.info("Revoking Google Photos access for user: {}", username);

        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hadAccessToken = !user.getGooglePhotosApiKey().isEmpty();
        boolean hadRefreshToken = !user.getGooglePhotosRefreshToken().isEmpty();

        // Clear OAuth tokens
        user.setGooglePhotosApiKey("");
        user.setGooglePhotosRefreshToken("");
        user.setGooglePhotosTokenExpiry("");
        userRepository.save(user);

        logger.info("Successfully revoked Google Photos access for user: {} (had access token: {}, had refresh token: {})",
                username, hadAccessToken, hadRefreshToken);

        return ResponseEntity.ok(Map.of("message", "Google Photos access revoked"));
    }

    /**
     * Exchange authorization code for access and refresh tokens
     */
    private Map<String, Object> exchangeCodeForTokens(String code, String username, String origin) {
        logger.debug("Exchanging authorization code for tokens (user: {}, origin: {})", username, origin);

        // Get Client Secret from global settings (application-wide)
        String effectiveClientSecret = globalSettingsService.getEffectiveClientSecret();

        if (effectiveClientSecret == null || effectiveClientSecret.trim().isEmpty()) {
            logger.error("Client Secret not configured. " +
                    "Librarian must set Client Secret in Global Settings or admin must set GOOGLE_CLIENT_SECRET environment variable.");
            throw new RuntimeException("Google Client Secret not configured. Contact your librarian to configure it in Global Settings.");
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
        body.add("client_id", clientId);
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
                throw new RuntimeException("Failed to exchange code for tokens");
            }

            Map<String, Object> responseBody = response.getBody();
            logger.debug("Token exchange response contains keys: {}", responseBody.keySet());

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

                throw new RuntimeException(error + ": " + errorDescription);
            }

            logger.info("Token exchange successful for user: {}", username);
            return responseBody;

        } catch (Exception e) {
            logger.error("Exception during token exchange for user: {}", username, e);
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

            throw new RuntimeException("Token exchange failed: " + e.getMessage(), e);
        }
    }
}
