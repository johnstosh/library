/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogFilterServicePlotTest {

    @Test
    void keepsABookUnlessBothFieldsAreProper() {
        String proper = "x".repeat(CatalogFilterService.PROPER_TEXT_MIN_CHARS);
        assertTrue(CatalogFilterService.lacksProperPlotOrDescription(null, null));
        assertTrue(CatalogFilterService.lacksProperPlotOrDescription(proper, "short"));
        assertTrue(CatalogFilterService.lacksProperPlotOrDescription("  short  ", proper));
        assertTrue(CatalogFilterService.lacksProperPlotOrDescription("   ", proper));
        assertTrue(CatalogFilterService.lacksProperPlotOrDescription(
                " " + "x".repeat(CatalogFilterService.PROPER_TEXT_MIN_CHARS - 1),
                proper));
        assertFalse(CatalogFilterService.lacksProperPlotOrDescription(proper, proper));
        assertFalse(CatalogFilterService.lacksProperPlotOrDescription("  " + proper + "  ", proper));
    }

    @Test
    void descriptionTrimCharsMatchJavaTrimExceptNul() {
        String chars = CatalogFilterService.DESCRIPTION_TRIM_CHARS;
        assertEquals(32, chars.length());
        assertFalse(chars.indexOf('\0') >= 0);
        assertTrue(chars.indexOf(' ') >= 0);
        assertTrue(chars.indexOf('\n') >= 0);
        assertTrue(chars.indexOf('\t') >= 0);
        assertTrue(chars.indexOf('\r') >= 0);
    }
}
