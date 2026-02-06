/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.SavedBookDto;
import com.muczynski.library.service.BookService;
import com.muczynski.library.service.BooksFromFeedService;
import com.muczynski.library.service.GooglePhotosService;
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

/**
 * Tests for BooksFromFeedController
 *
 * Note: Tests for processPhotos endpoint have been removed as the controller
 * was refactored to use Google Photos Picker API. The new endpoints are:
 * - GET /api/books-from-feed/saved-books (delegates to BookService.getBooksFromMostRecentDay)
 * - POST /api/books-from-feed/process-saved-photos
 * - POST /api/books-from-feed/save-photos
 * - POST /api/books-from-feed/picker-session
 * - GET /api/books-from-feed/picker-session/{sessionId}
 * - GET /api/books-from-feed/picker-session/{sessionId}/media-items
 */
@ExtendWith(MockitoExtension.class)
class BooksFromFeedControllerTest {

    @Mock
    private BooksFromFeedService booksFromFeedService;

    @Mock
    private BookService bookService;

    @Mock
    private GooglePhotosService googlePhotosService;

    @InjectMocks
    private BooksFromFeedController booksFromFeedController;

    @Test
    void getSavedBooks_returnsBooks() {
        // The saved-books endpoint now delegates to BookService.getBooksFromMostRecentDay
        // which returns books from most recent day + temporary title books
        SavedBookDto book1 = SavedBookDto.builder()
                .id(1L)
                .title("Recent Book")
                .author("Author 1")
                .library("Library 1")
                .photoCount(2L)
                .needsProcessing(false)
                .locNumber("PS3566.O5")
                .status("ACTIVE")
                .build();

        SavedBookDto book2 = SavedBookDto.builder()
                .id(2L)
                .title("2025-01-10_14:30:00") // Temporary title
                .library("Library 1")
                .photoCount(1L)
                .needsProcessing(true)
                .status("ACTIVE")
                .build();

        when(bookService.getBooksFromMostRecentDay()).thenReturn(Arrays.asList(book1, book2));

        ResponseEntity<List<SavedBookDto>> response = booksFromFeedController.getSavedBooks();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Recent Book", response.getBody().get(0).getTitle());
        assertFalse(response.getBody().get(0).getNeedsProcessing());
        assertTrue(response.getBody().get(1).getNeedsProcessing());
    }

    @Test
    void getSavedBooks_handlesException() {
        when(bookService.getBooksFromMostRecentDay()).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<List<SavedBookDto>> response = booksFromFeedController.getSavedBooks();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void processSingleBook_returnsSuccessResult() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("success", true);
        serviceResult.put("bookId", 42L);
        serviceResult.put("title", "The Great Book");
        serviceResult.put("author", "John Author");
        serviceResult.put("originalTitle", "2025-01-10_14:30:00");

        when(booksFromFeedService.processSingleBook(42L)).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processSingleBook(42L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals("The Great Book", response.getBody().get("title"));
        assertEquals("John Author", response.getBody().get("author"));
    }

    @Test
    void processSingleBook_returnsErrorFromService() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("success", false);
        serviceResult.put("bookId", 42L);
        serviceResult.put("error", "AI analysis failed (caused by: Connection timeout)");

        when(booksFromFeedService.processSingleBook(42L)).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processSingleBook(42L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("success"));
        assertTrue(response.getBody().get("error").toString().contains("Connection timeout"));
    }

    @Test
    void processSingleBook_handlesUnexpectedException() {
        when(booksFromFeedService.processSingleBook(42L)).thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processSingleBook(42L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("success"));
        assertEquals(42L, response.getBody().get("bookId"));
        assertEquals("Unexpected error", response.getBody().get("error"));
    }

    @Test
    void processSavedPhotos_returnsResults() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("processedCount", 3);
        serviceResult.put("failedCount", 1);
        serviceResult.put("totalBooks", 4);

        when(booksFromFeedService.processSavedPhotos()).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processSavedPhotos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().get("processedCount"));
        assertEquals(1, response.getBody().get("failedCount"));
        assertEquals(4, response.getBody().get("totalBooks"));
    }

    @Test
    void processSavedPhotos_handlesException() {
        when(booksFromFeedService.processSavedPhotos()).thenThrow(new RuntimeException("Processing failed"));

        ResponseEntity<Map<String, Object>> response = booksFromFeedController.processSavedPhotos();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().get("processedCount"));
        assertEquals(0, response.getBody().get("failedCount"));
        assertEquals(0, response.getBody().get("totalBooks"));
    }
}
