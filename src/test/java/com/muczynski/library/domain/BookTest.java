/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookTest {

    @Test
    void stripCopySuffix_removesCanonicalCopyNumber() {
        assertEquals("Gather Comprehensive", Book.stripCopySuffix("Gather Comprehensive, c. 2"));
    }

    @Test
    void stripCopySuffix_removesCopyNumberWithoutSpaceAfterPeriod() {
        assertEquals("Gather Comprehensive", Book.stripCopySuffix("Gather Comprehensive, c.2"));
    }

    @Test
    void stripCopySuffix_removesCopyNumberWithoutPeriod() {
        assertEquals("101 Things to Do with a Baby", Book.stripCopySuffix("101 Things to Do with a Baby, c 1"));
    }

    @Test
    void stripCopySuffix_isCaseInsensitiveAndTrims() {
        assertEquals("Way of the Cross", Book.stripCopySuffix("  Way of the Cross, C. 3  "));
    }

    @Test
    void stripCopySuffix_leavesTitleWithoutSuffixUnchanged() {
        assertEquals("The Spiritual Exercises", Book.stripCopySuffix("The Spiritual Exercises"));
    }

    @Test
    void stripCopySuffix_doesNotStripCopyNumberInTheMiddleOfTheTitle() {
        assertEquals("Volume 1, c. 2 extra", Book.stripCopySuffix("Volume 1, c. 2 extra"));
    }

    @Test
    void stripCopySuffix_returnsOriginalWhenStrippingWouldLeaveTitleEmpty() {
        assertEquals(", c. 2", Book.stripCopySuffix(", c. 2"));
    }

    @Test
    void stripCopySuffix_preservesNullAndBlank() {
        assertNull(Book.stripCopySuffix(null));
        assertEquals("   ", Book.stripCopySuffix("   "));
    }

    @Test
    void titleWithoutCopySuffix_usesTheBooksTitle() {
        Book book = new Book();
        book.setTitle("Butler's Lives of the Saints, c. 2");
        assertEquals("Butler's Lives of the Saints", book.titleWithoutCopySuffix());
    }
}
