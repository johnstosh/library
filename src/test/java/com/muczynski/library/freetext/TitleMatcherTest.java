/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TitleMatcherTest {

    @Test
    void titleMatches_exactMatch() {
        assertTrue(TitleMatcher.titleMatches("Pride and Prejudice", "Pride and Prejudice"));
    }

    @Test
    void titleMatches_caseInsensitive() {
        assertTrue(TitleMatcher.titleMatches("PRIDE AND PREJUDICE", "pride and prejudice"));
    }

    @Test
    void titleMatches_ignoresLeadingArticle_the() {
        assertTrue(TitleMatcher.titleMatches("The Hobbit", "Hobbit"));
        assertTrue(TitleMatcher.titleMatches("Hobbit", "The Hobbit"));
    }

    @Test
    void titleMatches_ignoresLeadingArticle_a() {
        assertTrue(TitleMatcher.titleMatches("A Tale of Two Cities", "Tale of Two Cities"));
    }

    @Test
    void titleMatches_ignoresLeadingArticle_an() {
        assertTrue(TitleMatcher.titleMatches("An Introduction to Programming", "Introduction to Programming"));
    }

    @Test
    void titleMatches_matchesMainTitleBeforeColon() {
        assertTrue(TitleMatcher.titleMatches(
                "Pride and Prejudice: A Novel",
                "Pride and Prejudice"));
        assertTrue(TitleMatcher.titleMatches(
                "Pride and Prejudice",
                "Pride and Prejudice: A Novel"));
    }

    @Test
    void titleMatches_ignoresPunctuation() {
        // Note: Hyphens are removed in normalization, so "mobydick" != "moby dick"
        // This tests that basic punctuation like apostrophes are removed
        assertTrue(TitleMatcher.titleMatches(
                "Alices Adventures in Wonderland",
                "Alice's Adventures in Wonderland"));
    }

    @Test
    void titleMatches_returnsFalseForDifferentTitles() {
        assertFalse(TitleMatcher.titleMatches("Pride and Prejudice", "Sense and Sensibility"));
    }

    @Test
    void titleMatches_handlesNullInput() {
        assertFalse(TitleMatcher.titleMatches(null, "Pride and Prejudice"));
        assertFalse(TitleMatcher.titleMatches("Pride and Prejudice", null));
        assertFalse(TitleMatcher.titleMatches(null, null));
    }

    @Test
    void titleMatches_longTitlesWithPartialMatch() {
        // Long titles that contain each other should match
        assertTrue(TitleMatcher.titleMatches(
                "The Complete Works of William Shakespeare",
                "Complete Works of William Shakespeare Including All Plays"));
    }

    @Test
    void authorMatches_exactMatch() {
        assertTrue(TitleMatcher.authorMatches("Jane Austen", "Jane Austen"));
    }

    @Test
    void authorMatches_caseInsensitive() {
        assertTrue(TitleMatcher.authorMatches("JANE AUSTEN", "jane austen"));
    }

    @Test
    void authorMatches_lastNameOnly() {
        assertTrue(TitleMatcher.authorMatches("Jane Austen", "Austen"));
        assertTrue(TitleMatcher.authorMatches("Austen", "Jane Austen"));
    }

    @Test
    void authorMatches_handlesNullInput() {
        assertFalse(TitleMatcher.authorMatches(null, "Jane Austen"));
        assertFalse(TitleMatcher.authorMatches("Jane Austen", null));
        assertFalse(TitleMatcher.authorMatches(null, null));
    }

    @Test
    void authorMatches_returnsFalseForDifferentAuthors() {
        assertFalse(TitleMatcher.authorMatches("Jane Austen", "Charles Dickens"));
    }

    @Test
    void authorMatches_multiPartName_secondToLastMatches() {
        // Spanish naming: "Gabriel García Márquez" - García is paternal surname
        assertTrue(TitleMatcher.authorMatches("Gabriel García Márquez", "García"));
        assertTrue(TitleMatcher.authorMatches("García", "Gabriel García Márquez"));
    }

    @Test
    void authorMatches_multiPartName_thirdToLastMatches() {
        // Four-part name where 2nd-to-last is the target
        assertTrue(TitleMatcher.authorMatches("Pope John Paul II", "Paul"));
        assertTrue(TitleMatcher.authorMatches("Jorge Mario Bergoglio", "Mario"));
    }

    @Test
    void authorMatches_lastFirstFormat() {
        // "Austen, Jane" format should match "Jane Austen"
        assertTrue(TitleMatcher.authorMatches("Austen, Jane", "Austen"));
    }

    @Test
    void titleMatches_sameMainTitleDifferentSubtitles() {
        // Both have the same main title "War and Peace" so they should match
        assertTrue(TitleMatcher.titleMatches(
                "War and Peace",
                "War and Peace: Complete Edition"));
    }
}
