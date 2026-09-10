/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses AbeBooks SearchResults HTML for the cheapest listing that is
 * good condition or better. Relies on stable {@code data-test-id} hooks
 * rather than hashed CSS class names.
 */
@Component
public class AbeBooksListingParser {

    private static final String BASE_URL = "https://www.abebooks.com";
    private static final Pattern MONEY = Pattern.compile(
            "(?:US\\s*\\$|\\$)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Returns the cheapest good-or-better listing on the page, if any.
     */
    public Optional<AbeBooksListing> cheapestGoodOrBetter(String html) {
        if (html == null || html.isBlank()) {
            return Optional.empty();
        }
        Document doc = Jsoup.parse(html);
        Elements listings = doc.select("li[data-srp-item-role=listing]");
        AbeBooksListing best = null;
        for (Element listing : listings) {
            Optional<AbeBooksListing> parsed = parseListing(listing);
            if (parsed.isEmpty()) {
                continue;
            }
            AbeBooksListing candidate = parsed.get();
            if (!isGoodOrBetter(candidate.getCondition())) {
                continue;
            }
            if (best == null || candidate.totalDollars().compareTo(best.totalDollars()) < 0) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
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
                .build());
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
                || containsWord(normalized, "new");
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
}
