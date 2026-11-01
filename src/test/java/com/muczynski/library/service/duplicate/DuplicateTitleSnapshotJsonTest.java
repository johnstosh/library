/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service.duplicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.service.duplicate.DuplicateTitleSnapshots.Payload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DuplicateTitleSnapshotJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsAnOldStringAndANewList() throws Exception {
        Payload oldSnapshot = mapper.readValue("""
                {"kind":"FIX_TITLES","fixBookBAlternate":"On Loving God","deletedBook":{"title":"Confession","alternateTitle":"Confessiones"}}
                """, Payload.class);
        assertEquals(List.of("On Loving God"), oldSnapshot.getFixBookBAlternate());
        assertEquals(List.of("Confessiones"), oldSnapshot.getDeletedBook().getAlternateTitles());

        Payload newSnapshot = mapper.readValue("""
                {"fixBookAAlternate":["On Loving God","De diligendo Deo"],"keeperBook":{"alternateTitles":["The Confessions"]}}
                """, Payload.class);
        assertEquals(List.of("On Loving God", "De diligendo Deo"), newSnapshot.getFixBookAAlternate());
        assertEquals(List.of("The Confessions"), newSnapshot.getKeeperBook().getAlternateTitles());

        Payload blank = mapper.readValue("{\"fixBookAAlternate\":\"   \"}", Payload.class);
        assertNull(blank.getFixBookAAlternate());
    }

    @Test
    void rejectsANonStringElement() {
        assertThrows(JsonProcessingException.class, () -> mapper.readValue(
                "{\"fixBookBAlternate\":[\"On Loving God\", 2]}", Payload.class));
        assertThrows(JsonProcessingException.class, () -> mapper.readValue(
                "{\"deletedBook\":{\"alternateTitles\":\"Confessiones\"}}", Payload.class));
    }
}
