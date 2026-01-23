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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class GutenbergProvider implements FreeTextProvider {

    private static final String API_BASE = "https://gutendex.com/books/";
    private static final String EBOOK_URL_TEMPLATE = "https://www.gutenberg.org/ebooks/%d";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Project Gutenberg";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            // Search by title first, optionally with author
            String searchQuery = title;
            if (authorName != null && !authorName.isBlank()) {
                searchQuery = title + " " + authorName;
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
            log.warn("Gutenberg search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
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
