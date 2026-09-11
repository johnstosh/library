/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookPriceLookupResultDto {
    private Long bookId;
    private String bookTitle;
    private boolean success;
    private boolean rateLimited;
    private boolean cancelled;
    private BookPriceDto hardcover;
    private BookPriceDto softcover;
    private String errorMessage;
}
