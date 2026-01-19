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
}
