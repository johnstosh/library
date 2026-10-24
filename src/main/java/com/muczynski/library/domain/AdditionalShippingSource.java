/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

/**
 * Where a seller's extra-book shipping amount came from.
 * AbeBooks publishes the first-book rate and applies the extra-book rate in the basket.
 */
public enum AdditionalShippingSource {
    /** Parsed from the seller's shipping-terms text. */
    PARSED,
    /** Entered on the Seller Info page. A later fetch does not replace it. */
    LIBRARIAN,
    /** Terms say each additional book ships free. */
    FREE,
    /** No extra-book amount is known. */
    UNKNOWN
}
