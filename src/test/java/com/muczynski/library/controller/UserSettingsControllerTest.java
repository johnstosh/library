/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.service.UserSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Integration Tests for UserSettingsController
 *
 * Tests REST endpoints with actual HTTP requests according to backend-development-requirements.md
 * Each endpoint should have:
 * - One test for successful request (2xx status)
 * - One test for unauthorized access (401/403 status)
 * - One test for invalid input (400 status) where applicable
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSettingsService userSettingsService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET /api/user-settings Tests ====================

    @Test
    void testGetUserSettings_Success() throws Exception {
        // Arrange
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setUsername("testuser");
        userDto.setAuthorities(Collections.singleton("USER"));
        userDto.setXaiApiKey("test-xai-key");
        userDto.setGooglePhotosApiKey("test-gp-key");
        userDto.setGoogleClientSecret("test-client-secret");

        when(userSettingsService.getUserSettings("testuser")).thenReturn(userDto);

        // Act & Assert
        mockMvc.perform(get("/api/user-settings")
                        .with(user("testuser").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.xaiApiKey").value("test-xai-key"))
                .andExpect(jsonPath("$.googlePhotosApiKey").value("test-gp-key"))
                .andExpect(jsonPath("$.googleClientSecret").value("test-client-secret"));
    }

    @Test
    void testGetUserSettings_Unauthorized() throws Exception {
        // Act & Assert - No authentication
        mockMvc.perform(get("/api/user-settings"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PUT /api/user-settings Tests ====================

    @Test
    void testUpdateUserSettings_Success() throws Exception {
        // Arrange
        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername("testuser");
        updateDto.setXaiApiKey("new-xai-key");
        updateDto.setGooglePhotosApiKey("new-gp-key");
        updateDto.setGoogleClientSecret("new-client-secret");
        updateDto.setPassword("newpassword123");

        UserDto responseDto = new UserDto();
        responseDto.setId(1L);
        responseDto.setUsername("testuser");
        responseDto.setAuthorities(Collections.singleton("USER"));
        responseDto.setXaiApiKey("new-xai-key");
        responseDto.setGooglePhotosApiKey("new-gp-key");
        responseDto.setGoogleClientSecret("new-client-secret");

        when(userSettingsService.updateUserSettings(eq("testuser"), any(UserSettingsDto.class)))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(put("/api/user-settings")
                        .with(user("testuser").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.xaiApiKey").value("new-xai-key"))
                .andExpect(jsonPath("$.googlePhotosApiKey").value("new-gp-key"))
                .andExpect(jsonPath("$.googleClientSecret").value("new-client-secret"));
    }

    @Test
    void testUpdateUserSettings_Unauthorized() throws Exception {
        // Arrange
        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername("testuser");
        updateDto.setXaiApiKey("new-key");

        // Act & Assert - No authentication
        mockMvc.perform(put("/api/user-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateUserSettings_InvalidInput_EmptyUsername() throws Exception {
        // Arrange - Empty username should trigger validation error
        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername("");  // Invalid - empty username
        updateDto.setXaiApiKey("test-key");

        when(userSettingsService.updateUserSettings(eq("testuser"), any(UserSettingsDto.class)))
                .thenThrow(new IllegalArgumentException("Username cannot be empty"));

        // Act & Assert
        mockMvc.perform(put("/api/user-settings")
                        .with(user("testuser").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testUpdateUserSettings_AllFieldsUpdated() throws Exception {
        // Test that all user settings fields can be updated together
        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername("librarian");
        updateDto.setPassword("newpass456");
        updateDto.setXaiApiKey("updated-xai-key");
        updateDto.setGooglePhotosApiKey("updated-gp-key");
        updateDto.setGoogleClientSecret("updated-client-secret");

        UserDto responseDto = new UserDto();
        responseDto.setId(2L);
        responseDto.setUsername("librarian");
        responseDto.setAuthorities(Collections.singleton("LIBRARIAN"));
        responseDto.setXaiApiKey("updated-xai-key");
        responseDto.setGooglePhotosApiKey("updated-gp-key");
        responseDto.setGoogleClientSecret("updated-client-secret");

        when(userSettingsService.updateUserSettings(eq("librarian"), any(UserSettingsDto.class)))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(put("/api/user-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("librarian"))
                .andExpect(jsonPath("$.xaiApiKey").value("updated-xai-key"))
                .andExpect(jsonPath("$.googlePhotosApiKey").value("updated-gp-key"))
                .andExpect(jsonPath("$.googleClientSecret").value("updated-client-secret"));
    }

    // ==================== DELETE /api/user-settings Tests ====================

    @Test
    void testDeleteUser_Success() throws Exception {
        // Arrange
        doNothing().when(userSettingsService).deleteUser("testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/user-settings")
                        .with(user("testuser").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteUser_Unauthorized() throws Exception {
        // Act & Assert - No authentication
        mockMvc.perform(delete("/api/user-settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDeleteUser_AsLibrarian() throws Exception {
        // Test that librarians can also delete their own account
        doNothing().when(userSettingsService).deleteUser("librarian");

        // Act & Assert
        mockMvc.perform(delete("/api/user-settings")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("LIBRARIAN"))))
                .andExpect(status().isNoContent());
    }
}
