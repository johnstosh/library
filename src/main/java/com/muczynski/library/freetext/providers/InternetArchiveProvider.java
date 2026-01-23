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
 * Provider for Internet Archive (archive.org).
 * Internet Archive provides free access to millions of books, including out-of-print books.
 *
 * API documentation: https://archive.org/developers/internetarchive/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternetArchiveProvider implements FreeTextProvider {

    private static final String API_BASE = "https://archive.org/advancedsearch.php";
    private static final String DETAILS_URL_TEMPLATE = "https://archive.org/details/%s";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Internet Archive";
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            // Build advanced search query
            StringBuilder query = new StringBuilder();
            query.append("mediatype:texts");
            query.append(" AND title:\"").append(escapeQuery(title)).append("\"");

            if (authorName != null && !authorName.isBlank()) {
                query.append(" AND creator:\"").append(escapeQuery(authorName)).append("\"");
            }

            String url = UriComponentsBuilder.fromHttpUrl(API_BASE)
                    .queryParam("q", query.toString())
                    .queryParam("fl[]", "identifier")
                    .queryParam("fl[]", "title")
                    .queryParam("fl[]", "creator")
                    .queryParam("output", "json")
                    .queryParam("rows", 10)
                    .build()
                    .toUriString();

            ArchiveResponse response = restTemplate.getForObject(url, ArchiveResponse.class);

            if (response == null || response.getResponse() == null ||
                response.getResponse().getDocs() == null ||
                response.getResponse().getDocs().isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No results found");
            }

            // Find best match
            for (ArchiveDoc doc : response.getResponse().getDocs()) {
                String docTitle = doc.getTitle();
                if (docTitle != null && TitleMatcher.titleMatches(docTitle, title)) {
                    String detailsUrl = String.format(DETAILS_URL_TEMPLATE, doc.getIdentifier());
                    return FreeTextLookupResult.success(getProviderName(), detailsUrl);
                }
            }

            // If exact title match failed, return first result as likely match
            if (!response.getResponse().getDocs().isEmpty()) {
                ArchiveDoc firstDoc = response.getResponse().getDocs().get(0);
                String detailsUrl = String.format(DETAILS_URL_TEMPLATE, firstDoc.getIdentifier());
                return FreeTextLookupResult.success(getProviderName(), detailsUrl);
            }

            return FreeTextLookupResult.error(getProviderName(), "Title not found");

        } catch (Exception e) {
            log.warn("Internet Archive search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }

    private String escapeQuery(String input) {
        // Escape special characters for Internet Archive query
        return input.replace("\"", "\\\"")
                .replace(":", "\\:");
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ArchiveResponse {
        private ArchiveResponseInner response;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ArchiveResponseInner {
        private Integer numFound;
        private List<ArchiveDoc> docs;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ArchiveDoc {
        private String identifier;
        private String title;
        private Object creator; // Can be String or List<String>

        public String getTitle() {
            return title;
        }
    }
}
