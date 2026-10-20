/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateTitleDifferenceTest {

    @Test
    void titlesNameExactCaseSpacingFunctionWordsAndOneLetter() {
        assertEquals("Titles match exactly.",
                DuplicateTitleDifference.titles("The Cat", "The Cat", "title", "title", null, null));
        assertEquals("Titles match case-insensitively.",
                DuplicateTitleDifference.titles("The Cat", "the cat", "title", "title", null, null));
        assertEquals("Titles match except for spacing or punctuation.",
                DuplicateTitleDifference.titles("Cat!", "Cat", "title", "title", null, null));
        assertEquals("Titles differ by the article \"the\".",
                DuplicateTitleDifference.titles("The Cat", "Cat", "title", "title", null, null));
        assertEquals("Titles differ by two articles: \"the\" and \"a\".",
                DuplicateTitleDifference.titles("The Cat", "A Cat", "title", "title", null, null));
        assertEquals("Titles differ by the preposition \"in\" and the article \"the\".",
                DuplicateTitleDifference.titles("Cat in the Hat", "Cat Hat", "title", "title", null, null));
        assertEquals("Titles differ by a one-letter misspelling.",
                DuplicateTitleDifference.titles("Cat", "Cats", "title", "title", null, null));
    }

    @Test
    void titlesSayWhichFieldsWereComparedWhenAnAlternateIsPresent() {
        String text = DuplicateTitleDifference.titles(
                "The Confessions", "the confessions", "alternate title", "alternate title",
                "The Confessions", "the confessions");
        assertTrue(text.startsWith("Compared Book A alternate title with Book B alternate title. "));
        assertTrue(text.endsWith("Titles match case-insensitively."));
    }

    @Test
    void authorsNameExactCaseSpacingMiddleNameAndSameRecord() {
        assertEquals("Same author record.",
                DuplicateTitleDifference.authors(4L, "Augustine", List.of(), 4L, "Augustine", List.of()));
        assertEquals("Authors match exactly.",
                DuplicateTitleDifference.authors(1L, "Augustine", List.of(), 2L, "Augustine", List.of()));
        assertEquals("Authors match case-insensitively.",
                DuplicateTitleDifference.authors(1L, "Augustine", List.of(), 2L, "augustine", List.of()));
        assertEquals("Authors match except for spacing or punctuation.",
                DuplicateTitleDifference.authors(1L, "J.R.R. Tolkien", List.of(), 2L, "J R R Tolkien", List.of()));
        assertEquals(
                "Authors differ by a middle name: \"Paul\" is present on Book A author name and absent on Book B author name.",
                DuplicateTitleDifference.authors(
                        1L, "John Paul Smith", List.of(), 2L, "John Smith", List.of()));
    }

    @Test
    void authorsPreferACaseInsensitiveAlternateMatch() {
        String text = DuplicateTitleDifference.authors(
                1L, "John Smith", List.of("Augustine"),
                2L, "Someone Else", List.of("augustine"));
        assertTrue(text.contains("Book A alternate name matches Book B alternate name \"Augustine\" case-insensitively."));
    }
}
