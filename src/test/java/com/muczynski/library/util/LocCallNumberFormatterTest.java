/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LocCallNumberFormatter}.
 */
class LocCallNumberFormatterTest {

    /**
     * Tests that a standard LOC call number is correctly split into spine-friendly format.
     *
     * Library book spines have limited horizontal space, so call numbers must be
     * displayed vertically with each component on its own line. This test verifies
     * that the formatter correctly parses a complete call number like "BX 4705.M124 A77 2005"
     * into its five components: class letters (BX), class number (4705), cutter number (.M124),
     * additional cutter (A77), and year (2005).
     */
    @Test
    void formatForSpine_standardCallNumber_splitsCorrectly() {
        String input = "BX 4705.M124 A77 2005";
        String expected = "BX\n4705\n.M124\nA77\n2005";

        String result = LocCallNumberFormatter.formatForSpine(input);

        assertEquals(expected, result);
    }

    /**
     * Tests formatting of a simple call number without cutter numbers.
     */
    @Test
    void formatForSpine_simpleCallNumber_splitsCorrectly() {
        String input = "PS 3545 2001";
        String expected = "PS\n3545\n2001";

        String result = LocCallNumberFormatter.formatForSpine(input);

        assertEquals(expected, result);
    }

    /**
     * Tests that a call number with only class and number works correctly.
     */
    @Test
    void formatForSpine_classAndNumberOnly_splitsCorrectly() {
        String input = "QA 76.73";
        String expected = "QA\n76\n.73";

        String result = LocCallNumberFormatter.formatForSpine(input);

        assertEquals(expected, result);
    }

    /**
     * Tests that null input returns empty string.
     */
    @Test
    void formatForSpine_nullInput_returnsEmptyString() {
        String result = LocCallNumberFormatter.formatForSpine(null);

        assertEquals("", result);
    }

    /**
     * Tests that blank input returns empty string.
     */
    @Test
    void formatForSpine_blankInput_returnsEmptyString() {
        String result = LocCallNumberFormatter.formatForSpine("   ");

        assertEquals("", result);
    }

    /**
     * Tests that empty string input returns empty string.
     */
    @Test
    void formatForSpine_emptyInput_returnsEmptyString() {
        String result = LocCallNumberFormatter.formatForSpine("");

        assertEquals("", result);
    }

    /**
     * Tests that extra whitespace is handled correctly.
     */
    @Test
    void formatForSpine_extraWhitespace_handledCorrectly() {
        String input = "  BX   4705.M124    A77  ";
        String expected = "BX\n4705\n.M124\nA77";

        String result = LocCallNumberFormatter.formatForSpine(input);

        assertEquals(expected, result);
    }

    /**
     * Tests HTML formatting with br tags instead of newlines.
     */
    @Test
    void formatForSpineHtml_standardCallNumber_usesHtmlBreaks() {
        String input = "BX 4705.M124 A77 2005";
        String expected = "BX<br>4705<br>.M124<br>A77<br>2005";

        String result = LocCallNumberFormatter.formatForSpineHtml(input);

        assertEquals(expected, result);
    }

    /**
     * Tests that null input to HTML formatter returns empty string.
     */
    @Test
    void formatForSpineHtml_nullInput_returnsEmptyString() {
        String result = LocCallNumberFormatter.formatForSpineHtml(null);

        assertEquals("", result);
    }

    /**
     * Tests a call number with multiple cutter numbers.
     */
    @Test
    void formatForSpine_multipleCutters_splitsCorrectly() {
        String input = "PR 6068.O93 Z46 1998";
        String expected = "PR\n6068\n.O93\nZ46\n1998";

        String result = LocCallNumberFormatter.formatForSpine(input);

        assertEquals(expected, result);
    }
}
