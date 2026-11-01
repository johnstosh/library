/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookTitlesTest {

    @Test
    void normalizeDropsBlanksPrimaryRepeatsAndKeepsTheFirstSpelling() {
        assertNull(BookTitles.normalize("On the Love of God", null));
        assertNull(BookTitles.normalize("On the Love of God", List.of()));
        assertNull(BookTitles.normalize("On the Love of God", List.of("  ", "\n", "On the Love of God")));
        assertNull(BookTitles.normalize(
                "On the Love of God",
                List.of("on the love of god", "On the Love of God, c. 2")));

        assertEquals(
                List.of("Traité de l'amour de Dieu, c. 2"),
                BookTitles.normalize(
                        "On the Love of God",
                        List.of("Traité de l'amour de Dieu, c. 2", "traité de l'amour de dieu")));

        assertEquals(
                List.of("On Loving God", "De diligendo Deo"),
                BookTitles.normalize(
                        "Treatise on the Love of God",
                        List.of("On Loving God\nDe diligendo Deo", "  on loving god  ")));
    }
}
