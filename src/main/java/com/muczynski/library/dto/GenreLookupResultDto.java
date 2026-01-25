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
 * Contains the book ID and suggested genres.
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
}
