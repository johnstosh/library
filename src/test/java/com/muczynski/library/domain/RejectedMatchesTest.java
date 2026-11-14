/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RejectedMatchesTest {

    @Test
    void grokipediaSlugMatchesTheGeneratedPageForHermannGeiger() {
        RejectedUrl rejected = new RejectedUrl(
                "https://grokipedia.com/page/hermann_geiger", "different person", LocalDate.of(2026, 11, 12), "librarian");

        assertTrue(RejectedMatches.rejectsUrl(List.of(rejected), "https://grokipedia.com/page/Hermann_Geiger"));
        assertTrue(RejectedMatches.rejectsUrl(List.of(rejected), "https://grokipedia.com/page/hermann_geiger/"));
        assertTrue(RejectedMatches.rejectsUrl(
                List.of(new RejectedUrl("hermann_geiger", "different person", null, null)),
                "https://grokipedia.com/page/Hermann_Geiger"));
        assertFalse(RejectedMatches.rejectsUrl(List.of(rejected), "https://grokipedia.com/page/Other_Person"));
    }

    @Test
    void freeTextAndAudioRejectionsStaySeparate() {
        RejectedUrl text = new RejectedUrl("https://gutenberg.org/1/", "wrong edition", null, null);
        RejectedUrl audio = new RejectedUrl("https://librivox.org/city-of-god", "wrong reading", null, null);
        String found = "https://gutenberg.org/1 https://archive.org/details/city https://librivox.org/city-of-god";

        assertEquals("https://archive.org/details/city https://librivox.org/city-of-god",
                RejectedMatches.withoutRejectedFreeText(found, List.of(text), List.of(), false));
        assertEquals("https://gutenberg.org/1 https://archive.org/details/city",
                RejectedMatches.withoutRejectedFreeText(found, List.of(), List.of(audio), false));
        assertNull(RejectedMatches.withoutRejectedFreeText(
                "https://librivox.org/city-of-god", List.of(), List.of(audio), true));
    }

    @Test
    void holdingsMatchIgnoresKeyCase() {
        RejectedHolding row = new RejectedHolding(
                PublicLibrary.EMU, "alma991", null, "different book", null, null);
        assertTrue(RejectedMatches.rejectsHolding(List.of(row), PublicLibrary.EMU, "ALMA991"));
        assertFalse(RejectedMatches.rejectsHolding(List.of(row), PublicLibrary.YDL, "alma991"));
        assertFalse(RejectedMatches.rejectsHolding(List.of(row), PublicLibrary.EMU, ""));
    }

    @Test
    void secondAddUpdatesReasonAndKeepsAnUnchangedRow() {
        RejectedUrl stored = new RejectedUrl(
                "https://example.com/a", "dead link", LocalDate.of(2026, 1, 2), "ada");
        List<RejectedUrl> edited = RejectedMatches.acceptEditedUrls(List.of(
                new RejectedUrl("https://example.com/a/", "dead link", LocalDate.of(2026, 11, 1), "mallory"),
                new RejectedUrl("https://example.com/a", "wrong edition", null, "mallory"),
                new RejectedUrl("  ", "ignored", null, null)
        ), List.of(stored), "librarian");

        assertEquals(1, edited.size());
        assertEquals("wrong edition", edited.get(0).getReason());
        assertEquals("librarian", edited.get(0).getBy());
        assertEquals(LocalDate.now(java.time.ZoneOffset.UTC), edited.get(0).getDate());
    }

    @Test
    void importKeepsExportedAttributionAndEmptyListClears() {
        List<RejectedUrl> imported = RejectedMatches.acceptImportedUrls(List.of(
                new RejectedUrl("https://example.com/a", "dead link", LocalDate.of(2026, 1, 2), "ada")));
        assertEquals("ada", imported.get(0).getBy());
        assertEquals(LocalDate.of(2026, 1, 2), imported.get(0).getDate());
        assertTrue(RejectedMatches.acceptImportedUrls(List.of()).isEmpty());
        assertNull(RejectedMatches.acceptImportedUrls(null));
    }

    @Test
    void unionKeepsTheKeeperEntry() {
        RejectedUrl keeper = new RejectedUrl("https://example.com/a", "keeper", null, "ada");
        RejectedUrl source = new RejectedUrl("https://example.com/a/", "source", null, "bea");
        RejectedUrl extra = new RejectedUrl("https://example.com/b", "source", null, "bea");
        List<RejectedUrl> union = RejectedMatches.unionUrls(List.of(keeper), List.of(source, extra));
        assertEquals(2, union.size());
        assertEquals("keeper", union.get(0).getReason());
        assertEquals("https://example.com/b", union.get(1).getUrl());
    }

    @Test
    void blankHoldingKeyIsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                RejectedMatches.acceptEditedHoldings(List.of(new RejectedHolding(
                        PublicLibrary.YDL, " ", null, "wrong book", null, null)), List.of(), "librarian"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
