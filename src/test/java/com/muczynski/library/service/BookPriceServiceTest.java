/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.AbeSeller;
import com.muczynski.library.domain.AdditionalShippingSource;
import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookListing;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.dto.BookPriceDto;
import com.muczynski.library.dto.BookPriceLookupResultDto;
import com.muczynski.library.exception.AbeBooksHttpException;
import com.muczynski.library.exception.AbeBooksRateLimitedException;
import com.muczynski.library.repository.AbeSellerRepository;
import com.muczynski.library.repository.BookListingRepository;
import com.muczynski.library.repository.BookPriceRepository;
import com.muczynski.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
        // Use constructor that accepts repository so createExcerptResult works
        bookPriceService = new BookPriceService(bookRepository, bookPriceRepository, abeBooksClient, 0, 0);
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
    void lookupAndUpdateBook_savesLibraryBindingWhenFound() {
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
                        .libraryBinding(AbeBooksListing.builder()
                                .priceDollars(new BigDecimal("12.00"))
                                .shippingDollars(BigDecimal.ZERO)
                                .condition("Used - Good")
                                .detailsUrl("https://www.abebooks.com/l")
                                .binding(BookCoverType.LIBRARY_BINDING)
                                .build())
                        .build());

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertEquals(BookCoverType.LIBRARY_BINDING, result.getLibraryBinding().getCover());
        assertEquals(new BigDecimal("12.00"), result.getLibraryBinding().getPriceDollars());
        verify(bookPriceRepository, times(3)).save(any(BookPrice.class));
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
        assertEquals(BookPriceService.NO_MATCHING_LISTING, result.getErrorMessage());
        ArgumentCaptor<BookPrice> captor = ArgumentCaptor.forClass(BookPrice.class);
        verify(bookPriceRepository, times(2)).save(captor.capture());
        assertEquals(BookPriceService.NO_MATCHING_LISTING, captor.getAllValues().get(0).getLookupError());
    }

    @Test
    void lookupAndUpdateBook_noListing_savesSearchUrlOnErrorRow() {
        Book book = book("Unknown Book", "Nobody");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String searchUrl = "https://www.abebooks.com/servlet/SearchResults?tn=Unknown+Book";
        when(abeBooksClient.findCheapestGoodOrBetter(any(), any()))
                .thenReturn(AbeBooksCoverListings.builder().searchUrl(searchUrl).build());

        bookPriceService.lookupAndUpdateBook(1L);

        ArgumentCaptor<BookPrice> captor = ArgumentCaptor.forClass(BookPrice.class);
        verify(bookPriceRepository, times(2)).save(captor.capture());
        for (BookPrice saved : captor.getAllValues()) {
            assertEquals(BookPriceService.NO_MATCHING_LISTING, saved.getLookupError());
            assertEquals(searchUrl, saved.getDetailsUrl());
            assertNull(saved.getPriceDollars());
        }
    }

    @Test
    void lookupAndUpdateBook_http500_savesStatusAndSearchUrl() {
        Book book = book("Emma", "Jane Austen");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String searchUrl = "https://www.abebooks.com/servlet/SearchResults?tn=Emma";
        when(abeBooksClient.findCheapestGoodOrBetter(any(), any()))
                .thenThrow(new AbeBooksHttpException(500, searchUrl, null));

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertFalse(result.isSuccess());
        assertFalse(result.isRateLimited());
        assertEquals("AbeBooks HTTP 500", result.getErrorMessage());
        ArgumentCaptor<BookPrice> captor = ArgumentCaptor.forClass(BookPrice.class);
        verify(bookPriceRepository, times(2)).save(captor.capture());
        assertEquals("AbeBooks HTTP 500", captor.getAllValues().get(0).getLookupError());
        assertEquals(searchUrl, captor.getAllValues().get(0).getDetailsUrl());
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
    void lookupAndUpdateBook_unknownBinding_savesUnknownCover() {
        Book book = book("Pride and Prejudice", "Jane Austen");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AbeBooksListing unknown = AbeBooksListing.builder()
                .priceDollars(new BigDecimal("4.86"))
                .shippingDollars(BigDecimal.ZERO)
                .condition("Used - Good")
                .detailsUrl("https://www.abebooks.com/u")
                .binding(null)
                .build();
        when(abeBooksClient.findCheapestGoodOrBetter("Pride and Prejudice", "Jane Austen"))
                .thenReturn(AbeBooksCoverListings.builder()
                        .hardcover(unknown)
                        .softcover(unknown)
                        .build());

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertEquals(BookCoverType.UNKNOWN, result.getHardcover().getCover());
        assertEquals(BookCoverType.UNKNOWN, result.getSoftcover().getCover());
        ArgumentCaptor<BookPrice> captor = ArgumentCaptor.forClass(BookPrice.class);
        verify(bookPriceRepository, times(1)).save(captor.capture());
        assertEquals(BookCoverType.UNKNOWN, captor.getValue().getCover());
        assertEquals(new BigDecimal("4.86"), captor.getValue().getPriceDollars());
    }

    @Test
    void lookupAndUpdateBook_typedHardcoverAndUnknownSoftcover_savesBoth() {
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
                                .detailsUrl("https://www.abebooks.com/u")
                                .binding(null)
                                .build())
                        .build());

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertEquals(BookCoverType.HARDCOVER, result.getHardcover().getCover());
        assertEquals(BookCoverType.UNKNOWN, result.getSoftcover().getCover());
        ArgumentCaptor<BookPrice> captor = ArgumentCaptor.forClass(BookPrice.class);
        verify(bookPriceRepository, times(2)).save(captor.capture());
        assertEquals(BookCoverType.UNKNOWN, captor.getAllValues().get(0).getCover());
        assertEquals(BookCoverType.HARDCOVER, captor.getAllValues().get(1).getCover());
    }

    @Test
    void backoffMsForAttempt_capsExponentialGrowth() {
        assertEquals(4000, BookPriceService.backoffMsForAttempt(0, 4000));
        assertEquals(8000, BookPriceService.backoffMsForAttempt(1, 4000));
        assertEquals(16000, BookPriceService.backoffMsForAttempt(2, 4000));
        assertEquals(32000, BookPriceService.backoffMsForAttempt(3, 4000));
        assertEquals(32000, BookPriceService.backoffMsForAttempt(20, 4000));
        assertEquals(BookPriceService.MAX_RATE_LIMIT_BACKOFF_MS,
                BookPriceService.backoffMsForAttempt(10, 60_000));
        assertEquals(0, BookPriceService.backoffMsForAttempt(0, 0));
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

    @Test
    void lookupAndUpdateBook_skipsExcerptTitle() {
        Book book = book("Excerpt from Summa Theologica", "Thomas Aquinas");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertEquals("Excerpt from Summa Theologica", result.getBookTitle());
        assertNotNull(result.getSoftcover());
        assertEquals(new BigDecimal("0.01"), result.getSoftcover().getPriceDollars());
        assertEquals(BigDecimal.ZERO, result.getSoftcover().getShippingDollars());
        assertEquals(BookCoverType.SOFTCOVER, result.getSoftcover().getCover());
        assertNull(result.getSoftcover().getLookupError());
        verify(abeBooksClient, times(0)).findCheapestGoodOrBetter(any(), any());
        verify(bookPriceRepository, times(1)).save(any(BookPrice.class));
    }

    @Test
    void lookupAndUpdateBook_skipsExcerptsTitleWithLeadingSpace() {
        Book book = book("  excerpts from the fathers  ", "Various");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookPriceLookupResultDto result = bookPriceService.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("0.01"), result.getSoftcover().getPriceDollars());
        verify(abeBooksClient, times(0)).findCheapestGoodOrBetter(any(), any());
    }

    @Test
    void lookupAndUpdateBook_normalTitleStillCallsAbeBooks() {
        Book book = book("Normal Title", "Jane Austen");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(abeBooksClient.findCheapestGoodOrBetter("Normal Title", "Jane Austen"))
                .thenReturn(AbeBooksCoverListings.builder().build());

        bookPriceService.lookupAndUpdateBook(1L);

        verify(abeBooksClient, times(1)).findCheapestGoodOrBetter(any(), any());
    }

    @Test
    void capListings_keepsCheapestAndTheCoverWinner() {
        List<AbeBooksListing> pool = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            pool.add(AbeBooksListing.builder()
                    .listingId((long) i)
                    .sellerId(1L)
                    .priceDollars(new BigDecimal(i))
                    .shippingDollars(BigDecimal.ZERO)
                    .build());
        }
        AbeBooksListing cover = pool.get(24);
        List<AbeBooksListing> kept = BookPriceService.capListings(pool, List.of(cover));
        assertEquals(25, kept.size());
        assertEquals(1L, kept.get(0).getListingId());
        assertTrue(kept.stream().anyMatch(listing -> listing.getListingId() == 25L));
    }

    @Test
    void lookupAndUpdateBook_storesSellerAndDoesNotOverwriteLibrarianRate() {
        Book book = book("Pride and Prejudice", "Jane Austen");
        AbeSeller existing = new AbeSeller();
        existing.setId(7L);
        existing.setVendorId(99L);
        existing.setName("World of Books");
        existing.setAdditionalShippingSource(AdditionalShippingSource.LIBRARIAN);
        existing.setAdditionalShippingDollars(new BigDecimal("2.00"));
        final AbeSeller[] stored = {existing};

        AbeSellerRepository sellerRepository = org.mockito.Mockito.mock(AbeSellerRepository.class);
        BookListingRepository listingRepository = org.mockito.Mockito.mock(BookListingRepository.class);
        BookPriceService persisting = new BookPriceService(
                bookRepository, bookPriceRepository, abeBooksClient, 0, 0, null, null,
                sellerRepository, listingRepository);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sellerRepository.findByVendorId(99L)).thenAnswer(invocation -> Optional.of(stored[0]));
        when(sellerRepository.save(any(AbeSeller.class))).thenAnswer(invocation -> {
            stored[0] = invocation.getArgument(0);
            return stored[0];
        });
        when(sellerRepository.findById(7L)).thenAnswer(invocation -> Optional.of(stored[0]));
        when(abeBooksClient.fetchShipRates(99L, 111L)).thenReturn(AbeBooksShipRates.builder()
                .standardDollars(new BigDecimal("6.00"))
                .priorityDollars(new BigDecimal("12.00"))
                .terms("Each additional book US$ 1.50")
                .additionalDollars(new BigDecimal("1.50"))
                .additionalSource(AdditionalShippingSource.PARSED)
                .build());
        AbeBooksListing offer = AbeBooksListing.builder()
                .listingId(111L)
                .sellerId(99L)
                .sellerName("World of Books")
                .priceDollars(new BigDecimal("4.86"))
                .shippingDollars(BigDecimal.ZERO)
                .condition("Used - Good")
                .detailsUrl("https://www.abebooks.com/h")
                .binding(BookCoverType.HARDCOVER)
                .listingTitle("Pride and Prejudice")
                .build();
        when(abeBooksClient.findCheapestGoodOrBetter("Pride and Prejudice", "Jane Austen"))
                .thenReturn(AbeBooksCoverListings.builder()
                        .hardcover(offer)
                        .candidates(List.of(offer))
                        .build());

        BookPriceLookupResultDto result = persisting.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertEquals(7L, result.getHardcover().getSellerId());
        assertEquals("World of Books", result.getHardcover().getSellerName());
        assertEquals(AdditionalShippingSource.LIBRARIAN, stored[0].getAdditionalShippingSource());
        assertEquals(new BigDecimal("2.00"), stored[0].getAdditionalShippingDollars());
        assertNotNull(stored[0].getRatesLookedUpAt());
        assertEquals("Each additional book US$ 1.50", stored[0].getShippingTerms());
        ArgumentCaptor<BookListing> listings = ArgumentCaptor.forClass(BookListing.class);
        verify(listingRepository).save(listings.capture());
        assertEquals(111L, listings.getValue().getAbeListingId());
        assertEquals(existing, listings.getValue().getSeller());
        verify(listingRepository).deleteByAbeListingIdIn(anyCollection());
        verify(listingRepository).deleteByBook_Id(1L);
        verify(listingRepository).flush();
    }

    @Test
    void lookupAndUpdateBook_shipRatesRateLimitKeepsSavedPrice() {
        Book book = book("Pride and Prejudice", "Jane Austen");
        final AbeSeller[] stored = {null};
        AbeSellerRepository sellerRepository = org.mockito.Mockito.mock(AbeSellerRepository.class);
        BookListingRepository listingRepository = org.mockito.Mockito.mock(BookListingRepository.class);
        BookPriceService persisting = new BookPriceService(
                bookRepository, bookPriceRepository, abeBooksClient, 0, 0, null, null,
                sellerRepository, listingRepository);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.findByBook_IdAndCover(eq(1L), any())).thenReturn(Optional.empty());
        when(bookPriceRepository.save(any(BookPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sellerRepository.findByVendorId(99L)).thenAnswer(invocation -> Optional.ofNullable(stored[0]));
        when(sellerRepository.save(any(AbeSeller.class))).thenAnswer(invocation -> {
            AbeSeller seller = invocation.getArgument(0);
            if (seller.getId() == null) {
                seller.setId(7L);
            }
            stored[0] = seller;
            return seller;
        });
        when(sellerRepository.findById(7L)).thenAnswer(invocation -> Optional.ofNullable(stored[0]));
        when(abeBooksClient.fetchShipRates(99L, 111L)).thenThrow(new AbeBooksRateLimitedException());
        AbeBooksListing offer = AbeBooksListing.builder()
                .listingId(111L)
                .sellerId(99L)
                .sellerName("World of Books")
                .priceDollars(new BigDecimal("4.86"))
                .shippingDollars(BigDecimal.ZERO)
                .condition("Used - Good")
                .detailsUrl("https://www.abebooks.com/h")
                .binding(BookCoverType.HARDCOVER)
                .build();
        when(abeBooksClient.findCheapestGoodOrBetter("Pride and Prejudice", "Jane Austen"))
                .thenReturn(AbeBooksCoverListings.builder().hardcover(offer).candidates(List.of(offer)).build());

        BookPriceLookupResultDto result = persisting.lookupAndUpdateBook(1L);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("4.86"), result.getHardcover().getPriceDollars());
        assertEquals(AdditionalShippingSource.UNKNOWN, stored[0].getAdditionalShippingSource());
        assertNull(stored[0].getRatesLookedUpAt());
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
