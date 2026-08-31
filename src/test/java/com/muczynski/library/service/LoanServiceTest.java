/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Book;
import com.muczynski.library.dto.TitleLoanedDto;
import com.muczynski.library.exception.ResourceNotFoundException;
import com.muczynski.library.repository.BookRepository;
import com.muczynski.library.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private LoanService loanService;

    @Test
    void getTitleLoaned_returnsTrueWhenBookHasOpenLoan() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Initial Book");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(loanRepository.countByBookIdAndReturnDateIsNull(1L)).thenReturn(1L);

        TitleLoanedDto result = loanService.getTitleLoaned(1L);

        assertEquals(1L, result.getBookId());
        assertEquals("Initial Book", result.getTitle());
        assertTrue(result.isLoaned());
    }

    @Test
    void getTitleLoaned_returnsFalseWhenBookHasNoOpenLoan() {
        Book book = new Book();
        book.setId(2L);
        book.setTitle("Available Book");
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book));
        when(loanRepository.countByBookIdAndReturnDateIsNull(2L)).thenReturn(0L);

        TitleLoanedDto result = loanService.getTitleLoaned(2L);

        assertFalse(result.isLoaned());
    }

    @Test
    void getTitleLoaned_throwsWhenBookMissing() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> loanService.getTitleLoaned(999L));
    }
}
