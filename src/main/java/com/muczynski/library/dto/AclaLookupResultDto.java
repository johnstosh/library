/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual ACLA (Allegheny County Library Association) availability lookup result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclaLookupResultDto {
    private Long bookId;
    private boolean success;
    private Boolean audioAvailable;
    private Boolean paperAvailable;
    private Boolean ebookAvailable;
    private String matchedTitle;
    private String errorMessage;
}
