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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The SRU response can contain several records. The free-text URL has to come
 * from the record whose author is the one we searched, not from an earlier hit
 * that only shared the title.
 */
@ExtendWith(MockitoExtension.class)
class LocCatalogOnlineProviderTest {

    private static final String TWO_RECORDS = """
            <collection>
              <record>
                <datafield tag="100" ind1="1" ind2=" ">
                  <subfield code="a">John Chrysostom</subfield>
                </datafield>
                <datafield tag="245" ind1="1" ind2="0">
                  <subfield code="a">On the Priesthood</subfield>
                </datafield>
                <datafield tag="856" ind1="4" ind2="0">
                  <subfield code="u">https://www.newadvent.org/fathers/1922.htm</subfield>
                </datafield>
              </record>
              <record>
                <datafield tag="100" ind1="1" ind2=" ">
                  <subfield code="a">John of Avila</subfield>
                </datafield>
                <datafield tag="245" ind1="1" ind2="0">
                  <subfield code="a">On the Priesthood</subfield>
                </datafield>
                <datafield tag="856" ind1="4" ind2="0">
                  <subfield code="u">https://example.org/avila-priesthood</subfield>
                </datafield>
              </record>
            </collection>
            """;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LocCatalogOnlineProvider provider;

    @Test
    void search_usesTheRecordWhoseAuthorMatches() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(TWO_RECORDS);

        FreeTextLookupResult result = provider.search("On the Priesthood", "John of Avila");

        assertTrue(result.isFound());
        assertEquals("https://example.org/avila-priesthood", result.getUrl());
    }

    @Test
    void search_rejectsATitleMatchBySomeoneElse() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(TWO_RECORDS);

        FreeTextLookupResult result = provider.search(
                "Treatise on the Love of God & Treatise on the Priesthood", "John of Avila");

        assertFalse(result.isFound());
    }
}
