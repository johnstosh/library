/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdditionalShippingUpdateDto {
    /** Null clears the librarian amount and re-reads the stored terms. */
    private BigDecimal additionalShippingDollars;
}
