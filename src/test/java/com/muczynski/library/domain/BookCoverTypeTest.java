/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookCoverTypeTest {

    @Test
    void displayName_coversEveryBinding() {
        assertEquals("Hardcover", BookCoverType.HARDCOVER.displayName());
        assertEquals("Softcover", BookCoverType.SOFTCOVER.displayName());
        assertEquals("Library Binding", BookCoverType.LIBRARY_BINDING.displayName());
        assertEquals("Other", BookCoverType.OTHER.displayName());
        assertEquals("Unknown", BookCoverType.UNKNOWN.displayName());
    }

    @Test
    void typedPriceCovers_excludeOtherAndUnknown() {
        assertTrue(BookCoverType.HARDCOVER.isTypedPriceCover());
        assertTrue(BookCoverType.SOFTCOVER.isTypedPriceCover());
        assertTrue(BookCoverType.LIBRARY_BINDING.isTypedPriceCover());
        assertFalse(BookCoverType.OTHER.isTypedPriceCover());
        assertFalse(BookCoverType.UNKNOWN.isTypedPriceCover());
        assertTrue(BookCoverType.OTHER.isOtherOrUnknown());
        assertTrue(BookCoverType.UNKNOWN.isOtherOrUnknown());
    }

    @Test
    void orUnknown_defaultsNull() {
        assertEquals(BookCoverType.UNKNOWN, BookCoverType.orUnknown(null));
        assertEquals(BookCoverType.HARDCOVER, BookCoverType.orUnknown(BookCoverType.HARDCOVER));
    }
}
