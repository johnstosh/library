/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.AdditionalShippingSource;
import com.muczynski.library.domain.BookCoverType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class BookListingDto {
    Long id;
    Long bookId;
    String catalogTitle;
    String listingTitle;
    Long sellerId;
    Long vendorId;
    String sellerName;
    String storefrontUrl;
    String shipRatesUrl;
    BigDecimal priceDollars;
    BigDecimal shippingDollars;
    BigDecimal priorityShippingDollars;
    BigDecimal additionalShippingDollars;
    AdditionalShippingSource additionalShippingSource;
    String condition;
    BookCoverType binding;
    String isbn13;
    Integer quantity;
    String detailsUrl;
    LocalDateTime lookedUpAt;
}
