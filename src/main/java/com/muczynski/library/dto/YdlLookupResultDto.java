/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual YDL (Ypsilanti District Library) availability lookup result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YdlLookupResultDto {
    private Long bookId;
    private boolean success;
    private Boolean audioAvailable;
    private Boolean paperAvailable;
    private Boolean ebookAvailable;
    private String matchedTitle;
    private String errorMessage;
}
