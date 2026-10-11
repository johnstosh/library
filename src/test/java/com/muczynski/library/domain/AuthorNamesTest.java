/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthorNamesTest {

    @Test
    void normalize_dropsBlanksCanonicalRepeatsAndDuplicates() {
        List<String> names = AuthorNames.normalize(
                "Augustine of Hippo",
                List.of("  ", "Augustine of Hippo", "Saint Augustine", "saint augustine", "Aurelius\nAugustinus"));

        assertEquals(List.of("Saint Augustine", "Aurelius", "Augustinus"), names);
    }

    @Test
    void normalize_returnsNullWhenNothingRemains() {
        assertNull(AuthorNames.normalize("Augustine", List.of("augustine", "  ")));
        assertNull(AuthorNames.normalize("Augustine", null));
    }

    @Test
    void lookupLastNames_usesCanonicalThenEachAlternate() {
        Author author = new Author();
        author.setName("Aurelius Augustinus");
        author.setAlternateNames(List.of("Saint Augustine", "Augustine of Hippo"));

        assertEquals(List.of("Augustinus", "Augustine", "Hippo"), AuthorNames.lookupLastNames(author));
        assertEquals(
                List.of("Aurelius Augustinus", "Saint Augustine", "Augustine of Hippo"),
                AuthorNames.lookupNames(author));
    }

    @Test
    void lookupLastNames_isNullWhenTheBookHasNoAuthor() {
        List<String> lastNames = AuthorNames.lookupLastNames(null);
        assertEquals(1, lastNames.size());
        assertNull(lastNames.get(0));
    }
}
