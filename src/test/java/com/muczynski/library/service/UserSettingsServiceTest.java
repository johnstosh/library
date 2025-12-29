// (c) Copyright 2025 by Muczynski

package com.muczynski.library.service;

import com.muczynski.library.domain.LibraryCardDesign;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserSettingsService userSettingsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$hashedpassword");
        testUser.setXaiApiKey("test-xai-key");
        testUser.setLibraryCardDesign(LibraryCardDesign.CLASSICAL_DEVOTION);
    }

    @Test
    void getUserSettings_shouldReturnUser() {
        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));

        User result = userSettingsService.getUserSettings("testuser");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserSettings_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByUsernameIgnoreCase("nonexistent"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSettingsService.getUserSettings("nonexistent"))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void updateUserSettings_shouldUpdateUsername() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setUsername("newusername");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.findByUsernameIgnoreCase("newusername"))
            .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getUsername()).isEqualTo("newusername");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserSettings_shouldThrowException_whenUsernameAlreadyExists() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setUsername("existinguser");

        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setUsername("existinguser");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.findByUsernameIgnoreCase("existinguser"))
            .thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userSettingsService.updateUserSettings("testuser", dto))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void updateUserSettings_shouldValidatePasswordHash() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setPassword("invalidhash"); // Not 64 hex characters

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userSettingsService.updateUserSettings("testuser", dto))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("SHA-256");
    }

    @Test
    void updateUserSettings_shouldEncodeValidPasswordHash() {
        UserSettingsDto dto = new UserSettingsDto();
        // Valid SHA-256 hash (64 hex characters)
        dto.setPassword("a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newhash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserSettings_shouldUpdateXaiApiKey() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setXaiApiKey("new-xai-key");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getXaiApiKey()).isEqualTo("new-xai-key");
    }

    @Test
    void updateUserSettings_shouldClearXaiApiKey_whenEmptyString() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setXaiApiKey("");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getXaiApiKey()).isNull();
    }

    @Test
    void updateUserSettings_shouldUpdateLibraryCardDesign() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setLibraryCardDesign(LibraryCardDesign.COUNTRYSIDE_YOUTH);

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getLibraryCardDesign()).isEqualTo(LibraryCardDesign.COUNTRYSIDE_YOUTH);
    }

    @Test
    void updateUserSettings_shouldUpdateGooglePhotosAlbumId() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosAlbumId("album-123-abc");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getGooglePhotosAlbumId()).isEqualTo("album-123-abc");
    }

    @Test
    void updateUserSettings_shouldUpdateGooglePhotosApiKey() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosApiKey("new-api-key");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getGooglePhotosApiKey()).isEqualTo("new-api-key");
    }

    @Test
    void updateUserSettings_shouldClearGooglePhotosApiKey_whenEmptyString() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosApiKey("");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getGooglePhotosApiKey()).isNull();
    }

    @Test
    void updateUserSettings_shouldUpdateGooglePhotosRefreshToken() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosRefreshToken("new-refresh-token");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getGooglePhotosRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void updateUserSettings_shouldClearGooglePhotosRefreshToken_whenEmptyString() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosRefreshToken("");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getGooglePhotosRefreshToken()).isNull();
    }

    @Test
    void updateUserSettings_shouldUpdateGooglePhotosTokenExpiry() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosTokenExpiry("2025-12-31T23:59:59");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getGooglePhotosTokenExpiry()).isEqualTo("2025-12-31T23:59:59");
    }

    @Test
    void updateUserSettings_shouldClearGooglePhotosTokenExpiry_whenEmptyString() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosTokenExpiry("");

        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userSettingsService.updateUserSettings("testuser", dto);

        assertThat(result.getGooglePhotosTokenExpiry()).isNull();
    }

    @Test
    void deleteUser_shouldDeleteUser() {
        when(userRepository.findByUsernameIgnoreCase("testuser"))
            .thenReturn(Optional.of(testUser));

        userSettingsService.deleteUser("testuser");

        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByUsernameIgnoreCase("nonexistent"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSettingsService.deleteUser("nonexistent"))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("User not found");
    }
}
