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
 * Provider for My Catholic Life! Books section.
 * Hosts Catholic devotional books and reflections.
 *
 * Website: https://mycatholic.life/books/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MyCatholicLifeProvider implements FreeTextProvider {

    private static final String BOOKS_URL = "https://mycatholic.life/books/";
    private static final String BASE_URL = "https://mycatholic.life";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "My Catholic Life! Books";
    }

    @Override
    public int getPriority() {
        return 70;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String html = restTemplate.getForObject(BOOKS_URL, String.class);

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

                // Skip external links (only accept mycatholic.life content)
                if (href.startsWith("http") && !href.contains("mycatholic.life")) {
                    continue;
                }

                // Use TitleMatcher for accurate title matching
                if (TitleMatcher.titleMatches(linkText, title)) {
                    String fullUrl = href;
                    if (!href.startsWith("http")) {
                        if (href.startsWith("/")) {
                            fullUrl = BASE_URL + href;
                        } else {
                            fullUrl = BOOKS_URL + href;
                        }
                    }
                    log.debug("My Catholic Life: Found match '{}' -> {}", linkText, fullUrl);
                    return FreeTextLookupResult.success(getProviderName(), fullUrl);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in My Catholic Life! Books");

        } catch (Exception e) {
            log.warn("My Catholic Life! search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
