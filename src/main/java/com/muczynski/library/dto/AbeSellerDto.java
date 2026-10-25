/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.AdditionalShippingSource;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class AbeSellerDto {
    Long id;
    Long vendorId;
    String name;
    String city;
    String region;
    String country;
    Integer starRating;
    String storefrontUrl;
    String shipRatesUrl;
    BigDecimal sampleStandardShipping;
    BigDecimal samplePriorityShipping;
    String shippingTerms;
    BigDecimal additionalShippingDollars;
    AdditionalShippingSource additionalShippingSource;
    int listingCount;
    LocalDateTime lookedUpAt;
    LocalDateTime ratesLookedUpAt;
    LocalDateTime lastModified;
}
