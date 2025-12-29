/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.GlobalSettingsDto;
import com.muczynski.library.service.GlobalSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Integration Tests for GlobalSettingsController
 *
 * Tests REST endpoints with actual HTTP requests according to backend-development-requirements.md
 * Each endpoint should have:
 * - One test for successful request (2xx status)
 * - One test for unauthorized access (401/403 status)
 * - One test for invalid input (400 status) where applicable
 *
 * This test simulates the sequence of API calls that the UI would make:
 * 1. GET to load current settings
 * 2. PUT to update settings (librarian-only)
 * 3. GET again to verify persistence
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GlobalSettingsService globalSettingsService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET /api/global-settings Tests ====================

    @Test
    void testGetGlobalSettings_Success_AsLibrarian() throws Exception {
        // Arrange
        GlobalSettingsDto dto = new GlobalSettingsDto();
        dto.setGoogleClientId("test-client-id-123.apps.googleusercontent.com");
        dto.setRedirectUri("https://library.muczynskifamily.com/api/oauth/google/callback");
        dto.setGoogleClientSecretPartial("...uXnb");
        dto.setGoogleClientSecretConfigured(true);
        dto.setGoogleClientSecretValidation("Valid");
        dto.setGoogleClientSecretUpdatedAt(Instant.parse("2025-01-15T10:30:00Z"));
        dto.setLastUpdated(Instant.parse("2025-01-15T10:30:00Z"));

        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/global-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientId").value("test-client-id-123.apps.googleusercontent.com"))
                .andExpect(jsonPath("$.redirectUri").value("https://library.muczynskifamily.com/api/oauth/google/callback"))
                .andExpect(jsonPath("$.googleClientSecretPartial").value("...uXnb"))
                .andExpect(jsonPath("$.googleClientSecretConfigured").value(true))
                .andExpect(jsonPath("$.googleClientSecretValidation").value("Valid"))
                .andExpect(jsonPath("$.googleClientSecretUpdatedAt").exists())
                .andExpect(jsonPath("$.lastUpdated").exists());
    }

    @Test
    void testGetGlobalSettings_Forbidden_AsRegularUser() throws Exception {
        // Act & Assert - Regular users should get 403 Forbidden (librarian-only endpoint)
        mockMvc.perform(get("/api/global-settings")
                        .with(user("regularuser").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetGlobalSettings_NotConfigured() throws Exception {
        // Arrange - Test when Client Secret is not configured
        GlobalSettingsDto dto = new GlobalSettingsDto();
        dto.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        dto.setRedirectUri("https://library.muczynskifamily.com/api/oauth/google/callback");
        dto.setGoogleClientSecretPartial("(not configured)");
        dto.setGoogleClientSecretConfigured(false);
        dto.setGoogleClientSecretValidation("Client Secret not configured");

        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/global-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientSecretPartial").value("(not configured)"))
                .andExpect(jsonPath("$.googleClientSecretConfigured").value(false))
                .andExpect(jsonPath("$.googleClientSecretValidation").value("Client Secret not configured"));
    }

    @Test
    void testGetGlobalSettings_Unauthorized() throws Exception {
        // Act & Assert - No authentication
        mockMvc.perform(get("/api/global-settings"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PUT /api/global-settings Tests ====================

    @Test
    void testUpdateGlobalSettings_Success_AsLibrarian() throws Exception {
        // Arrange
        GlobalSettingsDto requestDto = new GlobalSettingsDto();
        requestDto.setGoogleClientSecret("GOCSPX-newSecretValue123456789");

        GlobalSettingsDto responseDto = new GlobalSettingsDto();
        responseDto.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        responseDto.setRedirectUri("https://library.muczynskifamily.com/api/oauth/google/callback");
        responseDto.setGoogleClientSecretPartial("...789");
        responseDto.setGoogleClientSecretConfigured(true);
        responseDto.setGoogleClientSecretValidation("Valid");
        responseDto.setGoogleClientSecretUpdatedAt(Instant.now());
        responseDto.setLastUpdated(Instant.now());

        when(globalSettingsService.updateGlobalSettings(any(GlobalSettingsDto.class)))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(put("/api/global-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientSecretPartial").value("...789"))
                .andExpect(jsonPath("$.googleClientSecretConfigured").value(true))
                .andExpect(jsonPath("$.googleClientSecretValidation").value("Valid"))
                .andExpect(jsonPath("$.googleClientSecretUpdatedAt").exists());
    }

    @Test
    void testUpdateGlobalSettings_WithFormatWarning() throws Exception {
        // Arrange - Client Secret without GOCSPX- prefix should show warning
        GlobalSettingsDto requestDto = new GlobalSettingsDto();
        requestDto.setGoogleClientSecret("invalid-format-secret-123456789");

        GlobalSettingsDto responseDto = new GlobalSettingsDto();
        responseDto.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        responseDto.setRedirectUri("https://library.muczynskifamily.com/api/oauth/google/callback");
        responseDto.setGoogleClientSecretPartial("...789");
        responseDto.setGoogleClientSecretConfigured(true);
        responseDto.setGoogleClientSecretValidation("Warning: Client Secret does not start with 'GOCSPX-'. This may not be a valid Google OAuth client secret.");
        responseDto.setGoogleClientSecretUpdatedAt(Instant.now());

        when(globalSettingsService.updateGlobalSettings(any(GlobalSettingsDto.class)))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(put("/api/global-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientSecretValidation").value(org.hamcrest.Matchers.containsString("Warning")));
    }

    @Test
    void testUpdateGlobalSettings_Forbidden_AsRegularUser() throws Exception {
        // Arrange
        GlobalSettingsDto requestDto = new GlobalSettingsDto();
        requestDto.setGoogleClientSecret("GOCSPX-attemptedUpdate123456789");

        // Act & Assert - Regular users should get 403 Forbidden
        mockMvc.perform(put("/api/global-settings")
                        .with(user("regularuser").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateGlobalSettings_Unauthorized() throws Exception {
        // Arrange
        GlobalSettingsDto requestDto = new GlobalSettingsDto();
        requestDto.setGoogleClientSecret("GOCSPX-unauthorized123456789");

        // Act & Assert - No authentication
        mockMvc.perform(put("/api/global-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== UI Workflow Sequence Test ====================

    @Test
    void testUIWorkflowSequence_LoadUpdateAndVerify() throws Exception {
        // This test simulates the exact sequence of API calls the UI makes:
        // 1. Load initial settings (GET)
        // 2. Update Client Secret (PUT)
        // 3. Load settings again to verify (GET)

        // Step 1: Initial load (as done by loadGlobalSettings() in global-settings.js)
        GlobalSettingsDto initialDto = new GlobalSettingsDto();
        initialDto.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        initialDto.setRedirectUri("https://library.muczynskifamily.com/api/oauth/google/callback");
        initialDto.setGoogleClientSecretPartial("...oldValue");
        initialDto.setGoogleClientSecretConfigured(true);
        initialDto.setGoogleClientSecretValidation("Valid");
        initialDto.setGoogleClientSecretUpdatedAt(Instant.parse("2025-01-15T09:00:00Z"));

        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(initialDto);

        mockMvc.perform(get("/api/global-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientSecretPartial").value("...oldValue"));

        // Step 2: Update settings (as done by saveGlobalSettings() in global-settings.js)
        GlobalSettingsDto updateDto = new GlobalSettingsDto();
        updateDto.setGoogleClientSecret("GOCSPX-newSecretValue123456789");

        GlobalSettingsDto updatedDto = new GlobalSettingsDto();
        updatedDto.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        updatedDto.setRedirectUri("https://library.muczynskifamily.com/api/oauth/google/callback");
        updatedDto.setGoogleClientSecretPartial("...789");
        updatedDto.setGoogleClientSecretConfigured(true);
        updatedDto.setGoogleClientSecretValidation("Valid");
        updatedDto.setGoogleClientSecretUpdatedAt(Instant.now());
        updatedDto.setLastUpdated(Instant.now());

        when(globalSettingsService.updateGlobalSettings(any(GlobalSettingsDto.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(put("/api/global-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientSecretPartial").value("...789"))
                .andExpect(jsonPath("$.googleClientSecretValidation").value("Valid"));

        // Step 3: Verify persistence (reload to confirm update persisted)
        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(updatedDto);

        mockMvc.perform(get("/api/global-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientSecretPartial").value("...789"))
                .andExpect(jsonPath("$.googleClientSecretConfigured").value(true))
                .andExpect(jsonPath("$.googleClientSecretValidation").value("Valid"))
                .andExpect(jsonPath("$.googleClientSecretUpdatedAt").exists());
    }

    @Test
    void testUIWorkflowSequence_RegularUserCannotViewOrUpdate() throws Exception {
        // This test verifies that regular users cannot view or update settings (librarian-only feature)

        // Step 1: Regular user attempts to load settings (should fail with 403)
        mockMvc.perform(get("/api/global-settings")
                        .with(user("regularuser").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isForbidden());

        // Step 2: Regular user attempts to update settings (should also fail with 403)
        GlobalSettingsDto updateDto = new GlobalSettingsDto();
        updateDto.setGoogleClientSecret("GOCSPX-attemptedUpdate123456789");

        mockMvc.perform(put("/api/global-settings")
                        .with(user("regularuser").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSecretNeverReturnedInResponse() throws Exception {
        // Verify that the full Client Secret is never included in GET responses
        GlobalSettingsDto dto = new GlobalSettingsDto();
        dto.setGoogleClientId("test-client-id.apps.googleusercontent.com");
        dto.setRedirectUri("https://library.muczynskifamily.com/api/oauth/google/callback");
        dto.setGoogleClientSecretPartial("...XXXX");
        dto.setGoogleClientSecretConfigured(true);
        dto.setGoogleClientSecretValidation("Valid");
        dto.setGoogleClientSecret(null); // Full secret should be null in responses

        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(dto);

        // Act & Assert - Verify googleClientSecret field is not in response (or is null)
        mockMvc.perform(get("/api/global-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientSecretPartial").exists())
                .andExpect(jsonPath("$.googleClientSecret").doesNotExist()); // Full secret should not be in response
    }

    // ==================== GET /api/global-settings/sso-status Tests ====================

    @Test
    void testGetSsoStatus_Configured() throws Exception {
        // Arrange
        GlobalSettingsDto dto = new GlobalSettingsDto();
        dto.setGoogleSsoClientIdConfigured(true);
        dto.setGoogleSsoClientSecretConfigured(true);
        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/global-settings/sso-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ssoConfigured").value(true));
    }

    @Test
    void testGetSsoStatus_NotConfigured() throws Exception {
        // Arrange
        GlobalSettingsDto dto = new GlobalSettingsDto();
        dto.setGoogleSsoClientIdConfigured(false);
        dto.setGoogleSsoClientSecretConfigured(false);
        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/global-settings/sso-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ssoConfigured").value(false));
    }

    @Test
    void testGetSsoStatus_PublicAccess_NoAuthRequired() throws Exception {
        // Arrange
        GlobalSettingsDto dto = new GlobalSettingsDto();
        dto.setGoogleSsoClientIdConfigured(true);
        dto.setGoogleSsoClientSecretConfigured(true);
        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(dto);

        // Act & Assert - No authentication required for SSO status endpoint
        mockMvc.perform(get("/api/global-settings/sso-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ssoConfigured").value(true));
    }

    @Test
    void testGetSsoStatus_AsRegularUser() throws Exception {
        // Arrange
        GlobalSettingsDto dto = new GlobalSettingsDto();
        dto.setGoogleSsoClientIdConfigured(true);
        dto.setGoogleSsoClientSecretConfigured(true);
        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(dto);

        // Act & Assert - Regular users can check SSO status
        mockMvc.perform(get("/api/global-settings/sso-status")
                        .with(user("regularuser").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ssoConfigured").value(true));
    }

    @Test
    void testGetSsoStatus_AsLibrarian() throws Exception {
        // Arrange
        GlobalSettingsDto dto = new GlobalSettingsDto();
        dto.setGoogleSsoClientIdConfigured(false);
        dto.setGoogleSsoClientSecretConfigured(false);
        when(globalSettingsService.getGlobalSettingsDto()).thenReturn(dto);

        // Act & Assert - Librarians can check SSO status
        mockMvc.perform(get("/api/global-settings/sso-status")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ssoConfigured").value(false));
    }
}
