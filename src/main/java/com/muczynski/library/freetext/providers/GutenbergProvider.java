/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.freetext.FreeTextProvider;
import com.muczynski.library.freetext.TitleMatcher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Provider for Project Gutenberg using the Gutendex API.
 * Gutenberg offers free ebooks that are in the public domain.
 *
 * API documentation: https://gutendex.com/
 */
@Component
@Slf4j
public class GutenbergProvider implements FreeTextProvider {

    private static final String API_BASE = "https://gutendex.com/books/";
    private static final String EBOOK_URL_TEMPLATE = "https://www.gutenberg.org/ebooks/%d";

    @Autowired
    @Qualifier("providerRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Project Gutenberg";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public List<String> getExpectedDomains() {
        return List.of("gutenberg.org");
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            // Normalize title for API search (removes articles, short words, punctuation)
            String searchQuery = TitleMatcher.normalizeForSearch(title);
            if (authorName != null && !authorName.isBlank()) {
                // Add author's last name to improve search accuracy
                String[] authorParts = authorName.split("\\s+");
                String lastName = authorParts[authorParts.length - 1];
                searchQuery = searchQuery + " " + lastName;
            }

            String url = UriComponentsBuilder.fromHttpUrl(API_BASE)
                    .queryParam("search", searchQuery)
                    .build()
                    .toUriString();

            GutendexResponse response = restTemplate.getForObject(url, GutendexResponse.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No results found");
            }

            // Find best title match
            for (GutendexBook book : response.getResults()) {
                if (TitleMatcher.titleMatches(book.getTitle(), title)) {
                    // Verify author if provided
                    if (authorName != null && !authorName.isBlank()) {
                        boolean authorFound = false;
                        if (book.getAuthors() != null) {
                            for (GutendexAuthor author : book.getAuthors()) {
                                if (TitleMatcher.authorMatches(author.getName(), authorName)) {
                                    authorFound = true;
                                    break;
                                }
                            }
                        }
                        if (!authorFound) {
                            continue; // Skip this result, author doesn't match
                        }
                    }

                    String ebookUrl = String.format(EBOOK_URL_TEMPLATE, book.getId());
                    return FreeTextLookupResult.success(getProviderName(), ebookUrl);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Title not found in results");

        } catch (Exception e) {
            // Get root cause for better error messages (e.g., SocketTimeoutException)
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
            log.warn("Gutenberg search failed: {}", rootMessage);
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + rootMessage);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GutendexResponse {
        private Integer count;
        private List<GutendexBook> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GutendexBook {
        private Integer id;
        private String title;
        private List<GutendexAuthor> authors;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GutendexAuthor {
        private String name;
        @JsonProperty("birth_year")
        private Integer birthYear;
        @JsonProperty("death_year")
        private Integer deathYear;
    }
}
