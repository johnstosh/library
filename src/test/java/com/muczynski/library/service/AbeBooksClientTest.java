/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.BookCoverType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbeBooksClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AbeBooksListingParser parser;

    @InjectMocks
    private AbeBooksClient client;

    @Test
    void buildSearchUri_titleAndAuthorNoBindingFilter() {
        URI uri = client.buildSearchUri("Pride and Prejudice", "Austen", 0);
        String s = uri.toString();
        assertTrue(s.startsWith(AbeBooksClient.SEARCH_URL));
        assertFalse(s.contains("bi="));
        assertTrue(s.contains("sortby=17"));
        assertTrue(s.contains("tn=Pride"));
        assertTrue(s.contains("an=Austen"));
        assertTrue(s.contains("cond="));
        assertTrue(s.contains("new"));
        assertTrue(s.contains("good"));
        assertFalse(s.contains("p="));
    }

    @Test
    void buildSearchUri_laterPageUsesPAndSpo() {
        URI uri = client.buildSearchUri("Emma", "Austen", 1);
        String s = uri.toString();
        assertTrue(s.contains("p=1"));
        assertTrue(s.contains("spo=30"));
        assertFalse(s.contains("bi="));
    }

    @Test
    void buildSearchUri_omitsAuthorWhenBlank() {
        URI uri = client.buildSearchUri("Emma", null, 0);
        assertFalse(uri.toString().contains("an="));
    }

    @Test
    void cleanTitle_stripsCopyAndFormatSuffixesButKeepsSlashes() {
        assertEquals("Pride and Prejudice", AbeBooksClient.cleanTitle("Pride and Prejudice, c. 2"));
        assertEquals("Pride and Prejudice", AbeBooksClient.cleanTitle("Pride and Prejudice (DVD)"));
        assertEquals("The Norwayman / Bernadette of Lourdes",
                AbeBooksClient.cleanTitle("The Norwayman / Bernadette of Lourdes"));
    }

    @Test
    void authorLastName_takesFirstPersonAfterSemicolonsAndSuffixes() {
        assertEquals("Austen", AbeBooksClient.authorLastName("Jane Austen"));
        assertEquals("O'Connor", AbeBooksClient.authorLastName(
                "Joseph O'Connor; Frances Parkinson Keyes; et al."));
        assertEquals("Ciszek", AbeBooksClient.authorLastName("Walter J. Ciszek, SJ"));
        assertEquals("Schwarzkopf", AbeBooksClient.authorLastName("Herbert Norman Schwarzkopf Jr."));
        assertEquals("Farrow", AbeBooksClient.authorLastName(
                "John Farrow; et al. (Fife; O'Brien; Chavez; Luce)"));
        assertEquals("Bérulle", AbeBooksClient.authorLastName(
                "Pierre de Bérulle et al. (ed. William M. Thompson)"));
        assertEquals("O'Connor", AbeBooksClient.authorLastName("O'Connor, Joseph"));
        assertNull(AbeBooksClient.authorLastName("et al."));
        assertNull(AbeBooksClient.authorLastName(""));
    }

    @Test
    void letterWordCount_ignoresSlashTokens() {
        assertEquals(12, AbeBooksClient.letterWordCount(
                "The Norwayman / Bernadette of Lourdes / The Woodcarver of Tyrol / Sea of Glory"));
        assertEquals(3, AbeBooksClient.letterWordCount("Pride and Prejudice"));
    }

    @Test
    void findCheapestGoodOrBetter_pagesUntilBothBindingsFound() {
        AbeBooksListing soft = listing("s", "3.00", BookCoverType.SOFTCOVER);
        AbeBooksListing hard = listing("h", "4.86", BookCoverType.HARDCOVER);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    URI uri = invocation.getArgument(0);
                    String html = uri.toString().contains("p=1") ? "page2" : "page1";
                    return ResponseEntity.ok(html);
                });
        when(parser.parseGoodOrBetter("page1")).thenReturn(List.of(soft));
        when(parser.hasNextPage("page1")).thenReturn(true);
        when(parser.parseGoodOrBetter("page2")).thenReturn(List.of(hard));

        AbeBooksCoverListings found = client.findCheapestGoodOrBetter("Pride and Prejudice", "Jane Austen");

        assertEquals(hard, found.getHardcover());
        assertEquals(soft, found.getSoftcover());
        verify(restTemplate, times(2))
                .exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void findCheapestGoodOrBetter_unknownBindingFillsBothCovers() {
        AbeBooksListing unknown = listing("u", "33.00", null);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("page1"));
        when(parser.parseGoodOrBetter("page1")).thenReturn(List.of(unknown));
        when(parser.hasNextPage("page1")).thenReturn(false);

        AbeBooksCoverListings found = client.findCheapestGoodOrBetter(
                "The Norwayman / Bernadette of Lourdes / The Woodcarver of Tyrol / Sea of Glory",
                "Joseph O'Connor; Frances Parkinson Keyes; et al.");

        assertEquals(unknown, found.getHardcover());
        assertEquals(unknown, found.getSoftcover());
        verify(restTemplate, times(1))
                .exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void findCheapestGoodOrBetter_longTitleFallsBackToTitleOnly() {
        AbeBooksListing unknown = listing("u", "33.00", null);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    URI uri = invocation.getArgument(0);
                    return ResponseEntity.ok(uri.toString().contains("an=") ? "with-author" : "title-only");
                });
        when(parser.parseGoodOrBetter("with-author")).thenReturn(List.of());
        when(parser.hasNextPage("with-author")).thenReturn(false);
        when(parser.parseGoodOrBetter("title-only")).thenReturn(List.of(unknown));
        when(parser.hasNextPage("title-only")).thenReturn(false);

        AbeBooksCoverListings found = client.findCheapestGoodOrBetter(
                "The Story of Thomas More / Weddings in the Family / The Road to Damascus / From an Altar Screen",
                "John Farrow; et al. (Fife; O'Brien; Chavez; Luce)");

        assertEquals(unknown, found.getHardcover());
        assertEquals(unknown, found.getSoftcover());
        ArgumentCaptor<URI> uris = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate, times(2))
                .exchange(uris.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        String first = uris.getAllValues().get(0).toString();
        String fallback = uris.getAllValues().get(1).toString();
        assertTrue(first.contains("an=Farrow"));
        assertTrue(first.contains("cond="));
        assertFalse(fallback.contains("an="));
        assertFalse(fallback.contains("cond="));
    }

    @Test
    void findCheapestGoodOrBetter_shortTitleDoesNotTitleOnlyFallback() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("empty"));
        when(parser.parseGoodOrBetter("empty")).thenReturn(List.of());
        when(parser.hasNextPage("empty")).thenReturn(false);

        AbeBooksCoverListings found = client.findCheapestGoodOrBetter("Emma", "Jane Austen");

        assertNull(found.getHardcover());
        assertNull(found.getSoftcover());
        verify(restTemplate, times(1))
                .exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    private static AbeBooksListing listing(String id, String price, BookCoverType binding) {
        return AbeBooksListing.builder()
                .priceDollars(new BigDecimal(price))
                .shippingDollars(BigDecimal.ZERO)
                .condition("Used - Good")
                .detailsUrl("https://www.abebooks.com/" + id)
                .binding(binding)
                .build();
    }
}
