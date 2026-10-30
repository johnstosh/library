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
        String shipRates = """
                <html><body><table><tr><td>First book:</td><td>US$ 4.00</td></tr></table>
                <strong>Bookseller Shipping Terms</strong><p>Each additional book US$ 1.50</p>
                </body></html>
                """;
        assertFalse(parser.isBlockedPage(shipRates), "a ShipRates page is not a block page");
        assertFalse(parser.isRateLimited(shipRates, 40),
                "a fast ShipRates page is not a rate limit");
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

    @Test
    void parseShipRates_readsLiveNestedTermsAndIgnoresPriorityRate() {
        String html = """
                <html><body><table>
                <tr><td>First book:</td><td>US$ 0.00</td><td>US$ 5.99</td></tr>
                </table>
                <p><strong>Bookseller Shipping Terms:</strong> <p>STANDARD DOMESTIC U.S. SHIPPING IS FREE.  Priority Shipping is only $6.99 for first item, $1 each additional. Orders ship within 2 business days. Shipping outside of U.S. is typically between $8-13 and takes between 8-15 business days.  Heavier books may require additional shipping.  Faster Needs Please Inquire.</p></p>
                <p><span class="required">*</span> Shipping speeds are estimates based on the carriers' delivery times and cannot be guaranteed.</p>
                </body></html>
                """;
        AbeBooksShipRates rates = parser.parseShipRates(html);
        assertEquals(new BigDecimal("0.00"), rates.getStandardDollars());
        assertEquals(new BigDecimal("5.99"), rates.getPriorityDollars());
        assertEquals(BigDecimal.ZERO, rates.getAdditionalDollars());
        assertEquals(AdditionalShippingSource.FREE, rates.getAdditionalSource());
        assertTrue(rates.getTerms().contains("STANDARD DOMESTIC"));
        assertTrue(rates.getTerms().contains("$1 each additional"));
        assertFalse(rates.getTerms().toLowerCase().contains("shipping speeds"));
    }

    @Test
    void parseAdditionalRate_readsStandardMultiBookPhrases() {
        assertParsed("1.50", "Each additional book US$ 1.50");
        assertParsed("1.50", "$1.50 for each additional book");
        assertParsed("1.00", "US$ 1.00 per additional item");
        assertParsed("2", "each extra book $2");
        assertParsed("1.25", "First book $4.50, $1.25 thereafter");
        assertParsed("0.50", "second book $.50");
        assertEquals(AdditionalShippingSource.FREE, AbeBooksListingParser.parseAdditionalRate(
                "Additional books in the same order ship for FREE via Standard Shipping.").source());
        assertEquals(AdditionalShippingSource.FREE, AbeBooksListingParser.parseAdditionalRate(
                "No additional charge for extra books.").source());
    }

    @Test
    void parseAdditionalRate_skipsPriorityAndInternationalRates() {
        AbeBooksListingParser.AdditionalRate priorityOnly = AbeBooksListingParser.parseAdditionalRate(
                "Priority Shipping is only $6.99 for first item, $1 each additional.");
        assertNull(priorityOnly.dollars());
        assertEquals(AdditionalShippingSource.UNKNOWN, priorityOnly.source());

        AbeBooksListingParser.AdditionalRate abroad = AbeBooksListingParser.parseAdditionalRate(
                "Outside the U.S. each additional book is $12.");
        assertNull(abroad.dollars());
        assertEquals(AdditionalShippingSource.UNKNOWN, abroad.source());

        assertParsed("1.50", "Outside the U.S. each additional book is $8 and within the United States each additional book is $1.50.");
        assertEquals(AdditionalShippingSource.FREE, AbeBooksListingParser.parseAdditionalRate(
                "STANDARD DOMESTIC U.S. SHIPPING IS FREE. Priority Shipping is only $1 each additional.").source());
        assertEquals(BigDecimal.ZERO, AbeBooksListingParser.parseAdditionalRate(
                "STANDARD DOMESTIC U.S. SHIPPING IS FREE. Priority Shipping is only $1 each additional.").dollars());
        assertParsed("1.00", "Priority shipping is $3 each additional and standard is $1 each additional.");

        AbeBooksListingParser.AdditionalRate heavy = AbeBooksListingParser.parseAdditionalRate(
                "Heavier books may require additional shipping. The bookseller may contact you about extra charges.");
        assertEquals(AdditionalShippingSource.UNKNOWN, heavy.source());
    }

    @Test
    void parseAdditionalRate_readsLiveSellerWording() {
        assertUnknown("Insurance is normally provided at no extra charge for orders over 100 USD.");
        assertUnknown("Books with dustjackets are supplied with archival quality mylar covers at no additional charge. "
                + "Free Delivery Confirmation and Tracking.");
        assertUnknown("United States: Priority - $10.00 first book; $6.00 each additional.");
        assertUnknown("please select Expedited shipping, not Free shipping.");
        assertUnknown("I almost always offer free shipping, but if the buyer wants special shipping packaging, I will try to accomodate.");
        assertUnknown("Free Shipping");

        assertParsed("2.00", "SHIPPING: United States: Priority - $10.00 first book; $6.00 each additional. "
                + "Media Mail/Bookrate $2.99; $2.00 each additional. "
                + "International: Global Priority only (average price for a single book will be $24.00.)");
        assertParsed("1.00", "DOMESTIC: Priority $11.50 1st book, $2.25 each additional; "
                + "Media Mail 5.50 1st book, $1.00 each additional;");
        assertParsed("2.00", "Media mail postage and handling in the U.S. is $4.00 for the first volume "
                + "and $2.00 for each additional volume in the same shipment. "
                + "Priority Mail in the U.S. is $10.00 for the first volume and $2.00 for each additional volume.");
        assertParsed("2.00", "Domestic orders are shipped via the U.S. Postal Service (Priority Mail) starting at $18.00 "
                + "for the first book. Media Mail shipping rate is also available: $7.00 for the first book and $2.00 for each additional.");
        assertParsed("2.00", "Priority Mail in the U.S. is $10.00 for the first volume and $2.00 for each additional volume.");
        assertParsed("1.00", "Please include $5.00 (US) for media mail within USA, 5-14 days; $1.00 more for each additional item. "
                + "For INTERNATIONAL PURCHASES, minimum charge of $11.00.");
        assertParsed("1.00", "Shipping: Standard Shipping: $9.50 first book, $1.00 each other. "
                + "Expedited $10.00 first book $3.00 each other.");

        assertFree("All books within the US ship free of charge. "
                + "If your book order is heavy or oversized, we may contact you to let you know extra shipping is required.");
        assertFree("Free Shipping for All Domestic (US) Standard Orders\n"
                + "International shipments are tendered to the local postal service.");
        assertFree("Free Shipping for All Domestic (US) Standard Orders International shipments are tendered "
                + "to the local postal service.");
        assertFree("STANDARD DOMESTIC U.S. SHIPPING IS FREE. Priority Mail is $2.00 for each additional book.");
        assertFree("We offer free standard shipping to the contiguous US.");
    }

    private static void assertUnknown(String terms) {
        AbeBooksListingParser.AdditionalRate rate = AbeBooksListingParser.parseAdditionalRate(terms);
        assertNull(rate.dollars(), terms);
        assertEquals(AdditionalShippingSource.UNKNOWN, rate.source(), terms);
    }

    private static void assertFree(String terms) {
        AbeBooksListingParser.AdditionalRate rate = AbeBooksListingParser.parseAdditionalRate(terms);
        assertEquals(0, BigDecimal.ZERO.compareTo(rate.dollars()), terms);
        assertEquals(AdditionalShippingSource.FREE, rate.source(), terms);
    }

    private static void assertParsed(String dollars, String terms) {
        AbeBooksListingParser.AdditionalRate rate = AbeBooksListingParser.parseAdditionalRate(terms);
        assertEquals(0, new BigDecimal(dollars).compareTo(rate.dollars()), terms);
        assertEquals(AdditionalShippingSource.PARSED, rate.source(), terms);
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
