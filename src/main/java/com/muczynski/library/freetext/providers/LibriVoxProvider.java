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
 * Provider for LibriVox, which offers free audiobook versions of public domain books.
 *
 * API documentation: https://librivox.org/api/
 */
@Component
@Slf4j
public class LibriVoxProvider implements FreeTextProvider {

    private static final String API_BASE = "https://librivox.org/api/feed/audiobooks/";

    @Autowired
    @Qualifier("providerRestTemplate")
    private RestTemplate restTemplate;

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
            // Normalize title for API search (removes articles, short words, punctuation)
            String searchTitle = TitleMatcher.normalizeForSearch(title);

            // Note: LibriVox API can be unreliable when combining title+author filters (returns 500),
            // so we search by title only and use TitleMatcher to validate results
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_BASE)
                    .queryParam("format", "json")
                    .queryParam("title", "^" + searchTitle); // ^ means starts with

            String url = builder.build().toUriString();
            log.debug("LibriVox search URL: {}", url);
            LibriVoxResponse response = restTemplate.getForObject(url, LibriVoxResponse.class);

            if (response == null || response.getBooks() == null || response.getBooks().isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No audiobooks found");
            }

            log.debug("LibriVox: Got {} results for '{}'", response.getBooks().size(), title);

            // Find best title match - only return if title actually matches
            // Do NOT fall back to first result (causes false positives for copyrighted works)
            for (LibriVoxBook book : response.getBooks()) {
                if (TitleMatcher.titleMatches(book.getTitle(), title)) {
                    log.debug("LibriVox: Found match '{}' -> {}", book.getTitle(), book.getUrlLibrivox());
                    return FreeTextLookupResult.success(getProviderName(), book.getUrlLibrivox());
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Title not found in audiobooks");

        } catch (Exception e) {
            // Get root cause for better error messages (e.g., SocketTimeoutException)
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
            log.warn("LibriVox search failed: {}", rootMessage);
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + rootMessage);
        }
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
