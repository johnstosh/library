// (c) Copyright 2025 by Muczynski
package com.muczynski.library.service;

import com.muczynski.library.exception.GrokCreditsExhaustedException;
import com.muczynski.library.exception.LibraryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

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
        ReflectionTestUtils.setField(askGrok, "geminiPause", (LongConsumer) millis -> { });

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
        when(userDto.isUseXaiApiKey()).thenReturn(true);
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
        when(userDto.isUseXaiApiKey()).thenReturn(true);
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
        when(userDto.isUseXaiApiKey()).thenReturn(true);
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
        when(userDto.isUseXaiApiKey()).thenReturn(true);
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
        assertEquals("grok-4.7", request.get("model"));
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
        when(userDto.isUseXaiApiKey()).thenReturn(true);
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
        when(userDto.isUseXaiApiKey()).thenReturn(true);
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
        when(userDto.isUseXaiApiKey()).thenReturn(true);
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
        when(userDto.isUseXaiApiKey()).thenReturn(true);
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
    void toAscii_stripsDiacriticsFromKarolJozefWojtyla() {
        assertEquals("Karol Jozef Wojtyla", AskGrok.toAscii("Karol Józef Wojtyła"));
        assertNull(AskGrok.toAscii(null));
        assertEquals("Louisa May Alcott", AskGrok.toAscii("Louisa May Alcott"));
    }

    @Test
    void suggestGrokipediaUrlsForAuthor_putsAsciiNameInPrompt() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userDto.isUseXaiApiKey()).thenReturn(true);
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "[\"https://grokipedia.com/page/Pope_John_Paul_II\"]");
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

        askGrok.suggestGrokipediaUrlsForAuthor("Karol Józef Wojtyła", "Crossing the Threshold of Hope");

        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        String prompt = (String) messages.get(0).get("content");
        assertTrue(prompt.contains("Karol Jozef Wojtyla"));
        assertTrue(prompt.contains("https://grokipedia.com/search?q=Karol Jozef Wojtyla"));
        assertFalse(prompt.contains("Józef"));
        assertFalse(prompt.contains("Wojtyła"));
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

    @Test
    void parseJsonStringArrayOfArrays_nestedAndFlat() {
        assertEquals(
                List.of(List.of("children"), List.of("moderate", "accessible")),
                AskGrok.parseJsonStringArrayOfArrays(
                        "Here:\n[[\"children\"],[\"moderate\",\"accessible\"]]\n"));
        assertEquals(
                List.of(List.of("moderate"), List.of("accessible")),
                AskGrok.parseJsonStringArrayOfArrays("[\"moderate\",\"accessible\"]"));
        assertEquals(List.of(), AskGrok.parseJsonStringArrayOfArrays(null));
        assertEquals(List.of(), AskGrok.parseJsonStringArrayOfArrays("not json"));
    }

    @Test
    void suggestReadingDifficulties_returnsFirstKeyPerBook() {
        stubGrokContent("[[\"moderate\",\"accessible\"],[\"children\"]]");

        List<String> firsts = askGrok.suggestReadingDifficulties(List.of(
                "{\"id\":1,\"title\":\"Summa\"}",
                "{\"id\":2,\"title\":\"Little Women\"}"));

        assertEquals(List.of("moderate", "children"), firsts);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                entityCaptor.capture(),
                eq(Map.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> request = entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
        String prompt = (String) messages.get(0).get("content");
        assertTrue(prompt.contains("children: for kids / read-aloud"));
        assertTrue(prompt.contains("JSON array of those arrays"));
        assertTrue(prompt.contains("\"title\":\"Summa\""));
    }

    @Test
    void suggestReadingDifficulties_singleBookFlatArrayUsesFirst() {
        stubGrokContent("[\"demanding\",\"advanced\"]");

        List<String> firsts = askGrok.suggestReadingDifficulties(List.of("{\"id\":1,\"title\":\"Summa\"}"));

        assertEquals(List.of("demanding"), firsts);
    }

    @Test
    void suggestReadingDifficulties_rejectsMoreThanBatchSize() {
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < AskGrok.READING_DIFFICULTY_BATCH_SIZE + 1; i++) {
            tooMany.add("{\"id\":" + i + "}");
        }
        assertThrows(IllegalArgumentException.class, () -> askGrok.suggestReadingDifficulties(tooMany));
        verifyNoInteractions(restTemplate);
    }

    private void stubGrokContent(String content) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");

        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userDto.isUseXaiApiKey()).thenReturn(true);
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", content);
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);

        when(restTemplate.postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
    }

    // ==================== xAI out of credits ====================

    private static final String CREDITS_BODY = "{\"code\":\"permission-denied\",\"error\":\"Your team "
            + "has either used all available credits or reached its monthly spending limit. "
            + "To continue making API requests, please purchase more credits or raise your spending limit.\"}";

    private void stubAuthenticatedUserWithKey() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");
        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userDto.isUseXaiApiKey()).thenReturn(true);
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);
    }

    private void stubXaiError(HttpStatus status, String body) {
        when(restTemplate.postForEntity(
                eq("https://api.x.ai/v1/chat/completions"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(HttpClientErrorException.create(status, status.getReasonPhrase(),
                new HttpHeaders(), body.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void askQuestion_xai403OutOfCredits_throwsGrokCreditsExhausted() {
        stubAuthenticatedUserWithKey();
        stubXaiError(HttpStatus.FORBIDDEN, CREDITS_BODY);

        GrokCreditsExhaustedException ex = assertThrows(GrokCreditsExhaustedException.class,
                () -> askGrok.askQuestion("Who wrote this?"));
        assertEquals("Grok is out of credits. Add credits or raise the spending limit at console.x.ai, then try again.",
                ex.getMessage());
        assertInstanceOf(HttpClientErrorException.Forbidden.class, ex.getCause());
    }

    @Test
    void analyzePhoto_xai403OutOfCredits_throwsGrokCreditsExhausted() {
        stubAuthenticatedUserWithKey();
        stubXaiError(HttpStatus.FORBIDDEN, CREDITS_BODY);

        assertThrows(GrokCreditsExhaustedException.class,
                () -> askGrok.analyzePhoto("img".getBytes(), "image/jpeg", "Describe"));
    }

    @Test
    void askQuestion_xai429SpendingLimit_throwsGrokCreditsExhausted() {
        stubAuthenticatedUserWithKey();
        stubXaiError(HttpStatus.TOO_MANY_REQUESTS, "{\"error\":\"Monthly spending limit reached\"}");

        assertThrows(GrokCreditsExhaustedException.class, () -> askGrok.askQuestionFast("q"));
    }

    @Test
    void askQuestion_xai403Unrelated_rethrowsOriginal() {
        stubAuthenticatedUserWithKey();
        stubXaiError(HttpStatus.FORBIDDEN, "{\"error\":\"Model not available for this key\"}");

        assertThrows(HttpClientErrorException.Forbidden.class, () -> askGrok.askQuestion("q"));
    }

    @Test
    void askQuestion_xai429PlainRateLimit_rethrowsOriginal() {
        stubAuthenticatedUserWithKey();
        stubXaiError(HttpStatus.TOO_MANY_REQUESTS, "{\"error\":\"Too many requests\"}");

        assertThrows(HttpClientErrorException.TooManyRequests.class, () -> askGrok.askQuestion("q"));
    }

    @Test
    void isCreditsExhausted_classifiesStatusAndBody() {
        assertTrue(AskGrok.isCreditsExhausted(403, CREDITS_BODY));
        assertTrue(AskGrok.isCreditsExhausted(429, "used all available credits"));
        assertTrue(AskGrok.isCreditsExhausted(402, ""));
        assertFalse(AskGrok.isCreditsExhausted(403, "forbidden"));
        assertFalse(AskGrok.isCreditsExhausted(403, null));
        assertFalse(AskGrok.isCreditsExhausted(400, CREDITS_BODY));
        assertFalse(AskGrok.isCreditsExhausted(500, CREDITS_BODY));
    }

    @Test
    void askQuestion_xai401_reportsInvalidKey() {
        stubAuthenticatedUserWithKey();
        stubXaiError(HttpStatus.UNAUTHORIZED, "{\"error\":\"Incorrect API key\"}");

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestion("q"));
        assertEquals("xAI API key is invalid", ex.getMessage());
        assertFalse(ex.getMessage().contains("test-api-key"));
    }

    @Test
    void askQuestion_googleSelected_callsGeminiFreeModel() {
        stubGoogleUser("google-test-key");

        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL),
                entityCaptor.capture(),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(geminiBody("essay", true), HttpStatus.OK));

        String result = askGrok.askQuestion("Research Little Women", "Write essays, not blurbs.");

        assertEquals("essay", result);
        assertTrue(AskGrok.GEMINI_GENERATE_URL.contains(AskGrok.MODEL_GEMINI_FREE));
        assertEquals("gemini-4-hydrogen", AskGrok.MODEL_GEMINI_FREE);
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertEquals("google-test-key", headers.getFirst("x-goog-api-key"));
        assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertFalse(request.containsKey("temperature"));
        @SuppressWarnings("unchecked")
        Map<String, Object> generation = (Map<String, Object>) request.get("generationConfig");
        assertEquals(8000, generation.get("maxOutputTokens"));
        @SuppressWarnings("unchecked")
        Map<String, Object> thinking = (Map<String, Object>) generation.get("thinkingConfig");
        assertEquals("low", thinking.get("thinkingLevel"));
        assertEquals("Write essays, not blurbs.",
                ((Map<?, ?>) ((List<?>) ((Map<?, ?>) request.get("systemInstruction")).get("parts")).get(0)).get("text"));
        verify(restTemplate, never()).postForEntity(
                eq("https://api.x.ai/v1/chat/completions"), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void askQuestionFast_googleSelected_usesLowThinking() {
        stubGoogleUser("google-test-key");
        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL),
                entityCaptor.capture(),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(geminiBody("fiction", false), HttpStatus.OK));

        assertEquals("fiction", askGrok.askQuestionFast("tag this"));
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> generation = (Map<String, Object>) request.get("generationConfig");
        assertEquals(2048, generation.get("maxOutputTokens"));
        @SuppressWarnings("unchecked")
        Map<String, Object> thinking = (Map<String, Object>) generation.get("thinkingConfig");
        assertEquals("low", thinking.get("thinkingLevel"));
    }

    @Test
    void analyzePhoto_googleSelected_sendsInlineImage() {
        stubGoogleUser("google-test-key");
        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL),
                entityCaptor.capture(),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(geminiBody("a card", false), HttpStatus.OK));

        assertEquals("a card", askGrok.analyzePhoto("img".getBytes(), "image/jpeg", "Read the card"));

        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) entityCaptor.getValue().getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) request.get("contents");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        assertEquals("Read the card", parts.get(0).get("text"));
        @SuppressWarnings("unchecked")
        Map<String, Object> inline = (Map<String, Object>) parts.get(1).get("inlineData");
        assertEquals("image/jpeg", inline.get("mimeType"));
        assertEquals(java.util.Base64.getEncoder().encodeToString("img".getBytes()), inline.get("data"));
    }

    @Test
    void askQuestion_googleSelectedWithoutKey_namesTheMissingKey() {
        stubGoogleUser("  ");

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestion("q"));
        assertEquals("Google API key not configured. Add it in Settings.", ex.getMessage());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void askQuestion_googleInvalidKey_reportsInvalidKey() {
        stubGoogleUser("secret-google-key");
        when(restTemplate.postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                new HttpHeaders(),
                "{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.UTF_8));

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestion("q"));
        assertEquals("Google API key is invalid", ex.getMessage());
        assertFalse(ex.getMessage().contains("secret-google-key"));
        verify(restTemplate, times(1)).postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, never()).postForEntity(
                eq(AskGrok.geminiGenerateUrl("gemini-3.8-flash")), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void askQuestion_geminiRateLimit_reportsRateLimit() {
        stubGoogleUser("google-test-key");
        AtomicLong clock = new AtomicLong(1_000_000L);
        AtomicLong paused = new AtomicLong(-1);
        ReflectionTestUtils.setField(askGrok, "geminiNow", (LongSupplier) clock::get);
        ReflectionTestUtils.setField(askGrok, "geminiPause", (LongConsumer) paused::set);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(geminiClientError(HttpStatus.TOO_MANY_REQUESTS, "{}"));

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestion("q"));
        assertEquals("Gemini rate limit reached. Try again later.", ex.getMessage());
        assertFalse(ex.getMessage().contains("generativelanguage.googleapis.com"));
        assertEquals(-1L, paused.get());
        verify(restTemplate, times(1)).postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, never()).postForEntity(
                eq(AskGrok.geminiGenerateUrl("gemini-3.8-flash")), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void askQuestionFast_gemini429_waitsBeforeAnyModelIsCalledAgain() {
        stubGoogleUser("google-test-key");
        AtomicLong clock = new AtomicLong(1_000_000L);
        AtomicLong paused = new AtomicLong(-1);
        ReflectionTestUtils.setField(askGrok, "geminiNow", (LongSupplier) clock::get);
        ReflectionTestUtils.setField(askGrok, "geminiPause", (LongConsumer) paused::set);
        when(restTemplate.postForEntity(eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(geminiClientError(HttpStatus.TOO_MANY_REQUESTS, "{}"));

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestionFast("tag this"));
        assertEquals("Gemini rate limit reached. Try again later.", ex.getMessage());
        assertEquals(-1L, paused.get());
        verify(restTemplate, times(1)).postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, never()).postForEntity(
                eq(AskGrok.geminiGenerateUrl("gemini-3.8-flash")), any(HttpEntity.class), eq(Map.class));

        clearInvocations(restTemplate);
        ex = assertThrows(LibraryException.class, () -> askGrok.askQuestionFast("tag the next"));
        assertEquals("Gemini rate limit reached. Try again later.", ex.getMessage());
        verifyNoInteractions(restTemplate);

        clock.addAndGet(AskGrok.GEMINI_RATE_LIMIT_COOLDOWN_MS);
        when(restTemplate.postForEntity(eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(geminiBody("fiction", false), HttpStatus.OK));

        assertEquals("fiction", askGrok.askQuestionFast("tag after the pause"));
        verify(restTemplate, times(1)).postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void askQuestionFast_gemini429_doesNotPauseInsideTheRequest() {
        stubGoogleUser("google-test-key");
        AtomicLong clock = new AtomicLong(1_000_000L);
        ReflectionTestUtils.setField(askGrok, "geminiNow", (LongSupplier) clock::get);
        ReflectionTestUtils.setField(askGrok, "geminiPause", (LongConsumer) clock::addAndGet);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(geminiClientError(HttpStatus.TOO_MANY_REQUESTS, "{}"));

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestionFast("tag this"));
        assertEquals("Gemini rate limit reached. Try again later.", ex.getMessage());
        assertEquals(1_000_000L, clock.get());
        verify(restTemplate, times(1)).postForEntity(
                anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void askQuestionFast_gemini503_fallsBackToNextFreeModel() {
        stubGoogleUser("google-test-key");
        java.util.concurrent.atomic.AtomicLong paused = new java.util.concurrent.atomic.AtomicLong(-1);
        ReflectionTestUtils.setField(askGrok, "geminiPause", (LongConsumer) paused::set);
        HttpServerErrorException unavailable = geminiServerError(HttpStatus.SERVICE_UNAVAILABLE,
                "{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\",\"message\":\"high demand\"}}");
        when(restTemplate.postForEntity(eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(unavailable);
        when(restTemplate.postForEntity(
                eq(AskGrok.geminiGenerateUrl("gemini-3.8-flash")), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(geminiBody("fiction", false), HttpStatus.OK));

        assertEquals("fiction", askGrok.askQuestionFast("tag this"));
        assertEquals(1000L, paused.get());
        verify(restTemplate, times(2)).postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class));
        verify(restTemplate, never()).postForEntity(
                eq(AskGrok.geminiGenerateUrl("gemini-3.5-flash-lite")), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void askQuestionFast_gemini503OnEveryFreeModel_hidesTheRawError() {
        stubGoogleUser("google-test-key");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(geminiServerError(HttpStatus.SERVICE_UNAVAILABLE,
                        "{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\"}}"));

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestionFast("tag this"));
        assertEquals("Gemini is busy right now. Try again later.", ex.getMessage());
        assertFalse(ex.getMessage().contains("generativelanguage.googleapis.com"));
        assertFalse(ex.getMessage().contains("UNAVAILABLE"));
        verify(restTemplate, times(AskGrok.GEMINI_FREE_MODELS.size() * 2)).postForEntity(
                anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void askQuestionFast_thoughtOnlyMaxTokens_retriesWithALargerBudget() {
        stubGoogleUser("google-test-key");
        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL), entityCaptor.capture(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(geminiThoughtsOnly("MAX_TOKENS"), HttpStatus.OK))
                .thenReturn(new ResponseEntity<>(geminiBody("fiction", false), HttpStatus.OK));

        assertEquals("fiction", askGrok.askQuestionFast("tag this"));
        assertEquals(2, entityCaptor.getAllValues().size());
        assertEquals(2048, outputTokens(entityCaptor.getAllValues().get(0)));
        assertEquals(4096, outputTokens(entityCaptor.getAllValues().get(1)));
        verify(restTemplate, never()).postForEntity(
                eq(AskGrok.geminiGenerateUrl("gemini-3.8-flash")), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void askQuestionFast_thoughtOnlyOnEveryModel_namesTheFinishReason() {
        stubGoogleUser("google-test-key");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(geminiThoughtsOnly("MAX_TOKENS"), HttpStatus.OK));

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestionFast("tag this"));
        assertEquals("Gemini returned no answer (MAX_TOKENS).", ex.getMessage());
        assertFalse(ex.getMessage().contains("Unexpected response format"));
    }

    @Test
    void askQuestionFast_safetyBlock_doesNotTryAnotherModel() {
        stubGoogleUser("google-test-key");
        when(restTemplate.postForEntity(
                eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(geminiThoughtsOnly("SAFETY"), HttpStatus.OK));

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestionFast("tag this"));
        assertEquals("Gemini blocked the prompt (SAFETY).", ex.getMessage());
        verify(restTemplate, never()).postForEntity(
                eq(AskGrok.geminiGenerateUrl("gemini-3.8-flash")), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void askQuestionFast_fallbackRejectsThinking_retriesWithoutThinkingConfig() {
        stubGoogleUser("google-test-key");
        when(restTemplate.postForEntity(eq(AskGrok.GEMINI_GENERATE_URL), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(geminiServerError(HttpStatus.SERVICE_UNAVAILABLE, "{\"error\":{\"code\":503}}"));
        @SuppressWarnings("rawtypes")
        org.mockito.ArgumentCaptor<HttpEntity> entityCaptor =
                org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForEntity(
                eq(AskGrok.geminiGenerateUrl("gemini-3.8-flash")), entityCaptor.capture(), eq(Map.class)))
                .thenThrow(geminiClientError(HttpStatus.BAD_REQUEST,
                        "{\"error\":{\"message\":\"Thinking level is not supported for this model\"}}"))
                .thenReturn(new ResponseEntity<>(geminiBody("fiction", false), HttpStatus.OK));

        assertEquals("fiction", askGrok.askQuestionFast("tag this"));
        assertEquals(2, entityCaptor.getAllValues().size());
        @SuppressWarnings("unchecked")
        Map<String, Object> generation = (Map<String, Object>)
                ((Map<String, Object>) entityCaptor.getAllValues().get(1).getBody()).get("generationConfig");
        assertFalse(generation.containsKey("thinkingConfig"));
        assertEquals(2048, generation.get("maxOutputTokens"));
    }

    @Test
    void askQuestion_neitherProvider_namesTheMissingSelection() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");
        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.getXaiApiKey()).thenReturn("test-api-key");
        when(userDto.isUseXaiApiKey()).thenReturn(false);
        when(userDto.isUseGoogleApiKey()).thenReturn(false);
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestion("q"));
        assertEquals("No AI provider is enabled. Turn on the xAI or Google API key in Settings.", ex.getMessage());
    }

    @Test
    void askQuestion_bothProviders_rejectsTheSelection() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");
        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.isUseXaiApiKey()).thenReturn(true);
        when(userDto.isUseGoogleApiKey()).thenReturn(true);
        when(userDto.getXaiApiKey()).thenReturn("x");
        when(userDto.getGoogleApiKey()).thenReturn("g");
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);

        LibraryException ex = assertThrows(LibraryException.class, () -> askGrok.askQuestion("q"));
        assertEquals("Enable only one AI provider in Settings", ex.getMessage());
    }

    private void stubGoogleUser(String apiKey) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("123");
        var userDto = mock(com.muczynski.library.dto.UserDto.class);
        when(userDto.isUseGoogleApiKey()).thenReturn(true);
        when(userDto.isUseXaiApiKey()).thenReturn(false);
        when(userDto.getGoogleApiKey()).thenReturn(apiKey);
        when(userSettingsService.getUserSettings(123L)).thenReturn(userDto);
    }

    private static Map<String, Object> geminiBody(String answer, boolean includeThought) {
        List<Map<String, Object>> parts = new ArrayList<>();
        if (includeThought) {
            Map<String, Object> thought = new HashMap<>();
            thought.put("thought", true);
            thought.put("text", "hidden reasoning");
            parts.add(thought);
        }
        Map<String, Object> text = new HashMap<>();
        text.put("text", answer);
        parts.add(text);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("content", content);
        Map<String, Object> body = new HashMap<>();
        body.put("candidates", List.of(candidate));
        return body;
    }

    private static Map<String, Object> geminiThoughtsOnly(String finishReason) {
        Map<String, Object> thought = new HashMap<>();
        thought.put("thought", true);
        thought.put("text", "hidden reasoning");
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(thought));
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("content", content);
        candidate.put("finishReason", finishReason);
        Map<String, Object> body = new HashMap<>();
        body.put("candidates", List.of(candidate));
        return body;
    }

    @SuppressWarnings("unchecked")
    private static int outputTokens(HttpEntity<?> entity) {
        Map<String, Object> request = (Map<String, Object>) entity.getBody();
        Map<String, Object> generation = (Map<String, Object>) request.get("generationConfig");
        return (Integer) generation.get("maxOutputTokens");
    }

    private static HttpClientErrorException geminiClientError(HttpStatus status, String body) {
        return HttpClientErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(),
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private static HttpServerErrorException geminiServerError(HttpStatus status, String body) {
        return HttpServerErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(),
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
