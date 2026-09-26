/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JaroWinklerSimilarityTest {

    @Test
    void identicalStringsScoreOne() {
        assertEquals(1.0, JaroWinklerSimilarity.similarity("confession", "confession"));
    }

    @Test
    void slightMisspellingScoresHigh() {
        // "Confessions" vs "Confession" after stemming are identical; use near-miss spellings
        double score = JaroWinklerSimilarity.similarity("confession", "confesion");
        assertTrue(score >= 0.85, "expected >= 0.85 but was " + score);

        double stemLike = JaroWinklerSimilarity.similarity("confessions", "confession");
        assertTrue(stemLike >= 0.90, "expected >= 0.90 but was " + stemLike);
    }

    @Test
    void unrelatedTitlesScoreLow() {
        double score = JaroWinklerSimilarity.similarity("pride prejudice", "moby dick");
        assertTrue(score < 0.7, "expected < 0.7 but was " + score);
    }

    @Test
    void emptyOrNullIsZero() {
        assertEquals(0.0, JaroWinklerSimilarity.similarity("", "abc"));
        assertEquals(0.0, JaroWinklerSimilarity.similarity(null, "abc"));
        assertEquals(0.0, JaroWinklerSimilarity.similarity("abc", null));
    }
}
