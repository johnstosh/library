/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.GenreLookupResultDto;
import com.muczynski.library.exception.LibraryException;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Lookup Genres (#361): AI failures must return success=false
 * without requiring an ambient transaction (no UnexpectedRollbackException).
 */
@ExtendWith(MockitoExtension.class)
class BookServiceGenreLookupTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private AskGrok askGrok;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BookService bookService;

    @Test
    void lookupGenresForBook_whenAskGrokFails_returnsErrorWithoutThrowing() {
        Book book = new Book();
        book.setId(7L);
        book.setTitle("Confessions");
        book.setDetailedDescription("Augustine's autobiography.");

        Author author = new Author();
        author.setName("Augustine");
        book.setAuthor(author);

        BookDto bookDto = new BookDto();
        bookDto.setId(7L);
        bookDto.setTitle("Confessions");

        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(askGrok.suggestGenres(anyString(), anyString()))
                .thenThrow(new LibraryException("xAI API call failed: 429"));

        GenreLookupResultDto result = bookService.lookupGenresForBook(7L);

        assertFalse(result.isSuccess());
        assertEquals(7L, result.getBookId());
        assertEquals("Confessions", result.getTitle());
        assertTrue(result.getErrorMessage().contains("xAI API call failed"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void lookupGenresForBook_whenSuccessful_replacesTagsAndReturnsUpdatedBook() {
        Book book = new Book();
        book.setId(3L);
        book.setTitle("City of God");
        book.setDetailedDescription("A long theological work.");

        BookDto bookDto = new BookDto();
        bookDto.setId(3L);
        bookDto.setTitle("City of God");

        when(bookRepository.findById(3L)).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(bookDto);
        when(askGrok.suggestGenres(anyString(), any())).thenReturn("theology, early-church");
        when(bookRepository.save(book)).thenReturn(book);

        GenreLookupResultDto result = bookService.lookupGenresForBook(3L);

        assertTrue(result.isSuccess());
        assertEquals(List.of("theology", "early-church"), result.getSuggestedGenres());
        assertEquals(List.of("theology", "early-church"), book.getTagsList());
        verify(bookRepository).save(book);
    }

    @Test
    void lookupGenresForBook_whenNoDescription_skipsWithoutCallingGrok() {
        Book book = new Book();
        book.setId(9L);
        book.setTitle("Untitled");
        book.setDetailedDescription("  ");

        when(bookRepository.findById(9L)).thenReturn(Optional.of(book));

        GenreLookupResultDto result = bookService.lookupGenresForBook(9L);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("no description"));
        verify(askGrok, never()).suggestGenres(anyString(), any());
    }
}
