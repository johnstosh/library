/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChicagoTitleCaseTest {

    private static final String DOMINIC = "The History of St. Dominic: Founder of the Friars Preachers";

    @Test
    void rewritesLowerUpperRdaAndEveryWordCapsIncludingSubtitle() {
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(
                "the history of st. dominic: founder of the friars preachers"));
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(
                "THE HISTORY OF ST. DOMINIC: FOUNDER OF THE FRIARS PREACHERS"));
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(
                "The history of St. Dominic : founder of the Friars Preachers"));
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(
                "The History Of St. Dominic: Founder Of The Friars Preachers"));
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(DOMINIC));
        assertFalse(ChicagoTitleCase.needsWork(DOMINIC));
        assertTrue(ChicagoTitleCase.needsWork("THE HISTORY OF ST. DOMINIC: FOUNDER OF THE FRIARS PREACHERS"));
    }

    @Test
    void capitalizesFirstWordOfSubtitleAndLowersShortPrepositions() {
        assertEquals("Of Mice and Men", ChicagoTitleCase.toChicago("of mice and men"));
        assertEquals("Gone with the Wind", ChicagoTitleCase.toChicago("gone with the wind"));
        assertEquals("Foo: The Bar", ChicagoTitleCase.toChicago("foo: the bar"));
        assertEquals("The End", ChicagoTitleCase.toChicago("the end"));
        assertEquals("A Tale of Two Cities", ChicagoTitleCase.toChicago("a tale of two cities"));
        assertEquals("World War II", ChicagoTitleCase.toChicago("world war ii"));
        assertEquals("The Mix of Things", ChicagoTitleCase.toChicago("the mix of things"));
        assertEquals("O'Brien's Book", ChicagoTitleCase.toChicago("o'brien's book"));
        assertEquals("The State-of-the-Art Guide", ChicagoTitleCase.toChicago("the state-of-the-art guide"));
    }

    @Test
    void keepsCatalogCopySuffix() {
        assertEquals("The Lord of the Rings, c. 2",
                ChicagoTitleCase.toChicago("the lord of the rings, c. 2"));
        assertEquals("The Lord of the Rings, c. 2",
                ChicagoTitleCase.toChicago("THE LORD OF THE RINGS, C. 2"));
        assertFalse(ChicagoTitleCase.needsWork("The Lord of the Rings, c. 2"));
        assertTrue(ChicagoTitleCase.needsWork("The Lord of the Rings, c.2"));
    }

    @Test
    void leavesBlankTitlesAlone() {
        assertFalse(ChicagoTitleCase.needsWork(null));
        assertFalse(ChicagoTitleCase.needsWork("  "));
        assertEquals("  ", ChicagoTitleCase.toChicago("  "));
    }
}
