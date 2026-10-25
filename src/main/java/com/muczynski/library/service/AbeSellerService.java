/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.AbeSeller;
import com.muczynski.library.domain.AdditionalShippingSource;
import com.muczynski.library.domain.BookListing;
import com.muczynski.library.dto.AbeSellerDto;
import com.muczynski.library.dto.AdditionalShippingUpdateDto;
import com.muczynski.library.dto.BookListingDto;
import com.muczynski.library.exception.ResourceNotFoundException;
import com.muczynski.library.repository.AbeSellerRepository;
import com.muczynski.library.repository.BookListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AbeSellerService {

    private final AbeSellerRepository sellerRepository;
    private final BookListingRepository listingRepository;

    @Transactional(readOnly = true)
    public List<AbeSellerDto> list() {
        Map<Long, Integer> counts = listingCounts();
        return sellerRepository.findAllByOrderByNameAscIdAsc().stream()
                .map(seller -> toDto(seller, counts.getOrDefault(seller.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AbeSellerDto get(Long id) {
        AbeSeller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", id));
        int count = listingCounts().getOrDefault(id, 0);
        return toDto(seller, count);
    }

    /**
     * Sets the librarian extra-book amount. A null amount clears it and reapplies the stored terms.
     */
    @Transactional
    public AbeSellerDto updateAdditionalShipping(Long id, AdditionalShippingUpdateDto update) {
        AbeSeller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", id));
        if (update != null && update.getAdditionalShippingDollars() != null) {
            seller.setAdditionalShippingDollars(update.getAdditionalShippingDollars());
            seller.setAdditionalShippingSource(AdditionalShippingSource.LIBRARIAN);
        } else {
            AbeBooksListingParser.AdditionalRate parsed =
                    AbeBooksListingParser.parseAdditionalRate(seller.getShippingTerms());
            seller.setAdditionalShippingDollars(parsed.dollars());
            seller.setAdditionalShippingSource(parsed.source());
        }
        sellerRepository.save(seller);
        return toDto(seller, listingCounts().getOrDefault(id, 0));
    }

    @Transactional(readOnly = true)
    public List<BookListingDto> listingsForBooks(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }
        return listingRepository.findByBookIdsWithSeller(bookIds).stream()
                .map(AbeSellerService::toListingDto)
                .toList();
    }

    private Map<Long, Integer> listingCounts() {
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : listingRepository.countBySellerId()) {
            if (row[0] instanceof Number sellerId && row[1] instanceof Number count) {
                counts.put(sellerId.longValue(), count.intValue());
            }
        }
        return counts;
    }

    private static AbeSellerDto toDto(AbeSeller seller, int listingCount) {
        return AbeSellerDto.builder()
                .id(seller.getId())
                .vendorId(seller.getVendorId())
                .name(seller.getName())
                .city(seller.getCity())
                .region(seller.getRegion())
                .country(seller.getCountry())
                .starRating(seller.getStarRating())
                .storefrontUrl(seller.getStorefrontUrl())
                .shipRatesUrl(seller.shipRatesUrl())
                .sampleStandardShipping(seller.getSampleStandardShipping())
                .samplePriorityShipping(seller.getSamplePriorityShipping())
                .shippingTerms(seller.getShippingTerms())
                .additionalShippingDollars(seller.getAdditionalShippingDollars())
                .additionalShippingSource(seller.getAdditionalShippingSource())
                .listingCount(listingCount)
                .lookedUpAt(seller.getLookedUpAt())
                .ratesLookedUpAt(seller.getRatesLookedUpAt())
                .lastModified(seller.getLastModified())
                .build();
    }

    private static BookListingDto toListingDto(BookListing listing) {
        AbeSeller seller = listing.getSeller();
        return BookListingDto.builder()
                .id(listing.getId())
                .bookId(listing.getBook() != null ? listing.getBook().getId() : null)
                .catalogTitle(listing.getBook() != null ? listing.getBook().getTitle() : null)
                .listingTitle(listing.getListingTitle())
                .sellerId(seller != null ? seller.getId() : null)
                .vendorId(seller != null ? seller.getVendorId() : null)
                .sellerName(seller != null ? seller.getName() : null)
                .storefrontUrl(seller != null ? seller.getStorefrontUrl() : null)
                .shipRatesUrl(seller != null ? seller.shipRatesUrl() : null)
                .priceDollars(listing.getPriceDollars())
                .shippingDollars(listing.getShippingDollars())
                .priorityShippingDollars(listing.getPriorityShippingDollars())
                .additionalShippingDollars(seller != null ? seller.getAdditionalShippingDollars() : null)
                .additionalShippingSource(seller != null
                        ? seller.getAdditionalShippingSource() : AdditionalShippingSource.UNKNOWN)
                .condition(listing.getCondition())
                .binding(listing.getBinding())
                .isbn13(listing.getIsbn13())
                .quantity(listing.getQuantity())
                .detailsUrl(listing.getDetailsUrl())
                .lookedUpAt(listing.getLookedUpAt())
                .build();
    }
}
