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

            // Normalize title for searching (get main title before colon)
            String searchTerm = title.split(":")[0].trim().toLowerCase();
            String[] searchWords = searchTerm.split("\\s+");

            // Find all links and check for title matches
            Pattern linkPattern = Pattern.compile(
                    "<a[^>]+href=\"([^\"]+)\"[^>]*>([^<]+)</a>",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String href = matcher.group(1);
                String linkText = matcher.group(2).toLowerCase();

                // Check if link text contains key words from title
                int matchCount = 0;
                for (String word : searchWords) {
                    if (word.length() > 3 && linkText.contains(word)) {
                        matchCount++;
                    }
                }

                // If enough words match, consider it a hit
                if (matchCount >= 2 || (searchWords.length <= 2 && matchCount >= 1)) {
                    String fullUrl = href;
                    if (!href.startsWith("http")) {
                        if (href.startsWith("/")) {
                            fullUrl = BASE_URL + href;
                        } else {
                            fullUrl = BOOKS_URL + href;
                        }
                    }
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
