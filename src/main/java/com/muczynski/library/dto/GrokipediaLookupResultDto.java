/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual Grokipedia URL lookup result.
 * Used for both books and authors - one of bookId or authorId will be set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrokipediaLookupResultDto {
    private Long bookId;
    private Long authorId;
    private String name;
    private boolean success;
    private String grokipediaUrl;
    private String errorMessage;
}
