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
 * Provider for CatholicSat.com E-books list.
 * Hosts Catholic ebooks and documents.
 *
 * Website: https://www.catholicsat.com/e-books-list
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatholicSatProvider implements FreeTextProvider {

    private static final String EBOOKS_URL = "https://www.catholicsat.com/e-books-list";
    private static final String BASE_URL = "https://www.catholicsat.com";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "CatholicSat.com E-books";
    }

    @Override
    public int getPriority() {
        return 80;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String html = restTemplate.getForObject(EBOOKS_URL, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "Unable to fetch e-books page");
            }

            // Find all links and check for title matches using TitleMatcher
            Pattern linkPattern = Pattern.compile(
                    "<a[^>]+href=\"([^\"]+)\"[^>]*>([^<]+)</a>",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String href = matcher.group(1);
                String linkText = matcher.group(2);

                // Skip external links (only accept catholicsat.com content)
                if (href.startsWith("http") && !href.contains("catholicsat.com")) {
                    continue;
                }

                // Use TitleMatcher for accurate title matching
                if (TitleMatcher.titleMatches(linkText, title)) {
                    String fullUrl = href;
                    if (!href.startsWith("http")) {
                        if (href.startsWith("/")) {
                            fullUrl = BASE_URL + href;
                        } else {
                            fullUrl = EBOOKS_URL + "/" + href;
                        }
                    }
                    log.debug("CatholicSat: Found match '{}' -> {}", linkText, fullUrl);
                    return FreeTextLookupResult.success(getProviderName(), fullUrl);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in CatholicSat.com E-books");

        } catch (Exception e) {
            log.warn("CatholicSat.com search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
