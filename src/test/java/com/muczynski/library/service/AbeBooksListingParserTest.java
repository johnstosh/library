/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AbeBooksListingParserTest {

    private final AbeBooksListingParser parser = new AbeBooksListingParser();

    private static final String PAGE = """
            <html><body><ul>
            <li data-srp-item-role="listing">
              <a data-test-id="listing-title-link" href="/Pride-Prejudice/111/bd"><h2>Pride</h2></a>
              <span data-test-id="listing-condition">Used - Good</span>
              <span data-test-id="listing-price">US$ 4.86</span>
              <span data-test-id="item-shipping-price-0">Free Shipping</span>
            </li>
            <li data-srp-item-role="listing">
              <a data-test-id="listing-title-link" href="/Pride-Prejudice/222/bd"><h2>Pride</h2></a>
              <span data-test-id="listing-condition">Used - Very good</span>
              <span data-test-id="listing-price">US$ 2.99</span>
              <span data-test-id="item-shipping-price-1">US$ 3.75 shipping</span>
            </li>
            <li data-srp-item-role="listing">
              <a data-test-id="listing-title-link" href="/Pride-Prejudice/333/bd"><h2>Pride</h2></a>
              <span data-test-id="listing-condition">Used - Fair</span>
              <span data-test-id="listing-price">US$ 1.00</span>
              <span data-test-id="item-shipping-price-2">Free Shipping</span>
            </li>
            </ul></body></html>
            """;

    @Test
    void cheapestGoodOrBetter_skipsFairAndIncludesShipping() {
        Optional<AbeBooksListing> listing = parser.cheapestGoodOrBetter(PAGE);
        assertTrue(listing.isPresent());
        AbeBooksListing found = listing.get();
        assertEquals(new BigDecimal("4.86"), found.getPriceDollars());
        assertEquals(BigDecimal.ZERO, found.getShippingDollars());
        assertEquals("Used - Good", found.getCondition());
        assertEquals("https://www.abebooks.com/Pride-Prejudice/111/bd", found.getDetailsUrl());
        assertEquals(new BigDecimal("4.86"), found.totalDollars());
    }

    @Test
    void cheapestGoodOrBetter_emptyHtml_returnsEmpty() {
        assertTrue(parser.cheapestGoodOrBetter("").isEmpty());
        assertTrue(parser.cheapestGoodOrBetter("<html><body>No listings</body></html>").isEmpty());
    }

    @Test
    void isGoodOrBetter_acceptsNewVeryGoodAndGood() {
        assertTrue(AbeBooksListingParser.isGoodOrBetter("New"));
        assertTrue(AbeBooksListingParser.isGoodOrBetter("Used - Very good"));
        assertTrue(AbeBooksListingParser.isGoodOrBetter("Used - Good"));
        assertTrue(AbeBooksListingParser.isGoodOrBetter("As New"));
        assertFalse(AbeBooksListingParser.isGoodOrBetter("Used - Fair"));
        assertFalse(AbeBooksListingParser.isGoodOrBetter("Poor"));
        assertFalse(AbeBooksListingParser.isGoodOrBetter("As Described"));
        assertFalse(AbeBooksListingParser.isGoodOrBetter(""));
    }

    @Test
    void parseMoney_handlesNbspAndCommas() {
        assertEquals(new BigDecimal("4.86"), AbeBooksListingParser.parseMoney("US$\u00a04.86"));
        assertEquals(new BigDecimal("1234.50"), AbeBooksListingParser.parseMoney("US$ 1,234.50"));
        assertEquals(BigDecimal.ZERO, AbeBooksListingParser.parseShipping("Free Shipping"));
        assertEquals(new BigDecimal("3.75"), AbeBooksListingParser.parseShipping("US$ 3.75 shipping"));
    }
}
