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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for Catholic Planet eLibrary.
 * Hosts free Catholic ebooks and documents.
 *
 * Website: https://www.catholicplanet.com/ebooks/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatholicPlanetProvider implements FreeTextProvider {

    private static final String INDEX_URL = "https://www.catholicplanet.com/ebooks/";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Catholic Planet eLibrary";
    }

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String html = restTemplate.getForObject(INDEX_URL, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "Unable to fetch index page");
            }

            // Normalize title for searching
            String normalizedTitle = title.toLowerCase();
            String[] titleWords = normalizedTitle.split("\\s+");

            // Search for the title in the index page
            // Catholic Planet uses simple HTML lists with links
            String htmlLower = html.toLowerCase();

            // Find all links and check if any match the title
            Pattern linkPattern = Pattern.compile(
                    "<a[^>]+href=\"([^\"]+)\"[^>]*>([^<]+)</a>",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String href = matcher.group(1);
                String linkText = matcher.group(2).toLowerCase();

                // Check if link text contains key words from title
                int matchCount = 0;
                for (String word : titleWords) {
                    if (word.length() > 3 && linkText.contains(word)) {
                        matchCount++;
                    }
                }

                // If enough words match, consider it a hit
                if (matchCount >= 2 || (titleWords.length <= 2 && matchCount >= 1)) {
                    String fullUrl = href;
                    if (!href.startsWith("http")) {
                        fullUrl = INDEX_URL + href;
                    }
                    return FreeTextLookupResult.success(getProviderName(), fullUrl);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Title not found in Catholic Planet eLibrary");

        } catch (Exception e) {
            log.warn("Catholic Planet search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
