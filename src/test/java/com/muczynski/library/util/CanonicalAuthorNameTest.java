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
        assertEquals("Conrad De Meester OCD", CanonicalAuthorName.canonical("Conrad De Meester, OCD"));
        assertEquals("Michael E. Gaitley MIC", CanonicalAuthorName.canonical("Michael E. Gaitley, MIC"));
        assertEquals("Fr. Ignatius of the Side of Jesus CP",
                CanonicalAuthorName.canonical("Fr. Ignatius of the Side of Jesus, CP"));
        assertEquals("Edward Leen CSSp", CanonicalAuthorName.canonical("Edward Leen, CSSp"));
        assertEquals("Fr. Frederick Schmit O.Praem.",
                CanonicalAuthorName.canonical("Fr. Frederick Schmit, O.Praem."));
        assertEquals("John Henry Newman D.D.", CanonicalAuthorName.canonical("JOHN HENRY NEWMAN, D.D."));
        assertEquals("Raoul Plus S. J.", CanonicalAuthorName.canonical("Plus, Raoul, S. J."));
        assertEquals("C. L White", CanonicalAuthorName.canonical("White, C. L"));
        assertEquals("T. E Bridgett", CanonicalAuthorName.canonical("Bridgett, T. E"));
        assertEquals("Joseph Keller", CanonicalAuthorName.canonical("Keller, Joseph"));
        assertEquals("Japan Travel Bureau Inc", CanonicalAuthorName.canonical("Japan Travel Bureau, Inc"));
        assertEquals("Acme LLC", CanonicalAuthorName.canonical("Acme, LLC"));
        assertEquals("Acme L.L.C.", CanonicalAuthorName.canonical("Acme, L.L.C."));
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

    @Test
    void stripsApproximateYearsAndDoesNotMoveThemForward() {
        assertEquals("Peter Riga", CanonicalAuthorName.canonical("Peter Riga, approximately 1140-1209"));
        assertEquals("Peter Riga", CanonicalAuthorName.canonical("approximately 1140-1209 Peter Riga"));
        assertEquals("Peter Riga", CanonicalAuthorName.canonical("Riga, Peter, approximately 1140-1209"));
        assertEquals("Peter Riga", CanonicalAuthorName.canonical("Peter Riga, approx. 1140-1209"));
        assertEquals("Peter Riga", CanonicalAuthorName.canonical("PETER RIGA, APPROXIMATELY 1140-1209"));
        assertFalse(CanonicalAuthorName.needsWork("Peter Riga"));
    }

    @Test
    void replacesTwoLetterInitialsAndDropsRedundantInitials() {
        assertEquals("Louis Claude Fillion", CanonicalAuthorName.canonical("Fillion, L.-Cl. (Louis-Claude)"));
        assertEquals("William Stang", CanonicalAuthorName.canonical("Stang, Wm. (William)"));
        assertEquals("Cyril Charles Martindale",
                CanonicalAuthorName.canonical("Martindale, Cyril Charles (C. C.)"));
        assertEquals("Cyril Charles Martindale",
                CanonicalAuthorName.canonical("Martindale, Cyril Charles (C.C.)"));
        assertEquals("John Smith (D.D.)", CanonicalAuthorName.canonical("John Smith (D.D.)"));
        assertEquals("John Smith (S.J.)", CanonicalAuthorName.canonical("John Smith (S.J.)"));
        assertEquals("John Smith (S. J.)", CanonicalAuthorName.canonical("John Smith (S. J.)"));
        assertEquals("St. Edith Stein (Teresa Benedicta of the Cross)",
                CanonicalAuthorName.canonical("St. Edith Stein (Teresa Benedicta of the Cross)"));
    }

    @Test
    void removesOldCatalogNotes() {
        assertEquals("John Smith", CanonicalAuthorName.canonical("John Smith [from old catalog]"));
        assertEquals("John Smith", CanonicalAuthorName.canonical("Smith, John [from old catalog]"));
        assertEquals("John Smith", CanonicalAuthorName.canonical("John Smith [From Old Catalog]"));
        assertEquals("Peter Riga",
                CanonicalAuthorName.canonical("Peter Riga, approximately 1140-1209 [from old catalog]"));
        assertTrue(CanonicalAuthorName.needsWork("John Smith [from old catalog]"));
    }

    @Test
    void normalizesAllUpperAndAllLowerNamesToInitialCapitals() {
        String sister = "Sister Mary Antonia, B.V.M. Ph, D and Rev. Thomas J. Shahan D.D.";
        assertEquals(sister, CanonicalAuthorName.canonical(
                "sister mary antonia, b.v.m. ph, d and rev. thomas j. shahan d.d."));
        assertEquals(sister, CanonicalAuthorName.canonical(
                "sister mary antonia, b.v.m. ph,d and rev. thomas j. shahan d.d."));
        String keppler = "Rt. Rev. Paul William von Keppler D.D.";
        assertEquals(keppler, CanonicalAuthorName.canonical("RT. REV. PAUL WILLIAM VON KEPPLER D.D."));
        assertEquals(keppler, CanonicalAuthorName.canonical("RT. REV. PAUL WILLIAM VON KEPPLER, D.D."));
        assertEquals("John Henry Newman D.D.", CanonicalAuthorName.canonical("JOHN HENRY NEWMAN D.D."));
        assertEquals("Luis de la Puente", CanonicalAuthorName.canonical("LUIS DE LA PUENTE"));
        assertEquals("Claude La Colombière", CanonicalAuthorName.canonical("CLAUDE LA COLOMBIÈRE"));
        assertEquals("Jeanne d'Arc", CanonicalAuthorName.canonical("JEANNE D'ARC"));
        assertEquals("John McDonald", CanonicalAuthorName.canonical("JOHN MCDONALD"));
        assertEquals("John O'Brien", CanonicalAuthorName.canonical("JOHN O'BRIEN"));
        assertEquals("Venerable Louis of Granada OP",
                CanonicalAuthorName.canonical("VENERABLE LOUIS OF GRANADA, OP"));
        assertEquals("Louis XIV", CanonicalAuthorName.canonical("LOUIS XIV"));
        assertFalse(CanonicalAuthorName.needsWork(keppler));
        assertFalse(CanonicalAuthorName.needsWork("John Henry Newman D.D."));
        assertTrue(CanonicalAuthorName.needsWork("RT. REV. PAUL WILLIAM VON KEPPLER D.D."));
        assertTrue(CanonicalAuthorName.needsWork("sister mary antonia"));
    }

    @Test
    void expandsLongerDottedAbbreviations() {
        assertEquals("Johannes Evangelist Belser",
                CanonicalAuthorName.canonical("Belser, Joh. Evang. (Johannes Evangelist)"));
    }

    @Test
    void repairsUtf8ReadAsLatin1() {
        assertEquals("Edward Francis Garesché",
                CanonicalAuthorName.canonical("Garesch\u00C3\u00A9, Edward F. (Edward Francis)"));
        assertEquals("Jakob Grönings", CanonicalAuthorName.canonical("Gr\u00C3\u00B6nings, Jakob"));
        assertEquals("Joachim Trotti de La Chétardie",
                CanonicalAuthorName.canonical("Trotti de La Ch\u00C3\u00A9tardie, Joachim]"));
        assertEquals("François Xavier Schouppe",
                CanonicalAuthorName.canonical("Schouppe, F. X. (Fran\u00C3\u00A7ois Xavier)"));
        assertEquals("André Prévot", CanonicalAuthorName.canonical("Pr\u00C3\u00A9vot, Andr\u00C3\u00A9"));
        assertEquals("François Xavier Schouppe",
                CanonicalAuthorName.canonical("Schouppe, F. X. (François Xavier)"));
    }

    @Test
    void keepsTheLongerFormOfOneRepeatedName() {
        assertEquals("Joseph Adam Gustav Hergenröther", CanonicalAuthorName.canonical(
                "Joseph Hergenröther , Joseph Adam Gustav Hergenröther"));
        assertEquals("Gabriel José García Márquez",
                CanonicalAuthorName.canonical("García Márquez, Gabriel José"));
    }

    @Test
    void keepsThomasAKempisInReadingOrder() {
        assertEquals("Thomas à Kempis", CanonicalAuthorName.canonical("Thomas, à Kempis"));
        assertEquals("Thomas à Kempis", CanonicalAuthorName.canonical("Thomas, a\u0300 Kempis"));
        assertEquals("Thomas à Kempis", CanonicalAuthorName.canonical("à Kempis Thomas"));
        assertEquals("Thomas à Kempis", CanonicalAuthorName.canonical("a\u0300 Kempis Thomas"));
        assertEquals("Thomas à Kempis", CanonicalAuthorName.canonical("Thomas à Kempis"));
        assertFalse(CanonicalAuthorName.needsWork("Thomas à Kempis"));
    }

    @Test
    void doesNotMergeScaramelliWithManning() {
        assertEquals("John Baptist Scaramelli, Rev. Cardinal Archbishop Manning",
                CanonicalAuthorName.canonical(
                        "john baptist scaramelli, rev. cardinal archbishop manning"));
        assertEquals("Sister Maria Paula", CanonicalAuthorName.canonical("Maria Paula, Sister"));
        assertEquals("Pope Gregory I", CanonicalAuthorName.canonical("Gregory I, Pope, ca. 540-604"));
    }

    @Test
    void stripsTranslatorRelator() {
        assertEquals("Georgiana Fullerton", CanonicalAuthorName.canonical("FULLERTON, GEORGIANA TR."));
        assertEquals("Georgiana Fullerton", CanonicalAuthorName.canonical("GEORGIANA TR. FULLERTON"));
        assertEquals("John Troy", CanonicalAuthorName.canonical("John Troy"));
    }
}
