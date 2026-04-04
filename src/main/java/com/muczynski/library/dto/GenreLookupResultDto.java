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
 * DTO for individual genre lookup result.
 * Contains the book ID, suggested genres, and (on success) the updated BookDto
 * so the frontend can seed its cache without a follow-up by-ids fetch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreLookupResultDto {
    private Long bookId;
    private String title;
    private boolean success;
    private List<String> suggestedGenres;
    private String errorMessage;
    /** Populated on success so the frontend can update its book cache immediately. */
    private BookDto updatedBook;
}
