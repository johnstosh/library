/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalAuthorNameTest {

    @Test
    void invertsCommaAndStripsYears() {
        assertEquals("Richard Simpson", CanonicalAuthorName.canonical("Simpson, Richard"));
        assertEquals("Richard Simpson", CanonicalAuthorName.canonical("Simpson, Richard, 1920-1995"));
        assertEquals("Richard Simpson", CanonicalAuthorName.canonical("Simpson, Richard, 1920-"));
        assertEquals("Richard Simpson", CanonicalAuthorName.canonical("Richard Simpson (1920-1995)"));
        assertEquals("Richard Simpson", CanonicalAuthorName.canonical("Simpson, Richard, b. 1920"));
        assertEquals("Richard Simpson", CanonicalAuthorName.canonical("Simpson, Richard, d. 1995"));
        assertEquals("Gabriel García Márquez", CanonicalAuthorName.canonical("García Márquez, Gabriel"));
        assertEquals("Martin Luther King Jr.", CanonicalAuthorName.canonical("King, Martin Luther, Jr."));
        assertEquals("Richard Simpson Jr.", CanonicalAuthorName.canonical("Richard Simpson, Jr."));
        assertTrue(CanonicalAuthorName.needsWork("Simpson, Richard"));
        assertFalse(CanonicalAuthorName.needsWork("Richard Simpson"));
    }

    @Test
    void expandsParentheticalInitialsAndDropsPunctuation() {
        assertEquals("Barney John Paul Johnson", CanonicalAuthorName.canonical(
                "Johnson, B. J.-P. (Barney John-Paul)"));
        assertEquals("John Ronald Reuel Tolkien", CanonicalAuthorName.canonical(
                "Tolkien, J. R. R. (John Ronald Reuel), 1892-1973"));
        assertEquals("John Ronald Reuel Tolkien", CanonicalAuthorName.canonical(
                "Tolkien, J. R. R. (John Ronald Reuel Tolkien)"));
        assertEquals("Daniel Day Lewis", CanonicalAuthorName.canonical(
                "Day-Lewis, D. (Daniel Day-Lewis)"));
        assertFalse(CanonicalAuthorName.needsWork("Barney John Paul Johnson"));
    }

    @Test
    void leavesInitialsWithoutAnExpansion() {
        assertEquals("B. J. Johnson", CanonicalAuthorName.canonical("Johnson, B. J."));
        assertEquals("B. J.-P. Johnson", CanonicalAuthorName.canonical("B. J.-P. Johnson"));
        assertEquals("St. Thomas Aquinas", CanonicalAuthorName.canonical("St. Thomas Aquinas"));
        assertEquals("John-Paul II", CanonicalAuthorName.canonical("John-Paul II"));
        assertFalse(CanonicalAuthorName.needsWork("B. J. Johnson"));
        assertFalse(CanonicalAuthorName.needsWork("B. J.-P. Johnson"));
        assertFalse(CanonicalAuthorName.needsWork(null));
        assertFalse(CanonicalAuthorName.needsWork("  "));
    }
}
