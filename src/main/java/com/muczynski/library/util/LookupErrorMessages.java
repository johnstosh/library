/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import org.springframework.web.client.RestClientResponseException;

/**
 * Builds catalog-lookup error strings that fit {@code Book}'s varchar(255)
 * lookup-error columns. HTTP error bodies (HTML block pages, JSON dumps) must
 * not be persisted — they overflow the column and the flush is misreported as
 * a duplicate-key conflict.
 */
public final class LookupErrorMessages {

    public static final int MAX_LENGTH = 255;

    private LookupErrorMessages() {
    }

    public static String fromException(Exception exception) {
        String detail;
        if (exception instanceof RestClientResponseException restEx) {
            detail = httpStatusDetail(restEx);
        } else if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            detail = exception.getMessage();
        } else {
            detail = exception.getClass().getSimpleName();
        }
        return truncate("Error: " + detail, MAX_LENGTH);
    }

    private static String httpStatusDetail(RestClientResponseException exception) {
        StringBuilder detail = new StringBuilder("HTTP ").append(exception.getStatusCode().value());
        if (exception.getStatusText() != null && !exception.getStatusText().isBlank()) {
            detail.append(' ').append(exception.getStatusText());
        }
        return detail.toString();
    }

    static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        if (max <= 3) {
            return value.substring(0, max);
        }
        return value.substring(0, max - 3) + "...";
    }
}
