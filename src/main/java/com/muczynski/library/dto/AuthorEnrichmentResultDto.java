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
 * Result of filling blank author fields from a Grok prompt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorEnrichmentResultDto {
    private Long authorId;
    private String name;
    private boolean success;
    /** True when every fillable field already had a value, so Grok was not called. */
    private boolean skipped;
    private List<String> filledFields;
    private String errorMessage;
    private AuthorDto updatedAuthor;
}
