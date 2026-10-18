/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

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

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HathiTrustProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private HathiTrustProvider provider;

    private static final String OL_SEVEN_PILLARS = """
            {"docs":[
              {"title":"Seven pillars of peace","author_name":["Fulton J. Sheen"],"oclc":["2458794"],"lccn":["44047204"]},
              {"title":"Seven Storey Mountain","author_name":["Thomas Merton"],"oclc":["999"]}
            ]}
            """;

    private static final String BIB_SEVEN_PILLARS = """
            {"oclc:2458794":{"records":{"000974588":{"titles":["Seven pillars of peace"]}},
              "items":[{"fromRecord":"000974588","htid":"mdp.39015007041968","rightsCode":"pd","usRightsString":"Full view"}]},
             "lccn:44047204":{"records":{"000974588":{"titles":["Seven pillars of peace"]}},
              "items":[{"fromRecord":"000974588","htid":"mdp.39015007041968","rightsCode":"pd","usRightsString":"Full view"}]}}
            """;

    private static URI olUri() {
        return argThat(u -> u != null && u.toString().startsWith(HathiTrustProvider.OPEN_LIBRARY_SEARCH));
    }

    private static URI bibUri() {
        return argThat(u -> u != null && u.toString().startsWith(HathiTrustProvider.BIB_API));
    }

    @Test
    void metadata() {
        assertEquals("HathiTrust", provider.getProviderName());
        assertEquals(18, provider.getPriority());
        assertTrue(provider.getExpectedDomains().contains("hdl.handle.net"));
    }

    @Test
    void search_findsFullViewVolumeThroughOpenLibraryIdentifiers() {
        when(restTemplate.getForObject(olUri(), eq(String.class))).thenReturn(OL_SEVEN_PILLARS);
        when(restTemplate.getForObject(bibUri(), eq(String.class))).thenReturn(BIB_SEVEN_PILLARS);

        FreeTextLookupResult result = provider.search("Seven Pillars of Peace", "Fulton J. Sheen");

        assertTrue(result.isFound());
        assertEquals("https://hdl.handle.net/2027/mdp.39015007041968", result.getUrl());
        // The Merton edition's OCLC number must not be sent
        verify(restTemplate).getForObject(argThat((URI u) -> u != null
                && u.toString().startsWith(HathiTrustProvider.BIB_API)
                && u.toString().contains("oclc:2458794") && !u.toString().contains("999")), eq(String.class));
    }

    @Test
    void search_acceptsUsOnlyFullView() {
        when(restTemplate.getForObject(olUri(), eq(String.class))).thenReturn("""
                {"docs":[{"title":"The rainbow of sorrow","author_name":["Fulton J. Sheen"],"oclc":["2778907"]}]}
                """);
        when(restTemplate.getForObject(bibUri(), eq(String.class))).thenReturn("""
                {"oclc:2778907":{"records":{"1":{"titles":["The rainbow of sorrow"]}},
                  "items":[{"fromRecord":"1","htid":"pst.000005824250","rightsCode":"pdus","usRightsString":"Full view"}]}}
                """);

        FreeTextLookupResult result = provider.search("The Rainbow of Sorrow", "Fulton J. Sheen");

        assertTrue(result.isFound());
        assertEquals("https://hdl.handle.net/2027/pst.000005824250", result.getUrl());
    }

    @Test
    void search_prefersPublicDomainOverUsOnly() {
        when(restTemplate.getForObject(olUri(), eq(String.class))).thenReturn("""
                {"docs":[{"title":"God and war","author_name":["Fulton J. Sheen"],"oclc":["1","2"]}]}
                """);
        when(restTemplate.getForObject(bibUri(), eq(String.class))).thenReturn("""
                {"oclc:1":{"records":{"r":{"titles":["God and war"]}},
                  "items":[{"fromRecord":"r","htid":"inu.us","rightsCode":"pdus","usRightsString":"Full view"}]},
                 "oclc:2":{"records":{"r":{"titles":["God and war"]}},
                  "items":[{"fromRecord":"r","htid":"mdp.world","rightsCode":"pd","usRightsString":"Full view"}]}}
                """);

        assertEquals("https://hdl.handle.net/2027/mdp.world",
                provider.search("God and War", "Fulton J. Sheen").getUrl());
    }

    @Test
    void search_rejectsSearchOnlyVolumes() {
        when(restTemplate.getForObject(olUri(), eq(String.class))).thenReturn("""
                {"docs":[{"title":"The seven storey mountain","author_name":["Thomas Merton"],"oclc":["5"]}]}
                """);
        when(restTemplate.getForObject(bibUri(), eq(String.class))).thenReturn("""
                {"oclc:5":{"records":{"r":{"titles":["The seven storey mountain"]}},
                  "items":[{"fromRecord":"r","htid":"mdp.ic","rightsCode":"ic","usRightsString":"Limited (search-only)"}]}}
                """);

        assertFalse(provider.search("The Seven Storey Mountain", "Thomas Merton").isFound());
    }

    @Test
    void search_rejectsVolumeWhoseCatalogTitleIsAnotherWork() {
        // A shared OCLC number pointed at an unrelated record during research
        when(restTemplate.getForObject(olUri(), eq(String.class))).thenReturn("""
                {"docs":[{"title":"The spear","author_name":["Louis de Wohl"],"oclc":["7"]}]}
                """);
        when(restTemplate.getForObject(bibUri(), eq(String.class))).thenReturn("""
                {"oclc:7":{"records":{"r":{"titles":["William Bingham's Maine lands, 1790-1820"]}},
                  "items":[{"fromRecord":"r","htid":"inu.30000054415595","rightsCode":"pdus","usRightsString":"Full view"}]}}
                """);

        assertFalse(provider.search("The Spear", "Louis de Wohl").isFound());
    }

    @Test
    void search_skipsOpenLibraryEditionsByAnotherAuthor() {
        when(restTemplate.getForObject(olUri(), eq(String.class))).thenReturn("""
                {"docs":[{"title":"The River","author_name":["Gary Paulsen"],"oclc":["8"]}]}
                """);

        assertFalse(provider.search("The River", "Rumer Godden").isFound());
        verify(restTemplate, never()).getForObject(bibUri(), eq(String.class));
    }

    @Test
    void search_returnsErrorOnNetworkFailure() {
        when(restTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(new RuntimeException("timeout"));

        FreeTextLookupResult result = provider.search("Seven Pillars of Peace", "Fulton J. Sheen");

        assertFalse(result.isFound());
        assertTrue(result.getErrorMessage().contains("timeout"));
    }

    @Test
    @RequiresOpenNetwork
    @EnabledIfEnvironmentVariable(named = "OPEN_NETWORK", matches = "(?i)true")
    void live_findsSevenPillarsOfPeace() {
        HathiTrustProvider live = new HathiTrustProvider();
        ReflectionTestUtils.setField(live, "restTemplate", new RestTemplate());

        FreeTextLookupResult result = live.search("Seven Pillars of Peace", "Fulton J. Sheen");

        assertTrue(result.isFound(), result.getErrorMessage());
        assertTrue(result.getUrl().startsWith("https://hdl.handle.net/2027/"));
    }
}
