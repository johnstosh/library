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
class GutenbergProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GutenbergProvider provider;

    @Test
    void getProviderName_returnsCorrectName() {
        assertEquals("Project Gutenberg", provider.getProviderName());
    }

    @Test
    void getPriority_returns10() {
        assertEquals(10, provider.getPriority());
    }

    @Test
    void search_handlesEmptyResponse() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(null);

        FreeTextLookupResult result = provider.search("Some Book", "Some Author");

        assertFalse(result.isFound());
        assertEquals("Project Gutenberg", result.getProviderName());
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
    void search_constructsCorrectSearchUrl() {
        // Verify the search URL is correctly constructed
        // Note: TitleMatcher.normalizeForSearch removes articles and short words,
        // and author is reduced to last name only
        when(restTemplate.getForObject(
                argThat((String url) -> url != null &&
                        url.contains("gutendex.com/books/") &&
                        url.contains("search=")),
                any())).thenReturn(null);

        provider.search("Pride and Prejudice", "Jane Austen");

        verify(restTemplate).getForObject(
                argThat((String url) -> url != null &&
                        url.contains("pride") &&      // normalized: lowercase
                        url.contains("prejudice") &&  // normalized: "and" removed
                        url.contains("Austen")),      // author last name only
                any());
    }
}
