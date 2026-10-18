/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.testsupport.RequiresOpenNetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternetArchiveProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private InternetArchiveProvider provider;

    /** Answer the advanced search with this JSON, deserialized into the provider's own response type. */
    private void respondWith(String json) {
        when(restTemplate.getForObject(anyString(), any(Class.class))).thenAnswer(inv ->
                new ObjectMapper().readValue(json, (Class<?>) inv.getArgument(1)));
    }

    @Test
    void buildQuery_usesMainTitleWordsAndLastName() {
        assertEquals("mediatype:texts AND title:(philosophies war) AND creator:(sheen)",
                InternetArchiveProvider.buildQuery("Philosophies at War", "Fulton J. Sheen"));
        assertEquals("mediatype:texts AND title:(god myself) AND creator:(scott)",
                InternetArchiveProvider.buildQuery("God and Myself: An Inquiry into the True Religion", "Martin J. Scott"));
        assertEquals("mediatype:texts AND title:(vipers tangle) AND creator:(mauriac)",
                InternetArchiveProvider.buildQuery("Viper's Tangle", "François Mauriac"));
        assertEquals("mediatype:texts AND title:(temperament god gave you) AND creator:(bennett)",
                InternetArchiveProvider.buildQuery("The Temperament God Gave You", "Art Bennett and Laraine Bennett"));
    }

    @Test
    void search_findsReadableItemWhoseCreatorIsInLastFirstForm() {
        respondWith("""
                {"response":{"numFound":2,"docs":[
                  {"identifier":"philosophiesatwa0000shee","title":"Philosophies at war","creator":"Sheen, Fulton J. (Fulton John), 1895-1979","lending___status":"is_lendable"},
                  {"identifier":"philosophiesatwa00shee","title":"Philosophies at war","creator":["Sheen, Fulton J"],"lending___status":"is_readable"}
                ]}}
                """);

        FreeTextLookupResult result = provider.search("Philosophies at War", "Fulton J. Sheen");

        assertTrue(result.isFound());
        assertEquals("https://archive.org/details/philosophiesatwa00shee", result.getUrl());
    }

    @Test
    void search_matchesSubtitledTitleAndSecondCreator() {
        respondWith("""
                {"response":{"numFound":1,"docs":[
                  {"identifier":"a590650100lesluoft","title":"Henry Edward Manning : his life and labours",
                   "creator":["Burns & Oates","Leslie, Shane, 1885-1971"],"lending___status":"is_readable"}
                ]}}
                """);

        FreeTextLookupResult result = provider.search("Henry Edward Manning: His Life and Labours", "Shane Leslie");

        assertTrue(result.isFound());
        assertEquals("https://archive.org/details/a590650100lesluoft", result.getUrl());
    }

    @Test
    void search_stillSkipsBorrowOnlyItems() {
        respondWith("""
                {"response":{"numFound":1,"docs":[
                  {"identifier":"diaryofcountrypr00bern","title":"The diary of a country priest","creator":"Bernanos, Georges, 1888-1948","lending___status":"is_lendable"}
                ]}}
                """);

        assertFalse(provider.search("The Diary of a Country Priest", "Georges Bernanos").isFound());
        assertTrue(provider.searchAllowingBorrowable("The Diary of a Country Priest", "Georges Bernanos").isFound());
    }

    @Test
    void search_skipsUploadsWithoutLendingStatus() {
        respondWith("""
                {"response":{"numFound":1,"docs":[
                  {"identifier":"fulton-sheen-liberty-equality-and-fraternity","title":"Liberty Equality And Fraternity - Archbishop Fulton J. Sheen","creator":"Fulton J. Sheen"}
                ]}}
                """);

        assertFalse(provider.search("Liberty, Equality and Fraternity", "Fulton J. Sheen").isFound());
    }

    @Test
    void search_rejectsOtherAuthorsAndOtherVolumes() {
        respondWith("""
                {"response":{"numFound":2,"docs":[
                  {"identifier":"x1","title":"Jesus of Nazareth: Holy Week","creator":"Benedict XVI, Pope","lending___status":"is_readable"},
                  {"identifier":"x2","title":"The river","creator":"Paulsen, Gary","lending___status":"is_readable"}
                ]}}
                """);

        assertFalse(provider.search("Jesus of Nazareth: The Infancy Narratives", "Joseph Ratzinger").isFound());
        assertFalse(provider.search("The River", "Rumer Godden").isFound());
    }

    @Test
    @RequiresOpenNetwork
    @EnabledIfEnvironmentVariable(named = "OPEN_NETWORK", matches = "(?i)true")
    void live_findsPhilosophiesAtWar() {
        InternetArchiveProvider live = new InternetArchiveProvider();
        ReflectionTestUtils.setField(live, "restTemplate", new RestTemplate());

        FreeTextLookupResult result = live.search("Philosophies at War", "Fulton J. Sheen");

        assertTrue(result.isFound(), result.getErrorMessage());
        assertEquals("https://archive.org/details/philosophiesatwa00shee", result.getUrl());
    }
}
