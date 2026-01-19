/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.LibraryCardDesign;
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

    @Test
    void getUserSettings_authenticatedUser_returnsUserSettings() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setUsername("testuser");
        userDto.setAuthorities(Collections.singleton("USER"));
        userDto.setXaiApiKey("test-xai-key");
        userDto.setLibraryCardDesign(LibraryCardDesign.CLASSICAL_DEVOTION);

        when(userSettingsService.getUserSettings(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.xaiApiKey").value("test-xai-key"))
                .andExpect(jsonPath("$.libraryCardDesign").value("CLASSICAL_DEVOTION"));
    }

    @Test
    void getUserSettings_unauthenticatedUser_returns401() throws Exception {
        mockMvc.perform(get("/api/user-settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUserSettings_changeUsername_updatesUsername() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setUsername("newusername");

        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("newusername");
        updatedUser.setAuthorities(Collections.singleton("USER"));

        when(userSettingsService.updateUserSettings(eq(1L), any(UserSettingsDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newusername"));
    }

    @Test
    void updateUserSettings_changePassword_updatesPassword() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setPassword("a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"); // SHA-256 of "123"

        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setAuthorities(Collections.singleton("USER"));

        when(userSettingsService.updateUserSettings(eq(1L), any(UserSettingsDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserSettings_changeXaiApiKey_updatesApiKey() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setXaiApiKey("new-xai-api-key");

        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setAuthorities(Collections.singleton("USER"));
        updatedUser.setXaiApiKey("new-xai-api-key");

        when(userSettingsService.updateUserSettings(eq(1L), any(UserSettingsDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xaiApiKey").value("new-xai-api-key"));
    }

    @Test
    void updateUserSettings_changeLibraryCardDesignToClassicalDevotion_updatesDesign() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setLibraryCardDesign(LibraryCardDesign.CLASSICAL_DEVOTION);

        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setAuthorities(Collections.singleton("USER"));
        updatedUser.setLibraryCardDesign(LibraryCardDesign.CLASSICAL_DEVOTION);

        when(userSettingsService.updateUserSettings(eq(1L), any(UserSettingsDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryCardDesign").value("CLASSICAL_DEVOTION"));
    }

    @Test
    void updateUserSettings_changeLibraryCardDesignToCountrysideYouth_updatesDesign() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setLibraryCardDesign(LibraryCardDesign.COUNTRYSIDE_YOUTH);

        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setAuthorities(Collections.singleton("USER"));
        updatedUser.setLibraryCardDesign(LibraryCardDesign.COUNTRYSIDE_YOUTH);

        when(userSettingsService.updateUserSettings(eq(1L), any(UserSettingsDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryCardDesign").value("COUNTRYSIDE_YOUTH"));
    }

    @Test
    void updateUserSettings_changeLibraryCardDesignToSacredHeartPortrait_updatesDesign() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setLibraryCardDesign(LibraryCardDesign.SACRED_HEART_PORTRAIT);

        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setAuthorities(Collections.singleton("USER"));
        updatedUser.setLibraryCardDesign(LibraryCardDesign.SACRED_HEART_PORTRAIT);

        when(userSettingsService.updateUserSettings(eq(1L), any(UserSettingsDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryCardDesign").value("SACRED_HEART_PORTRAIT"));
    }

    @Test
    void updateUserSettings_changeLibraryCardDesignToRadiantBlessing_updatesDesign() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setLibraryCardDesign(LibraryCardDesign.RADIANT_BLESSING);

        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setAuthorities(Collections.singleton("USER"));
        updatedUser.setLibraryCardDesign(LibraryCardDesign.RADIANT_BLESSING);

        when(userSettingsService.updateUserSettings(eq(1L), any(UserSettingsDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryCardDesign").value("RADIANT_BLESSING"));
    }

    @Test
    void updateUserSettings_changeLibraryCardDesignToPatronOfCreatures_updatesDesign() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setLibraryCardDesign(LibraryCardDesign.PATRON_OF_CREATURES);

        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setAuthorities(Collections.singleton("USER"));
        updatedUser.setLibraryCardDesign(LibraryCardDesign.PATRON_OF_CREATURES);

        when(userSettingsService.updateUserSettings(eq(1L), any(UserSettingsDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryCardDesign").value("PATRON_OF_CREATURES"));
    }

    @Test
    void updateUserSettings_changeGooglePhotosSettings_updatesSettings() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setGooglePhotosApiKey("new-google-photos-key");
        settingsDto.setGoogleClientSecret("new-client-secret");
        settingsDto.setGooglePhotosAlbumId("album-123");

        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setAuthorities(Collections.singleton("USER"));
        updatedUser.setGooglePhotosApiKey("new-google-photos-key");
        updatedUser.setGoogleClientSecret("new-client-secret");
        updatedUser.setGooglePhotosAlbumId("album-123");

        when(userSettingsService.updateUserSettings(eq(1L), any(UserSettingsDto.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googlePhotosApiKey").value("new-google-photos-key"))
                .andExpect(jsonPath("$.googleClientSecret").value("new-client-secret"))
                .andExpect(jsonPath("$.googlePhotosAlbumId").value("album-123"));
    }

    @Test
    void updateUserSettings_unauthenticatedUser_returns401() throws Exception {
        UserSettingsDto settingsDto = new UserSettingsDto();
        settingsDto.setUsername("newusername");

        mockMvc.perform(put("/api/user-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_authenticatedUser_deletesAccount() throws Exception {
        doNothing().when(userSettingsService).deleteUser(1L);

        mockMvc.perform(delete("/api/user-settings")
                        .with(user("1").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_unauthenticatedUser_returns401() throws Exception {
        mockMvc.perform(delete("/api/user-settings"))
                .andExpect(status().isUnauthorized());
    }
}
