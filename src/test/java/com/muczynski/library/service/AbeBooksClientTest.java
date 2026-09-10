/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.BookCoverType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AbeBooksClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AbeBooksListingParser parser;

    @InjectMocks
    private AbeBooksClient client;

    @Test
    void buildSearchUri_hardcoverGoodOrBetterLowestTotal() {
        URI uri = client.buildSearchUri("Pride and Prejudice", "Austen", BookCoverType.HARDCOVER);
        String s = uri.toString();
        assertTrue(s.startsWith(AbeBooksClient.SEARCH_URL));
        assertTrue(s.contains("bi=h"));
        assertTrue(s.contains("sortby=17"));
        assertTrue(s.contains("tn=Pride"));
        assertTrue(s.contains("an=Austen"));
        assertTrue(s.contains("cond="));
        assertTrue(s.contains("new"));
        assertTrue(s.contains("good"));
    }

    @Test
    void buildSearchUri_softcoverBindingParam() {
        URI uri = client.buildSearchUri("Emma", "Austen", BookCoverType.SOFTCOVER);
        assertTrue(uri.toString().contains("bi=s"));
    }

    @Test
    void cleanTitle_stripsCopyAndFormatSuffixes() {
        assertEquals("Pride and Prejudice", AbeBooksClient.cleanTitle("Pride and Prejudice, c. 2"));
        assertEquals("Pride and Prejudice", AbeBooksClient.cleanTitle("Pride and Prejudice (DVD)"));
        assertEquals("Austen", AbeBooksClient.authorLastName("Jane Austen"));
    }
}
