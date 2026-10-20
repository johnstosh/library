/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.muczynski.library.service.duplicate.DuplicateTitlePlans.AuthorMergePlan;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.AuthorSide;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.TitlePlan;
import com.muczynski.library.service.duplicate.DuplicateTitlePlans.TitleSide;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateTitlePlansTest {

    @Test
    void middleNameKeepsTheAuthorThatAlreadyHasTheFullerName() {
        AuthorMergePlan plan = DuplicateTitlePlans.authorMerge(
                new AuthorSide(2, "John Smith", List.of()),
                new AuthorSide(9, "John Paul Smith", List.of()));

        assertTrue(plan.available());
        assertEquals(9L, plan.keeperId());
        assertEquals(2L, plan.sourceId());
        assertEquals("John Paul Smith", plan.survivingName());
        assertEquals(List.of("John Smith"), plan.alternateNames());
        assertTrue(plan.summary().contains("\"John Smith\" is stored as an alternate name."));
    }

    @Test
    void caseInsensitiveAuthorKeepsCapitalizationAndDoesNotStoreTheOtherCase() {
        AuthorMergePlan plan = DuplicateTitlePlans.authorMerge(
                new AuthorSide(1, "Augustine", List.of()),
                new AuthorSide(4, "augustine", List.of()));

        assertEquals(1L, plan.keeperId());
        assertEquals("Augustine", plan.survivingName());
        assertTrue(plan.alternateNames().isEmpty());
        assertTrue(plan.summary().contains("Author names match case-insensitively."));
        assertTrue(plan.summary().contains("The kept capitalization is \"Augustine\"."));
        assertTrue(plan.summary().contains("The other capitalization is not stored as an alternate name."));
    }

    @Test
    void oneLetterAuthorDifferenceKeepsTheLowerIdSpelling() {
        AuthorMergePlan plan = DuplicateTitlePlans.authorMerge(
                new AuthorSide(3, "Augustin", List.of()),
                new AuthorSide(8, "Augustine", List.of()));

        assertEquals(3L, plan.keeperId());
        assertEquals("Augustin", plan.survivingName());
        assertEquals(List.of("Augustine"), plan.alternateNames());
        assertTrue(plan.summary().contains("The longer spelling is not chosen."));
    }

    @Test
    void bothCallNumbersUseFixTitlesAndDoNotMerge() {
        TitlePlan plan = DuplicateTitlePlans.titles(
                new TitleSide(1, "Confessions", null, "BR65", false, null),
                new TitleSide(2, "Confession", null, "BR65.A", false, "https://example.com/a"));

        assertEquals(DuplicateTitlePlans.FIX_TITLES, plan.actionKind());
        assertTrue(plan.changeNeeded());
        assertNull(plan.keeperId());
        assertEquals("Confessions", plan.leftTitle());
        assertEquals("Confession, c. 2", plan.rightTitle());
        assertEquals("Confession", plan.rightAlternate());
        assertTrue(plan.summary().contains("does not delete either book"));
        assertTrue(plan.summary().contains("BR65"));
        assertTrue(plan.summary().contains("BR65.A"));
    }

    @Test
    void oneCallNumberKeepsThatBookAndCombinesUrls() {
        TitlePlan plan = DuplicateTitlePlans.titles(
                new TitleSide(5, "Confessions", null, null, true, "https://electronic.example/b"),
                new TitleSide(2, "Confession", null, "BR65", false, "https://shelf.example/a"));

        assertEquals(DuplicateTitlePlans.MERGE_TITLES, plan.actionKind());
        assertEquals(2L, plan.keeperId());
        assertEquals(5L, plan.deletedId());
        assertEquals("https://shelf.example/a https://electronic.example/b", plan.keeperFreeTextUrl());
        assertTrue(plan.keeperElectronic());
        assertTrue(plan.summary().contains("Call number \"BR65\" stays on book #2."));
        assertTrue(plan.summary().contains("is marked as an electronic resource."));
        assertFalse(plan.summary().contains("BR65 https"));
    }

    @Test
    void caseOnlyTitlesShareTheLowerIdCapitalizationOnFix() {
        TitlePlan plan = DuplicateTitlePlans.titles(
                new TitleSide(4, "confessions", null, "BX", false, null),
                new TitleSide(1, "Confessions", null, "BR", false, null));

        assertEquals(DuplicateTitlePlans.FIX_TITLES, plan.actionKind());
        assertEquals("Confessions", plan.rightTitle());
        assertEquals("Confessions, c. 2", plan.leftTitle());
    }
}
