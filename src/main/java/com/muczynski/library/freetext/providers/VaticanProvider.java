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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for Vatican.va documents.
 * Vatican.va hosts papal encyclicals, apostolic letters, and other Church documents.
 *
 * Note: Vatican.va doesn't have a public API, so this uses HTML parsing of search results.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VaticanProvider implements FreeTextProvider {

    private static final String SEARCH_URL = "https://www.vatican.va/content/vatican/en/search.html";

    private final RestTemplate restTemplate;

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
            // Normalize title for API search (removes articles, short words, punctuation)
            String query = TitleMatcher.normalizeForSearch(title);
            if (authorName != null && !authorName.isBlank()) {
                // Add author's last name to improve search accuracy
                String[] authorParts = authorName.split("\\s+");
                String lastName = authorParts[authorParts.length - 1];
                query = query + " " + lastName;
            }

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = SEARCH_URL + "?q=" + encodedQuery;

            String html = restTemplate.getForObject(searchUrl, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from Vatican search");
            }

            // Find document/archive links with their associated text and validate title
            // Pattern captures URL and link text
            Pattern linkPattern = Pattern.compile(
                    "<a[^>]+href=\"(https?://www\\.vatican\\.va/(?:content|archive)/[^\"]+)\"[^>]*>([^<]+)</a>",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String href = matcher.group(1);
                String linkText = matcher.group(2);

                // Use TitleMatcher for accurate title matching
                if (TitleMatcher.titleMatches(linkText, title)) {
                    log.debug("Vatican: Found match '{}' -> {}", linkText, href);
                    return FreeTextLookupResult.success(getProviderName(), href);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Document not found on Vatican.va");

        } catch (Exception e) {
            log.warn("Vatican.va search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
