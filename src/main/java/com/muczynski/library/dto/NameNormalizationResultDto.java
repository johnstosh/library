/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One book's before/after for a Chicago title or canonical author rewrite.
 * {@code updatedBook} is set only when this book changed, so the client can
 * refresh that row without a follow-up fetch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NameNormalizationResultDto {
    private Long bookId;
    /** Book title after this operation, for the results list. */
    private String title;
    private String before;
    private String after;
    private boolean changed;
    private boolean success;
    private String errorMessage;
    private BookDto updatedBook;
}
