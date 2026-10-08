/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.Author;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthorMergerTest {

    @Test
    void fillsBlanksAndKeepsTheLongerText() {
        Author keeper = author("St. Alphonsus Liguori");
        keeper.setBiographicalEssay("Short");
        keeper.setReligiousAffiliation("Catholic priest and bishop");
        keeper.setNationality("   ");
        keeper.setGrokipediaUrl("-");
        keeper.setDateOfBirth(LocalDate.of(1696, 9, 27));
        keeper.setAlternateNames(new ArrayList<>(List.of("Alphonsus Liguori")));

        Author source = author("St. Alphonsus Liguori (ed. Frederick M. Jones, C.Ss.R.)");
        source.setBiographicalEssay("A longer biographical essay about the saint");
        source.setReligiousAffiliation("Priest");
        source.setNationality("Italian");
        source.setBirthCountry("Italy");
        source.setGrokipediaUrl("https://grokipedia.com/page/Alphonsus_Liguori");
        source.setDateOfBirth(LocalDate.of(1700, 1, 1));
        source.setDateOfDeath(LocalDate.of(1787, 8, 1));
        source.setAlternateNames(new ArrayList<>(List.of("alphonsus liguori", "Alfonso de Liguori")));

        AuthorMerger.mergeFields(keeper, source);

        assertEquals("St. Alphonsus Liguori", keeper.getName());
        assertEquals("A longer biographical essay about the saint", keeper.getBiographicalEssay());
        assertEquals("Catholic priest and bishop", keeper.getReligiousAffiliation());
        assertEquals("Italian", keeper.getNationality());
        assertEquals("Italy", keeper.getBirthCountry());
        assertEquals("https://grokipedia.com/page/Alphonsus_Liguori", keeper.getGrokipediaUrl());
        assertEquals(LocalDate.of(1696, 9, 27), keeper.getDateOfBirth());
        assertEquals(LocalDate.of(1787, 8, 1), keeper.getDateOfDeath());
        assertEquals(List.of("Alphonsus Liguori", "Alfonso de Liguori"), keeper.getAlternateNames());
    }

    @Test
    void keepsTheKeeperTextWhenLengthsMatch() {
        Author keeper = author("Acme");
        keeper.setBirthCountry("Spain");
        Author source = author("Acme, LLC");
        source.setBirthCountry("Italy");

        AuthorMerger.mergeFields(keeper, source);

        assertEquals("Spain", keeper.getBirthCountry());
        assertNull(keeper.getAlternateNames());
    }

    private static Author author(String name) {
        Author author = new Author();
        author.setName(name);
        return author;
    }
}
