/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.dto.BookPriceLookupResultDto;
import com.muczynski.library.exception.AbeBooksRateLimitedException;
import com.muczynski.library.repository.BookPriceRepository;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookPriceServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookPriceRepository bookPriceRepository;

    @Mock
    private AbeBooksClient abeBooksClient;

    private BookPriceService bookPriceService;

    @BeforeEach
    void setUp() {
        bookPriceService = new BookPriceService(bookRepository, bookPriceRepository, abeBooksClient);
    }

    @Test
    void lookupAndUpdateBook_savesHardcoverAndSoftcover() {
        Book book = book("Pride and Prejudice", "Jane Austen");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(abeBooksClient.findCheapestGoodOrBetter("Pride and Prejudice", "Jane Austen"))
                .thenReturn(AbeBooksCoverListings.builder()
                        .hardcover(AbeBooksListing.builder()
                                .priceDollars(new BigDecimal("4.86"))
                                .shippingDollars(BigDecimal.ZERO)
                                .condition("Used - Good")
                                .detailsUrl("https://www.abebooks.com/h")
                                .binding(BookCoverType.HARDCOVER)
                                .build())
                        .softcover(AbeBooksListing.builder()
                                .priceDollars(new BigDecimal("3.00"))
                                .shippingDollars(new BigDecimal("4.00"))
                                .condition("Used - Very good")
                                .detailsUrl("https://www.abebooks.com/s")
                                .binding(BookCoverType.SOFTCOVER)
                                .build())
                        .build());

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertEquals("Pride and Prejudice", result.getBookTitle());
        assertEquals(new BigDecimal("4.86"), result.getHardcover().getPriceDollars());
        assertEquals(new BigDecimal("7.00"), result.getSoftcover().getTotalDollars());
        verify(bookPriceRepository, times(2)).save(any(BookPrice.class));
        verify(abeBooksClient, times(1)).findCheapestGoodOrBetter("Pride and Prejudice", "Jane Austen");
    }

    @Test
    void lookupAndUpdateBook_noListing_savesError() {
        Book book = book("Unknown Book", "Nobody");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(abeBooksClient.findCheapestGoodOrBetter(any(), any()))
                .thenReturn(AbeBooksCoverListings.builder().build());

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("No matching listing", result.getErrorMessage());
        ArgumentCaptor<BookPrice> captor = ArgumentCaptor.forClass(BookPrice.class);
        verify(bookPriceRepository, times(2)).save(captor.capture());
        assertEquals("No matching listing", captor.getAllValues().get(0).getLookupError());
    }

    @Test
    void lookupAndUpdateBook_rateLimited_setsFlagWithoutRetryWhenRetriesZero() {
        Book book = book("Emma", "Jane Austen");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(abeBooksClient.findCheapestGoodOrBetter(any(), any()))
                .thenThrow(new AbeBooksRateLimitedException());

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertTrue(result.isRateLimited());
        assertEquals(AbeBooksRateLimitedException.MESSAGE, result.getErrorMessage());
        verify(abeBooksClient, times(1)).findCheapestGoodOrBetter(any(), any());
    }

    @Test
    void lookupAndUpdateBook_rateLimited_retriesThenSucceeds() {
        Book book = book("Emma", "Jane Austen");
        BookPriceService retrying = new BookPriceService(
                bookRepository, bookPriceRepository, abeBooksClient, 2, 0);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(abeBooksClient.findCheapestGoodOrBetter("Emma", "Jane Austen"))
                .thenThrow(new AbeBooksRateLimitedException())
                .thenThrow(new AbeBooksRateLimitedException())
                .thenReturn(AbeBooksCoverListings.builder()
                        .hardcover(AbeBooksListing.builder()
                                .priceDollars(new BigDecimal("4.86"))
                                .shippingDollars(BigDecimal.ZERO)
                                .condition("Used - Good")
                                .detailsUrl("https://www.abebooks.com/h")
                                .binding(BookCoverType.HARDCOVER)
                                .build())
                        .build());

        BookPriceLookupResultDto result = retrying.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertFalse(result.isRateLimited());
        verify(abeBooksClient, times(3)).findCheapestGoodOrBetter("Emma", "Jane Austen");
    }

    @Test
    void lookupAndUpdateBook_skipsTemporaryTitle() {
        Book book = book("2026-09-10 photo", "Author");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertEquals("Not Ready - Temporary title", result.getErrorMessage());
        verify(abeBooksClient, times(0)).findCheapestGoodOrBetter(any(), any());
    }

    private static Book book(String title, String authorName) {
        Book book = new Book();
        book.setId(1L);
        book.setTitle(title);
        Author author = new Author();
        author.setName(authorName);
        book.setAuthor(author);
        return book;
    }
}
