/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookStatusFilterTest {

    @Test
    void parseFilterValuesIgnoresUnknownAndDedupes() {
        assertTrue(BookStatusFilter.parseFilterValues(null).isEmpty());
        assertTrue(BookStatusFilter.parseFilterValues("").isEmpty());
        assertEquals(
                List.of(BookStatusFilter.IN_LIBRARY, BookStatusFilter.REQUESTED),
                BookStatusFilter.parseFilterValues("in-library,bogus,REQUESTED,in-library"));
    }
}
