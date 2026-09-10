/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.BookCoverType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbeBooksListingParserTest {

    private final AbeBooksListingParser parser = new AbeBooksListingParser();

    private static final String PAGE = """
            <html><body><ul>
            %s
            %s
            %s
            </ul>
            <button data-test-id="next-page">Next</button>
            </body></html>
            """.formatted(
            listing("/Pride/111/bd", "Used - Good", "US$ 4.86", "Free Shipping", "Hardcover"),
            listing("/Pride/222/bd", "Used - Very good", "US$ 2.99", "US$ 3.75 shipping", "Softcover"),
            listing("/Pride/333/bd", "Used - Fair", "US$ 1.00", "Free Shipping", "Hardcover"));

    @Test
    void parseGoodOrBetter_skipsFairAndReadsBinding() {
        List<AbeBooksListing> listings = parser.parseGoodOrBetter(PAGE);
        assertEquals(2, listings.size());

        AbeBooksListing hardcover = listings.get(0);
        assertEquals(new BigDecimal("4.86"), hardcover.getPriceDollars());
        assertEquals(BigDecimal.ZERO, hardcover.getShippingDollars());
        assertEquals("Used - Good", hardcover.getCondition());
        assertEquals("https://www.abebooks.com/Pride/111/bd", hardcover.getDetailsUrl());
        assertEquals(BookCoverType.HARDCOVER, hardcover.getBinding());

        AbeBooksListing softcover = listings.get(1);
        assertEquals(new BigDecimal("2.99"), softcover.getPriceDollars());
        assertEquals(new BigDecimal("3.75"), softcover.getShippingDollars());
        assertEquals(BookCoverType.SOFTCOVER, softcover.getBinding());
    }

    @Test
    void parseGoodOrBetter_missingAttributes_isUnknownBinding() {
        String html = "<html><body><ul>"
                + listing("/omnibus/1/bd", "Used - Very good", "US$ 33.00", "Free Shipping", null)
                + "</ul></body></html>";
        List<AbeBooksListing> listings = parser.parseGoodOrBetter(html);
        assertEquals(1, listings.size());
        assertNull(listings.get(0).getBinding());
        assertEquals(new BigDecimal("33.00"), listings.get(0).getPriceDollars());
    }

    @Test
    void parseGoodOrBetter_emptyHtml_returnsEmpty() {
        assertTrue(parser.parseGoodOrBetter("").isEmpty());
        assertTrue(parser.parseGoodOrBetter("<html><body>No listings</body></html>").isEmpty());
    }

    @Test
    void hasNextPage_enabledAndDisabled() {
        assertTrue(parser.hasNextPage(PAGE));
        assertFalse(parser.hasNextPage("<html><button data-test-id=\"next-page\" disabled>Next</button></html>"));
        assertFalse(parser.hasNextPage("<html><body>no pager</body></html>"));
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

    @Test
    void bindingFromLabel_hardcoverAndSoftcoverSynonyms() {
        assertEquals(BookCoverType.HARDCOVER, AbeBooksListingParser.bindingFromLabel("Hardcover"));
        assertEquals(BookCoverType.HARDCOVER, AbeBooksListingParser.bindingFromLabel("Hardback"));
        assertEquals(BookCoverType.SOFTCOVER, AbeBooksListingParser.bindingFromLabel("Softcover"));
        assertEquals(BookCoverType.SOFTCOVER, AbeBooksListingParser.bindingFromLabel("Paperback"));
        assertNull(AbeBooksListingParser.bindingFromLabel("First Edition"));
        assertNull(AbeBooksListingParser.bindingFromLabel(""));
    }

    private static String listing(String href, String condition, String price, String shipping, String binding) {
        String attributes = binding == null ? ""
                : "<ul aria-label=\"Attributes\"><li><span aria-label=\"" + binding + "\">"
                + binding + "</span></li></ul>";
        return """
                <li data-srp-item-role="listing">
                  <a data-test-id="listing-title-link" href="%s"><h2>Pride</h2></a>
                  <span data-test-id="listing-condition">%s</span>
                  <span data-test-id="listing-price">%s</span>
                  <span data-test-id="item-shipping-price-0">%s</span>
                  %s
                </li>
                """.formatted(href, condition, price, shipping, attributes);
    }
}
