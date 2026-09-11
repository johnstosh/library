/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.BookCoverType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookPriceDto {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String author;
    private BookCoverType cover;
    private BigDecimal priceDollars;
    private BigDecimal shippingDollars;
    private BigDecimal totalDollars;
    private String condition;
    private LocalDateTime lookedUpAt;
    private String detailsUrl;
    private String lookupError;
    private LocalDateTime lastModified;
}
