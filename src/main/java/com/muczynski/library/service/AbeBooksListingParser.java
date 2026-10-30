/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.domain.AdditionalShippingSource;
import com.muczynski.library.domain.BookCoverType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses AbeBooks SearchResults HTML for good-or-better listings.
 * Relies on stable {@code data-test-id} hooks rather than hashed CSS class names.
 */
@Component
public class AbeBooksListingParser {

    private static final String BASE_URL = "https://www.abebooks.com";
    private static final Pattern MONEY = Pattern.compile(
            "(?:US\\s*\\$|\\$)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE);
    private static final String MONEY_AMOUNT =
            "([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|\\.[0-9]{1,2})";
    private static final String MONEY_TOKEN = "(?:US\\s*\\$|USD|\\$)\\s*" + MONEY_AMOUNT;
    private static final String EXTRA_WORD = "(?:additional|extra|add'l|addl\\.?)";
    private static final String ITEM_WORD =
            "(?:books|book|items|item|copies|copy|volumes|volume)";
    /** Prose that states additional books cost nothing on standard shipping. */
    private static final Pattern MULTI_BOOK_FREE = Pattern.compile(
            "(?i)(?:"
                    + "additional\\s+" + ITEM_WORD + "(?:\\s+[\\w']+){0,8}\\s+ship(?:\\s+for)?\\s+(?<!not )free\\b"
                    + "|each\\s+" + EXTRA_WORD + "(?:\\s+" + ITEM_WORD + ")?(?:\\s+(?!free\\b)[\\w']+){0,6}\\s+(?<!not )free\\b"
                    + "|no\\s+(?:additional|extra)\\s+charge\\s+(?:for\\s+)?(?:extra\\s+|additional\\s+)?"
                    + ITEM_WORD
                    + "|additional\\s+" + ITEM_WORD + "(?:\\s+[\\w']+){0,8}\\s+(?:at\\s+)?no\\s+(?:extra\\s+)?charge"
                    + "|free\\s+shipping\\s+on\\s+additional\\s+" + ITEM_WORD
                    + ")");
    /**
     * A per-extra-book dollar amount. Groups are mutually exclusive; the first
     * one that matches is the amount. "Each other" is the Robinson Street wording,
     * not a general "other".
     */
    private static final Pattern MULTI_BOOK_AMOUNT = Pattern.compile(
            "(?i)(?:"
                    + MONEY_TOKEN + "\\s*(?:for\\s+|per\\s+)?each\\s+" + EXTRA_WORD
                    + "(?:\\s+" + ITEM_WORD + ")?"
                    + "|"
                    + MONEY_TOKEN + "\\s*(?:for\\s+|per\\s+)?(?:each\\s+)?" + EXTRA_WORD
                    + "\\s+" + ITEM_WORD
                    + "|"
                    + "(?:each\\s+)?" + EXTRA_WORD + "\\s+" + ITEM_WORD
                    + "[^$]{0,50}?" + MONEY_TOKEN
                    + "|"
                    + "(?:second|2nd)\\s+" + ITEM_WORD + "[^$]{0,30}?" + MONEY_TOKEN
                    + "|"
                    + MONEY_TOKEN + "\\s+(?:each\\s+)?thereafter\\b"
                    + "|thereafter[^$]{0,20}?" + MONEY_TOKEN
                    + "|"
                    + MONEY_TOKEN + "\\s+more\\s+for\\s+each\\s+" + EXTRA_WORD
                    + "(?:\\s+" + ITEM_WORD + ")?"
                    + "|"
                    + MONEY_TOKEN + "\\s+each\\s+other\\b"
                    + ")");
    /** Media mail, book rate, or standard. Preferred over Priority Mail. */
    private static final Pattern STANDARD_SERVICE = Pattern.compile(
            "(?i)media\\s+mail|book\\s*rate|\\bstandard\\b");
    /**
     * AbeBooks' faster column, not the USPS Priority Mail service sellers use
     * for ordinary parcels. "Priority -" is that column written in prose.
     */
    private static final Pattern FASTER_SERVICE = Pattern.compile(
            "(?i)priority\\s+shipping|\\bexpedited\\b|express\\s+shipping|priority\\s*[-–—]");
    /** USPS Priority Mail, or a bare "Priority" that is not the faster column. */
    private static final Pattern PRIORITY_MAIL_SERVICE = Pattern.compile(
            "(?i)priority\\s+mail|\\bpriority\\b");
    private static final Pattern SHIPS_FREE = Pattern.compile(
            "(?i)\\bship(?:ping|s)?\\b.*?\\b(?<!not )free\\b|\\b(?<!not )free\\b.*?\\bship(?:ping|s)?\\b");
    private static final Pattern FREE_OF_SHIPPING = Pattern.compile(
            "(?i)\\bfree\\s+shipping\\b|\\bship(?:ping|s)?\\s+(?:for\\s+)?(?<!not )free\\b");
    private static final Pattern WITHIN_US = Pattern.compile(
            "(?i)(?:within|inside)\\s+the\\s+u\\.?s\\b|contiguous\\s+us\\b");
    private static final Pattern USA_WORD = Pattern.compile("(?i)\\busa\\b");
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    /**
     * Good-or-better listings on the page, cheapest-first is not applied here —
     * callers pick the cheapest per binding.
     */
    public List<AbeBooksListing> parseGoodOrBetter(String html) {
        List<AbeBooksListing> found = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return found;
        }
        Document doc = Jsoup.parse(html);
        Elements listings = doc.select("li[data-srp-item-role=listing]");
        for (Element listing : listings) {
            Optional<AbeBooksListing> parsed = parseListing(listing);
            if (parsed.isEmpty()) {
                continue;
            }
            AbeBooksListing candidate = parsed.get();
            if (!isGoodOrBetter(candidate.getCondition())) {
                continue;
            }
            found.add(candidate);
        }
        return found;
    }

    /**
     * True when the HTML (or lack of it) looks like a block/captcha page
     * rather than an AbeBooks search-results or ShipRates page.
     */
    public boolean isBlockedPage(String html) {
        if (html == null || html.isBlank()) {
            return true;
        }
        String lower = html.toLowerCase(Locale.ROOT);
        if (lower.contains("pardon our interruption")
                || lower.contains("captcha")
                || lower.contains("access denied")
                || lower.contains("unusual traffic")
                || lower.contains("verify you are human")
                || lower.contains("cf-browser-verification")
                || lower.contains("request unsuccessful")) {
            return true;
        }
        if (hasListing(html) || looksLikeSearchResults(html) || looksLikeShipRates(html)) {
            return false;
        }
        return true;
    }

    /**
     * Rate-limited when the body is a block/captcha page, or a tiny no-listing
     * response that came back faster than a real SearchResults page (~250ms).
     * Full AbeBooks result pages (even with zero listings) often arrive in
     * under 250ms and must not be retried as rate limits.
     */
    public boolean isRateLimited(String html, long elapsedMs) {
        if (isBlockedPage(html)) {
            return true;
        }
        if (looksLikeSearchResults(html) || hasListing(html) || looksLikeShipRates(html)) {
            return false;
        }
        return elapsedMs >= 0 && elapsedMs < 250;
    }

    static boolean hasListing(String html) {
        return html != null && (html.contains("data-srp-item-role=\"listing\"")
                || html.contains("data-srp-item-role='listing'"));
    }

    static boolean looksLikeSearchResults(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        String lower = html.toLowerCase(Locale.ROOT);
        return html.contains("srp-search-results-list")
                || lower.contains("closest match to your search")
                || html.contains("data-test-id=\"result-count\"");
    }

    /**
     * A seller ShipRates page. It has no search-result markup, so the search
     * block check must not treat it as a captcha.
     */
    static boolean looksLikeShipRates(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        String lower = html.toLowerCase(Locale.ROOT);
        return lower.contains("bookseller shipping terms") || lower.contains("first book:");
    }

    /**
     * True when the search-results pager has an enabled Next control.
     */
    public boolean hasNextPage(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        Document doc = Jsoup.parse(html);
        Element next = doc.selectFirst("[data-test-id=next-page]");
        if (next == null) {
            return false;
        }
        if (next.hasAttr("disabled")) {
            return false;
        }
        String ariaDisabled = next.attr("aria-disabled");
        return !"true".equalsIgnoreCase(ariaDisabled);
    }

    Optional<AbeBooksListing> parseListing(Element listing) {
        String priceText = firstText(listing, "[data-test-id=listing-price]");
        BigDecimal price = parseMoney(priceText);
        if (price == null) {
            return Optional.empty();
        }
        String condition = firstText(listing, "[data-test-id=listing-condition]");
        if (condition == null || condition.isBlank()) {
            condition = firstText(listing, "[data-test-id^=listing-book-condition]");
        }
        String shippingText = firstText(listing, "[data-test-id^=item-shipping-price]");
        BigDecimal shipping = parseShipping(shippingText);
        String href = null;
        Element link = listing.selectFirst("a[data-test-id=listing-title-link]");
        if (link != null) {
            href = link.attr("href");
        }
        return Optional.of(AbeBooksListing.builder()
                .priceDollars(price)
                .shippingDollars(shipping != null ? shipping : BigDecimal.ZERO)
                .condition(normalizeCondition(condition))
                .detailsUrl(absoluteUrl(href))
                .binding(parseBinding(listing))
                .build());
    }

    /**
     * Binding from the listing Attributes row. Null when the listing does not
     * name a binding (AbeBooks "Unknown"). Library binding is preferred over
     * hardcover when both words appear.
     */
    static BookCoverType parseBinding(Element listing) {
        Element attributes = listing.selectFirst("ul[aria-label=Attributes]");
        if (attributes == null) {
            return null;
        }
        for (Element labeled : attributes.select("[aria-label]")) {
            BookCoverType type = bindingFromLabel(labeled.attr("aria-label"));
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    static BookCoverType bindingFromLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        if (normalized.contains("library bind")
                || normalized.contains("library-bind")
                || normalized.contains("reinforced")) {
            return BookCoverType.LIBRARY_BINDING;
        }
        if (normalized.contains("hardcover")
                || normalized.contains("hard cover")
                || normalized.contains("hardback")
                || normalized.equals("cloth")) {
            return BookCoverType.HARDCOVER;
        }
        if (normalized.contains("softcover")
                || normalized.contains("soft cover")
                || normalized.contains("paperback")
                || normalized.contains("mass market")) {
            return BookCoverType.SOFTCOVER;
        }
        if (normalized.contains("leather")
                || normalized.contains("spiral")
                || normalized.contains("board book")
                || normalized.contains("comb bound")
                || normalized.contains("ring bound")) {
            return BookCoverType.OTHER;
        }
        return null;
    }

    static boolean isGoodOrBetter(String condition) {
        if (condition == null || condition.isBlank()) {
            return false;
        }
        String normalized = condition.toLowerCase(Locale.ROOT);
        if (containsWord(normalized, "fair")
                || containsWord(normalized, "poor")
                || containsWord(normalized, "acceptable")
                || normalized.contains("as described")) {
            return false;
        }
        return containsWord(normalized, "good")
                || containsWord(normalized, "fine")
                || normalized.contains("very good")
                || normalized.contains("like new")
                || normalized.contains("as new")
                || normalized.contains("near fine")
                || containsWord(normalized, "new")
                || containsWord(normalized, "used");
    }

    static BigDecimal parseMoney(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = MONEY.matcher(text.replace('\u00a0', ' '));
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1).replace(",", ""));
    }

    static BigDecimal parseShipping(String text) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = text.replace('\u00a0', ' ').toLowerCase(Locale.ROOT);
        if (normalized.contains("free")) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = parseMoney(text);
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private static String firstText(Element root, String css) {
        Element el = root.selectFirst(css);
        return el == null ? null : el.text();
    }

    private static String normalizeCondition(String condition) {
        if (condition == null) {
            return null;
        }
        String trimmed = condition.replace('\u00a0', ' ').trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String absoluteUrl(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (href.startsWith("/")) {
            return BASE_URL + href;
        }
        return BASE_URL + "/" + href;
    }

    private static boolean containsWord(String haystack, String word) {
        int idx = haystack.indexOf(word);
        while (idx >= 0) {
            boolean startOk = idx == 0 || !Character.isLetter(haystack.charAt(idx - 1));
            int end = idx + word.length();
            boolean endOk = end >= haystack.length() || !Character.isLetter(haystack.charAt(end));
            if (startOk && endOk) {
                return true;
            }
            idx = haystack.indexOf(word, idx + 1);
        }
        return false;
    }

    /**
     * Good-or-better offers from the embedded search JSON. Empty when that payload is absent,
     * so callers can fall back to the visible listing cards.
     */
    public List<AbeBooksListing> parseEmbeddedListings(String html) {
        List<AbeBooksListing> found = new ArrayList<>();
        String object = searchResultsObject(html);
        if (object == null) {
            return found;
        }
        try {
            JsonNode results = JSON.readTree(object).path("searchResultsArray").path("results");
            if (!results.isArray()) {
                return found;
            }
            for (JsonNode wrapper : results) {
                AbeBooksListing listing = listingFromJson(wrapper.path("listing"));
                if (listing != null && isGoodOrBetter(listing.getCondition())) {
                    found.add(listing);
                }
            }
        } catch (Exception ex) {
            return List.of();
        }
        return found;
    }

    /**
     * First-book rates and seller terms from a ShipRates page.
     * The extra-book amount is parsed from the terms text when the prose states one.
     */
    public AbeBooksShipRates parseShipRates(String html) {
        if (html == null || html.isBlank()) {
            return AbeBooksShipRates.unknown();
        }
        Document doc = Jsoup.parse(html);
        BigDecimal standard = null;
        BigDecimal priority = null;
        for (Element td : doc.select("td")) {
            String label = td.text().replace('\u00a0', ' ').trim().toLowerCase(Locale.ROOT);
            if (!label.startsWith("first book")) {
                continue;
            }
            Element row = td.parent();
            if (row == null) {
                continue;
            }
            Elements cells = row.select("td");
            List<BigDecimal> prices = new ArrayList<>();
            for (int i = 1; i < cells.size(); i++) {
                BigDecimal amount = parseMoney(cells.get(i).text());
                if (amount != null) {
                    prices.add(amount);
                }
            }
            if (!prices.isEmpty()) {
                standard = prices.get(0);
            }
            if (prices.size() > 1) {
                priority = prices.get(1);
            }
            break;
        }
        String terms = shippingTerms(doc);
        AdditionalRate extra = parseAdditionalRate(terms);
        return AbeBooksShipRates.builder()
                .standardDollars(standard)
                .priorityDollars(priority)
                .terms(terms)
                .additionalDollars(extra.dollars)
                .additionalSource(extra.source)
                .build();
    }

    /**
     * Standard extra-book rate stated in seller terms.
     * Unknown when the text does not name one.
     * Media mail, book rate, and standard shipping win over USPS Priority Mail.
     * Priority Mail is kept only when the terms state no media, book-rate, or
     * standard extra, and standard shipping is not itself free.
     * AbeBooks priority shipping, expedited, express, and "Priority -" are ignored,
     * as is a rate that applies only to international shipping.
     * When standard or domestic shipping to the US is free, additional books are free.
     * A service named before a comma or semicolon still applies to the next amount,
     * so "Priority $2.25 each additional; Media Mail $1.00 each additional" is $1.00.
     */
    public static AdditionalRate parseAdditionalRate(String terms) {
        if (terms == null || terms.isBlank()) {
            return AdditionalRate.UNKNOWN;
        }
        String text = terms.replace('\u00a0', ' ').replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();
        AdditionalRate best = null;
        int bestRank = 0;
        boolean standardFree = false;
        // Split on a sentence end. A single capital before the period, as in "U.S.",
        // is an abbreviation. "FREE. Priority" is a new sentence.
        for (String sentence : text.split("(?<![^A-Za-z][A-Za-z][.!?])(?<=[.!?])\\s+(?=[A-Z])|\\n+")) {
            ShippingClass mode = ShippingClass.UNSPECIFIED;
            boolean international = mentionsInternational(sentence);
            for (String clause : sentence.split("\\s*(?:;|,|\\band\\b)\\s*")) {
                if (clause.isBlank()) {
                    continue;
                }
                ShippingClass named = classInClause(clause);
                if (named != ShippingClass.UNSPECIFIED) {
                    mode = named;
                }
                boolean domestic = mentionsDomestic(clause) || mentionsWithinUs(clause) || mentionsUsa(clause);
                if ((international || mentionsInternational(clause)) && !domestic) {
                    continue;
                }
                if (mode == ShippingClass.FASTER) {
                    continue;
                }
                int rank = rank(mode);
                if (rank > bestRank) {
                    AdditionalRate rate = rateInClause(clause);
                    if (rate != null) {
                        best = rate;
                        bestRank = rank;
                    }
                }
                if (standardShippingIsFree(clause)) {
                    standardFree = true;
                }
            }
        }
        // A named standard or unspecified extra-book price beats "shipping is free".
        // Free standard shipping beats a Priority Mail upcharge.
        if (best != null && bestRank >= rank(ShippingClass.UNSPECIFIED)) {
            return best;
        }
        if (standardFree) {
            return new AdditionalRate(BigDecimal.ZERO, AdditionalShippingSource.FREE);
        }
        if (best != null) {
            return best;
        }
        return AdditionalRate.UNKNOWN;
    }

    /** Service class named in one clause. Unspecified means the clause names none. */
    private enum ShippingClass {
        FASTER,
        PRIORITY_MAIL,
        UNSPECIFIED,
        STANDARD
    }

    private static int rank(ShippingClass shippingClass) {
        return switch (shippingClass) {
            case STANDARD -> 3;
            case UNSPECIFIED -> 2;
            case PRIORITY_MAIL -> 1;
            case FASTER -> 0;
        };
    }

    private static ShippingClass classInClause(String clause) {
        if (STANDARD_SERVICE.matcher(clause).find()) {
            return ShippingClass.STANDARD;
        }
        if (FASTER_SERVICE.matcher(clause).find()) {
            return ShippingClass.FASTER;
        }
        if (PRIORITY_MAIL_SERVICE.matcher(clause).find()) {
            return ShippingClass.PRIORITY_MAIL;
        }
        return ShippingClass.UNSPECIFIED;
    }

    private static AdditionalRate rateInClause(String clause) {
        if (MULTI_BOOK_FREE.matcher(clause).find()) {
            return new AdditionalRate(BigDecimal.ZERO, AdditionalShippingSource.FREE);
        }
        Matcher matcher = MULTI_BOOK_AMOUNT.matcher(clause);
        if (!matcher.find()) {
            return null;
        }
        for (int group = 1; group <= matcher.groupCount(); group++) {
            String raw = matcher.group(group);
            if (raw == null) {
                continue;
            }
            return new AdditionalRate(new BigDecimal(raw.replace(",", "")), AdditionalShippingSource.PARSED);
        }
        return null;
    }

    private static boolean mentionsInternational(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("international")
                || lower.contains("overseas")
                || lower.contains("abroad")
                || lower.contains("outside of the u")
                || lower.contains("outside of u")
                || lower.contains("outside the u");
    }

    private static boolean mentionsDomestic(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("domestic")
                || lower.contains("standard")
                || lower.contains("within the united states")
                || lower.contains("inside the united states");
    }

    private static boolean standardShippingIsFree(String clause) {
        String lower = clause.toLowerCase(Locale.ROOT);
        if (!lower.contains("free") || lower.contains("not free")
                || lower.contains("additional") || lower.contains("extra")
                || lower.contains("add'l") || lower.contains("addl")) {
            return false;
        }
        if (!SHIPS_FREE.matcher(lower).find()) {
            return false;
        }
        // "Free Delivery Confirmation" is tracking, not a free shipment.
        if (lower.contains("free delivery") && !FREE_OF_SHIPPING.matcher(lower).find()) {
            return false;
        }
        boolean domestic = mentionsDomestic(lower) || mentionsWithinUs(lower);
        if (mentionsInternational(lower) && !domestic) {
            return false;
        }
        return domestic || mentionsUsa(lower);
    }

    private static boolean mentionsWithinUs(String text) {
        return WITHIN_US.matcher(text).find();
    }

    private static boolean mentionsUsa(String text) {
        return USA_WORD.matcher(text).find();
    }

    private static String shippingTerms(Document doc) {
        Element strong = null;
        for (Element el : doc.select("strong")) {
            if (el.text().toLowerCase(Locale.ROOT).contains("bookseller shipping terms")) {
                strong = el;
                break;
            }
        }
        if (strong == null) {
            return null;
        }
        // AbeBooks nests a second <p> inside the terms <p>. Jsoup hoists that
        // paragraph out, so the prose is the parent's next sibling, not the strong's.
        String following = textOfFollowingSiblings(strong);
        if (following == null && strong.parent() != null) {
            following = textOfFollowingSiblings(strong.parent());
        }
        if (following != null) {
            return following;
        }
        return inlineTerms(strong);
    }

    private static String textOfFollowingSiblings(Element start) {
        StringBuilder terms = new StringBuilder();
        for (Element node = start.nextElementSibling(); node != null; node = node.nextElementSibling()) {
            String tag = node.tagName();
            if (tag.equals("script") || tag.equals("style")) {
                break;
            }
            String text = node.text().replace('\u00a0', ' ').trim();
            if (text.isEmpty()) {
                continue;
            }
            if (isShippingDisclaimer(text)) {
                break;
            }
            if (terms.length() > 0) {
                terms.append('\n');
            }
            terms.append(text);
        }
        return terms.isEmpty() ? null : terms.toString();
    }

    private static boolean isShippingDisclaimer(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replaceFirst("^[^a-z]+", "");
        return lower.startsWith("shipping speeds are estimates");
    }

    private static String inlineTerms(Element strong) {
        Element parent = strong.parent();
        if (parent == null) {
            return null;
        }
        String text = parent.text().replace('\u00a0', ' ').trim();
        int idx = text.toLowerCase(Locale.ROOT).indexOf("bookseller shipping terms");
        if (idx >= 0) {
            text = text.substring(idx + "bookseller shipping terms".length()).replaceFirst("^[:\\s]+", "").trim();
        }
        int disclaimer = text.toLowerCase(Locale.ROOT).indexOf("shipping speeds are estimates");
        if (disclaimer >= 0) {
            text = text.substring(0, disclaimer).replaceFirst("[*\\s]+$", "").trim();
        }
        return text.isEmpty() ? null : text;
    }

    static String searchResultsObject(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        int marker = html.indexOf("searchResultsArray");
        if (marker < 0) {
            return null;
        }
        int push = html.lastIndexOf("push([1,\"", marker);
        if (push < 0) {
            return null;
        }
        int start = push + "push([1,\"".length();
        String decoded = unescapeJsString(html, start);
        if (decoded == null) {
            return null;
        }
        int jsonStart = decoded.indexOf("{\"searchResultsArray\"");
        if (jsonStart < 0) {
            jsonStart = decoded.indexOf("{\"searchResultsArray\"");
        }
        if (jsonStart < 0) {
            return null;
        }
        return extractObject(decoded, jsonStart);
    }

    private static String unescapeJsString(String html, int start) {
        StringBuilder out = new StringBuilder();
        for (int i = start; i < html.length(); i++) {
            char c = html.charAt(i);
            if (c == '\\' && i + 1 < html.length()) {
                char next = html.charAt(i + 1);
                if (next == 'u' && i + 5 < html.length()) {
                    String hex = html.substring(i + 2, i + 6);
                    try {
                        out.append((char) Integer.parseInt(hex, 16));
                        i += 5;
                        continue;
                    } catch (NumberFormatException ex) {
                        out.append(next);
                        i++;
                        continue;
                    }
                }
                if (next == 'n') {
                    out.append('\n');
                } else if (next == 't') {
                    out.append('\t');
                } else {
                    out.append(next);
                }
                i++;
                continue;
            }
            if (c == '"') {
                return out.toString();
            }
            out.append(c);
        }
        return null;
    }

    private static String extractObject(String decoded, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < decoded.length(); i++) {
            char c = decoded.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                    continue;
                }
                if (c == '\\') {
                    escape = true;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return decoded.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private AbeBooksListing listingFromJson(JsonNode listing) {
        if (listing == null || listing.isMissingNode()) {
            return null;
        }
        JsonNode priceNode = listing.path("priceInPurchaseCurrency");
        if (!usd(priceNode)) {
            return null;
        }
        BigDecimal price = moneyAmount(priceNode);
        if (price == null) {
            return null;
        }
        JsonNode shippingNode = listing.path("shippingPriceInPurchaseCurrency");
        if (!shippingNode.isMissingNode() && !shippingNode.isNull() && !usd(shippingNode)) {
            return null;
        }
        BigDecimal shipping = moneyAmount(shippingNode);
        String vendorCondition = text(listing, "vendorCondition");
        String conditionCode = text(listing, "condition");
        String condition = conditionLabel(conditionCode, vendorCondition);
        if (!isGoodOrBetter(condition)) {
            return null;
        }
        Long listingId = longValue(listing.get("listingId"));
        Long vendorId = longValue(listing.get("vendorId"));
        return AbeBooksListing.builder()
                .priceDollars(price)
                .shippingDollars(shipping != null ? shipping : BigDecimal.ZERO)
                .priorityShippingDollars(priorityShipping(listing))
                .condition(condition)
                .detailsUrl(absoluteUrl(text(listing, "bdpCanonicalUrl")))
                .binding(bindingFromLabel(text(listing, "binding")))
                .listingId(listingId)
                .listingTitle(text(listing, "title"))
                .isbn13(text(listing, "isbn13"))
                .quantity(listing.path("quantity").isNumber() ? listing.path("quantity").asInt() : null)
                .currency("USD")
                .sellerId(vendorId)
                .sellerName(text(listing, "vendorName"))
                .sellerCity(text(listing, "vendorCity"))
                .sellerRegion(text(listing, "vendorProvState"))
                .sellerCountry(text(listing, "vendorCountry"))
                .sellerStars(listing.path("vendorStarRating").isNumber()
                        ? listing.path("vendorStarRating").asInt() : null)
                .sellerStorefrontUrl(absoluteUrl(stripQuery(text(listing, "vendorStorefrontUrl"))))
                .build();
    }

    private static BigDecimal priorityShipping(JsonNode listing) {
        JsonNode rates = listing.path("shippingRates");
        if (!rates.isArray() || rates.size() < 2) {
            return null;
        }
        JsonNode priority = rates.get(1).path("shippingPriceInPurchaseCurrency");
        if (!usd(priority)) {
            return null;
        }
        return moneyAmount(priority);
    }

    private static boolean usd(JsonNode money) {
        if (money == null || money.isMissingNode() || money.isNull()) {
            return true;
        }
        JsonNode currency = money.get("currency");
        if (currency == null || currency.isNull() || !currency.isTextual()) {
            return true;
        }
        return "USD".equalsIgnoreCase(currency.asText());
    }

    private static BigDecimal moneyAmount(JsonNode money) {
        if (money == null || money.isMissingNode() || money.isNull()) {
            return null;
        }
        JsonNode amount = money.get("amount");
        if (amount == null || !amount.isNumber()) {
            return null;
        }
        return amount.decimalValue();
    }

    private static String conditionLabel(String code, String vendorCondition) {
        if (vendorCondition != null && !vendorCondition.isBlank()) {
            String value = vendorCondition.trim();
            if (code != null && code.toUpperCase(Locale.ROOT).startsWith("USED")
                    && !value.toLowerCase(Locale.ROOT).startsWith("used")) {
                return "Used - " + value;
            }
            return value;
        }
        return code;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private static Long longValue(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.asLong();
    }

    private static String stripQuery(String href) {
        if (href == null) {
            return null;
        }
        int query = href.indexOf('?');
        return query >= 0 ? href.substring(0, query) : href;
    }

    /**
     * Extra-book rate read from seller terms.
     */
    public record AdditionalRate(BigDecimal dollars, AdditionalShippingSource source) {
        static final AdditionalRate UNKNOWN = new AdditionalRate(null, AdditionalShippingSource.UNKNOWN);
    }
}
