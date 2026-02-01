/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for import operations.
 * Provides structured JSON response with success status, message, counts, and per-entity errors.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportResponseDto {
    private boolean success;
    private String message;
    private ImportCounts counts;
    private List<ImportErrorDto> errors;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportCounts {
        private int branches;
        private int authors;
        private int users;
        private int books;
        private int loans;
        private int photos;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportErrorDto {
        private String entityType;
        private String entityName;
        private String errorMessage;
    }

    public static ImportResponseDto success(String message, ImportCounts counts) {
        return new ImportResponseDto(true, message, counts, new ArrayList<>());
    }

    public static ImportResponseDto success(String message, ImportCounts counts, List<ImportErrorDto> errors) {
        return new ImportResponseDto(true, message, counts, errors);
    }

    public static ImportResponseDto partialSuccess(String message, ImportCounts counts, List<ImportErrorDto> errors) {
        return new ImportResponseDto(!errors.isEmpty() && counts != null, message, counts, errors);
    }

    public static ImportResponseDto error(String message) {
        return new ImportResponseDto(false, message, null, new ArrayList<>());
    }

    /**
     * Result object returned by ImportService.importData() containing counts and errors.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportResult {
        private ImportCounts counts;
        private List<ImportErrorDto> errors;

        public ImportResult(ImportCounts counts) {
            this.counts = counts;
            this.errors = new ArrayList<>();
        }

        public void addError(String entityType, String entityName, String errorMessage) {
            if (errors == null) {
                errors = new ArrayList<>();
            }
            errors.add(new ImportErrorDto(entityType, entityName, errorMessage));
        }

        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
    }
}
