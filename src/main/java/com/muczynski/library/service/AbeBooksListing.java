/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.BookCoverType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * One AbeBooks search-result listing after HTML parsing.
 * {@code binding} is null when the listing does not name a binding.
 */
@Value
@Builder
public class AbeBooksListing {
    BigDecimal priceDollars;
    BigDecimal shippingDollars;
    String condition;
    String detailsUrl;
    BookCoverType binding;
    Long listingId;
    String listingTitle;
    String isbn13;
    Integer quantity;
    BigDecimal priorityShippingDollars;
    String currency;
    Long sellerId;
    String sellerName;
    String sellerCity;
    String sellerRegion;
    String sellerCountry;
    Integer sellerStars;
    String sellerStorefrontUrl;

    public BigDecimal totalDollars() {
        BigDecimal price = priceDollars != null ? priceDollars : BigDecimal.ZERO;
        BigDecimal shipping = shippingDollars != null ? shippingDollars : BigDecimal.ZERO;
        return price.add(shipping);
    }
}
