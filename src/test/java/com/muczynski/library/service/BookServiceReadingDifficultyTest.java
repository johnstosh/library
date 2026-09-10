/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.dto.ReadingDifficultyLookupResultDto;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceReadingDifficultyTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AskGrok askGrok;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BookService bookService;

    @Test
    void lookupReadingDifficultyForBooks_fillsUnsetAndSkipsAlreadySet() {
        Book unset = book(1L, "Little Women", ReadingDifficulty.UNSET);
        Book already = book(2L, "Summa", ReadingDifficulty.DEMANDING);
        when(bookRepository.findAllById(List.of(1L, 2L, 99L))).thenReturn(List.of(unset, already));
        when(askGrok.suggestReadingDifficulties(anyList())).thenReturn(List.of("children"));
        BookDto updated = new BookDto();
        updated.setId(1L);
        updated.setTitle("Little Women");
        updated.setReadingDifficulty(ReadingDifficulty.CHILDREN);
        when(bookMapper.toDto(unset)).thenReturn(updated);
        when(bookRepository.save(unset)).thenReturn(unset);

        List<ReadingDifficultyLookupResultDto> results =
                bookService.lookupReadingDifficultyForBooks(List.of(1L, 2L, 99L));

        assertEquals(3, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals(ReadingDifficulty.CHILDREN, results.get(0).getSuggestedDifficulty());
        assertEquals("Little Women", results.get(0).getTitle());
        assertFalse(results.get(1).isSuccess());
        assertEquals("Already has a reading difficulty", results.get(1).getErrorMessage());
        assertEquals(ReadingDifficulty.DEMANDING, results.get(1).getSuggestedDifficulty());
        assertFalse(results.get(2).isSuccess());
        assertEquals("Book not found", results.get(2).getErrorMessage());
        assertEquals(ReadingDifficulty.CHILDREN, unset.getReadingDifficulty());
        verify(bookRepository).save(unset);
        verify(bookRepository, never()).save(already);
    }

    @Test
    void lookupReadingDifficultyForBooks_batchesTenAtATime() {
        List<Long> ids = new ArrayList<>();
        List<Book> books = new ArrayList<>();
        for (long i = 1; i <= 11; i++) {
            ids.add(i);
            books.add(book(i, "Book " + i, ReadingDifficulty.UNSET));
        }
        when(bookRepository.findAllById(ids)).thenReturn(books);
        when(askGrok.suggestReadingDifficulties(anyList())).thenAnswer(invocation -> {
            List<?> jsons = invocation.getArgument(0);
            List<String> keys = new ArrayList<>();
            for (int i = 0; i < jsons.size(); i++) {
                keys.add("accessible");
            }
            return keys;
        });
        when(bookMapper.toDto(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            BookDto dto = new BookDto();
            dto.setId(book.getId());
            dto.setReadingDifficulty(book.getReadingDifficulty());
            return dto;
        });
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ReadingDifficultyLookupResultDto> results = bookService.lookupReadingDifficultyForBooks(ids);

        assertEquals(11, results.size());
        assertTrue(results.stream().allMatch(ReadingDifficultyLookupResultDto::isSuccess));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(askGrok, times(2)).suggestReadingDifficulties(captor.capture());
        assertEquals(10, captor.getAllValues().get(0).size());
        assertEquals(1, captor.getAllValues().get(1).size());
    }

    @Test
    void lookupReadingDifficultyForBooks_unparseableAnswerDoesNotSave() {
        Book unset = book(1L, "Obscure", ReadingDifficulty.UNSET);
        when(bookRepository.findAllById(List.of(1L))).thenReturn(List.of(unset));
        when(askGrok.suggestReadingDifficulties(anyList())).thenReturn(List.of("bogus"));

        List<ReadingDifficultyLookupResultDto> results =
                bookService.lookupReadingDifficultyForBooks(List.of(1L));

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertEquals("Could not determine reading difficulty", results.get(0).getErrorMessage());
        assertEquals(ReadingDifficulty.UNSET, unset.getReadingDifficulty());
        verify(bookRepository, never()).save(any());
        assertNull(results.get(0).getUpdatedBook());
    }

    @Test
    void lookupReadingDifficultyForBooks_emptyInputDoesNotCallGrok() {
        assertEquals(List.of(), bookService.lookupReadingDifficultyForBooks(List.of()));
        verify(askGrok, never()).suggestReadingDifficulties(anyList());
    }

    private static Book book(long id, String title, ReadingDifficulty difficulty) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setReadingDifficulty(difficulty);
        Author author = new Author();
        author.setName("Test Author");
        book.setAuthor(author);
        return book;
    }
}
