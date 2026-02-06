/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.AuthorRepository;
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

/**
 * Tests for BooksFromFeedService
 *
 * Note: Tests for processPhotosFromFeed method have been removed as the service
 * was refactored to use Google Photos Picker API. The new workflow is:
 * 1. savePhotosFromPicker - saves selected photos from picker to database
 * 2. processSavedPhotos - processes the saved photos with AI
 */
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
    private AuthorRepository authorRepository;

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
        lenient().when(authentication.getName()).thenReturn("1");
        SecurityContextHolder.setContext(securityContext);

        // Set up test user
        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setGooglePhotosApiKey("test-api-key");
        testUser.setLastPhotoTimestamp("2025-01-01T00:00:00Z");

        lenient().when(userSettingsService.getUserSettings(1L)).thenReturn(testUser);
    }

    @Test
    void processSingleBook_success() {
        BookDto tempBook = new BookDto();
        tempBook.setId(1L);
        tempBook.setTitle("2025-01-10_14:30:00");

        BookDto processedBook = new BookDto();
        processedBook.setId(1L);
        processedBook.setTitle("The Great Gatsby");
        processedBook.setAuthorId(10L);

        Author author = new Author();
        author.setId(10L);
        author.setName("F. Scott Fitzgerald");

        when(bookService.getBookById(1L)).thenReturn(tempBook);
        when(bookService.generateTempBook(1L)).thenReturn(processedBook);
        when(authorRepository.findById(10L)).thenReturn(Optional.of(author));

        Map<String, Object> result = booksFromFeedService.processSingleBook(1L);

        assertEquals(true, result.get("success"));
        assertEquals(1L, result.get("bookId"));
        assertEquals("The Great Gatsby", result.get("title"));
        assertEquals("F. Scott Fitzgerald", result.get("author"));
        assertEquals("2025-01-10_14:30:00", result.get("originalTitle"));
    }

    @Test
    void processSingleBook_returnsErrorWithRootCause() {
        BookDto tempBook = new BookDto();
        tempBook.setId(1L);
        tempBook.setTitle("2025-01-10_14:30:00");

        RuntimeException rootCause = new RuntimeException("Connection refused to AI service");
        LibraryException wrappedException = new LibraryException("AI analysis failed", rootCause);

        when(bookService.getBookById(1L)).thenReturn(tempBook);
        when(bookService.generateTempBook(1L)).thenThrow(wrappedException);

        Map<String, Object> result = booksFromFeedService.processSingleBook(1L);

        assertEquals(false, result.get("success"));
        assertEquals(1L, result.get("bookId"));
        String error = (String) result.get("error");
        assertNotNull(error);
        assertTrue(error.contains("Connection refused to AI service"),
                "Error should contain root cause message, got: " + error);
    }

    @Test
    void processSingleBook_bookNotFound() {
        when(bookService.getBookById(999L)).thenReturn(null);

        Map<String, Object> result = booksFromFeedService.processSingleBook(999L);

        assertEquals(false, result.get("success"));
        assertEquals(999L, result.get("bookId"));
        String error = (String) result.get("error");
        assertTrue(error.contains("Book not found"));
    }

    @Test
    void processSavedPhotos_processesMultipleBooks() {
        BookDto book1 = new BookDto();
        book1.setId(1L);
        book1.setTitle("2025-01-10_14:30:00");

        BookDto book2 = new BookDto();
        book2.setId(2L);
        book2.setTitle("2025-01-10_14:31:00");

        BookDto processed1 = new BookDto();
        processed1.setId(1L);
        processed1.setTitle("Book One");
        processed1.setAuthorId(10L);

        Author author = new Author();
        author.setId(10L);
        author.setName("Author One");

        when(bookService.getBooksWithTemporaryTitles()).thenReturn(List.of(book1, book2));
        when(bookService.generateTempBook(1L)).thenReturn(processed1);
        when(bookService.generateTempBook(2L)).thenThrow(new RuntimeException("AI timeout"));
        when(authorRepository.findById(10L)).thenReturn(Optional.of(author));

        Map<String, Object> result = booksFromFeedService.processSavedPhotos();

        assertEquals(1, result.get("processedCount"));
        assertEquals(1, result.get("failedCount"));
        assertEquals(2, result.get("totalBooks"));
    }
}
