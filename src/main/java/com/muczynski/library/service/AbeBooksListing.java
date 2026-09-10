/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * One AbeBooks search-result listing after HTML parsing.
 */
@Value
@Builder
public class AbeBooksListing {
    BigDecimal priceDollars;
    BigDecimal shippingDollars;
    String condition;
    String detailsUrl;

    public BigDecimal totalDollars() {
        BigDecimal price = priceDollars != null ? priceDollars : BigDecimal.ZERO;
        BigDecimal shipping = shippingDollars != null ? shippingDollars : BigDecimal.ZERO;
        return price.add(shipping);
    }
}
