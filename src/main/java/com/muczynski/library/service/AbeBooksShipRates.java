/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.AdditionalShippingSource;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Seller shipping page: first-book rates plus whatever extra-book amount the terms state.
 */
@Value
@Builder
public class AbeBooksShipRates {
    BigDecimal standardDollars;
    BigDecimal priorityDollars;
    String terms;
    BigDecimal additionalDollars;
    AdditionalShippingSource additionalSource;

    public static AbeBooksShipRates unknown() {
        return AbeBooksShipRates.builder()
                .additionalSource(AdditionalShippingSource.UNKNOWN)
                .build();
    }
}
