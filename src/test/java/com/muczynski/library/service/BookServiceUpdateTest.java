/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.BookDto;
import com.muczynski.library.mapper.BookMapper;
import com.muczynski.library.repository.AuthorRepository;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.BranchRepository;
import com.muczynski.library.repository.LoanRepository;
import com.muczynski.library.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceUpdateTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void updateBook_writesClearedAndCheckedAvailabilityWithoutTouchingLookupMetadata() {
        LocalDateTime checked = LocalDateTime.of(2026, 3, 4, 5, 6);
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Confessions");
        book.setYdlAudioAvailable(true);
        book.setYdlPaperAvailable(true);
        book.setYdlEbookAvailable(false);
        book.setEmuAudioAvailable(true);
        book.setEmuPaperAvailable(true);
        book.setEmuEbookAvailable(true);
        book.setAclaAudioAvailable(false);
        book.setAclaPaperAvailable(true);
        book.setAclaEbookAvailable(null);
        book.setWccAudioAvailable(true);
        book.setWccPaperAvailable(true);
        book.setWccEbookAvailable(false);
        book.setMsuAudioAvailable(true);
        book.setMsuPaperAvailable(true);
        book.setMsuEbookAvailable(false);
        book.setYdlLastChecked(checked);
        book.setEmuLastChecked(checked);
        book.setAclaLastChecked(checked);
        book.setWccLastChecked(checked);
        book.setMsuLastChecked(checked);
        book.setYdlLookupError("ydl down");
        book.setEmuLookupError("emu down");
        book.setAclaLookupError("acla down");
        book.setWccLookupError("wcc down");
        book.setMsuLookupError("msu down");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.existsByTitle("Confessions")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookMapper.toDto(any(Book.class))).thenReturn(new BookDto());

        BookDto incoming = new BookDto();
        incoming.setTitle("Confessions");
        incoming.setYdlAudioAvailable(false);
        incoming.setYdlPaperAvailable(true);
        incoming.setYdlEbookAvailable(false);
        incoming.setEmuAudioAvailable(false);
        incoming.setEmuPaperAvailable(false);
        incoming.setEmuEbookAvailable(true);
        incoming.setAclaAudioAvailable(true);
        incoming.setAclaPaperAvailable(false);
        incoming.setAclaEbookAvailable(true);
        incoming.setWccAudioAvailable(false);
        incoming.setWccPaperAvailable(false);
        incoming.setWccEbookAvailable(true);
        incoming.setMsuAudioAvailable(false);
        incoming.setMsuPaperAvailable(false);
        incoming.setMsuEbookAvailable(true);

        bookService.updateBook(1L, incoming);

        ArgumentCaptor<Book> saved = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(saved.capture());
        Book result = saved.getValue();

        assertEquals(Boolean.FALSE, result.getYdlAudioAvailable());
        assertEquals(Boolean.TRUE, result.getYdlPaperAvailable());
        assertEquals(Boolean.FALSE, result.getYdlEbookAvailable());
        assertEquals(Boolean.FALSE, result.getEmuAudioAvailable());
        assertEquals(Boolean.FALSE, result.getEmuPaperAvailable());
        assertEquals(Boolean.TRUE, result.getEmuEbookAvailable());
        assertEquals(Boolean.TRUE, result.getAclaAudioAvailable());
        assertEquals(Boolean.FALSE, result.getAclaPaperAvailable());
        assertEquals(Boolean.TRUE, result.getAclaEbookAvailable());
        assertEquals(Boolean.FALSE, result.getWccAudioAvailable());
        assertEquals(Boolean.FALSE, result.getWccPaperAvailable());
        assertEquals(Boolean.TRUE, result.getWccEbookAvailable());
        assertEquals(Boolean.FALSE, result.getMsuAudioAvailable());
        assertEquals(Boolean.FALSE, result.getMsuPaperAvailable());
        assertEquals(Boolean.TRUE, result.getMsuEbookAvailable());

        assertEquals(checked, result.getYdlLastChecked());
        assertEquals(checked, result.getEmuLastChecked());
        assertEquals(checked, result.getAclaLastChecked());
        assertEquals(checked, result.getWccLastChecked());
        assertEquals(checked, result.getMsuLastChecked());
        assertEquals("ydl down", result.getYdlLookupError());
        assertEquals("emu down", result.getEmuLookupError());
        assertEquals("acla down", result.getAclaLookupError());
        assertEquals("wcc down", result.getWccLookupError());
        assertEquals("msu down", result.getMsuLookupError());
    }

    @Test
    void updateBook_keepsAvailabilityWhenTheCallerOmitsTheFlag() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Confessions");
        book.setEmuPaperAvailable(true);
        book.setYdlAudioAvailable(false);
        book.setWccPaperAvailable(true);
        book.setMsuPaperAvailable(true);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.existsByTitle("Confessions")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookMapper.toDto(any(Book.class))).thenReturn(new BookDto());

        BookDto incoming = new BookDto();
        incoming.setTitle("Confessions");

        bookService.updateBook(1L, incoming);

        ArgumentCaptor<Book> saved = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(saved.capture());
        assertEquals(Boolean.TRUE, saved.getValue().getEmuPaperAvailable());
        assertEquals(Boolean.FALSE, saved.getValue().getYdlAudioAvailable());
        assertNull(saved.getValue().getAclaEbookAvailable());
        assertEquals(Boolean.TRUE, saved.getValue().getWccPaperAvailable());
        assertEquals(Boolean.TRUE, saved.getValue().getMsuPaperAvailable());
    }
}
