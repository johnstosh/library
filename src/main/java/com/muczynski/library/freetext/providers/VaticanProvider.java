/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.freetext.FreeTextProvider;
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
            // Vatican search is limited - build search URL with query
            String query = title;
            if (authorName != null && !authorName.isBlank()) {
                query = title + " " + authorName;
            }

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = SEARCH_URL + "?q=" + encodedQuery;

            String html = restTemplate.getForObject(searchUrl, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from Vatican search");
            }

            // Look for document links in search results
            // Vatican documents typically have URLs like /content/{pope}/en/...
            Pattern docPattern = Pattern.compile(
                    "<a[^>]+href=\"(https?://www\\.vatican\\.va/content/[^\"]+\\.html)\"",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = docPattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.success(getProviderName(), matcher.group(1));
            }

            // Check for archive links
            Pattern archivePattern = Pattern.compile(
                    "<a[^>]+href=\"(https?://www\\.vatican\\.va/archive/[^\"]+)\"",
                    Pattern.CASE_INSENSITIVE);

            matcher = archivePattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.success(getProviderName(), matcher.group(1));
            }

            return FreeTextLookupResult.error(getProviderName(), "Document not found on Vatican.va");

        } catch (Exception e) {
            log.warn("Vatican.va search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
