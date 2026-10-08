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
    void capitalizesTheWordAfterASentenceBreak() {
        String council = "The Seventh Ecumenical Council. The Second Council of Nice";
        assertEquals(council, ChicagoTitleCase.toChicago(council));
        assertEquals(council, ChicagoTitleCase.toChicago(
                "The Seventh Ecumenical Council. the Second Council of Nice"));
        assertEquals(council, ChicagoTitleCase.toChicago(
                "the seventh ecumenical council. the second council of nice"));
        assertFalse(ChicagoTitleCase.needsWork(council));
        assertTrue(ChicagoTitleCase.needsWork(
                "The Seventh Ecumenical Council. the Second Council of Nice"));
        assertEquals("The End. Of Mice and Men", ChicagoTitleCase.toChicago("the end. of mice and men"));
        assertEquals("The World We Live In. A History",
                ChicagoTitleCase.toChicago("the world we live in. a history"));
        assertEquals("What Is It? The Answer", ChicagoTitleCase.toChicago("what is it? the answer"));
        assertEquals("Stop! The Book", ChicagoTitleCase.toChicago("stop! the book"));
        assertEquals("Wait... The End", ChicagoTitleCase.toChicago("wait... the end"));
        assertEquals("Done. Next-to Last", ChicagoTitleCase.toChicago("done. next-to last"));
    }

    @Test
    void leavesAbbreviationsAndInitialsLowerThanANewSentence() {
        assertEquals("Mr. and Mrs. Smith", ChicagoTitleCase.toChicago("mr. and mrs. smith"));
        assertEquals("Poems, Etc. and Essays", ChicagoTitleCase.toChicago("poems, etc. and essays"));
        assertEquals("J. R. R. Tolkien", ChicagoTitleCase.toChicago("j. r. r. tolkien"));
        assertEquals("U.S. History", ChicagoTitleCase.toChicago("u.s. history"));
        assertEquals("Volume II. The Council", ChicagoTitleCase.toChicago("volume ii. the council"));
        assertEquals("World War II.", ChicagoTitleCase.toChicago("world war ii."));
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
