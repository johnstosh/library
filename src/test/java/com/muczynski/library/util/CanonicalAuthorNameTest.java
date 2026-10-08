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

    @Test
    void leavesACommaInsideAPhrase() {
        String community = "Sisters of Charity of Our Lady, Mother of the Church";
        assertEquals(community, CanonicalAuthorName.canonical(community));
        assertFalse(CanonicalAuthorName.needsWork(community));
        assertEquals("Ignatius, of Loyola", CanonicalAuthorName.canonical("Ignatius, of Loyola"));
        assertEquals("Bernard, of Clairvaux, Saint",
                CanonicalAuthorName.canonical("Bernard, of Clairvaux, Saint"));
        assertEquals("Luis de la Puente", CanonicalAuthorName.canonical("Puente, Luis de la"));
        assertEquals("Venerable Louis of Granada OP",
                CanonicalAuthorName.canonical("Venerable Louis of Granada, OP"));
        assertEquals("John E. Rotelle O.S.A.",
                CanonicalAuthorName.canonical("John E. Rotelle, O.S.A. (ed.)"));
        assertEquals("Benedicta Ward SLG",
                CanonicalAuthorName.canonical("Benedicta Ward, SLG (translator; Desert Fathers)"));
    }

    @Test
    void removesEditorAndTranslatorCredits() {
        assertEquals("Christoph Cardinal Schönborn",
                CanonicalAuthorName.canonical("Christoph Cardinal Schönborn (editor)"));
        assertEquals("St. Francis de Sales",
                CanonicalAuthorName.canonical("St. Francis de Sales (ed. John Kirvan)"));
        assertEquals("Francis Aidan Gasquet",
                CanonicalAuthorName.canonical("Francis Aidan Gasquet (ed.)"));
        assertEquals("Edward G. Bagshawe",
                CanonicalAuthorName.canonical("Edward G. Bagshawe (tr.)"));
        assertEquals("Louis Lallemant SJ",
                CanonicalAuthorName.canonical("Louis Lallemant, SJ (ed. Rigoleuc / Champion; Faber English)"));
        assertEquals("Félix Martin S.J.",
                CanonicalAuthorName.canonical("Félix Martin, S.J.; translated by John Gilmary Shea"));
        assertEquals("Enid Maud Dinnis",
                CanonicalAuthorName.canonical("Dinnis, Enid Maud, editor"));
        assertEquals("John L. Stoddard",
                CanonicalAuthorName.canonical("John L. Stoddard, editor"));
        assertEquals("Bourdaloue & Massillon",
                CanonicalAuthorName.canonical("Bourdaloue & Massillon. Edited by D. O'Mahony Bossuet"));
        assertEquals("Desert Fathers (Verba Seniorum)",
                CanonicalAuthorName.canonical("Desert Fathers (Verba Seniorum; tr. Richard J. Goodrich)"));
        assertEquals("James Socias; Midwest Theological Forum",
                CanonicalAuthorName.canonical("James Socias (editor); Midwest Theological Forum"));
        assertEquals("Pierre de Bérulle et al.",
                CanonicalAuthorName.canonical("Pierre de Bérulle et al. (ed. William M. Thompson)"));
        assertEquals("Edith Martha Almedingen",
                CanonicalAuthorName.canonical("Almedingen, E. M. (Edith Martha)"));
        assertEquals("St. Edith Stein (Teresa Benedicta of the Cross)",
                CanonicalAuthorName.canonical("St. Edith Stein (Teresa Benedicta of the Cross)"));
        assertEquals("Catholic Church (Benziger ed.)",
                CanonicalAuthorName.canonical("Catholic Church (Benziger ed.)"));
        assertEquals("Reader's Digest Editors",
                CanonicalAuthorName.canonical("Reader's Digest Editors"));
        assertEquals("Editors of Fine Homebuilding",
                CanonicalAuthorName.canonical("Editors of Fine Homebuilding"));
        assertEquals("Libreria Editrice Vaticana (English edition)",
                CanonicalAuthorName.canonical("Libreria Editrice Vaticana (English edition)"));
        assertTrue(CanonicalAuthorName.needsWork("Francis Aidan Gasquet (ed.)"));
        assertFalse(CanonicalAuthorName.needsWork("Francis Aidan Gasquet"));
    }
}
