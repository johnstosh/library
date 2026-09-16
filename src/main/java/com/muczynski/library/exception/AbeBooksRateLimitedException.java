/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

/**
 * AbeBooks rejected or throttled a SearchResults request (HTTP 403/429/502/503/504,
 * I/O timeout, captcha/block page, or a no-listing response that came back too quickly).
 */
public class AbeBooksRateLimitedException extends RuntimeException {

    public static final String MESSAGE = "AbeBooks rate limited";

    private final String searchUrl;

    public AbeBooksRateLimitedException() {
        this(MESSAGE, null, null);
    }

    public AbeBooksRateLimitedException(String message) {
        this(message, null, null);
    }

    public AbeBooksRateLimitedException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public AbeBooksRateLimitedException(String message, String searchUrl, Throwable cause) {
        super(message, cause);
        this.searchUrl = searchUrl;
    }

    public String getSearchUrl() {
        return searchUrl;
    }
}
