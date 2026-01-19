/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.User;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.GlobalSettingsService;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * API Integration Tests for GoogleOAuthController using RestAssured
 *
 * Tests REST endpoints with actual HTTP requests according to backend-development-requirements.md
 *
 * Note: OAuth flow involves redirects which are difficult to test in unit/integration tests.
 * Full OAuth flow should be tested in UI tests or manual testing.
 * These tests focus on authorization checks and error handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleOAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private GlobalSettingsService globalSettingsService;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        // Mock GlobalSettingsService to return test OAuth configuration
        when(globalSettingsService.getEffectiveClientId()).thenReturn("test-client-id");
        when(globalSettingsService.getEffectiveClientSecret()).thenReturn("test-client-secret");
    }

    // ==================== GET /api/oauth/google/authorize Tests ====================

    @Test
    @WithMockUser(username = "1")
    void testAuthorize_Success() {
        // Act & Assert - Should redirect to Google (302 Found)
        given()
            .param("origin", "http://localhost:8080")
            .auth().none()
        .when()
            .get("/api/oauth/google/authorize")
        .then()
            .statusCode(302) // Redirect
            .header("Location", containsString("accounts.google.com"));
    }

    @Test
    @WithMockUser(username = "1")
    void testAuthorize_ContainsAllRequiredScopes() {
        // Act - Get the redirect Location header
        String locationHeader = given()
            .param("origin", "http://localhost:8080")
            .auth().none()
        .when()
            .get("/api/oauth/google/authorize")
        .then()
            .statusCode(302)
            .extract()
            .header("Location");

        // Assert - Verify all 5 Google Photos scopes are present in the authorization URL
        // These scopes are required for the Google Photos Picker API
        org.junit.jupiter.api.Assertions.assertAll(
            () -> org.junit.jupiter.api.Assertions.assertTrue(locationHeader.contains("photoslibrary.edit.appcreateddata"),
                "Should contain photoslibrary.edit.appcreateddata scope"),
            () -> org.junit.jupiter.api.Assertions.assertTrue(locationHeader.contains("photoslibrary.readonly.appcreateddata"),
                "Should contain photoslibrary.readonly.appcreateddata scope"),
            () -> org.junit.jupiter.api.Assertions.assertTrue(locationHeader.contains("photospicker.mediaitems.readonly"),
                "Should contain photospicker.mediaitems.readonly scope"),
            () -> org.junit.jupiter.api.Assertions.assertTrue(locationHeader.contains("photoslibrary.appendonly"),
                "Should contain photoslibrary.appendonly scope"),
            () -> org.junit.jupiter.api.Assertions.assertTrue(locationHeader.contains("photoslibrary.sharing"),
                "Should contain photoslibrary.sharing scope")
        );
    }

    @Test
    void testAuthorize_Unauthorized() {
        // Act & Assert - No authentication - Returns 400 Bad Request
        given()
            .param("origin", "http://localhost:8080")
        .when()
            .get("/api/oauth/google/authorize")
        .then()
            .statusCode(400); // Bad Request (authentication failure)
    }

    @Test
    @WithMockUser(username = "1")
    void testAuthorize_MissingOriginParameter() {
        // Act & Assert - Missing required origin parameter
        given()
            .auth().none()
        .when()
            .get("/api/oauth/google/authorize")
        .then()
            .statusCode(400); // Bad Request
    }

    // ==================== GET /api/oauth/google/callback Tests ====================

    // Note: OAuth callback tests are complex and better suited for UI/integration tests
    // The callback endpoint handles redirects from Google which are difficult to test
    // in unit tests. These are better tested through full OAuth flow in UI tests.

    // ==================== POST /api/oauth/google/revoke Tests ====================

    @Test
    @WithMockUser(username = "1")
    void testRevoke_Success() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setGooglePhotosApiKey("existing-token");
        user.setGooglePhotosRefreshToken("refresh-token");
        user.setGooglePhotosTokenExpiry("2025-12-31T23:59:59Z");

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act & Assert
        given()
            .auth().none()
        .when()
            .post("/api/oauth/google/revoke")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("message", equalTo("Google Photos access revoked"));

        // Verify tokens were cleared
        verify(userRepository).save(argThat(u ->
            u.getGooglePhotosApiKey().isEmpty() &&
            u.getGooglePhotosRefreshToken().isEmpty() &&
            u.getGooglePhotosTokenExpiry().isEmpty()
        ));
    }

    @Test
    void testRevoke_Unauthorized() {
        // Act & Assert - No authentication
        given()
        .when()
            .post("/api/oauth/google/revoke")
        .then()
            .statusCode(401);
    }

    @Test
    @WithMockUser(username = "999")
    void testRevoke_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L))
            .thenReturn(Optional.empty());

        // Act & Assert - LibraryException returns 422 (Unprocessable Entity)
        given()
            .auth().none()
        .when()
            .post("/api/oauth/google/revoke")
        .then()
            .statusCode(422); // Unprocessable Entity (LibraryException)
    }

    @Test
    @WithMockUser(username = "1")
    void testRevoke_AlreadyRevoked() {
        // Arrange - User with no tokens
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setGooglePhotosApiKey("");
        user.setGooglePhotosRefreshToken("");
        user.setGooglePhotosTokenExpiry("");

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act & Assert - Should still succeed
        given()
            .auth().none()
        .when()
            .post("/api/oauth/google/revoke")
        .then()
            .statusCode(200)
            .body("message", equalTo("Google Photos access revoked"));
    }
}
