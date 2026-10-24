/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.muczynski.library.domain.BookCoverType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ImportListingDto {
    private String bookTitle;
    private String bookAuthorName;
    private Long vendorId;
    private Long abeListingId;
    private String listingTitle;
    private String detailsUrl;
    private BigDecimal priceDollars;
    private BigDecimal shippingDollars;
    private BigDecimal priorityShippingDollars;
    private String condition;
    private BookCoverType binding;
    private String isbn13;
    private Integer quantity;
    private LocalDateTime lookedUpAt;
}
