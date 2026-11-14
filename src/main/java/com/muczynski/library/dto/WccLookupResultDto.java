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
 * DTO for an individual Washtenaw Community College availability lookup result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WccLookupResultDto {
    private Long bookId;
    private boolean success;
    private Boolean audioAvailable;
    private Boolean paperAvailable;
    private Boolean ebookAvailable;
    private String matchedTitle;
    /** Records that set the flags. Empty when the lookup did not use a keyed record. */
    private List<CatalogMatchRef> matches;
    private String errorMessage;
}
