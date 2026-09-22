/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import lombok.Builder;
import lombok.Value;

/**
 * Cheapest good-or-better AbeBooks listings found for each cover.
 * Hardcover/softcover may be filled from an unknown-binding listing.
 * Library binding and other named bindings are only set when parsed as such.
 * {@code searchUrl} is the last SearchResults URL fetched (used when no listing).
 */
@Value
@Builder
public class AbeBooksCoverListings {
    AbeBooksListing hardcover;
    AbeBooksListing softcover;
    AbeBooksListing libraryBinding;
    AbeBooksListing other;
    String searchUrl;
    boolean rateLimited;
}
