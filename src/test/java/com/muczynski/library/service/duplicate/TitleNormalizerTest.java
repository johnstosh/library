/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.service.duplicate.TitleNormalizer.NormalizedTitle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TitleNormalizerTest {

    @Test
    void dropsStopWordsAndJoinsKeywords() {
        NormalizedTitle n = TitleNormalizer.normalize("The Confessions of St. Augustine");
        assertTrue(n.keywords().contains("confession"));
        assertTrue(n.keywords().contains("augustine"));
        assertFalse(n.keywords().contains("the"));
        assertFalse(n.keywords().contains("of"));
        assertFalse(n.keywords().contains("st"));
        assertEquals("confession augustine", n.normalizedString());
    }

    @Test
    void removesPunctuationAndCollapsesWhitespace() {
        NormalizedTitle n = TitleNormalizer.normalize("Pride  &  Prejudice!!!");
        assertEquals("pride prejudice", n.normalizedString());
    }

    @Test
    void truncatesSubtitleAtColon() {
        assertEquals("Summa Theologica", TitleNormalizer.truncateSubtitle("Summa Theologica: Part I"));
        NormalizedTitle n = TitleNormalizer.normalize("City of God: A Treatise on...");
        assertTrue(n.keywords().contains("city"));
        assertTrue(n.keywords().contains("god"));
        assertFalse(n.keywords().contains("treatise"));
    }

    @Test
    void truncatesAtSemicolonEmDashAndSpacedHyphen() {
        assertEquals("Title", TitleNormalizer.truncateSubtitle("Title; subtitle"));
        assertEquals("Title", TitleNormalizer.truncateSubtitle("Title—subtitle"));
        assertEquals("Title", TitleNormalizer.truncateSubtitle("Title - subtitle"));
        assertEquals("Title", TitleNormalizer.truncateSubtitle("Title (extra)"));
    }

    @Test
    void doesNotTruncateBareHyphenInsideWords() {
        // "well-known" should not be cut at the hyphen mid-token path via subtitle cut
        assertEquals("A well-known book", TitleNormalizer.truncateSubtitle("A well-known book"));
    }

    @Test
    void stripsCopySuffixBeforeNormalizing() {
        NormalizedTitle n = TitleNormalizer.normalize("Gather Comprehensive, c. 2");
        assertEquals("gather comprehensive", n.normalizedString());
    }

    @Test
    void lightStemRemovesTrailingSAndIng() {
        assertEquals("confession", TitleNormalizer.lightStem("confessions"));
        assertEquals("read", TitleNormalizer.lightStem("reading"));
        assertEquals("walk", TitleNormalizer.lightStem("walked"));
        assertEquals("butler", TitleNormalizer.lightStem("butler's"));
    }

    @Test
    void blankAndNullYieldEmpty() {
        assertTrue(TitleNormalizer.normalize(null).isEmpty());
        assertTrue(TitleNormalizer.normalize("   ").isEmpty());
        assertTrue(TitleNormalizer.normalize("The of a").isEmpty());
    }
}
