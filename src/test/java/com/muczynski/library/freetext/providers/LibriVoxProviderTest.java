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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibriVoxProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LibriVoxProvider provider;

    @Test
    void getProviderName_returnsCorrectName() {
        assertEquals("LibriVox (Audiobooks)", provider.getProviderName());
    }

    @Test
    void getPriority_returns30() {
        assertEquals(30, provider.getPriority());
    }

    @Test
    void search_handlesEmptyResponse() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(null);

        FreeTextLookupResult result = provider.search("Some Book", "Some Author");

        assertFalse(result.isFound());
        assertEquals("LibriVox (Audiobooks)", result.getProviderName());
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
    void search_usesPrefixQueryKeepingMiddleWords_andOmitsAuthor() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(null);

        provider.search("The Wisdom of Father Brown", "Gilbert Keith Chesterton");

        verify(restTemplate).getForObject(
                argThat((String url) -> {
                    String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
                    return decoded.contains("librivox.org/api/feed/audiobooks")
                            && decoded.contains("title=^wisdom of father brown")
                            && !decoded.contains("author=")
                            && !decoded.contains("Chesterton")
                            && !decoded.contains("title=^wisdom father brown");
                }),
                any());
    }

    @Test
    void search_keepsAndInPrefixQuery() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(null);

        provider.search("War and Peace", "Leo Tolstoy");

        verify(restTemplate).getForObject(
                argThat((String url) -> {
                    String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
                    return decoded.contains("title=^war and peace")
                            && !decoded.contains("title=^war peace");
                }),
                any());
    }
}
