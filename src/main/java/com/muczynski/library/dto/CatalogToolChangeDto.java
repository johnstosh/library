/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One book a Data Management catalog tool would change or did change.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogToolChangeDto {

    private Long bookId;
    private String title;
    private String before;
    private String after;
    /** Free-text URLs that would be added. Empty for the other tools. */
    private String added;
    /** Why this book was left alone. Empty when the change will be applied. */
    private String reason;
}
