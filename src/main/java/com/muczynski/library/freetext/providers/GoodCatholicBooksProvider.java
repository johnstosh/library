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
 * Provider for Good Catholic Books.
 * Hosts free Catholic books organized by author.
 *
 * Website: https://www.goodcatholicbooks.org
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoodCatholicBooksProvider implements FreeTextProvider {

    private static final String BASE_URL = "https://www.goodcatholicbooks.org";
    private static final String SEARCH_URL = BASE_URL + "/search";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Good Catholic Books";
    }

    @Override
    public int getPriority() {
        return 65;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            // Try site search first
            String query = title;
            if (authorName != null && !authorName.isBlank()) {
                query = title + " " + authorName;
            }

            String searchUrl = SEARCH_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            String html = restTemplate.getForObject(searchUrl, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from search");
            }

            // Look for book links in search results
            Pattern bookPattern = Pattern.compile(
                    "<a[^>]+href=\"(" + Pattern.quote(BASE_URL) + "/[^\"]+)\"[^>]*class=\"[^\"]*book[^\"]*\"",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = bookPattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.success(getProviderName(), matcher.group(1));
            }

            // Alternative: look for any internal link that might be a book page
            Pattern altPattern = Pattern.compile(
                    "<a[^>]+href=\"(" + Pattern.quote(BASE_URL) + "/author/[^\"]+)\"",
                    Pattern.CASE_INSENSITIVE);

            matcher = altPattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.success(getProviderName(), matcher.group(1));
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in Good Catholic Books");

        } catch (Exception e) {
            log.warn("Good Catholic Books search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
