// (c) Copyright 2025 by Muczynski

package com.muczynski.library.service;

import com.muczynski.library.domain.LibraryCardDesign;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.mapper.UserMapper;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

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

        // Mock the mapper to create a UserDto with current values from the User
        // Use lenient() to avoid UnnecessaryStubbingException in tests that don't use the mapper
        lenient().when(userMapper.toDto(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setXaiApiKey(user.getXaiApiKey());
            dto.setGooglePhotosApiKey(user.getGooglePhotosApiKey());
            dto.setGooglePhotosRefreshToken(user.getGooglePhotosRefreshToken());
            dto.setGooglePhotosTokenExpiry(user.getGooglePhotosTokenExpiry());
            dto.setGoogleClientSecret(user.getGoogleClientSecret());
            dto.setGooglePhotosAlbumId(user.getGooglePhotosAlbumId());
            dto.setLastPhotoTimestamp(user.getLastPhotoTimestamp());
            dto.setLibraryCardDesign(user.getLibraryCardDesign());
            dto.setSsoProvider(user.getSsoProvider());
            dto.setSsoSubjectId(user.getSsoSubjectId());
            dto.setEmail(user.getEmail());
            dto.setLastModified(user.getLastModified());
            return dto;
        });
    }

    @Test
    void getUserSettings_shouldReturnUser() {
        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));

        UserDto result = userSettingsService.getUserSettings(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserSettings_shouldThrowException_whenUserNotFound() {
        when(userRepository.findById(999L))
            .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userSettingsService.getUserSettings(999L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void updateUserSettings_shouldUpdateUsername() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setUsername("newusername");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.findAllByUsernameIgnoreCaseOrderByIdAsc("newusername"))
            .thenReturn(java.util.List.of());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

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

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.findAllByUsernameIgnoreCaseOrderByIdAsc("existinguser"))
            .thenReturn(java.util.List.of(existingUser));

        assertThatThrownBy(() -> userSettingsService.updateUserSettings(1L, dto))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("Username already taken");
    }

    @Test
    void updateUserSettings_shouldValidatePasswordHash() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setPassword("invalidhash"); // Not 64 hex characters

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));

        assertThatThrownBy(() -> userSettingsService.updateUserSettings(1L, dto))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("SHA-256");
    }

    @Test
    void updateUserSettings_shouldEncodeValidPasswordHash() {
        UserSettingsDto dto = new UserSettingsDto();
        // Valid SHA-256 hash (64 hex characters)
        dto.setPassword("a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newhash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserSettings_shouldUpdateXaiApiKey() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setXaiApiKey("new-xai-key");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getXaiApiKey()).isEqualTo("new-xai-key");
    }

    @Test
    void updateUserSettings_shouldClearXaiApiKey_whenEmptyString() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setXaiApiKey("");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getXaiApiKey()).isEmpty();
    }

    @Test
    void updateUserSettings_shouldUpdateLibraryCardDesign() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setLibraryCardDesign(LibraryCardDesign.COUNTRYSIDE_YOUTH);

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getLibraryCardDesign()).isEqualTo(LibraryCardDesign.COUNTRYSIDE_YOUTH);
    }

    @Test
    void updateUserSettings_shouldUpdateGooglePhotosAlbumId() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosAlbumId("album-123-abc");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getGooglePhotosAlbumId()).isEqualTo("album-123-abc");
    }

    @Test
    void updateUserSettings_shouldUpdateGooglePhotosApiKey() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosApiKey("new-api-key");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getGooglePhotosApiKey()).isEqualTo("new-api-key");
    }

    @Test
    void updateUserSettings_shouldClearGooglePhotosApiKey_whenEmptyString() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosApiKey("");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getGooglePhotosApiKey()).isEmpty();
    }

    @Test
    void updateUserSettings_shouldUpdateGooglePhotosRefreshToken() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosRefreshToken("new-refresh-token");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getGooglePhotosRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void updateUserSettings_shouldClearGooglePhotosRefreshToken_whenEmptyString() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosRefreshToken("");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getGooglePhotosRefreshToken()).isEmpty();
    }

    @Test
    void updateUserSettings_shouldUpdateGooglePhotosTokenExpiry() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosTokenExpiry("2025-12-31T23:59:59");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getGooglePhotosTokenExpiry()).isEqualTo("2025-12-31T23:59:59");
    }

    @Test
    void updateUserSettings_shouldClearGooglePhotosTokenExpiry_whenEmptyString() {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setGooglePhotosTokenExpiry("");

        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userSettingsService.updateUserSettings(1L, dto);

        assertThat(result.getGooglePhotosTokenExpiry()).isEmpty();
    }

    @Test
    void deleteUser_shouldDeleteUser() {
        when(userRepository.findById(1L))
            .thenReturn(java.util.Optional.of(testUser));

        userSettingsService.deleteUser(1L);

        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_shouldThrowException_whenUserNotFound() {
        when(userRepository.findById(999L))
            .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userSettingsService.deleteUser(999L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("User not found");
    }
}
