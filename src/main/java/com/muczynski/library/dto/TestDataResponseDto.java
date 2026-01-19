/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

/**
 * DTO for test data generation responses
 */
public class TestDataResponseDto {
    private boolean success;
    private String message;

    public TestDataResponseDto() {
    }

    public TestDataResponseDto(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
