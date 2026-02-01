/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RandomAuthorTest {

    @Test
    void testRandomAuthorCreation() {
        RandomAuthor randomAuthor = new RandomAuthor();
        Author author = randomAuthor.create();
        assertNotNull(author);
        assertNotNull(author.getName());
        assertFalse(author.getName().isEmpty());
        // Verify all populated fields
        assertNotNull(author.getReligiousAffiliation(), "Religious affiliation should be generated");
        assertTrue(author.getReligiousAffiliation().startsWith("test-data"),
                "Religious affiliation should be marked as test-data");
        assertNotNull(author.getBirthCountry(), "Birth country should be generated");
        assertNotNull(author.getNationality(), "Nationality should be generated");
        assertNotNull(author.getDateOfBirth(), "Date of birth should be generated");
        // Date of death is only set ~60% of the time (some authors are alive)
        assertNotNull(author.getBriefBiography(), "Biography should be generated");
        assertNotNull(author.getGrokipediaUrl(), "Grokipedia URL should be generated");
    }
}