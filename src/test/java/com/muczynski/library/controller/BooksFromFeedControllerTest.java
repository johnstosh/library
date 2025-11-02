/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.service.BooksFromFeedService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooksFromFeedControllerTest {

    @Mock
    private BooksFromFeedService booksFromFeedService;

    @InjectMocks
    private BooksFromFeedController booksFromFeedController;

    @Test
    void processPhotos_withSuccessfulProcessing_returnsOkResponse() {
        // Arrange
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("processedCount", 5);
        serviceResult.put("skippedCount", 2);
        serviceResult.put("totalPhotos", 7);
        serviceResult.put("processedBooks", createProcessedBooksList());
        serviceResult.put("skippedPhotos", createSkippedPhotosList());
        serviceResult.put("lastTimestamp", "2025-01-01T12:00:00Z");

        when(booksFromFeedService.processPhotosFromFeed()).thenReturn(serviceResult);

        // Act
        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processPhotos();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().get("processedCount"));
        assertEquals(2, response.getBody().get("skippedCount"));
        assertEquals(7, response.getBody().get("totalPhotos"));
        assertEquals("2025-01-01T12:00:00Z", response.getBody().get("lastTimestamp"));

        verify(booksFromFeedService).processPhotosFromFeed();
    }

    @Test
    void processPhotos_withNoPhotosProcessed_returnsOkWithZeroCounts() {
        // Arrange
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("processedCount", 0);
        serviceResult.put("skippedCount", 0);
        serviceResult.put("totalPhotos", 0);
        serviceResult.put("processedBooks", new ArrayList<>());
        serviceResult.put("skippedPhotos", new ArrayList<>());
        serviceResult.put("lastTimestamp", "2025-01-01T00:00:00Z");

        when(booksFromFeedService.processPhotosFromFeed()).thenReturn(serviceResult);

        // Act
        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processPhotos();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().get("processedCount"));
        assertEquals(0, response.getBody().get("skippedCount"));
        assertEquals(0, response.getBody().get("totalPhotos"));

        verify(booksFromFeedService).processPhotosFromFeed();
    }

    @Test
    void processPhotos_withServiceException_returnsBadRequest() {
        // Arrange
        String errorMessage = "Google Photos API key not configured";
        when(booksFromFeedService.processPhotosFromFeed())
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processPhotos();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().get("error"));
        assertEquals(0, response.getBody().get("processedCount"));
        assertEquals(0, response.getBody().get("skippedCount"));
        assertEquals(0, response.getBody().get("totalPhotos"));

        verify(booksFromFeedService).processPhotosFromFeed();
    }

    @Test
    void processPhotos_withNetworkException_returnsBadRequest() {
        // Arrange
        String errorMessage = "Failed to fetch photos from Google Photos";
        when(booksFromFeedService.processPhotosFromFeed())
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processPhotos();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").toString().contains("Failed to fetch photos"));

        verify(booksFromFeedService).processPhotosFromFeed();
    }

    @Test
    void processPhotos_withAuthenticationException_returnsBadRequest() {
        // Arrange
        when(booksFromFeedService.processPhotosFromFeed())
                .thenThrow(new RuntimeException("No authenticated user found"));

        // Act
        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processPhotos();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").toString().contains("authenticated"));

        verify(booksFromFeedService).processPhotosFromFeed();
    }

    @Test
    void processPhotos_withMixedResults_returnsPartialSuccess() {
        // Arrange
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("processedCount", 3);
        serviceResult.put("skippedCount", 5);
        serviceResult.put("totalPhotos", 8);

        List<Map<String, Object>> processedBooks = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> book = new HashMap<>();
            book.put("photoId", "photo" + i);
            book.put("title", "Book " + i);
            book.put("author", "Author " + i);
            book.put("bookId", (long) i);
            processedBooks.add(book);
        }
        serviceResult.put("processedBooks", processedBooks);

        List<Map<String, Object>> skippedPhotos = new ArrayList<>();
        for (int i = 4; i <= 8; i++) {
            Map<String, Object> photo = new HashMap<>();
            photo.put("id", "photo" + i);
            photo.put("reason", "Not a book photo");
            skippedPhotos.add(photo);
        }
        serviceResult.put("skippedPhotos", skippedPhotos);
        serviceResult.put("lastTimestamp", "2025-01-01T15:30:00Z");

        when(booksFromFeedService.processPhotosFromFeed()).thenReturn(serviceResult);

        // Act
        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processPhotos();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().get("processedCount"));
        assertEquals(5, response.getBody().get("skippedCount"));
        assertEquals(8, response.getBody().get("totalPhotos"));

        List<Map<String, Object>> returnedBooks = (List<Map<String, Object>>) response.getBody().get("processedBooks");
        assertEquals(3, returnedBooks.size());

        List<Map<String, Object>> returnedSkipped = (List<Map<String, Object>>) response.getBody().get("skippedPhotos");
        assertEquals(5, returnedSkipped.size());

        verify(booksFromFeedService).processPhotosFromFeed();
    }

    // Helper methods
    private List<Map<String, Object>> createProcessedBooksList() {
        List<Map<String, Object>> books = new ArrayList<>();

        Map<String, Object> book1 = new HashMap<>();
        book1.put("photoId", "photo1");
        book1.put("title", "The Great Book");
        book1.put("author", "Famous Author");
        book1.put("bookId", 101L);
        books.add(book1);

        Map<String, Object> book2 = new HashMap<>();
        book2.put("photoId", "photo2");
        book2.put("title", "Another Book");
        book2.put("author", "Another Author");
        book2.put("bookId", 102L);
        books.add(book2);

        return books;
    }

    private List<Map<String, Object>> createSkippedPhotosList() {
        List<Map<String, Object>> skipped = new ArrayList<>();

        Map<String, Object> skip1 = new HashMap<>();
        skip1.put("id", "photo3");
        skip1.put("reason", "Not a book photo");
        skipped.add(skip1);

        Map<String, Object> skip2 = new HashMap<>();
        skip2.put("id", "photo4");
        skip2.put("reason", "Already processed");
        skipped.add(skip2);

        return skipped;
    }
}
