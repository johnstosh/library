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

            // Normalize title for searching
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
                            fullUrl = INDEX_URL + href;
                        }
                    }
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
