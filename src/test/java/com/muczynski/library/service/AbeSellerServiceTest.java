/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.AbeSeller;
import com.muczynski.library.domain.AdditionalShippingSource;
import com.muczynski.library.dto.SellerReparseResultDto;
import com.muczynski.library.repository.AbeSellerRepository;
import com.muczynski.library.repository.BookListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbeSellerServiceTest {

    @Mock
    private AbeSellerRepository sellerRepository;

    @Mock
    private BookListingRepository listingRepository;

    @Test
    void reparseStoredTermsUpdatesMismatchedRatesAndKeepsLibrarianAmounts() {
        AbeSeller mismatched = seller(
                "STANDARD DOMESTIC U.S. SHIPPING IS FREE.",
                new BigDecimal("1.00"),
                AdditionalShippingSource.PARSED);
        AbeSeller already = seller(
                "STANDARD DOMESTIC U.S. SHIPPING IS FREE.",
                BigDecimal.ZERO,
                AdditionalShippingSource.FREE);
        AbeSeller librarian = seller(
                "STANDARD DOMESTIC U.S. SHIPPING IS FREE.",
                new BigDecimal("9.00"),
                AdditionalShippingSource.LIBRARIAN);
        when(sellerRepository.findAllByOrderByNameAscIdAsc()).thenReturn(List.of(mismatched, already, librarian));

        SellerReparseResultDto result = new AbeSellerService(sellerRepository, listingRepository).reparseStoredTerms();

        assertEquals(3, result.getExamined());
        assertEquals(1, result.getUpdated());
        assertEquals(1, result.getUnchanged());
        assertEquals(1, result.getKeptLibrarian());
        assertEquals(0, BigDecimal.ZERO.compareTo(mismatched.getAdditionalShippingDollars()));
        assertEquals(AdditionalShippingSource.FREE, mismatched.getAdditionalShippingSource());
        assertEquals(AdditionalShippingSource.LIBRARIAN, librarian.getAdditionalShippingSource());
        assertEquals(new BigDecimal("9.00"), librarian.getAdditionalShippingDollars());
        verify(sellerRepository).save(mismatched);
        verify(sellerRepository, never()).save(already);
        verify(sellerRepository, never()).save(librarian);
    }

    private static AbeSeller seller(String terms, BigDecimal dollars, AdditionalShippingSource source) {
        AbeSeller seller = new AbeSeller();
        seller.setShippingTerms(terms);
        seller.setAdditionalShippingDollars(dollars);
        seller.setAdditionalShippingSource(source);
        return seller;
    }
}
