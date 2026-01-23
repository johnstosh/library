/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk free text lookup result.
 * Contains the result of searching for free online text for a single book across all providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreeTextBulkLookupResultDto {

    /**
     * ID of the book that was searched
     */
    private Long bookId;

    /**
     * Title of the book
     */
    private String bookTitle;

    /**
     * Author name (may be null)
     */
    private String authorName;

    /**
     * Whether a free text URL was found
     */
    private boolean success;

    /**
     * URL to the free text version (set if success=true)
     */
    private String freeTextUrl;

    /**
     * Name of the provider that found the URL (set if success=true)
     */
    private String providerName;

    /**
     * Error message explaining why the search failed (set if success=false)
     */
    private String errorMessage;

    /**
     * List of all provider names that were searched
     */
    private List<String> providersSearched;
}
