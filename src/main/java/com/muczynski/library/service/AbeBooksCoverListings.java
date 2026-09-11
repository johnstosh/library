/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import lombok.Builder;
import lombok.Value;

/**
 * Cheapest good-or-better AbeBooks listings found for each cover.
 * Either field may be null when that cover had no usable listing.
 * An unknown-binding listing may be used for both covers.
 */
@Value
@Builder
public class AbeBooksCoverListings {
    AbeBooksListing hardcover;
    AbeBooksListing softcover;
}
