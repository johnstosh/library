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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for TraditionalCatholic.co Free Catholic Books section.
 * Hosts traditional Catholic books and documents.
 *
 * Website: https://www.traditionalcatholic.co/free-catholic-books/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TraditionalCatholicProvider implements FreeTextProvider {

    private static final String INDEX_URL = "https://www.traditionalcatholic.co/free-catholic-books/";
    private static final String BASE_URL = "https://www.traditionalcatholic.co";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "TraditionalCatholic.co";
    }

    @Override
    public int getPriority() {
        return 75;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String html = restTemplate.getForObject(INDEX_URL, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "Unable to fetch books page");
            }

            // Find all links and check for title matches using TitleMatcher
            Pattern linkPattern = Pattern.compile(
                    "<a[^>]+href=\"([^\"]+)\"[^>]*>([^<]+)</a>",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String href = matcher.group(1);
                String linkText = matcher.group(2);

                // Skip external links (only accept traditionalcatholic.co content)
                if (href.startsWith("http") && !href.contains("traditionalcatholic.co")) {
                    continue;
                }

                // Use TitleMatcher for accurate title matching
                if (TitleMatcher.titleMatches(linkText, title)) {
                    String fullUrl = href;
                    if (!href.startsWith("http")) {
                        if (href.startsWith("/")) {
                            fullUrl = BASE_URL + href;
                        } else {
                            fullUrl = INDEX_URL + href;
                        }
                    }
                    log.debug("TraditionalCatholic: Found match '{}' -> {}", linkText, fullUrl);
                    return FreeTextLookupResult.success(getProviderName(), fullUrl);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in TraditionalCatholic.co");

        } catch (Exception e) {
            log.warn("TraditionalCatholic.co search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
