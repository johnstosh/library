/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation error response DTO for request validation failures
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ValidationErrorResponse extends ErrorResponse {
    private List<FieldError> fieldErrors = new ArrayList<>();

    public ValidationErrorResponse(String message) {
        super("VALIDATION_ERROR", message);
    }

    public void addFieldError(String field, String message) {
        fieldErrors.add(new FieldError(field, message));
    }

    @Data
    @EqualsAndHashCode
    public static class FieldError {
        private final String field;
        private final String message;
    }
}
