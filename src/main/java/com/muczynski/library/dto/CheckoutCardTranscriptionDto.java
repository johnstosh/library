/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for library checkout card transcription result from Grok AI.
 * Contains book information and last checkout details extracted from a photo.
 * Note: Grok returns snake_case JSON which is parsed using a snake_case ObjectMapper
 * in CheckoutCardTranscriptionService. This DTO uses camelCase for the API response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutCardTranscriptionDto {
    private String title;
    private String author;
    private String callNumber;
    private String lastDate;
    private String lastIssuedTo;
    private String lastDue;
}
