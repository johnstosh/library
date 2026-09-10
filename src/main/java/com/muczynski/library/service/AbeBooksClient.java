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
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Fetches AbeBooks SearchResults HTML for a title/author/cover and returns
 * the cheapest good-or-better listing.
 */
@Component
@Slf4j
public class AbeBooksClient {

    static final String SEARCH_URL = "https://www.abebooks.com/servlet/SearchResults";
    /** New, As New/Fine/Near Fine, Very Good, and Good — excludes Fair/Poor/As Described. */
    static final String GOOD_OR_BETTER_COND = "new an fine nf vg good";

    /**
     * Trailing physical-format labels our titles often carry that AbeBooks
     * listings typically do not, e.g. " (DVD)", ", Audio CD".
     */
    private static final Pattern TRAILING_FORMAT_SUFFIX_PATTERN = Pattern.compile(
            "(?i)(\\s*\\((?:dvd|cd|vhs|blu-?ray|book on cd|large print|audiobook)\\)"
                    + "|,\\s*(?:audio\\s*cd|book\\s*on\\s*cd|large\\s*print|dvd|cd|vhs))\\s*$");

    private final RestTemplate restTemplate;
    private final AbeBooksListingParser parser;

    public AbeBooksClient(@Qualifier("abeBooksRestTemplate") RestTemplate restTemplate,
                          AbeBooksListingParser parser) {
        this.restTemplate = restTemplate;
        this.parser = parser;
    }

    public Optional<AbeBooksListing> findCheapestGoodOrBetter(String title, String author, BookCoverType cover) {
        String cleanedTitle = cleanTitle(title);
        if (cleanedTitle == null || cleanedTitle.isBlank()) {
            return Optional.empty();
        }
        String authorQuery = authorLastName(author);
        Optional<AbeBooksListing> listing = search(cleanedTitle, authorQuery, cover);
        if (listing.isEmpty() && author != null && !author.isBlank()
                && authorQuery != null && !authorQuery.equalsIgnoreCase(author.trim())) {
            listing = search(cleanedTitle, author.trim(), cover);
        }
        return listing;
    }

    Optional<AbeBooksListing> search(String title, String author, BookCoverType cover) {
        URI uri = buildSearchUri(title, author, cover);
        log.info("AbeBooks search: {}", uri);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
        ResponseEntity<String> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        return parser.cheapestGoodOrBetter(body);
    }

    URI buildSearchUri(String title, String author, BookCoverType cover) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("sts", "t")
                .queryParam("tn", title)
                .queryParam("bi", cover.abeBooksBindingParam())
                .queryParam("sortby", "17")
                .queryParam("ds", "30")
                .queryParam("pt", "book")
                .queryParam("cond", GOOD_OR_BETTER_COND)
                .queryParam("dym", "on");
        if (author != null && !author.isBlank()) {
            builder.queryParam("an", author);
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

    static String authorLastName(String author) {
        if (author == null) {
            return null;
        }
        String trimmed = author.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split("\\s+");
        return parts[parts.length - 1];
    }
}
