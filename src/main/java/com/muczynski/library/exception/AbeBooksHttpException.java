/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.exception;

/**
 * AbeBooks returned an HTTP error that is not treated as rate-limiting
 * (for example 500). The Prices page stores {@link #getMessage()} as
 * {@code lookupError} and the search URL as {@code detailsUrl}.
 */
public class AbeBooksHttpException extends RuntimeException {

    private final int statusCode;
    private final String searchUrl;

    public AbeBooksHttpException(int statusCode, String searchUrl, Throwable cause) {
        super(messageFor(statusCode), cause);
        this.statusCode = statusCode;
        this.searchUrl = searchUrl;
    }

    public static String messageFor(int statusCode) {
        return "AbeBooks HTTP " + statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getSearchUrl() {
        return searchUrl;
    }
}
