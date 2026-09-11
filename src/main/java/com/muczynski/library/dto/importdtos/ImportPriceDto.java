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
public class ImportPriceDto {
    private String bookTitle;
    private String bookAuthorName;
    private BookCoverType cover;
    private BigDecimal priceDollars;
    private BigDecimal shippingDollars;
    private String condition;
    private LocalDateTime lookedUpAt;
    private String detailsUrl;
    private String lookupError;
}
