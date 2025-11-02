/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BooksFromFeedServiceTest {

    @Mock
    private GooglePhotosService googlePhotosService;

    @Mock
    private AskGrok askGrok;

    @Mock
    private UserSettingsService userSettingsService;

    @Mock
    private BookService bookService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private BooksFromFeedService booksFromFeedService;

    private UserDto testUser;

    @BeforeEach
    void setUp() {
        // Set up security context
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        // Set up test user
        testUser = new UserDto();
        testUser.setUsername("testuser");
        testUser.setGooglePhotosApiKey("test-api-key");
        testUser.setLastPhotoTimestamp("2025-01-01T00:00:00Z");

        lenient().when(userSettingsService.getUserSettings("testuser")).thenReturn(testUser);
    }

    @Test
    void processPhotosFromFeed_withNoPhotos_returnsEmptyResults() {
        // Arrange
        when(googlePhotosService.fetchPhotos(anyString())).thenReturn(new ArrayList<>());

        // Act
        Map<String, Object> result = booksFromFeedService.processPhotosFromFeed();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.get("processedCount"));
        assertEquals(0, result.get("skippedCount"));
        assertEquals(0, result.get("totalPhotos"));
        verify(googlePhotosService).fetchPhotos(anyString());
        verify(userSettingsService).updateUserSettings(anyString(), any(UserSettingsDto.class));
    }

    @Test
    void processPhotosFromFeed_withAlreadyProcessedPhoto_skipsPhoto() {
        // Arrange
        Map<String, Object> photo = createTestPhoto("photo1", "Title: Book 1\nAuthor: Author 1");
        List<Map<String, Object>> photos = Collections.singletonList(photo);

        when(googlePhotosService.fetchPhotos(anyString())).thenReturn(photos);

        // Act
        Map<String, Object> result = booksFromFeedService.processPhotosFromFeed();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.get("processedCount"));
        assertEquals(1, result.get("skippedCount"));
        assertEquals(1, result.get("totalPhotos"));

        List<Map<String, Object>> skippedPhotos = (List<Map<String, Object>>) result.get("skippedPhotos");
        assertEquals(1, skippedPhotos.size());
        assertTrue(skippedPhotos.get(0).get("reason").toString().contains("Already processed"));

        verify(googlePhotosService, never()).downloadPhoto(anyString());
    }

    @Test
    void processPhotosFromFeed_withNonBookPhoto_skipsPhoto() {
        // Arrange
        Map<String, Object> photo = createTestPhoto("photo1", null);
        List<Map<String, Object>> photos = Collections.singletonList(photo);

        when(googlePhotosService.fetchPhotos(anyString())).thenReturn(photos);
        when(googlePhotosService.downloadPhoto(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(askGrok.askAboutPhoto(any(byte[].class), anyString(), anyString())).thenReturn("NO");

        // Act
        Map<String, Object> result = booksFromFeedService.processPhotosFromFeed();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.get("processedCount"));
        assertEquals(1, result.get("skippedCount"));
        assertEquals(1, result.get("totalPhotos"));

        List<Map<String, Object>> skippedPhotos = (List<Map<String, Object>>) result.get("skippedPhotos");
        assertEquals(1, skippedPhotos.size());
        assertEquals("Not a book photo", skippedPhotos.get(0).get("reason"));

        verify(googlePhotosService).downloadPhoto(anyString());
        verify(askGrok).askAboutPhoto(any(byte[].class), anyString(), contains("Is this image a photo of a book"));
    }

    @Test
    void processPhotosFromFeed_withValidBookPhoto_createsBook() {
        // Arrange
        Map<String, Object> photo = createTestPhoto("photo1", null);
        List<Map<String, Object>> photos = Collections.singletonList(photo);

        byte[] photoBytes = new byte[]{1, 2, 3};
        String aiDetectionResponse = "YES, this is a book";
        String aiMetadataResponse = "{\"title\": \"Test Book\", \"author\": \"Test Author\"}";

        BookDto createdBook = new BookDto();
        createdBook.setId(123L);
        createdBook.setTitle("Test Book");
        createdBook.setStatus(BookStatus.ACTIVE);

        when(googlePhotosService.fetchPhotos(anyString())).thenReturn(photos);
        when(googlePhotosService.downloadPhoto(anyString())).thenReturn(photoBytes);
        when(askGrok.askAboutPhoto(any(byte[].class), anyString(), contains("Is this image")))
                .thenReturn(aiDetectionResponse);
        when(askGrok.askAboutPhoto(any(byte[].class), anyString(), contains("extract the book information")))
                .thenReturn(aiMetadataResponse);
        when(bookService.createBook(any(BookDto.class))).thenReturn(createdBook);

        // Act
        Map<String, Object> result = booksFromFeedService.processPhotosFromFeed();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.get("processedCount"));
        assertEquals(0, result.get("skippedCount"));
        assertEquals(1, result.get("totalPhotos"));

        List<Map<String, Object>> processedBooks = (List<Map<String, Object>>) result.get("processedBooks");
        assertEquals(1, processedBooks.size());
        assertEquals("Test Book", processedBooks.get(0).get("title"));
        assertEquals("Test Author", processedBooks.get(0).get("author"));
        assertEquals(123L, processedBooks.get(0).get("bookId"));

        verify(googlePhotosService).downloadPhoto(anyString());
        verify(googlePhotosService).updatePhotoDescription(eq("photo1"), contains("Test Book"));
        verify(bookService).createBook(any(BookDto.class));
        verify(userSettingsService).updateUserSettings(anyString(), any(UserSettingsDto.class));
    }

    @Test
    void processPhotosFromFeed_withInvalidJsonResponse_skipsPhoto() {
        // Arrange
        Map<String, Object> photo = createTestPhoto("photo1", null);
        List<Map<String, Object>> photos = Collections.singletonList(photo);

        byte[] photoBytes = new byte[]{1, 2, 3};
        String aiDetectionResponse = "YES";
        String aiMetadataResponse = "Invalid JSON response";

        when(googlePhotosService.fetchPhotos(anyString())).thenReturn(photos);
        when(googlePhotosService.downloadPhoto(anyString())).thenReturn(photoBytes);
        when(askGrok.askAboutPhoto(any(byte[].class), anyString(), contains("Is this image")))
                .thenReturn(aiDetectionResponse);
        when(askGrok.askAboutPhoto(any(byte[].class), anyString(), contains("extract the book information")))
                .thenReturn(aiMetadataResponse);

        // Act
        Map<String, Object> result = booksFromFeedService.processPhotosFromFeed();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.get("processedCount"));
        assertEquals(1, result.get("skippedCount"));

        List<Map<String, Object>> skippedPhotos = (List<Map<String, Object>>) result.get("skippedPhotos");
        assertEquals(1, skippedPhotos.size());
        assertEquals("Could not extract book metadata", skippedPhotos.get(0).get("reason"));

        verify(bookService, never()).createBook(any(BookDto.class));
    }

    @Test
    void processPhotosFromFeed_withException_skipsPhoto() {
        // Arrange
        Map<String, Object> photo = createTestPhoto("photo1", null);
        List<Map<String, Object>> photos = Collections.singletonList(photo);

        when(googlePhotosService.fetchPhotos(anyString())).thenReturn(photos);
        when(googlePhotosService.downloadPhoto(anyString())).thenThrow(new RuntimeException("Network error"));

        // Act
        Map<String, Object> result = booksFromFeedService.processPhotosFromFeed();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.get("processedCount"));
        assertEquals(1, result.get("skippedCount"));

        List<Map<String, Object>> skippedPhotos = (List<Map<String, Object>>) result.get("skippedPhotos");
        assertEquals(1, skippedPhotos.size());
        assertTrue(skippedPhotos.get(0).get("reason").toString().contains("Error"));
    }

    @Test
    void processPhotosFromFeed_withNoLastTimestamp_usesYesterday() {
        // Arrange
        testUser.setLastPhotoTimestamp(null);
        when(userSettingsService.getUserSettings("testuser")).thenReturn(testUser);
        when(googlePhotosService.fetchPhotos(anyString())).thenReturn(new ArrayList<>());

        // Act
        Map<String, Object> result = booksFromFeedService.processPhotosFromFeed();

        // Assert
        assertNotNull(result);
        verify(googlePhotosService).fetchPhotos(argThat(timestamp ->
                timestamp != null && !timestamp.isEmpty()
        ));
    }

    @Test
    void processPhotosFromFeed_updatesLastTimestamp() {
        // Arrange
        Map<String, Object> photo1 = createTestPhoto("photo1", null);
        Map<String, Object> photo2 = createTestPhoto("photo2", null);

        // Set different timestamps
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("creationTime", "2025-01-01T10:00:00Z");
        photo1.put("mediaMetadata", metadata1);

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("creationTime", "2025-01-02T10:00:00Z");
        photo2.put("mediaMetadata", metadata2);

        List<Map<String, Object>> photos = Arrays.asList(photo1, photo2);

        when(googlePhotosService.fetchPhotos(anyString())).thenReturn(photos);
        when(googlePhotosService.downloadPhoto(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(askGrok.askAboutPhoto(any(byte[].class), anyString(), anyString())).thenReturn("NO");

        // Act
        Map<String, Object> result = booksFromFeedService.processPhotosFromFeed();

        // Assert
        assertNotNull(result);
        assertEquals("2025-01-02T10:00:00Z", result.get("lastTimestamp"));
        verify(userSettingsService).updateUserSettings(eq("testuser"), argThat(dto ->
                "2025-01-02T10:00:00Z".equals(dto.getLastPhotoTimestamp())
        ));
    }

    @Test
    void processPhotosFromFeed_withoutAuthentication_throwsException() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            booksFromFeedService.processPhotosFromFeed();
        });

        assertTrue(exception.getMessage().contains("No authenticated user found"));
    }

    // Helper method to create test photos
    private Map<String, Object> createTestPhoto(String id, String description) {
        Map<String, Object> photo = new HashMap<>();
        photo.put("id", id);
        photo.put("baseUrl", "https://example.com/" + id);
        if (description != null) {
            photo.put("description", description);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("creationTime", "2025-01-01T00:00:00Z");
        photo.put("mediaMetadata", metadata);

        return photo;
    }
}
