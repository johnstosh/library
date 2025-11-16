/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;
import com.muczynski.library.exception.LibraryException;

import com.muczynski.library.domain.User;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic controller to test various Google Photos API endpoints
 * to determine which ones work and which don't
 */
@RestController
@RequestMapping("/api/diagnostic/google-photos")
@PreAuthorize("hasAuthority('LIBRARIAN')")
public class GooglePhotosDiagnosticController {

    private static final Logger logger = LoggerFactory.getLogger(GooglePhotosDiagnosticController.class);

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Test basic authentication by getting token info
     */
    @GetMapping("/test-token")
    public ResponseEntity<?> testToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new LibraryException("User not found"));

            String accessToken = user.getGooglePhotosApiKey();

            if (accessToken == null || accessToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No access token found"));
            }

            // Use POST with token in body instead of URL parameter for security
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "access_token=" + accessToken;
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/tokeninfo",
                    entity,
                    Map.class
            );

            return ResponseEntity.ok(Map.of(
                    "status", "Token info retrieved successfully",
                    "tokenInfo", response.getBody()
            ));
        } catch (Exception e) {
            logger.error("Token test failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Test 1: List albums (simple GET request)
     */
    @GetMapping("/test-albums")
    public ResponseEntity<?> testListAlbums() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new LibraryException("User not found"));

            String accessToken = user.getGooglePhotosApiKey();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            logger.info("=== TEST: List Albums ===");
            logger.info("GET https://photoslibrary.googleapis.com/v1/albums");

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://photoslibrary.googleapis.com/v1/albums",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            logger.info("Albums test SUCCESS: {}", response.getStatusCode());
            return ResponseEntity.ok(Map.of(
                    "test", "List Albums",
                    "status", "SUCCESS",
                    "statusCode", response.getStatusCode().value(),
                    "response", response.getBody()
            ));
        } catch (Exception e) {
            logger.error("Albums test FAILED", e);
            return ResponseEntity.status(500).body(Map.of(
                    "test", "List Albums",
                    "status", "FAILED",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Test 2: Search media items WITHOUT filters (simplest search)
     */
    @PostMapping("/test-search-simple")
    public ResponseEntity<?> testSearchSimple() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new LibraryException("User not found"));

            String accessToken = user.getGooglePhotosApiKey();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("pageSize", 10);
            // NO FILTERS - just a simple search

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            logger.info("=== TEST: Search Media Items (NO FILTERS) ===");
            logger.info("POST https://photoslibrary.googleapis.com/v1/mediaItems:search");
            logger.info("Request: {}", request);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://photoslibrary.googleapis.com/v1/mediaItems:search",
                    entity,
                    Map.class
            );

            logger.info("Simple search test SUCCESS: {}", response.getStatusCode());
            return ResponseEntity.ok(Map.of(
                    "test", "Search Media Items (no filters)",
                    "status", "SUCCESS",
                    "statusCode", response.getStatusCode().value(),
                    "response", response.getBody()
            ));
        } catch (Exception e) {
            logger.error("Simple search test FAILED", e);
            return ResponseEntity.status(500).body(Map.of(
                    "test", "Search Media Items (no filters)",
                    "status", "FAILED",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Test 3: Search with date filter (what we're actually trying to do)
     */
    @PostMapping("/test-search-with-date-filter")
    public ResponseEntity<?> testSearchWithDateFilter() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new LibraryException("User not found"));

            String accessToken = user.getGooglePhotosApiKey();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> startDate = new HashMap<>();
            startDate.put("year", 2025);
            startDate.put("month", 11);
            startDate.put("day", 13);

            Map<String, Object> endDate = new HashMap<>();
            endDate.put("year", 2025);
            endDate.put("month", 11);
            endDate.put("day", 14);

            Map<String, Object> dateRange = new HashMap<>();
            dateRange.put("startDate", startDate);
            dateRange.put("endDate", endDate);

            Map<String, Object> dateFilter = new HashMap<>();
            dateFilter.put("ranges", java.util.List.of(dateRange));

            Map<String, Object> filters = new HashMap<>();
            filters.put("dateFilter", dateFilter);

            Map<String, Object> request = new HashMap<>();
            request.put("pageSize", 10);
            request.put("filters", filters);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            logger.info("=== TEST: Search Media Items (WITH DATE FILTER) ===");
            logger.info("POST https://photoslibrary.googleapis.com/v1/mediaItems:search");
            logger.info("Request: {}", request);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://photoslibrary.googleapis.com/v1/mediaItems:search",
                    entity,
                    Map.class
            );

            logger.info("Date filter search test SUCCESS: {}", response.getStatusCode());
            return ResponseEntity.ok(Map.of(
                    "test", "Search Media Items (with date filter)",
                    "status", "SUCCESS",
                    "statusCode", response.getStatusCode().value(),
                    "response", response.getBody()
            ));
        } catch (Exception e) {
            logger.error("Date filter search test FAILED", e);
            return ResponseEntity.status(500).body(Map.of(
                    "test", "Search Media Items (with date filter)",
                    "status", "FAILED",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Run all diagnostic tests
     */
    @GetMapping("/test-all")
    public ResponseEntity<?> testAll() {
        Map<String, Object> results = new HashMap<>();

        // Test 1: Token info
        try {
            ResponseEntity<?> tokenResult = testToken();
            results.put("1_token_info", Map.of(
                    "status", tokenResult.getStatusCode().is2xxSuccessful() ? "PASS" : "FAIL",
                    "details", tokenResult.getBody()
            ));
        } catch (Exception e) {
            results.put("1_token_info", Map.of("status", "FAIL", "error", e.getMessage()));
        }

        // Test 2: List albums
        try {
            ResponseEntity<?> albumsResult = testListAlbums();
            results.put("2_list_albums", Map.of(
                    "status", albumsResult.getStatusCode().is2xxSuccessful() ? "PASS" : "FAIL",
                    "details", albumsResult.getBody()
            ));
        } catch (Exception e) {
            results.put("2_list_albums", Map.of("status", "FAIL", "error", e.getMessage()));
        }

        // Test 3: Simple search (no filters)
        try {
            ResponseEntity<?> simpleSearchResult = testSearchSimple();
            results.put("3_search_no_filters", Map.of(
                    "status", simpleSearchResult.getStatusCode().is2xxSuccessful() ? "PASS" : "FAIL",
                    "details", simpleSearchResult.getBody()
            ));
        } catch (Exception e) {
            results.put("3_search_no_filters", Map.of("status", "FAIL", "error", e.getMessage()));
        }

        // Test 4: Search with date filter
        try {
            ResponseEntity<?> dateFilterResult = testSearchWithDateFilter();
            results.put("4_search_with_date_filter", Map.of(
                    "status", dateFilterResult.getStatusCode().is2xxSuccessful() ? "PASS" : "FAIL",
                    "details", dateFilterResult.getBody()
            ));
        } catch (Exception e) {
            results.put("4_search_with_date_filter", Map.of("status", "FAIL", "error", e.getMessage()));
        }

        return ResponseEntity.ok(results);
    }
}
