/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.ReadingDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of filling a book's reading difficulty from Grok.
 * On success, {@code updatedBook} is populated so the frontend can seed its cache.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingDifficultyLookupResultDto {
    private Long bookId;
    private String title;
    private boolean success;
    private ReadingDifficulty suggestedDifficulty;
    private String errorMessage;
    /** Populated on success so the frontend can update its book cache immediately. */
    private BookDto updatedBook;
}
