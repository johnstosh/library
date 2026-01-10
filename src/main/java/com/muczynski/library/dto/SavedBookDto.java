/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for books from most recent day or with temporary titles.
 * Used for efficient queries with projections - no N+1 queries.
 * Contains fields needed by both Books page table and Books from Feed page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedBookDto {
    private Long id;
    private String title;
    private String author;
    private String library;
    private Long photoCount;
    private Boolean needsProcessing;

    // Additional fields for Books page table
    private String locNumber;
    private String status;
    private String grokipediaUrl;
}
