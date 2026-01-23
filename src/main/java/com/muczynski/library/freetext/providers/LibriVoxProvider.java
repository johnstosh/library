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
 * Provider for LibriVox, which offers free audiobook versions of public domain books.
 *
 * API documentation: https://librivox.org/api/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibriVoxProvider implements FreeTextProvider {

    private static final String API_BASE = "https://librivox.org/api/feed/audiobooks/";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "LibriVox (Audiobooks)";
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_BASE)
                    .queryParam("format", "json")
                    .queryParam("title", "^" + title); // ^ means starts with

            if (authorName != null && !authorName.isBlank()) {
                // Extract last name for author search
                String lastName = getLastName(authorName);
                builder.queryParam("author", "^" + lastName);
            }

            String url = builder.build().toUriString();
            LibriVoxResponse response = restTemplate.getForObject(url, LibriVoxResponse.class);

            if (response == null || response.getBooks() == null || response.getBooks().isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No audiobooks found");
            }

            // Find best title match
            for (LibriVoxBook book : response.getBooks()) {
                if (TitleMatcher.titleMatches(book.getTitle(), title)) {
                    return FreeTextLookupResult.success(getProviderName(), book.getUrlLibrivox());
                }
            }

            // Return first result if no exact match
            if (!response.getBooks().isEmpty()) {
                return FreeTextLookupResult.success(getProviderName(), response.getBooks().get(0).getUrlLibrivox());
            }

            return FreeTextLookupResult.error(getProviderName(), "Title not found in audiobooks");

        } catch (Exception e) {
            log.warn("LibriVox search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }

    private String getLastName(String fullName) {
        String[] parts = fullName.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : fullName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LibriVoxResponse {
        private List<LibriVoxBook> books;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LibriVoxBook {
        private String id;
        private String title;
        @JsonProperty("url_librivox")
        private String urlLibrivox;
        private List<LibriVoxAuthor> authors;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LibriVoxAuthor {
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;
    }
}
