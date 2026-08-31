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

    @Test
    void askQuestion_usesFlagshipModelSystemPromptAndCompletionCap() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "essay");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);

        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                entityCaptor.capture(),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        String result = askGrok.askQuestion("Research Little Women", "Write essays, not blurbs.");

        assertEquals("essay", result);
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertEquals(AskGrok.MODEL_GROK_FLAGSHIP, request.get("model"));
        assertEquals("grok-4.6", request.get("model"));
        assertEquals(8000, request.get("max_completion_tokens"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("Write essays, not blurbs.", messages.get(0).get("content"));
        assertEquals("user", messages.get(1).get("role"));
        assertEquals("Research Little Women", messages.get(1).get("content"));
    }

    @Test
    void analyzePhoto_withHighDetail_putsDetailOnImageUrl() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "{}");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);

        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                entityCaptor.capture(),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        askGrok.analyzePhoto(
                "img".getBytes(),
                "image/jpeg",
                "transcribe",
                AskGrok.MODEL_GROK_4_FAST,
                null,
                AskGrok.IMAGE_DETAIL_HIGH);

        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) messages.get(0).get("content");
        @SuppressWarnings("unchecked")
        Map<String, Object> imagePart = content.stream()
                .filter(part -> "image_url".equals(part.get("type")))
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> imageUrl = (Map<String, Object>) imagePart.get("image_url");
        assertEquals("high", imageUrl.get("detail"));
    }

    @Test
    void parseCallNumberArray_sortsAlphabeticallyAndTakesDistinctStrings() {
        List<String> parsed = AskGrok.parseCallNumberArray(
                "Here you go:\n[\"PS3511.I9 G7\", \"BX4700.T4\", \"  \"]\n");
        assertEquals(List.of("BX4700.T4", "PS3511.I9 G7"), parsed);
    }

    @Test
    void parseCallNumberArray_emptyOrInvalid_returnsEmpty() {
        assertEquals(List.of(), AskGrok.parseCallNumberArray(null));
        assertEquals(List.of(), AskGrok.parseCallNumberArray("PS3511.I9 G7"));
        assertEquals(List.of(), AskGrok.parseCallNumberArray("[]"));
    }

    @Test
    void suggestLocNumber_returnsAlphabeticallyFirstFromArray() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "[\"PS3511.I9 G7\", \"BX4700.T4\"]");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);

        when(restTemplate.postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        assertEquals("BX4700.T4", askGrok.suggestLocNumber("Little Women", "Louisa May Alcott"));
    }

    @Test
    void suggestGrokipediaUrlsForBook_asksForJsonArrayWithAuthorContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "[\"https://grokipedia.com/page/Little_Women\"]");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);

        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                entityCaptor.capture(),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<String> urls = askGrok.suggestGrokipediaUrlsForBook("Little Women", "Louisa May Alcott");

        assertEquals(List.of("https://grokipedia.com/page/Little_Women"), urls);
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        String prompt = (String) messages.get(0).get("content");
        assertTrue(prompt.contains("This task is about authors and books."));
        assertTrue(prompt.contains("the book \"Little Women\" by Louisa May Alcott"));
        assertTrue(prompt.contains("https://grokipedia.com/search?q=Little Women"));
        assertFalse(prompt.contains("https://grokipedia.com/search?q=Little Women Louisa"));
        assertTrue(prompt.contains("Grokipedia search as the source of truth"));
        assertTrue(prompt.contains("nothing before or after the JSON"));
        assertTrue(prompt.contains("[\"https://grokipedia.com/page/The_Song_of_Bernadette_(novel)\"]"));
    }

    @Test
    void suggestGrokipediaUrlsForAuthor_includesCorrespondingBook() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "[\"https://grokipedia.com/page/Louisa_May_Alcott\"]");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);

        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                entityCaptor.capture(),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<String> urls = askGrok.suggestGrokipediaUrlsForAuthor("Louisa May Alcott", "Little Women");

        assertEquals(List.of("https://grokipedia.com/page/Louisa_May_Alcott"), urls);
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        String prompt = (String) messages.get(0).get("content");
        assertTrue(prompt.contains("This task is about authors and books."));
        assertTrue(prompt.contains("Louisa May Alcott, known for the book \"Little Women\""));
        assertTrue(prompt.contains("https://grokipedia.com/search?q=Louisa May Alcott"));
        assertFalse(prompt.contains("https://grokipedia.com/search?q=Louisa May Alcott Little Women"));
        assertTrue(prompt.contains("Grokipedia search as the source of truth"));
        assertTrue(prompt.contains("nothing before or after the JSON"));
        assertTrue(prompt.contains("[\"https://grokipedia.com/page/C._S._Lewis\"]"));
    }

    @Test
    void parseJsonStringArray_preservesOrderAndIgnoresBlanks() {
        List<String> parsed = AskGrok.parseJsonStringArray(
                "Here:\n[\"https://grokipedia.com/page/B\", \"  \", \"https://grokipedia.com/page/A\"]\n");
        assertEquals(
                List.of("https://grokipedia.com/page/B", "https://grokipedia.com/page/A"),
                parsed);
        assertEquals(List.of(), AskGrok.parseJsonStringArray(null));
        assertEquals(List.of(), AskGrok.parseJsonStringArray("not json"));
    }
}
