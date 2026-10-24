/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.AdditionalShippingSource;
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
    void isRateLimited_blockPageAndFastEmptyResponse() {
        assertTrue(parser.isBlockedPage(""));
        assertTrue(parser.isBlockedPage("<html>Pardon Our Interruption captcha</html>"));
        assertTrue(parser.isBlockedPage("<html>Access Denied</html>"));
        assertFalse(parser.isBlockedPage(
                "<html>Closest match to your search<div data-test-id=\"srp-search-results-list\"></div></html>"));
        assertFalse(parser.isRateLimited(PAGE, 80));
        String emptyResults = "<html>Closest match to your search<div data-test-id=\"srp-search-results-list\"></div></html>";
        assertFalse(parser.isRateLimited(emptyResults, 80),
                "a real empty SearchResults page is not a rate limit even when fast");
        assertFalse(parser.isRateLimited(emptyResults, 400));
        assertTrue(parser.isRateLimited("<html>not a search page</html>", 80),
                "a tiny non-search page that returns too fast is treated as rate limited");
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
        assertTrue(AbeBooksListingParser.isGoodOrBetter("Used"));
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
        assertEquals(BookCoverType.LIBRARY_BINDING, AbeBooksListingParser.bindingFromLabel("Library Binding"));
        assertEquals(BookCoverType.LIBRARY_BINDING,
                AbeBooksListingParser.bindingFromLabel("Hardcover, Library Binding"));
        assertEquals(BookCoverType.OTHER, AbeBooksListingParser.bindingFromLabel("Leather Bound"));
        assertEquals(BookCoverType.OTHER, AbeBooksListingParser.bindingFromLabel("Spiral Bound"));
        assertNull(AbeBooksListingParser.bindingFromLabel("First Edition"));
        assertNull(AbeBooksListingParser.bindingFromLabel(""));
    }

    @Test
    void parseEmbeddedListings_readsSellerAndSkipsFair() {
        String json = "{\"searchResultsArray\":{\"results\":["
                + "{\"listing\":{"
                + "\"listingId\":111,\"title\":\"Pride and Prejudice\",\"binding\":\"Hardcover\","
                + "\"condition\":\"USED_VERYGOOD\",\"vendorCondition\":\"Very Good\","
                + "\"priceInPurchaseCurrency\":{\"amount\":4.86,\"currency\":\"USD\"},"
                + "\"shippingPriceInPurchaseCurrency\":{\"amount\":0,\"currency\":\"USD\"},"
                + "\"shippingRates\":[{\"shippingPriceInPurchaseCurrency\":{\"amount\":0,\"currency\":\"USD\"}},"
                + "{\"shippingPriceInPurchaseCurrency\":{\"amount\":8.99,\"currency\":\"USD\"}}],"
                + "\"vendorId\":52274160,\"vendorName\":\"World of Books\","
                + "\"vendorCity\":\"Montgomery\",\"vendorProvState\":\"IL\",\"vendorCountry\":\"USA\","
                + "\"vendorStarRating\":5,"
                + "\"vendorStorefrontUrl\":\"/World-of-Books/52274160/sf?ref_=nav\","
                + "\"bdpCanonicalUrl\":\"/Pride/111/bd\",\"isbn13\":\"9780141439518\",\"quantity\":1"
                + "}},"
                + "{\"listing\":{"
                + "\"listingId\":222,\"title\":\"York Notes\",\"binding\":\"Paperback\","
                + "\"condition\":\"USED_FAIR\",\"vendorCondition\":\"Fair\","
                + "\"priceInPurchaseCurrency\":{\"amount\":1.00,\"currency\":\"USD\"},"
                + "\"shippingPriceInPurchaseCurrency\":{\"amount\":0,\"currency\":\"USD\"},"
                + "\"vendorId\":9,\"vendorName\":\"Skip Me\""
                + "}}"
                + "]}}";
        String html = "<script>self.__next_f.push([1,\"" + json.replace("\"", "\\\"") + "\"])</script>";

        List<AbeBooksListing> listings = parser.parseEmbeddedListings(html);

        assertEquals(1, listings.size());
        AbeBooksListing offer = listings.get(0);
        assertEquals(111L, offer.getListingId());
        assertEquals("Pride and Prejudice", offer.getListingTitle());
        assertEquals(new BigDecimal("4.86"), offer.getPriceDollars());
        assertEquals(BigDecimal.ZERO, offer.getShippingDollars());
        assertEquals(new BigDecimal("8.99"), offer.getPriorityShippingDollars());
        assertEquals("Used - Very Good", offer.getCondition());
        assertEquals(BookCoverType.HARDCOVER, offer.getBinding());
        assertEquals(52274160L, offer.getSellerId());
        assertEquals("World of Books", offer.getSellerName());
        assertEquals("Montgomery", offer.getSellerCity());
        assertEquals("https://www.abebooks.com/World-of-Books/52274160/sf", offer.getSellerStorefrontUrl());
        assertEquals("https://www.abebooks.com/Pride/111/bd", offer.getDetailsUrl());
        assertEquals("9780141439518", offer.getIsbn13());
    }

    @Test
    void parseShipRates_readsFirstBookAndAdditionalAmount() {
        String html = """
                <html><body><table>
                <tr><td>First book:</td><td>US$ 6.00</td><td>US$ 12.00</td></tr>
                </table>
                <strong>Bookseller Shipping Terms</strong>
                <p>Each additional book US$ 1.50</p>
                <p>Shipping speeds are estimates and are not guaranteed.</p>
                </body></html>
                """;
        AbeBooksShipRates rates = parser.parseShipRates(html);
        assertEquals(new BigDecimal("6.00"), rates.getStandardDollars());
        assertEquals(new BigDecimal("12.00"), rates.getPriorityDollars());
        assertEquals(new BigDecimal("1.50"), rates.getAdditionalDollars());
        assertEquals(AdditionalShippingSource.PARSED, rates.getAdditionalSource());
        assertTrue(rates.getTerms().contains("Each additional book"));
        assertFalse(rates.getTerms().toLowerCase().contains("shipping speeds"));
    }

    @Test
    void parseAdditionalRate_freeProseAndUnknownBoilerplate() {
        AbeBooksListingParser.AdditionalRate free = AbeBooksListingParser.parseAdditionalRate(
                "Additional books ship free within the United States.");
        assertEquals(BigDecimal.ZERO, free.dollars());
        assertEquals(AdditionalShippingSource.FREE, free.source());

        AbeBooksListingParser.AdditionalRate unknown = AbeBooksListingParser.parseAdditionalRate(
                "* Shipping speeds are estimates. Additional items may vary.");
        assertNull(unknown.dollars());
        assertEquals(AdditionalShippingSource.UNKNOWN, unknown.source());
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
