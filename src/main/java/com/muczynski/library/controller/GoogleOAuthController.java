/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.User;
import com.muczynski.library.repository.UserRepository;
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

    private final RestTemplate restTemplate = new RestTemplate();

    // Store state tokens temporarily (in production, use Redis or similar)
    // Maps state token to "username:origin"
    private final Map<String, String> stateTokens = new ConcurrentHashMap<>();

    /**
     * Initiate OAuth flow - redirects user to Google consent screen
     */
    @GetMapping("/authorize")
    public RedirectView authorize(@RequestParam("origin") String origin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User must be logged in to authorize Google Photos");
        }

        // Generate state token for CSRF protection
        String state = UUID.randomUUID().toString();
        String username = authentication.getName();

        // Store both username and origin, separated by colon
        stateTokens.put(state, username + ":" + origin);

        // Build redirect URI from origin
        String redirectUri = origin + "/api/oauth/google/callback";

        // Build authorization URL
        String authUrl = authUri + "?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + scope +
                "&state=" + state +
                "&access_type=offline" + // Request refresh token
                "&prompt=consent"; // Force consent to get refresh token

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

        // Check for errors
        if (error != null) {
            return new RedirectView("/?oauth_error=" + error);
        }

        // Verify state token and extract username and origin
        String stateData = stateTokens.remove(state);
        if (stateData == null) {
            return new RedirectView("/?oauth_error=invalid_state");
        }

        // Split username and origin
        String[] parts = stateData.split(":", 2);
        if (parts.length != 2) {
            return new RedirectView("/?oauth_error=invalid_state_format");
        }
        String username = parts[0];
        String origin = parts[1];

        try {
            // Exchange authorization code for tokens
            Map<String, Object> tokenResponse = exchangeCodeForTokens(code, username, origin);

            // Extract tokens
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            if (accessToken == null) {
                return new RedirectView("/?oauth_error=no_access_token");
            }

            // Calculate token expiry
            Instant expiry = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600);
            String expiryTime = expiry.toString();

            // Save tokens to user
            User user = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setGooglePhotosApiKey(accessToken);
            if (refreshToken != null) {
                user.setGooglePhotosRefreshToken(refreshToken);
            }
            user.setGooglePhotosTokenExpiry(expiryTime);
            userRepository.save(user);

            // Redirect back to settings with success message
            return new RedirectView("/?oauth_success=true#settings");

        } catch (Exception e) {
            return new RedirectView("/?oauth_error=" + e.getMessage());
        }
    }

    /**
     * Revoke Google Photos access
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, String>> revoke() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Clear OAuth tokens
        user.setGooglePhotosApiKey("");
        user.setGooglePhotosRefreshToken("");
        user.setGooglePhotosTokenExpiry("");
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Google Photos access revoked"));
    }

    /**
     * Exchange authorization code for access and refresh tokens
     */
    private Map<String, Object> exchangeCodeForTokens(String code, String username, String origin) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userClientSecret = user.getGoogleClientSecret();

        // Fall back to environment variable if user hasn't set their own
        String effectiveClientSecret = (userClientSecret != null && !userClientSecret.trim().isEmpty())
                ? userClientSecret
                : clientSecret;

        if (effectiveClientSecret == null || effectiveClientSecret.trim().isEmpty()) {
            throw new RuntimeException("Google Client Secret not configured. Please set it in Settings or contact administrator.");
        }

        // Build redirect URI from origin (must match what was sent to Google)
        String redirectUri = origin + "/api/oauth/google/callback";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", effectiveClientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUri,
                request,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to exchange code for tokens");
        }

        return response.getBody();
    }
}
