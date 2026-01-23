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

    private static final String SEARCH_URL = "http://onlinebooks.library.upenn.edu/webbin/book/search";

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
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("tmode", "words")
                    .queryParam("tterm", title);

            if (authorName != null && !authorName.isBlank()) {
                builder.queryParam("amode", "words");
                builder.queryParam("aterm", authorName);
            }

            String url = builder.build().toUriString();
            String html = restTemplate.getForObject(url, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from search");
            }

            // Look for book links in search results
            // Pattern to find links to actual online books (external links in results)
            Pattern bookLinkPattern = Pattern.compile(
                    "<a[^>]+href=\"(https?://[^\"]+)\"[^>]*>\\s*(?:Read|Online|View|Access)",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = bookLinkPattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.success(getProviderName(), matcher.group(1));
            }

            // Alternative: Look for any external link in the results section
            Pattern altPattern = Pattern.compile(
                    "class=\"[^\"]*bookrecord[^\"]*\"[^>]*>.*?<a[^>]+href=\"(https?://[^\"]+)\"",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

            matcher = altPattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.success(getProviderName(), matcher.group(1));
            }

            // Check if there are any results at all
            if (html.contains("No books matched") || html.contains("0 books found")) {
                return FreeTextLookupResult.error(getProviderName(), "No books found");
            }

            return FreeTextLookupResult.error(getProviderName(), "No online link found in results");

        } catch (Exception e) {
            log.warn("Online Books Page search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
