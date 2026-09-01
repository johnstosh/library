/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EwtnLibraryProviderTest {

    private static final String SITEMAP = """
            <?xml version="1.0" encoding="utf-8"?>
            <urlset>
            <url><loc>https://www.ewtn.com/catholicism/library/home-library-864</loc></url>
            <url><loc>https://www.ewtn.com/catholicism/library/wisdom-of-solomon-12472</loc></url>
            <url><loc>https://www.ewtn.com/catholicism/library/wisdom-of-fr-brown-10889</loc></url>
            <url><loc>https://www.ewtn.com/catholicism/library/innocence-of-fr-brown-10829</loc></url>
            <url><loc>https://www.ewtn.com/catholicism/library/sinners-guide-9832</loc></url>
            </urlset>
            """;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EwtnLibraryProvider provider;

    @Test
    void getProviderName_returnsCorrectName() {
        assertEquals("EWTN Catholic Library", provider.getProviderName());
    }

    @Test
    void getPriority_returns40() {
        assertEquals(40, provider.getPriority());
    }

    @Test
    void getExpectedDomains_includesEwtn() {
        assertTrue(provider.getExpectedDomains().contains("ewtn.com"));
    }

    @Test
    void search_findsWisdomOfFatherBrown_despiteFrAbbreviation() {
        when(restTemplate.getForObject(EwtnLibraryProvider.SITEMAP_URL, String.class))
                .thenReturn(SITEMAP);

        FreeTextLookupResult result = provider.search(
                "The Wisdom of Father Brown", "Gilbert Keith Chesterton");

        assertTrue(result.isFound());
        assertEquals("https://www.ewtn.com/catholicism/library/wisdom-of-fr-brown-10889", result.getUrl());
        assertEquals("EWTN Catholic Library", result.getProviderName());
    }

    @Test
    void search_findsWisdomOfSolomon_notFatherBrown() {
        when(restTemplate.getForObject(EwtnLibraryProvider.SITEMAP_URL, String.class))
                .thenReturn(SITEMAP);

        FreeTextLookupResult result = provider.search("The Wisdom of Solomon", "Unknown");

        assertTrue(result.isFound());
        assertEquals("https://www.ewtn.com/catholicism/library/wisdom-of-solomon-12472", result.getUrl());
    }

    @Test
    void search_returnsNotFoundWhenTitleAbsent() {
        when(restTemplate.getForObject(EwtnLibraryProvider.SITEMAP_URL, String.class))
                .thenReturn(SITEMAP);

        FreeTextLookupResult result = provider.search("How the Grinch Stole Christmas", "Dr. Seuss");

        assertFalse(result.isFound());
        assertTrue(result.getErrorMessage().contains("Not found"));
    }

    @Test
    void search_handlesEmptyResponse() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(null);

        FreeTextLookupResult result = provider.search("Some Book", "Some Author");

        assertFalse(result.isFound());
        assertEquals("EWTN Catholic Library", result.getProviderName());
    }

    @Test
    void search_handlesApiException() {
        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new RuntimeException("Connection failed"));

        FreeTextLookupResult result = provider.search("Test Book", null);

        assertFalse(result.isFound());
        assertTrue(result.getErrorMessage().contains("Search error"));
    }

    @Test
    void search_fetchesSitemapOnce() {
        when(restTemplate.getForObject(EwtnLibraryProvider.SITEMAP_URL, String.class))
                .thenReturn(SITEMAP);

        provider.search("The Wisdom of Father Brown", "Chesterton");
        provider.search("The Innocence of Father Brown", "Chesterton");

        verify(restTemplate, times(1)).getForObject(EwtnLibraryProvider.SITEMAP_URL, String.class);
    }

    @Test
    void slugToTitle_stripsIdAndHyphens() {
        assertEquals("wisdom of fr brown",
                EwtnLibraryProvider.slugToTitle(
                        "https://www.ewtn.com/catholicism/library/wisdom-of-fr-brown-10889"));
    }
}
