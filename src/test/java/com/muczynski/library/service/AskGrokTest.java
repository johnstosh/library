// (c) Copyright 2025 by Muczynski
package com.muczynski.library.service;

import com.muczynski.library.exception.LibraryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AskGrokTest {

    private RestTemplate restTemplate;
    private UserSettingsService userSettingsService;
    private SecurityContext securityContext;
    private Authentication authentication;
    private AskGrok askGrok;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        userSettingsService = mock(UserSettingsService.class);
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        askGrok = new AskGrok();

        // Inject mocks using reflection
        ReflectionTestUtils.setField(askGrok, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(askGrok, "userSettingsService", userSettingsService);

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAskAboutPhotos_withNullImageBytes_shouldSkipPhoto() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        List<Map<String, Object>> photoDataList = new ArrayList<>();

        // Add photo with null imageBytes
        Map<String, Object> nullPhoto = new HashMap<>();
        nullPhoto.put("imageBytes", null);
        nullPhoto.put("contentType", "image/jpeg");
        photoDataList.add(nullPhoto);

        // Add photo with valid imageBytes
        Map<String, Object> validPhoto = new HashMap<>();
        validPhoto.put("imageBytes", "test-image".getBytes());
        validPhoto.put("contentType", "image/jpeg");
        photoDataList.add(validPhoto);

        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "Test response");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);

        when(restTemplate.postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Act
        String result = askGrok.analyzePhotos(photoDataList, "test question");

        // Assert
        assertNotNull(result);
        assertEquals("Test response", result);

        // Verify that the API was called (which means null photos were skipped without error)
        verify(restTemplate).postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    void testAskAboutPhotos_withEmptyImageBytes_shouldSkipPhoto() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        List<Map<String, Object>> photoDataList = new ArrayList<>();

        // Add photo with empty imageBytes
        Map<String, Object> emptyPhoto = new HashMap<>();
        emptyPhoto.put("imageBytes", new byte[0]);
        emptyPhoto.put("contentType", "image/jpeg");
        photoDataList.add(emptyPhoto);

        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "Test response");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);

        when(restTemplate.postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Act
        String result = askGrok.analyzePhotos(photoDataList, "test question");

        // Assert
        assertNotNull(result);
        assertEquals("Test response", result);
    }

    @Test
    void testAskAboutPhotos_withNoAuthentication_shouldThrowException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        List<Map<String, Object>> photoDataList = new ArrayList<>();

        // Act & Assert
        assertThrows(LibraryException.class, () ->
            askGrok.analyzePhotos(photoDataList, "test question")
        );
    }

    @Test
    void testAskAboutPhotos_withNoApiKey_shouldThrowException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn(null);
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        List<Map<String, Object>> photoDataList = new ArrayList<>();

        // Act & Assert
        assertThrows(LibraryException.class, () ->
            askGrok.analyzePhotos(photoDataList, "test question")
        );
    }
}
