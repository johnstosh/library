/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChicagoTitleCaseTest {

    private static final String DOMINIC = "The History of St. Dominic: Founder of the Friars Preachers";

    @Test
    void rewritesLowerUpperRdaAndEveryWordCapsIncludingSubtitle() {
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(
                "the history of st. dominic: founder of the friars preachers"));
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(
                "THE HISTORY OF ST. DOMINIC: FOUNDER OF THE FRIARS PREACHERS"));
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(
                "The history of St. Dominic : founder of the Friars Preachers"));
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(
                "The History Of St. Dominic: Founder Of The Friars Preachers"));
        assertEquals(DOMINIC, ChicagoTitleCase.toChicago(DOMINIC));
        assertFalse(ChicagoTitleCase.needsWork(DOMINIC));
        assertTrue(ChicagoTitleCase.needsWork("THE HISTORY OF ST. DOMINIC: FOUNDER OF THE FRIARS PREACHERS"));
    }

    @Test
    void capitalizesFirstWordOfSubtitleAndLowersShortPrepositions() {
        assertEquals("Of Mice and Men", ChicagoTitleCase.toChicago("of mice and men"));
        assertEquals("Gone with the Wind", ChicagoTitleCase.toChicago("gone with the wind"));
        assertEquals("Foo: The Bar", ChicagoTitleCase.toChicago("foo: the bar"));
        assertEquals("The End", ChicagoTitleCase.toChicago("the end"));
        assertEquals("A Tale of Two Cities", ChicagoTitleCase.toChicago("a tale of two cities"));
        assertEquals("World War II", ChicagoTitleCase.toChicago("world war ii"));
        assertEquals("The Mix of Things", ChicagoTitleCase.toChicago("the mix of things"));
        assertEquals("O'Brien's Book", ChicagoTitleCase.toChicago("o'brien's book"));
        assertEquals("The State-of-the-Art Guide", ChicagoTitleCase.toChicago("the state-of-the-art guide"));
    }

    @Test
    void capitalizesTheWordAfterASentenceBreak() {
        String council = "The Seventh Ecumenical Council. The Second Council of Nice";
        assertEquals(council, ChicagoTitleCase.toChicago(council));
        assertEquals(council, ChicagoTitleCase.toChicago(
                "The Seventh Ecumenical Council. the Second Council of Nice"));
        assertEquals(council, ChicagoTitleCase.toChicago(
                "the seventh ecumenical council. the second council of nice"));
        assertFalse(ChicagoTitleCase.needsWork(council));
        assertTrue(ChicagoTitleCase.needsWork(
                "The Seventh Ecumenical Council. the Second Council of Nice"));
        assertEquals("The End. Of Mice and Men", ChicagoTitleCase.toChicago("the end. of mice and men"));
        assertEquals("The World We Live In. A History",
                ChicagoTitleCase.toChicago("the world we live in. a history"));
        assertEquals("What Is It? The Answer", ChicagoTitleCase.toChicago("what is it? the answer"));
        assertEquals("Stop! The Book", ChicagoTitleCase.toChicago("stop! the book"));
        assertEquals("Wait... The End", ChicagoTitleCase.toChicago("wait... the end"));
        assertEquals("Done. Next-to Last", ChicagoTitleCase.toChicago("done. next-to last"));
    }

    @Test
    void leavesAbbreviationsAndInitialsLowerThanANewSentence() {
        assertEquals("Mr. and Mrs. Smith", ChicagoTitleCase.toChicago("mr. and mrs. smith"));
        assertEquals("Body, Etc. From Original Mss.",
                ChicagoTitleCase.toChicago("body, etc. From original mss."));
        assertEquals("Pass from Me,' Etc., and Against",
                ChicagoTitleCase.toChicago("pass from me,' etc., and against"));
        assertEquals("J. R. R. Tolkien", ChicagoTitleCase.toChicago("j. r. r. tolkien"));
        assertEquals("U.S. History", ChicagoTitleCase.toChicago("u.s. history"));
        assertEquals("Volume II. The Council", ChicagoTitleCase.toChicago("volume ii. the council"));
        assertEquals("World War II.", ChicagoTitleCase.toChicago("world war ii."));
        assertEquals("I. The Mystical Explanation",
                ChicagoTitleCase.toChicago("i. the mystical explanation"));
        assertEquals("Life of Mrs. Eliza A. Seton",
                ChicagoTitleCase.toChicago("life of mrs. eliza a. seton"));
    }

    @Test
    void keepsCatalogShapesThatChicagoWouldOtherwiseFlatten() {
        assertEquals("The Soul's Journey into God; The Tree of Life; The Life of St. Francis",
                ChicagoTitleCase.toChicago(
                        "The Soul's Journey into God; The Tree of Life; The Life of St. Francis"));
        assertEquals("Fabiola; Or, The Church of the Catacombs",
                ChicagoTitleCase.toChicago("fabiola; or, the church of the catacombs"));
        assertEquals("All for Jesus: Or, The Easy Ways of Divine Love",
                ChicagoTitleCase.toChicago("all for jesus: or, the easy ways of divine love"));
        assertEquals("Sophocles II: Ajax, the Women of Trachis, Electra, Philoctetes",
                ChicagoTitleCase.toChicago(
                        "Sophocles II: Ajax, The Women of Trachis, Electra, Philoctetes"));
        assertEquals("The Founding of Christendom, a History of Christendom Vol. 1",
                ChicagoTitleCase.toChicago(
                        "the founding of christendom, a history of christendom vol. 1"));
        assertEquals("St. Martin de Porres", ChicagoTitleCase.toChicago("st. martin de porres"));
        assertEquals("Francis de Sales", ChicagoTitleCase.toChicago("francis de sales"));
        assertEquals("Saint John Baptist de la Salle",
                ChicagoTitleCase.toChicago("saint john baptist de la salle"));
        assertEquals("Claude La Colombière", ChicagoTitleCase.toChicago("claude la colombière"));
        assertEquals("Ricordo di Roma", ChicagoTitleCase.toChicago("ricordo di roma"));
        assertEquals("John Paul II's Theology", ChicagoTitleCase.toChicago("john paul ii's theology"));
        assertEquals("The Rosary: The Great Weapon of the 21st Century",
                ChicagoTitleCase.toChicago("the rosary: the great weapon of the 21st century"));
        assertEquals("Blessed Miguel Pro: 20th-Century Mexican Martyr",
                ChicagoTitleCase.toChicago("blessed miguel pro: 20th-century mexican martyr"));
        assertEquals("SQL Pocket Guide", ChicagoTitleCase.toChicago("SQL Pocket Guide"));
        assertEquals("Head First HTML with CSS & XHTML",
                ChicagoTitleCase.toChicago("Head First HTML with CSS & XHTML"));
        assertEquals("YOUCAT: Youth Catechism of the Catholic Church",
                ChicagoTitleCase.toChicago("YOUCAT: Youth Catechism of the Catholic Church"));
        assertEquals("The Kanji ABC", ChicagoTitleCase.toChicago("The Kanji ABC"));
        assertEquals("The Miracle of Our Lady of Fatima (DVD)",
                ChicagoTitleCase.toChicago("The Miracle of Our Lady of Fatima (DVD)"));
        assertEquals("Mr. McFadden's Hallowe'en", ChicagoTitleCase.toChicago("mr. mcfadden's hallowe'en"));
        assertEquals("Mother Mary Catherine McAuley",
                ChicagoTitleCase.toChicago("mother mary catherine mcauley"));
        assertEquals("The Story of Saint Jeanne d'Arc",
                ChicagoTitleCase.toChicago("the story of saint jeanne d'arc"));
        assertEquals("The Passion of SS. Perpetua and Felicity, MM",
                ChicagoTitleCase.toChicago("the passion of ss. perpetua and felicity, mm"));
        assertEquals("John N. Neumann, D.D., Fourth Bishop",
                ChicagoTitleCase.toChicago("john n. neumann, d.d., fourth bishop"));
        assertEquals("Father Chaignon, S.J., Volume 1",
                ChicagoTitleCase.toChicago("father chaignon, s.j., volume 1"));
        assertEquals(
                "The Story of Thomas More / Weddings in the Family / The Road to Damascus / From an Altar Screen",
                ChicagoTitleCase.toChicago(
                        "the story of thomas more / weddings in the family / the road to damascus / from an altar screen"));
        assertEquals("Men & Women Are from Eden: A Study Guide to John Paul II's Theology of the Body",
                ChicagoTitleCase.toChicago(
                        "Men & Women Are From Eden: A Study Guide to John Paul II's Theology of the Body"));
        assertEquals("The Lion, the Witch and the Wardrobe",
                ChicagoTitleCase.toChicago("The Lion, the Witch and the Wardrobe"));
        assertEquals("Life of Father Damien, the Apostle of the Lepers",
                ChicagoTitleCase.toChicago("life of father damien, the apostle of the lepers"));
        assertEquals("In the 16th, 17th and 18th Centuries",
                ChicagoTitleCase.toChicago("in the 16th, 17th and 18th centuries"));
        assertEquals("De Trinitate (On the Trinity)",
                ChicagoTitleCase.toChicago("De Trinitate (On the Trinity)"));
        assertEquals("Spy × Family, v. 3", ChicagoTitleCase.toChicago("Spy × Family, v. 3"));
        assertEquals("The GIFTionary", ChicagoTitleCase.toChicago("The GIFTionary"));
    }

    @Test
    void keepsCatalogCopySuffix() {
        assertEquals("The Lord of the Rings, c. 2",
                ChicagoTitleCase.toChicago("the lord of the rings, c. 2"));
        assertEquals("The Lord of the Rings, c. 2",
                ChicagoTitleCase.toChicago("THE LORD OF THE RINGS, C. 2"));
        assertFalse(ChicagoTitleCase.needsWork("The Lord of the Rings, c. 2"));
        assertTrue(ChicagoTitleCase.needsWork("The Lord of the Rings, c.2"));
    }

    @Test
    void leavesBlankTitlesAlone() {
        assertFalse(ChicagoTitleCase.needsWork(null));
        assertFalse(ChicagoTitleCase.needsWork("  "));
        assertEquals("  ", ChicagoTitleCase.toChicago("  "));
    }
}
