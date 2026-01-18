/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for library checkout card transcription result from Grok AI.
 * Contains book information and last checkout details extracted from a photo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutCardTranscriptionDto {
    private String title;
    private String author;

    @JsonProperty("call_number")
    private String callNumber;

    @JsonProperty("last_date")
    private String lastDate;

    @JsonProperty("last_issued_to")
    private String lastIssuedTo;

    @JsonProperty("last_due")
    private String lastDue;
}
