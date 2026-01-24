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
            // Normalize title for API search (removes articles, short words, punctuation)
            String query = TitleMatcher.normalizeForSearch(title);
            if (authorName != null && !authorName.isBlank()) {
                // Add author's last name to improve search accuracy
                String[] authorParts = authorName.split("\\s+");
                String lastName = authorParts[authorParts.length - 1];
                query = query + " " + lastName;
            }

            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("qu", query)
                    .build()
                    .toUriString();

            String html = restTemplate.getForObject(url, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from CCEL search");
            }

            // CCEL search results show book cards. Each card has:
            // - A div with class "card" containing the book
            // - Book link: href="https://ccel.org/ccel/{author}/{work}/{work}"
            // - Title in span within card-title: <span>Book Title</span>
            //
            // Strategy: Find each book card and extract URL+title together

            // Pattern to find book cards (div with class containing "card")
            // Each card starts with <div class="card and ends when next card starts
            Pattern cardPattern = Pattern.compile(
                    "<div[^>]+class=\"card[^\"]*mx-auto[^>]*>(.*?)</div>\\s*</div>\\s*</div>",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

            // Pattern to extract URL from card
            Pattern urlPattern = Pattern.compile(
                    "href=\"(https?://ccel\\.org/ccel/[^/]+/[^/]+/[^\"]+)\"",
                    Pattern.CASE_INSENSITIVE);

            // Pattern to extract title from card-title span
            Pattern titlePattern = Pattern.compile(
                    "<h5[^>]*class=\"[^\"]*card-title[^\"]*\"[^>]*>\\s*<span>([^<]+)</span>",
                    Pattern.CASE_INSENSITIVE);

            Matcher cardMatcher = cardPattern.matcher(html);
            while (cardMatcher.find()) {
                String cardHtml = cardMatcher.group(1);

                // Extract title from this card
                Matcher titleMatcher = titlePattern.matcher(cardHtml);
                if (titleMatcher.find()) {
                    String cardTitle = titleMatcher.group(1).trim();

                    if (TitleMatcher.titleMatches(cardTitle, title)) {
                        // Found matching title, get the URL from this card
                        Matcher urlMatcher = urlPattern.matcher(cardHtml);
                        if (urlMatcher.find()) {
                            return FreeTextLookupResult.success(getProviderName(), urlMatcher.group(1));
                        }
                    }
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in CCEL");

        } catch (Exception e) {
            log.warn("CCEL search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
