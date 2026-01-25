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
import java.util.Map;

/**
 * Provider for Library of Congress Open Access Books Collection.
 * These are books that are freely available online through the LOC.
 *
 * API documentation: https://www.loc.gov/apis/
 */
@Component
@Slf4j
public class LocOpenAccessProvider implements FreeTextProvider {

    private static final String API_BASE = "https://www.loc.gov/books/";

    @Autowired
    @Qualifier("providerRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "LOC Open Access Books";
    }

    @Override
    public int getPriority() {
        return 15;
    }

    @Override
    public List<String> getExpectedDomains() {
        return List.of("loc.gov");
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

            String url = UriComponentsBuilder.fromHttpUrl(API_BASE)
                    .queryParam("q", query)
                    .queryParam("fo", "json")
                    .queryParam("fa", "online-format:online text")
                    .queryParam("c", 10)
                    .build()
                    .toUriString();

            log.debug("LOC Open Access search URL: {}", url);

            LocSearchResponse response = restTemplate.getForObject(url, LocSearchResponse.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                log.debug("LOC Open Access: No results returned for '{}'", title);
                return FreeTextLookupResult.error(getProviderName(), "No open access books found");
            }

            log.debug("LOC Open Access: Got {} results for '{}'", response.getResults().size(), title);

            // Find best title match - only return if title actually matches
            // Do NOT fall back to first result (causes false positives for copyrighted works)
            for (LocResult result : response.getResults()) {
                String resultTitle = result.getTitle();
                boolean titleMatches = resultTitle != null && TitleMatcher.titleMatches(resultTitle, title);
                log.debug("LOC Open Access: Checking '{}' vs '{}' - match: {}", resultTitle, title, titleMatches);

                if (titleMatches) {
                    // Verify it has online text format (not just catalog entry)
                    List<String> formats = result.getOnlineFormat();
                    log.debug("LOC Open Access: Formats for '{}': {}", resultTitle, formats);

                    if (formats != null && formats.stream().anyMatch(f ->
                            f.toLowerCase().contains("online text") ||
                            f.toLowerCase().contains("full text") ||
                            f.toLowerCase().contains("pdf") ||
                            f.toLowerCase().contains("epub"))) {
                        String resultUrl = result.getUrl();
                        if (resultUrl != null) {
                            log.info("LOC Open Access: Found match for '{}' -> {}", title, resultUrl);
                            return FreeTextLookupResult.success(getProviderName(), resultUrl);
                        }
                    }
                }
            }

            log.debug("LOC Open Access: No matching title with online text found for '{}'", title);
            return FreeTextLookupResult.error(getProviderName(), "No matching title with online text found");

        } catch (Exception e) {
            // Get root cause for better error messages (e.g., SocketTimeoutException)
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
            log.warn("LOC Open Access search failed: {}", rootMessage);
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + rootMessage);
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
