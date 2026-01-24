/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.freetext.FreeTextProvider;
import com.muczynski.library.freetext.TitleMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for The Online Books Page (University of Pennsylvania).
 * This is a curated index of free online books linked from the Library of Congress.
 *
 * Website: http://onlinebooks.library.upenn.edu/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocOnlineBooksPageProvider implements FreeTextProvider {

    private static final String LOOKUP_URL = "https://onlinebooks.library.upenn.edu/webbin/book/lookupname";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Online Books Page (LOC Index)";
    }

    @Override
    public int getPriority() {
        return 12;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            // Online Books Page works best with author lookup
            // The lookupname endpoint shows authors matching the search,
            // then we need to follow the link to the author's page
            if (authorName == null || authorName.isBlank()) {
                return FreeTextLookupResult.error(getProviderName(), "Author name required for search");
            }

            // Convert author name to OBP format (Last, First or just Last)
            String authorKey = formatAuthorKey(authorName);

            String url = UriComponentsBuilder.fromHttpUrl(LOOKUP_URL)
                    .queryParam("key", authorKey)
                    .build()
                    .toUriString();

            String html = restTemplate.getForObject(url, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from search");
            }

            // First, check if this page directly contains our title (author's books page)
            String bookUrl = findBookOnPage(html, title);
            if (bookUrl != null) {
                return FreeTextLookupResult.success(getProviderName(), bookUrl);
            }

            // If not, this might be a list of matching authors
            // Look for author links that might match our author
            Pattern authorLinkPattern = Pattern.compile(
                    "<a[^>]+href=\"(https://onlinebooks\\.library\\.upenn\\.edu/webbin/book/lookupname\\?key=[^\"]+)\"[^>]*>([^<]+)</a>\\s*\\(",
                    Pattern.CASE_INSENSITIVE);

            Matcher authorMatcher = authorLinkPattern.matcher(html);
            int linkCount = 0;
            while (authorMatcher.find()) {
                linkCount++;
                String authorPageUrl = authorMatcher.group(1);
                String foundAuthorName = authorMatcher.group(2);


                // Check if this author matches what we're looking for
                // Use TitleMatcher.authorMatches which handles various name formats
                if (TitleMatcher.authorMatches(foundAuthorName, authorName)) {
                    // Follow this link to get the author's books
                    // Use URI to prevent double-encoding (URL is already encoded from HTML)
                    try {
                        java.net.URI authorUri = new java.net.URI(authorPageUrl);
                        String authorPageHtml = restTemplate.getForObject(authorUri, String.class);
                        if (authorPageHtml != null) {
                            bookUrl = findBookOnPage(authorPageHtml, title);
                            if (bookUrl != null) {
                                return FreeTextLookupResult.success(getProviderName(), bookUrl);
                            }
                        }
                    } catch (java.net.URISyntaxException e) {
                        log.warn("OBP: Invalid author page URL: {}", authorPageUrl);
                    }
                }
            }

            // Check if author was found at all
            if (html.contains("did not match any author in our database")) {
                return FreeTextLookupResult.error(getProviderName(), "Author not found");
            }

            return FreeTextLookupResult.error(getProviderName(), "Title not found for this author");

        } catch (Exception e) {
            log.warn("Online Books Page search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }

    /**
     * Search for a book title on an OBP page and return the external book URL if found.
     * OBP format: <cite>Book Title</cite> followed by external links to the book.
     */
    private String findBookOnPage(String html, String title) {
        // Pattern to match: <cite>Title</cite>...external link
        Pattern titleLinkPattern = Pattern.compile(
                "<cite>([^<]+)</cite>.*?<a[^>]+href=\"(https?://[^\"]+)\"[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher = titleLinkPattern.matcher(html);
        while (matcher.find()) {
            String foundTitle = matcher.group(1).trim();
            String foundUrl = matcher.group(2);

            // Check if title matches what we're looking for
            if (TitleMatcher.titleMatches(foundTitle, title)) {
                // Don't return links to other OBP pages, only external links
                if (!foundUrl.contains("onlinebooks.library.upenn.edu")) {
                    return foundUrl;
                }
            }
        }
        return null;
    }

    /**
     * Format author name for OBP lookup.
     * OBP uses "Last, First" format with proper casing.
     * Examples:
     * - "Ignatius of Loyola" -> "Ignatius, of Loyola" (handles historical names)
     * - "Jane Austen" -> "Austen, Jane"
     */
    private String formatAuthorKey(String authorName) {
        // Handle "Last, First" format already
        if (authorName.contains(",")) {
            return authorName;
        }

        // Handle single-word names
        String[] parts = authorName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }

        // For multi-word names, try "FirstWord, rest"
        // This works for historical names like "Ignatius of Loyola"
        return parts[0] + ", " + String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
    }

}
