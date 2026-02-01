/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for API errors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private String entityType;
    private String entityName;
    private Long existingEntityId;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String error, String message, String entityType, String entityName, Long existingEntityId) {
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.entityType = entityType;
        this.entityName = entityName;
        this.existingEntityId = existingEntityId;
    }
}
