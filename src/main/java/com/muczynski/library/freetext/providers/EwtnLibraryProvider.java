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
 * Provider for EWTN Catholic Library.
 * EWTN hosts a large collection of Catholic documents, books, and articles.
 *
 * Website: https://www.ewtn.com/catholicism/library
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EwtnLibraryProvider implements FreeTextProvider {

    private static final String SEARCH_URL = "https://www.ewtn.com/search";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "EWTN Catholic Library";
    }

    @Override
    public int getPriority() {
        return 40;
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

            String searchUrl = SEARCH_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            String html = restTemplate.getForObject(searchUrl, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from EWTN search");
            }

            // Find library/teachings links with their associated text and validate title
            // Pattern captures URL and link text
            Pattern linkPattern = Pattern.compile(
                    "<a[^>]+href=\"(https?://www\\.ewtn\\.com/catholicism/(?:library|teachings)/[^\"]+)\"[^>]*>([^<]+)</a>",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String href = matcher.group(1);
                String linkText = matcher.group(2);

                // Use TitleMatcher for accurate title matching
                if (TitleMatcher.titleMatches(linkText, title)) {
                    log.debug("EWTN: Found match '{}' -> {}", linkText, href);
                    return FreeTextLookupResult.success(getProviderName(), href);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in EWTN library");

        } catch (Exception e) {
            log.warn("EWTN Library search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
