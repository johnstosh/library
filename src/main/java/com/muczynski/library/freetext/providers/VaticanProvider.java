/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.freetext.FreeTextProvider;
import com.muczynski.library.freetext.TitleMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for Vatican.va documents.
 * Vatican.va hosts papal encyclicals, apostolic letters, and other Church documents.
 *
 * This provider searches the encyclicals index pages for each pope to find matching documents.
 * URL pattern: https://www.vatican.va/content/{pope-slug}/en/encyclicals.index.html
 */
@Component
@Slf4j
public class VaticanProvider implements FreeTextProvider {

    private static final String BASE_URL = "https://www.vatican.va";

    /**
     * Map of pope names/identifiers to their Vatican.va URL slugs.
     * Keys are lowercase for matching.
     */
    private static final Map<String, String> POPE_SLUGS = new HashMap<>();
    static {
        // Modern popes (most likely to be searched)
        POPE_SLUGS.put("francis", "francesco");
        POPE_SLUGS.put("pope francis", "francesco");
        POPE_SLUGS.put("francesco", "francesco");

        POPE_SLUGS.put("benedict xvi", "benedict-xvi");
        POPE_SLUGS.put("pope benedict xvi", "benedict-xvi");
        POPE_SLUGS.put("benedict", "benedict-xvi");

        POPE_SLUGS.put("john paul ii", "john-paul-ii");
        POPE_SLUGS.put("pope john paul ii", "john-paul-ii");
        POPE_SLUGS.put("jp2", "john-paul-ii");
        POPE_SLUGS.put("jpii", "john-paul-ii");

        POPE_SLUGS.put("paul vi", "paul-vi");
        POPE_SLUGS.put("pope paul vi", "paul-vi");

        POPE_SLUGS.put("john xxiii", "john-xxiii");
        POPE_SLUGS.put("pope john xxiii", "john-xxiii");

        POPE_SLUGS.put("pius xii", "pius-xii");
        POPE_SLUGS.put("pope pius xii", "pius-xii");

        POPE_SLUGS.put("pius xi", "pius-xi");
        POPE_SLUGS.put("pope pius xi", "pius-xi");

        POPE_SLUGS.put("pius x", "pius-x");
        POPE_SLUGS.put("pope pius x", "pius-x");

        POPE_SLUGS.put("leo xiii", "leo-xiii");
        POPE_SLUGS.put("pope leo xiii", "leo-xiii");
    }

    @Autowired
    @Qualifier("providerRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Vatican.va";
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            // First, try to identify the pope from the author name
            String popeSlug = identifyPope(authorName);

            if (popeSlug != null) {
                // Search that pope's encyclicals index
                FreeTextLookupResult result = searchPopeEncyclicals(popeSlug, title);
                if (result.isFound()) {
                    return result;
                }
            }

            // If no pope identified or not found, try all recent popes
            String[] recentPopes = {"francesco", "benedict-xvi", "john-paul-ii", "paul-vi", "john-xxiii", "leo-xiii"};
            for (String slug : recentPopes) {
                if (!slug.equals(popeSlug)) { // Skip if already tried
                    FreeTextLookupResult result = searchPopeEncyclicals(slug, title);
                    if (result.isFound()) {
                        return result;
                    }
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Document not found on Vatican.va");

        } catch (Exception e) {
            // Get root cause for better error messages (e.g., SocketTimeoutException)
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
            log.warn("Vatican.va search failed: {}", rootMessage);
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + rootMessage);
        }
    }

    /**
     * Identify the pope slug from the author name.
     */
    private String identifyPope(String authorName) {
        if (authorName == null || authorName.isBlank()) {
            return null;
        }

        String normalized = authorName.toLowerCase().trim();

        // Direct lookup
        if (POPE_SLUGS.containsKey(normalized)) {
            return POPE_SLUGS.get(normalized);
        }

        // Try partial matches
        for (Map.Entry<String, String> entry : POPE_SLUGS.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Search a specific pope's encyclicals index page for the title.
     */
    private FreeTextLookupResult searchPopeEncyclicals(String popeSlug, String title) {
        String indexUrl = BASE_URL + "/content/" + popeSlug + "/en/encyclicals.index.html";

        try {
            log.debug("Vatican: Searching {} for '{}'", indexUrl, title);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept", "text/html");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    indexUrl, HttpMethod.GET, entity, String.class);
            String html = response.getBody();

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from Vatican index");
            }

            // Find encyclical links - they're in the format:
            // <a href="/content/{pope}/en/encyclicals/documents/...">Title</a>
            // or with italic: <a href="..."><i>Title</i> (date)</a>
            Pattern linkPattern = Pattern.compile(
                    "<a[^>]+href=\"(/content/[^\"]+/encyclicals/documents/[^\"]+\\.html)\"[^>]*>\\s*(?:<i>)?([^<]+)(?:</i>)?",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String href = matcher.group(1);
                String linkText = matcher.group(2).trim();
                // TitleMatcher.normalize() handles trailing parenthetical dates

                // Use TitleMatcher for accurate title matching
                if (TitleMatcher.titleMatches(linkText, title)) {
                    String fullUrl = BASE_URL + href;
                    log.debug("Vatican: Found match '{}' -> {}", linkText, fullUrl);
                    return FreeTextLookupResult.success(getProviderName(), fullUrl);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in " + popeSlug + " encyclicals");

        } catch (Exception e) {
            log.debug("Vatican: Failed to search {}: {}", popeSlug, e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error for " + popeSlug);
        }
    }
}
