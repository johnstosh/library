/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.muczynski.library.domain.AdditionalShippingSource;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ImportSellerDto {
    private Long vendorId;
    private String name;
    private String city;
    private String region;
    private String country;
    private Integer starRating;
    private String storefrontUrl;
    private Long sampleListingId;
    private BigDecimal sampleStandardShipping;
    private BigDecimal samplePriorityShipping;
    private String shippingTerms;
    private BigDecimal additionalShippingDollars;
    private AdditionalShippingSource additionalShippingSource;
    private LocalDateTime lookedUpAt;
    private LocalDateTime ratesLookedUpAt;
}
