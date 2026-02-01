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

    // Tests for normalizeForSearch

    @Test
    void normalizeForSearch_removesLeadingArticles() {
        assertEquals("spiritual exercises", TitleMatcher.normalizeForSearch("The Spiritual Exercises"));
        assertEquals("tale two cities", TitleMatcher.normalizeForSearch("A Tale of Two Cities"));
        assertEquals("apple day", TitleMatcher.normalizeForSearch("An Apple a Day"));
    }

    @Test
    void normalizeForSearch_removesCommonWords() {
        // Removes: the, a, an, in, at, to, of, on, by, for, and, or, with, from
        assertEquals("history world", TitleMatcher.normalizeForSearch("The History of the World"));
        assertEquals("war peace", TitleMatcher.normalizeForSearch("War and Peace"));
        assertEquals("journey center earth", TitleMatcher.normalizeForSearch("A Journey to the Center of the Earth"));
    }

    @Test
    void normalizeForSearch_removesPunctuation() {
        assertEquals("hello world", TitleMatcher.normalizeForSearch("Hello, World!"));
        assertEquals("its wonderful life", TitleMatcher.normalizeForSearch("It's a Wonderful Life"));
    }

    @Test
    void normalizeForSearch_handlesNullAndEmpty() {
        assertEquals("", TitleMatcher.normalizeForSearch(null));
        assertEquals("", TitleMatcher.normalizeForSearch(""));
        assertEquals("", TitleMatcher.normalizeForSearch("   "));
    }

    @Test
    void normalizeForSearch_preservesSignificantWords() {
        // "Exercises" should be preserved (not a stop word)
        String result = TitleMatcher.normalizeForSearch("The Spiritual Exercises of Saint Ignatius");
        assertTrue(result.contains("spiritual"));
        assertTrue(result.contains("exercises"));
        assertTrue(result.contains("saint"));
        assertTrue(result.contains("ignatius"));
        assertFalse(result.contains("the"));
        assertFalse(result.contains("of"));
    }

    @Test
    void normalizeForSearch_removesNumbers() {
        // Pure numbers like publication years should be removed
        assertEquals("war peace edition", TitleMatcher.normalizeForSearch("War and Peace 1952 Edition"));
        assertEquals("history world vol", TitleMatcher.normalizeForSearch("A History of the World Vol 2"));
        assertEquals("collected poems", TitleMatcher.normalizeForSearch("Collected Poems 1909-1962"));
    }

    @Test
    void titleMatches_ignoresNumbers_simpleCase() {
        // Publication years should not affect matching - base title is the same
        assertTrue(TitleMatcher.titleMatches("War and Peace", "War and Peace 1952"));
        assertTrue(TitleMatcher.titleMatches("War and Peace 1952", "War and Peace"));
    }

    @Test
    void titleMatches_ignoresNumbers_withDifferentSuffixes() {
        // These have 4 significant words each: war, and, peace, edition/complete
        // 3 out of 4 match (75% >= 70% threshold) so they should match
        // The years 1952/1869 are ignored as insignificant
        assertTrue(TitleMatcher.titleMatches("War and Peace 1952 Edition", "War and Peace 1869 Complete"));
    }

    @Test
    void titleMatches_singleWordChristmasDoesNotMatchLongTitle() {
        // Regression test: Single word should not match a long title containing that word
        // because bidirectional matching requires 70% in BOTH directions
        assertFalse(TitleMatcher.titleMatches("Christmas", "How the Grinch Stole Christmas"));
    }

    @Test
    void titleMatches_ignoresTrailingParentheticalDates() {
        // Titles with trailing dates in parentheses should match the base title
        assertTrue(TitleMatcher.titleMatches("Humanae Vitae (July 25, 1968)", "Humanae Vitae"));
        assertTrue(TitleMatcher.titleMatches("Humanae Vitae", "Humanae Vitae (July 25, 1968)"));
        assertTrue(TitleMatcher.titleMatches("Rerum Novarum (May 15, 1891)", "Rerum Novarum"));
    }

    @Test
    void titleMatches_ignoresTrailingParentheticalEditions() {
        // Titles with edition info in parentheses should match
        assertTrue(TitleMatcher.titleMatches("War and Peace (2nd Edition)", "War and Peace"));
        assertTrue(TitleMatcher.titleMatches("The Republic (Penguin Classics)", "The Republic"));
    }
}
