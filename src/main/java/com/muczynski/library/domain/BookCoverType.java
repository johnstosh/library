/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

/**
 * Physical binding for catalog books and AbeBooks price listings.
 * {@code OTHER} is a named binding that is not hardcover, softcover, or
 * library binding (the issue's "etc"). {@code UNKNOWN} is unspecified.
 */
public enum BookCoverType {
    HARDCOVER,
    SOFTCOVER,
    LIBRARY_BINDING,
    OTHER,
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
        if (this == LIBRARY_BINDING) {
            return "Library Binding";
        }
        if (this == OTHER) {
            return "Other";
        }
        return "Unknown";
    }

    /** Hardcover, softcover, and library binding — the typed price covers. */
    public boolean isTypedPriceCover() {
        return this == HARDCOVER || this == SOFTCOVER || this == LIBRARY_BINDING;
    }

    public boolean isOtherOrUnknown() {
        return this == OTHER || this == UNKNOWN;
    }

    public static BookCoverType orUnknown(BookCoverType value) {
        return value == null ? UNKNOWN : value;
    }
}
