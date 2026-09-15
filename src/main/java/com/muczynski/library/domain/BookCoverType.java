/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

/**
 * Physical cover type used when looking up used-book prices on AbeBooks.
 */
public enum BookCoverType {
    HARDCOVER,
    SOFTCOVER,
    UNKNOWN;

    /** AbeBooks SearchResults {@code bi} parameter: {@code h} or {@code s}. */
    public String abeBooksBindingParam() {
        if (this == HARDCOVER) {
            return "h";
        }
        if (this == SOFTCOVER) {
            return "s";
        }
        return "";
    }

    public String displayName() {
        if (this == HARDCOVER) {
            return "Hardcover";
        }
        if (this == SOFTCOVER) {
            return "Softcover";
        }
        return "Other/Unknown";
    }
}
