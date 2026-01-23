/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.FreeTextBulkLookupResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreeTextLookupServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private FreeTextProvider mockProvider1;

    @Mock
    private FreeTextProvider mockProvider2;

    private FreeTextLookupService service;

    @BeforeEach
    void setUp() {
        // Set up provider priorities
        when(mockProvider1.getProviderName()).thenReturn("Provider1");
        when(mockProvider1.getPriority()).thenReturn(10);

        when(mockProvider2.getProviderName()).thenReturn("Provider2");
        when(mockProvider2.getPriority()).thenReturn(20);

        // Create service with mocked providers
        List<FreeTextProvider> providers = new ArrayList<>(Arrays.asList(mockProvider1, mockProvider2));
        service = new FreeTextLookupService(bookRepository, providers);
        service.init(); // This sorts providers by priority
    }

    @Test
    void lookupBook_findsUrlFromFirstProvider() {
        Book book = createBook(1L, "Pride and Prejudice", "Jane Austen");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(mockProvider1.search(eq("Pride and Prejudice"), eq("Jane Austen")))
                .thenReturn(FreeTextLookupResult.success("Provider1", "https://example.com/book"));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        FreeTextBulkLookupResultDto result = service.lookupBook(1L);

        assertTrue(result.isSuccess());
        assertEquals("https://example.com/book", result.getFreeTextUrl());
        assertEquals("Provider1", result.getProviderName());
        assertEquals(1, result.getProvidersSearched().size());
        verify(mockProvider2, never()).search(anyString(), anyString());
    }

    @Test
    void lookupBook_fallsBackToSecondProvider() {
        Book book = createBook(1L, "Some Book", "Some Author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(mockProvider1.search(eq("Some Book"), eq("Some Author")))
                .thenReturn(FreeTextLookupResult.error("Provider1", "Not found"));
        when(mockProvider2.search(eq("Some Book"), eq("Some Author")))
                .thenReturn(FreeTextLookupResult.success("Provider2", "https://example2.com/book"));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        FreeTextBulkLookupResultDto result = service.lookupBook(1L);

        assertTrue(result.isSuccess());
        assertEquals("https://example2.com/book", result.getFreeTextUrl());
        assertEquals("Provider2", result.getProviderName());
        assertEquals(2, result.getProvidersSearched().size());
    }

    @Test
    void lookupBook_returnsErrorWhenNoProviderFindsUrl() {
        Book book = createBook(1L, "Obscure Book", "Unknown Author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(mockProvider1.search(anyString(), anyString()))
                .thenReturn(FreeTextLookupResult.error("Provider1", "Not found"));
        when(mockProvider2.search(anyString(), anyString()))
                .thenReturn(FreeTextLookupResult.error("Provider2", "Not found"));

        FreeTextBulkLookupResultDto result = service.lookupBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not found in any provider", result.getErrorMessage());
        assertEquals(2, result.getProvidersSearched().size());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void lookupBook_skipsTemporaryTitle() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("2025-01-15_123456_book.jpg");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        FreeTextBulkLookupResultDto result = service.lookupBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Temporary title - skipped", result.getErrorMessage());
        assertTrue(result.getProvidersSearched().isEmpty());
        verify(mockProvider1, never()).search(anyString(), anyString());
    }

    @Test
    void lookupBook_throwsExceptionWhenBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(LibraryException.class, () -> service.lookupBook(999L));
    }

    @Test
    void lookupBook_handlesBookWithoutAuthor() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Anonymous Book");
        book.setAuthor(null);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(mockProvider1.search(eq("Anonymous Book"), isNull()))
                .thenReturn(FreeTextLookupResult.success("Provider1", "https://example.com/book"));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        FreeTextBulkLookupResultDto result = service.lookupBook(1L);

        assertTrue(result.isSuccess());
        assertNull(result.getAuthorName());
    }

    @Test
    void lookupBooks_processesMultipleBooks() {
        Book book1 = createBook(1L, "Book One", "Author One");
        Book book2 = createBook(2L, "Book Two", "Author Two");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book1));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book2));

        when(mockProvider1.search(eq("Book One"), eq("Author One")))
                .thenReturn(FreeTextLookupResult.success("Provider1", "https://example.com/book1"));
        when(mockProvider1.search(eq("Book Two"), eq("Author Two")))
                .thenReturn(FreeTextLookupResult.error("Provider1", "Not found"));
        when(mockProvider2.search(eq("Book Two"), eq("Author Two")))
                .thenReturn(FreeTextLookupResult.error("Provider2", "Not found"));

        when(bookRepository.save(any(Book.class))).thenReturn(book1);

        List<FreeTextBulkLookupResultDto> results = service.lookupBooks(Arrays.asList(1L, 2L));

        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccess());
        assertFalse(results.get(1).isSuccess());
    }

    @Test
    void lookupBooks_handlesExceptionForSingleBook() {
        when(bookRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        List<FreeTextBulkLookupResultDto> results = service.lookupBooks(Arrays.asList(1L));

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertTrue(results.get(0).getErrorMessage().contains("Error"));
    }

    @Test
    void lookupBook_updatesBookWithFoundUrl() {
        Book book = createBook(1L, "Test Book", "Test Author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(mockProvider1.search(anyString(), anyString()))
                .thenReturn(FreeTextLookupResult.success("Provider1", "https://example.com/book"));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.lookupBook(1L);

        verify(bookRepository).save(argThat(savedBook ->
                "https://example.com/book".equals(savedBook.getFreeTextUrl()) &&
                savedBook.getLastModified() != null
        ));
    }

    @Test
    void getProviderNames_returnsProvidersInPriorityOrder() {
        List<String> names = service.getProviderNames();

        assertEquals(2, names.size());
        assertEquals("Provider1", names.get(0)); // Priority 10
        assertEquals("Provider2", names.get(1)); // Priority 20
    }

    private Book createBook(Long id, String title, String authorName) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);

        if (authorName != null) {
            Author author = new Author();
            author.setName(authorName);
            book.setAuthor(author);
        }

        return book;
    }
}
