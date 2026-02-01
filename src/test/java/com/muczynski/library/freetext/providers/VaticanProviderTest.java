/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VaticanProvider searching papal encyclicals.
 * These tests require network access to vatican.va.
 */
@SpringBootTest
class VaticanProviderTest {

    @Autowired
    private VaticanProvider provider;

    @Test
    void search_rerumNovarum_shouldFindEncyclical() {
        FreeTextLookupResult result = provider.search("Rerum Novarum", "Pope Leo XIII");
        System.out.println("Rerum Novarum: " + result);

        assertTrue(result.isFound(), "Should find Rerum Novarum");
        assertTrue(result.getUrl().contains("rerum-novarum"), "URL should contain encyclical name");
        assertTrue(result.getUrl().contains("leo-xiii"), "URL should contain pope name");
    }

    @Test
    void search_humanaeVitae_shouldFindEncyclical() {
        FreeTextLookupResult result = provider.search("Humanae Vitae", "Pope Paul VI");
        System.out.println("Humanae Vitae: " + result);

        assertTrue(result.isFound(), "Should find Humanae Vitae");
        assertTrue(result.getUrl().contains("humanae-vitae"), "URL should contain encyclical name");
    }

    @Test
    void search_laudatoSi_shouldFindEncyclical() {
        FreeTextLookupResult result = provider.search("Laudato Si'", "Pope Francis");
        System.out.println("Laudato Si': " + result);

        assertTrue(result.isFound(), "Should find Laudato Si'");
        assertTrue(result.getUrl().contains("laudato-si"), "URL should contain encyclical name");
    }

    @Test
    void search_fidesEtRatio_shouldFindEncyclical() {
        FreeTextLookupResult result = provider.search("Fides et Ratio", "Pope John Paul II");
        System.out.println("Fides et Ratio: " + result);

        assertTrue(result.isFound(), "Should find Fides et Ratio");
        assertTrue(result.getUrl().contains("fides-et-ratio"), "URL should contain encyclical name");
    }

    @Test
    void search_deusCaritasEst_shouldFindEncyclical() {
        FreeTextLookupResult result = provider.search("Deus Caritas Est", "Pope Benedict XVI");
        System.out.println("Deus Caritas Est: " + result);

        assertTrue(result.isFound(), "Should find Deus Caritas Est");
        assertTrue(result.getUrl().contains("deus-caritas-est"), "URL should contain encyclical name");
    }

    @Test
    void search_nonExistentEncyclical_shouldNotFind() {
        FreeTextLookupResult result = provider.search("How the Grinch Stole Christmas", "Dr. Seuss");
        System.out.println("Grinch: " + result);

        assertFalse(result.isFound(), "Should not find non-existent encyclical");
    }
}
