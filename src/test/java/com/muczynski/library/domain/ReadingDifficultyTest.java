/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadingDifficultyTest {

    @Test
    void parseFilterValues_ignoresBlankUnknownAndDuplicates() {
        assertTrue(ReadingDifficulty.parseFilterValues(null).isEmpty());
        assertTrue(ReadingDifficulty.parseFilterValues("").isEmpty());
        assertEquals(
                List.of(ReadingDifficulty.CHILDREN, ReadingDifficulty.UNSET),
                ReadingDifficulty.parseFilterValues("children,bogus,UNSET,children"));
    }

    @Test
    void fromString_mapsNullAndBlankToUnset() {
        assertEquals(ReadingDifficulty.UNSET, ReadingDifficulty.fromString(null));
        assertEquals(ReadingDifficulty.UNSET, ReadingDifficulty.fromString(""));
        assertEquals(ReadingDifficulty.UNSET, ReadingDifficulty.fromString("  "));
        assertEquals(ReadingDifficulty.CHILDREN, ReadingDifficulty.fromString("children"));
    }

    @Test
    void tryParseAssignable_rejectsUnsetBlankAndUnknown() {
        assertEquals(null, ReadingDifficulty.tryParseAssignable(null));
        assertEquals(null, ReadingDifficulty.tryParseAssignable(""));
        assertEquals(null, ReadingDifficulty.tryParseAssignable("unset"));
        assertEquals(null, ReadingDifficulty.tryParseAssignable("bogus"));
        assertEquals(ReadingDifficulty.DEMANDING, ReadingDifficulty.tryParseAssignable("DEMANDING"));
        assertEquals(ReadingDifficulty.CHILDREN, ReadingDifficulty.tryParseAssignable("children"));
    }
}
