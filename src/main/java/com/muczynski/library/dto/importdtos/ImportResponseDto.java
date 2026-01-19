/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for import operations.
 * Provides structured JSON response with success status, message, and counts.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportResponseDto {
    private boolean success;
    private String message;
    private ImportCounts counts;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportCounts {
        private int libraries;
        private int authors;
        private int users;
        private int books;
        private int loans;
        private int photos;
    }

    public static ImportResponseDto success(String message, ImportCounts counts) {
        return new ImportResponseDto(true, message, counts);
    }

    public static ImportResponseDto error(String message) {
        return new ImportResponseDto(false, message, null);
    }
}
