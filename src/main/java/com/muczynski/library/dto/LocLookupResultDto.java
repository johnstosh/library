package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual LOC lookup result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocLookupResultDto {
    private Long bookId;
    private boolean success;
    private String locNumber;
    private String errorMessage;
    private int matchCount;
}
