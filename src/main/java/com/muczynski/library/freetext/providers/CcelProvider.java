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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for Christian Classics Ethereal Library (CCEL).
 * CCEL hosts free classic Christian books, including Church Fathers, Reformation works, etc.
 *
 * Website: https://www.ccel.org
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CcelProvider implements FreeTextProvider {

    private static final String SEARCH_URL = "https://www.ccel.org/search";
    private static final String BASE_URL = "https://www.ccel.org";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Christian Classics Ethereal Library";
    }

    @Override
    public int getPriority() {
        return 35;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String query = title;
            if (authorName != null && !authorName.isBlank()) {
                query = title + " " + authorName;
            }

            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("qu", query)
                    .build()
                    .toUriString();

            String html = restTemplate.getForObject(url, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from CCEL search");
            }

            // CCEL book links follow pattern /ccel/{author}/{work}
            // Look for these in search results
            Pattern bookPattern = Pattern.compile(
                    "<a[^>]+href=\"(/ccel/[^/]+/[^\"]+)\"[^>]*>",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = bookPattern.matcher(html);
            if (matcher.find()) {
                String bookPath = matcher.group(1);
                return FreeTextLookupResult.success(getProviderName(), BASE_URL + bookPath);
            }

            // Also check for author pages that might list the book
            Pattern authorPattern = Pattern.compile(
                    "<a[^>]+href=\"(/ccel/[^\"]+)\"[^>]*class=\"[^\"]*author[^\"]*\"",
                    Pattern.CASE_INSENSITIVE);

            matcher = authorPattern.matcher(html);
            if (matcher.find()) {
                String authorPath = matcher.group(1);
                return FreeTextLookupResult.success(getProviderName(), BASE_URL + authorPath);
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in CCEL");

        } catch (Exception e) {
            log.warn("CCEL search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
