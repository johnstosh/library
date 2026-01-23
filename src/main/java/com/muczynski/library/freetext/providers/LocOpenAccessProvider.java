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
import java.util.Map;

/**
 * Provider for Library of Congress Open Access Books Collection.
 * These are books that are freely available online through the LOC.
 *
 * API documentation: https://www.loc.gov/apis/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocOpenAccessProvider implements FreeTextProvider {

    private static final String API_BASE = "https://www.loc.gov/books/";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "LOC Open Access Books";
    }

    @Override
    public int getPriority() {
        return 15;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String query = title;
            if (authorName != null && !authorName.isBlank()) {
                query = title + " " + authorName;
            }

            String url = UriComponentsBuilder.fromHttpUrl(API_BASE)
                    .queryParam("q", query)
                    .queryParam("fo", "json")
                    .queryParam("fa", "online-format:online text")
                    .queryParam("c", 10)
                    .build()
                    .toUriString();

            LocSearchResponse response = restTemplate.getForObject(url, LocSearchResponse.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No open access books found");
            }

            // Find best title match - only return if title actually matches
            // Do NOT fall back to first result (causes false positives for copyrighted works)
            for (LocResult result : response.getResults()) {
                String resultTitle = result.getTitle();
                if (resultTitle != null && TitleMatcher.titleMatches(resultTitle, title)) {
                    // Verify it has online text format (not just catalog entry)
                    List<String> formats = result.getOnlineFormat();
                    if (formats != null && formats.stream().anyMatch(f ->
                            f.toLowerCase().contains("online text") ||
                            f.toLowerCase().contains("full text") ||
                            f.toLowerCase().contains("pdf") ||
                            f.toLowerCase().contains("epub"))) {
                        String resultUrl = result.getUrl();
                        if (resultUrl != null) {
                            return FreeTextLookupResult.success(getProviderName(), resultUrl);
                        }
                    }
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "No matching title with online text found");

        } catch (Exception e) {
            log.warn("LOC Open Access search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LocSearchResponse {
        private List<LocResult> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LocResult {
        private String title;
        private String url;
        private String id;
        @JsonProperty("online_format")
        private List<String> onlineFormat;
    }
}
