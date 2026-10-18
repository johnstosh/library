/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocOpenAccessContributorTest {

    @Test
    void contributorMatches_rejectsAnotherAuthor() {
        assertFalse(LocOpenAccessProvider.contributorMatches(
                List.of("frink, maria mrs"), "Paul O'Sullivan"));
    }

    @Test
    void contributorMatches_acceptsCatalogFormOfSameAuthor() {
        assertTrue(LocOpenAccessProvider.contributorMatches(
                List.of("osullivan, paul, 1871-1958"), "Paul O'Sullivan"));
    }

    @Test
    void contributorMatches_allowsMissingContributorsOrAuthor() {
        assertTrue(LocOpenAccessProvider.contributorMatches(null, "Paul O'Sullivan"));
        assertTrue(LocOpenAccessProvider.contributorMatches(List.of("frink, maria mrs"), null));
    }
}
