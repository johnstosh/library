/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

/**
 * Physical cover type used when looking up used-book prices on AbeBooks.
 */
public enum BookCoverType {
    HARDCOVER,
    SOFTCOVER;

    /** AbeBooks SearchResults {@code bi} parameter: {@code h} or {@code s}. */
    public String abeBooksBindingParam() {
        return this == HARDCOVER ? "h" : "s";
    }

    public String displayName() {
        return this == HARDCOVER ? "Hardcover" : "Softcover";
    }
}
