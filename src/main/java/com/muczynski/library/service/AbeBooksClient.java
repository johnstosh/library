/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.BookCoverType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fetches AbeBooks SearchResults HTML for a title/author and returns the
 * cheapest good-or-better hardcover and softcover listings. Binding is read
 * from each result rather than requested via {@code bi}.
 */
@Component
@Slf4j
public class AbeBooksClient {

    static final String SEARCH_URL = "https://www.abebooks.com/servlet/SearchResults";
    /** New, As New/Fine/Near Fine, Very Good, and Good — excludes Fair/Poor/As Described. */
    static final String GOOD_OR_BETTER_COND = "new an fine nf vg good";
    static final int PAGE_SIZE = 30;
    static final int MAX_PAGES = 5;
    static final int TITLE_ONLY_MIN_WORDS = 8;

    /**
     * Trailing physical-format labels our titles often carry that AbeBooks
     * listings typically do not, e.g. " (DVD)", ", Audio CD".
     */
    private static final Pattern TRAILING_FORMAT_SUFFIX_PATTERN = Pattern.compile(
            "(?i)(\\s*\\((?:dvd|cd|vhs|blu-?ray|book on cd|large print|audiobook)\\)"
                    + "|,\\s*(?:audio\\s*cd|book\\s*on\\s*cd|large\\s*print|dvd|cd|vhs))\\s*$");

    private static final Pattern PARENTHESES = Pattern.compile("\\([^)]*\\)");

    private static final Set<String> NAME_PREFIXES = Set.of(
            "fr", "father", "st", "saint", "sr", "sister", "br", "brother",
            "dom", "ven", "venerable", "blessed", "bl", "rev", "reverend",
            "dr", "mr", "mrs", "ms", "prof", "editor", "ed", "sir", "dame");

    private static final Set<String> NAME_SUFFIXES = Set.of(
            "jr", "sr", "ii", "iii", "iv", "phd", "md", "esq", "ed", "editor",
            "sj", "op", "ocd", "osb", "mic", "cssp", "svd", "sm", "ocso", "ocr",
            "osa", "slg", "lc", "cpm", "ss", "sscc", "cp", "opraem", "pcc", "mc",
            "cssr", "ofm", "fsc", "csc", "std", "jcd", "ssl", "osf", "rsm",
            "oh", "cfp", "ibvm", "shcj", "sgs");

    private final RestTemplate restTemplate;
    private final AbeBooksListingParser parser;

    public AbeBooksClient(@Qualifier("abeBooksRestTemplate") RestTemplate restTemplate,
                          AbeBooksListingParser parser) {
        this.restTemplate = restTemplate;
        this.parser = parser;
    }

    /**
     * Searches AbeBooks by title and author last name, pages until both
     * covers have a typed listing (or pages run out), then fills any missing
     * cover from the cheapest unknown-binding listing. Long titles retry
     * without the author when the first search cannot fill both covers.
     */
    public AbeBooksCoverListings findCheapestGoodOrBetter(String title, String author) {
        String cleanedTitle = cleanTitle(title);
        if (cleanedTitle == null || cleanedTitle.isBlank()) {
            return AbeBooksCoverListings.builder().build();
        }
        String lastName = authorLastName(author);
        CoverAccumulator acc = new CoverAccumulator();
        searchPaged(cleanedTitle, lastName, acc);
        if (!acc.canAssignBoth()
                && lastName != null
                && letterWordCount(cleanedTitle) >= TITLE_ONLY_MIN_WORDS) {
            log.info("AbeBooks title-only fallback for long title: {}", cleanedTitle);
            searchPaged(cleanedTitle, null, acc);
        }
        return acc.toResult();
    }

    private void searchPaged(String title, String author, CoverAccumulator acc) {
        for (int page = 0; page < MAX_PAGES; page++) {
            String html = fetch(buildSearchUri(title, author, page));
            if (html == null || html.isBlank()) {
                break;
            }
            List<AbeBooksListing> listings = parser.parseGoodOrBetter(html);
            for (AbeBooksListing listing : listings) {
                acc.add(listing);
            }
            if (acc.hasBothTyped()) {
                break;
            }
            if (!parser.hasNextPage(html)) {
                break;
            }
        }
    }

    private String fetch(URI uri) {
        log.info("AbeBooks search: {}", uri);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
        ResponseEntity<String> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return response.getBody();
    }

    URI buildSearchUri(String title, String author, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("sts", "t")
                .queryParam("tn", title)
                .queryParam("sortby", "17")
                .queryParam("ds", PAGE_SIZE)
                .queryParam("pt", "book")
                .queryParam("cond", GOOD_OR_BETTER_COND)
                .queryParam("dym", "on");
        if (author != null && !author.isBlank()) {
            builder.queryParam("an", author);
        }
        if (page > 0) {
            builder.queryParam("p", page);
            builder.queryParam("spo", page * PAGE_SIZE);
        }
        return builder.encode().build().toUri();
    }

    static String cleanTitle(String title) {
        if (title == null) {
            return null;
        }
        String cleaned = com.muczynski.library.domain.Book.stripCopySuffix(title);
        if (cleaned == null) {
            return null;
        }
        String previous;
        do {
            previous = cleaned;
            cleaned = TRAILING_FORMAT_SUFFIX_PATTERN.matcher(cleaned).replaceFirst("").trim();
        } while (!cleaned.equals(previous));
        return cleaned;
    }

    /**
     * Last name of the first real person in an author string. Splits on
     * semicolons, drops {@code et al.} and parentheticals, and strips
     * honorifics / generational / religious-order suffixes.
     */
    static String authorLastName(String author) {
        if (author == null) {
            return null;
        }
        String remaining = author.trim();
        if (remaining.isEmpty()) {
            return null;
        }
        remaining = stripParentheticals(remaining);
        for (String person : remaining.split(";")) {
            String cleaned = stripHonorificsAndSuffixes(person.trim());
            if (cleaned == null || cleaned.isEmpty() || isEtAl(cleaned)) {
                continue;
            }
            return lastNameFromPerson(cleaned);
        }
        return null;
    }

    static int letterWordCount(String title) {
        if (title == null || title.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String token : title.trim().split("\\s+")) {
            if (token.chars().anyMatch(Character::isLetter)) {
                count++;
            }
        }
        return count;
    }

    private static String stripParentheticals(String value) {
        String current = value;
        while (true) {
            String next = PARENTHESES.matcher(current).replaceAll(" ");
            next = next.replaceAll("\\s+", " ").trim();
            if (next.equals(current)) {
                return current;
            }
            current = next;
        }
    }

    private static String stripHonorificsAndSuffixes(String person) {
        if (person == null || person.isBlank()) {
            return null;
        }
        List<String> tokens = new ArrayList<>(List.of(person.trim().split("\\s+")));
        while (!tokens.isEmpty()) {
            String lastNorm = normalizeNameToken(tokens.get(tokens.size() - 1));
            if (NAME_SUFFIXES.contains(lastNorm) || "etal".equals(lastNorm)) {
                tokens.remove(tokens.size() - 1);
                continue;
            }
            if ("al".equals(lastNorm) && tokens.size() >= 2
                    && "et".equals(normalizeNameToken(tokens.get(tokens.size() - 2)))) {
                tokens.remove(tokens.size() - 1);
                tokens.remove(tokens.size() - 1);
                continue;
            }
            break;
        }
        while (!tokens.isEmpty() && NAME_PREFIXES.contains(normalizeNameToken(tokens.get(0)))) {
            tokens.remove(0);
        }
        if (tokens.isEmpty()) {
            return null;
        }
        return String.join(" ", tokens);
    }

    private static boolean isEtAl(String value) {
        String normalized = normalizeNameToken(value);
        return "etal".equals(normalized) || "al".equals(normalized);
    }

    private static String lastNameFromPerson(String person) {
        String working = person;
        int comma = working.indexOf(',');
        if (comma >= 0) {
            working = working.substring(0, comma).trim();
        }
        if (working.isEmpty()) {
            return null;
        }
        String[] parts = working.split("\\s+");
        String last = parts[parts.length - 1].replaceAll("^[,;:.]+|[,;:.]+$", "");
        return last.isEmpty() ? null : last;
    }

    private static String normalizeNameToken(String token) {
        if (token == null) {
            return "";
        }
        return token.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static AbeBooksListing cheaper(AbeBooksListing current, AbeBooksListing candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.totalDollars().compareTo(current.totalDollars()) < 0) {
            return candidate;
        }
        return current;
    }

    private static final class CoverAccumulator {
        private AbeBooksListing hardcover;
        private AbeBooksListing softcover;
        private AbeBooksListing unknown;

        void add(AbeBooksListing listing) {
            if (listing == null) {
                return;
            }
            BookCoverType binding = listing.getBinding();
            if (binding == BookCoverType.HARDCOVER) {
                hardcover = cheaper(hardcover, listing);
            } else if (binding == BookCoverType.SOFTCOVER) {
                softcover = cheaper(softcover, listing);
            } else {
                unknown = cheaper(unknown, listing);
            }
        }

        boolean hasBothTyped() {
            return hardcover != null && softcover != null;
        }

        boolean canAssignBoth() {
            return (hardcover != null || unknown != null)
                    && (softcover != null || unknown != null);
        }

        AbeBooksCoverListings toResult() {
            return AbeBooksCoverListings.builder()
                    .hardcover(hardcover != null ? hardcover : unknown)
                    .softcover(softcover != null ? softcover : unknown)
                    .build();
        }
    }
}
