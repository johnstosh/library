/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GooglePhotosServiceTest {

    @Mock
    private UserSettingsService userSettingsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private GooglePhotosService googlePhotosService;

    private UserDto testUser;

    @BeforeEach
    void setUp() {
        // Set up security context
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("1");
        SecurityContextHolder.setContext(securityContext);

        // Set up test user DTO
        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setGooglePhotosApiKey("test-api-key");

        lenient().when(userSettingsService.getUserSettings(1L)).thenReturn(testUser);

        // Set up test user entity for repository
        User userEntity = new User();
        userEntity.setId(1L);
        userEntity.setUsername("testuser");
        userEntity.setGooglePhotosApiKey("test-api-key");
        userEntity.setGooglePhotosRefreshToken("test-refresh-token");
        userEntity.setGooglePhotosTokenExpiry(Instant.now().plusSeconds(3600).toString());

        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(userEntity));
        lenient().when(userRepository.save(any(User.class))).thenReturn(userEntity);

        // Inject mock RestTemplate into service
        try {
            java.lang.reflect.Field restTemplateField = GooglePhotosService.class.getDeclaredField("restTemplate");
            restTemplateField.setAccessible(true);
            restTemplateField.set(googlePhotosService, restTemplate);
        } catch (Exception e) {
            fail("Failed to inject mock RestTemplate: " + e.getMessage());
        }
    }

    // Tests for fetchPhotos method removed - this method was replaced with
    // fetchPickerMediaItems which uses Google Photos Picker API instead

    @Test
    void downloadPhoto_withValidUrl_returnsBytes() {
        // Arrange
        String baseUrl = "https://example.com/photo";
        byte[] expectedBytes = new byte[]{1, 2, 3, 4, 5};

        ResponseEntity<byte[]> response = new ResponseEntity<>(expectedBytes, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(baseUrl + "=d"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(response);

        // Act
        byte[] result = googlePhotosService.downloadPhoto(baseUrl);

        // Assert
        assertNotNull(result);
        assertArrayEquals(expectedBytes, result);
        verify(restTemplate).exchange(
                eq(baseUrl + "=d"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class)
        );
    }

    @Test
    void downloadPhoto_withoutApiKeyOrRefreshToken_throwsNotConnected() {
        // Arrange - no access token, no refresh token
        User userEntity = new User();
        userEntity.setId(1L);
        userEntity.setUsername("testuser");
        userEntity.setGooglePhotosApiKey(null);
        userEntity.setGooglePhotosRefreshToken(null);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(userEntity));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            googlePhotosService.downloadPhoto("https://example.com/photo");
        });

        assertTrue(exception.getMessage().contains("has not been connected"),
                "Expected 'has not been connected' message, got: " + exception.getMessage());
    }

    @Test
    void getValidAccessToken_noTokenNoRefresh_throwsNotConnected() {
        User user = new User();
        user.setId(2L);
        user.setUsername("notoken");
        user.setGooglePhotosApiKey(null);
        user.setGooglePhotosRefreshToken(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            googlePhotosService.getValidAccessToken(user);
        });

        assertTrue(exception.getMessage().contains("has not been connected"),
                "Expected 'has not been connected' message, got: " + exception.getMessage());
    }

    @Test
    void getValidAccessToken_noTokenButHasRefresh_attemptsRefresh() {
        User user = new User();
        user.setId(3L);
        user.setUsername("expireduser");
        user.setGooglePhotosApiKey(null);
        user.setGooglePhotosRefreshToken("valid-refresh-token");

        // Refresh will fail because GlobalSettingsService is not mocked
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            googlePhotosService.getValidAccessToken(user);
        });

        // Should get "expired and could not be refreshed" since refresh fails
        assertTrue(exception.getMessage().contains("expired and could not be refreshed"),
                "Expected 'expired and could not be refreshed' message, got: " + exception.getMessage());
    }

    @Test
    void getValidAccessToken_validToken_returnsToken() {
        User user = new User();
        user.setId(4L);
        user.setUsername("validuser");
        user.setGooglePhotosApiKey("valid-access-token");
        user.setGooglePhotosRefreshToken("refresh-token");
        user.setGooglePhotosTokenExpiry(Instant.now().plusSeconds(3600).toString());

        String result = googlePhotosService.getValidAccessToken(user);

        assertEquals("valid-access-token", result);
    }

    @Test
    void updatePhotoDescription_withValidPhotoId_updatesSuccessfully() {
        // Arrange
        String photoId = "photo123";
        String description = "Title: Test Book\nAuthor: Test Author";

        ResponseEntity<Map> response = new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            googlePhotosService.updatePhotoDescription(photoId, description);
        });

        verify(restTemplate).exchange(
                contains(photoId),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    void updatePhotoDescription_withException_throwsRuntimeException() {
        // Arrange
        String photoId = "photo123";
        String description = "Title: Test Book\nAuthor: Test Author";

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            googlePhotosService.updatePhotoDescription(photoId, description);
        });

        assertTrue(exception.getMessage().contains("Failed to update photo description"));
    }
}
