/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

/**
 * AbeBooks rejected or throttled a SearchResults request (HTTP 403/429/503,
 * captcha/block page, or a no-listing response that came back too quickly).
 */
public class AbeBooksRateLimitedException extends RuntimeException {

    public static final String MESSAGE = "AbeBooks rate limited";

    public AbeBooksRateLimitedException() {
        super(MESSAGE);
    }

    public AbeBooksRateLimitedException(String message) {
        super(message);
    }

    public AbeBooksRateLimitedException(String message, Throwable cause) {
        super(message, cause);
    }
}
