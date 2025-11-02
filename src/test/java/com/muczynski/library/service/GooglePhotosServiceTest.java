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
        lenient().when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        // Set up test user DTO
        testUser = new UserDto();
        testUser.setUsername("testuser");
        testUser.setGooglePhotosApiKey("test-api-key");

        lenient().when(userSettingsService.getUserSettings("testuser")).thenReturn(testUser);

        // Set up test user entity for repository
        User userEntity = new User();
        userEntity.setUsername("testuser");
        userEntity.setGooglePhotosApiKey("test-api-key");
        userEntity.setGooglePhotosRefreshToken("test-refresh-token");
        userEntity.setGooglePhotosTokenExpiry(Instant.now().plusSeconds(3600).toString());

        lenient().when(userRepository.findByUsernameIgnoreCase("testuser"))
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

    @Test
    void fetchPhotos_withValidApiKey_returnsPhotos() {
        // Arrange
        String timestamp = "2025-01-01T00:00:00Z";
        Map<String, Object> photoItem1 = new HashMap<>();
        photoItem1.put("id", "photo1");
        photoItem1.put("baseUrl", "https://example.com/photo1");
        photoItem1.put("description", "Test photo 1");

        Map<String, Object> photoItem2 = new HashMap<>();
        photoItem2.put("id", "photo2");
        photoItem2.put("baseUrl", "https://example.com/photo2");

        List<Map<String, Object>> mediaItems = Arrays.asList(photoItem1, photoItem2);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("mediaItems", mediaItems);

        ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        // Act
        List<Map<String, Object>> result = googlePhotosService.fetchPhotos(timestamp);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("photo1", result.get(0).get("id"));
        assertEquals("photo2", result.get(1).get("id"));
        verify(restTemplate).postForEntity(
                eq("https://photoslibrary.googleapis.com/v1/mediaItems:search"),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    void fetchPhotos_withNoMediaItems_returnsEmptyList() {
        // Arrange
        Map<String, Object> responseBody = new HashMap<>();
        ResponseEntity<Map> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);

        // Act
        List<Map<String, Object>> result = googlePhotosService.fetchPhotos(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchPhotos_withoutApiKey_throwsException() {
        // Arrange
        User userEntity = new User();
        userEntity.setUsername("testuser");
        userEntity.setGooglePhotosApiKey("");
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(userEntity));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            googlePhotosService.fetchPhotos("2025-01-01T00:00:00Z");
        });

        assertTrue(exception.getMessage().contains("Google Photos not authorized"));
    }

    @Test
    void fetchPhotos_withNoAuthentication_throwsException() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            googlePhotosService.fetchPhotos("2025-01-01T00:00:00Z");
        });

        assertTrue(exception.getMessage().contains("No authenticated user found"));
    }

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
    void downloadPhoto_withoutApiKey_throwsException() {
        // Arrange
        User userEntity = new User();
        userEntity.setUsername("testuser");
        userEntity.setGooglePhotosApiKey(null);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(userEntity));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            googlePhotosService.downloadPhoto("https://example.com/photo");
        });

        assertTrue(exception.getMessage().contains("Google Photos not authorized"));
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
