/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One author's before/after for a canonical-name rewrite.
 * {@code updatedAuthor} is set only when this author changed, so the client can
 * refresh that row without a follow-up fetch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorNameNormalizationResultDto {
    private Long authorId;
    /** Author name after this operation, for the results list. */
    private String name;
    private String before;
    private String after;
    private boolean changed;
    private boolean success;
    private String errorMessage;
    /**
     * Set when this author was merged into another row and then deleted.
     * The client should open that author, not this id.
     */
    private Long mergedIntoAuthorId;
    private AuthorDto updatedAuthor;
}
