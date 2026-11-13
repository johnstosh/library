/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto.importdtos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.PublicLibrary;
import com.muczynski.library.domain.RejectedHolding;
import com.muczynski.library.domain.RejectedUrl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportRejectedMatchesJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void exportOmitsEmptyListsAndImportOfAMissingFieldStaysNull() throws Exception {
        ImportBookDto empty = new ImportBookDto();
        empty.setTitle("Confessions");
        String exported = mapper.writeValueAsString(empty);
        assertFalse(exported.contains("invalidGrokipediaUrls"));
        assertFalse(exported.contains("invalidHoldings"));

        ImportBookDto oldExport = mapper.readValue("{\"title\":\"Confessions\"}", ImportBookDto.class);
        assertNull(oldExport.getInvalidGrokipediaUrls());
        assertNull(oldExport.getInvalidFreeTextUrls());
        assertNull(oldExport.getInvalidHoldings());

        ImportBookDto cleared = mapper.readValue(
                "{\"title\":\"Confessions\",\"invalidGrokipediaUrls\":[]}", ImportBookDto.class);
        assertTrue(cleared.getInvalidGrokipediaUrls().isEmpty());
    }

    @Test
    void exportWritesARejection() throws Exception {
        ImportBookDto book = new ImportBookDto();
        book.setTitle("Confessions");
        book.setInvalidGrokipediaUrls(List.of(new RejectedUrl(
                "https://grokipedia.com/page/Other", "different book", LocalDate.of(2026, 11, 12), "ada")));
        book.setInvalidHoldings(List.of(new RejectedHolding(
                PublicLibrary.YDL, "e71f", null, "wrong book", LocalDate.of(2026, 11, 12), "ada")));
        String exported = mapper.writeValueAsString(book);
        assertTrue(exported.contains("https://grokipedia.com/page/Other"));
        assertTrue(exported.contains("e71f"));

        ImportAuthorDto author = new ImportAuthorDto();
        author.setName("Hermann Geiger");
        String authorExport = mapper.writeValueAsString(author);
        assertFalse(authorExport.contains("invalidGrokipediaUrls"));
    }
}
