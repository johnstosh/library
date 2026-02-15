/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.LocLookupResultDto;
import com.muczynski.library.model.LocCallNumberResponse;
import com.muczynski.library.model.LocSearchRequest;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocBulkLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private LocCatalogService locCatalogService;

    @Mock
    private AskGrok askGrok;

    @InjectMocks
    private LocBulkLookupService locBulkLookupService;

    @Test
    void lookupAndUpdateBook_aiSuggestFallback_whenAllStrategiesFail() {
        // Arrange - book without colon in title (skips strategies 3 & 4)
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(locCatalogService.getLocCallNumber(any(LocSearchRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No results found"));
        when(askGrok.suggestLocNumber("Test Book", "Test Author")).thenReturn("PS3511.I9 G7");
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        // Act
        LocLookupResultDto result = locBulkLookupService.lookupAndUpdateBook(1L);

        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.isAiSuggested());
        assertEquals("PS3511.I9 G7", result.getLocNumber());
        verify(askGrok).suggestLocNumber("Test Book", "Test Author");
    }

    @Test
    void lookupAndUpdateBook_aiSuggestFallback_whenAiAlsoFails() {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(locCatalogService.getLocCallNumber(any(LocSearchRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No results found"));
        when(askGrok.suggestLocNumber("Test Book", "Test Author"))
                .thenThrow(new RuntimeException("No API key configured"));

        // Act
        LocLookupResultDto result = locBulkLookupService.lookupAndUpdateBook(1L);

        // Assert
        assertFalse(result.isSuccess());
        assertFalse(result.isAiSuggested());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void lookupAndUpdateBook_aiSuggestFallback_afterTruncatedTitleFails() {
        // Arrange - book with colon in title (exercises strategies 3 & 4 before AI)
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Main Title: Subtitle");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(locCatalogService.getLocCallNumber(any(LocSearchRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No results found"));
        when(askGrok.suggestLocNumber("Main Title: Subtitle", "Test Author")).thenReturn("BX1234.A5");
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        // Act
        LocLookupResultDto result = locBulkLookupService.lookupAndUpdateBook(1L);

        // Assert
        assertTrue(result.isSuccess());
        assertTrue(result.isAiSuggested());
        assertEquals("BX1234.A5", result.getLocNumber());
    }

    @Test
    void lookupAndUpdateBook_locSuccess_noAiFallback() {
        // Arrange - LOC lookup succeeds on first strategy
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);

        LocCallNumberResponse response = LocCallNumberResponse.builder()
                .callNumber("PS3511.I9 G7")
                .matchCount(1)
                .build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(locCatalogService.getLocCallNumber(any(LocSearchRequest.class))).thenReturn(response);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        // Act
        LocLookupResultDto result = locBulkLookupService.lookupAndUpdateBook(1L);

        // Assert
        assertTrue(result.isSuccess());
        assertFalse(result.isAiSuggested());
        assertEquals("PS3511.I9 G7", result.getLocNumber());
        verifyNoInteractions(askGrok);
    }
}
