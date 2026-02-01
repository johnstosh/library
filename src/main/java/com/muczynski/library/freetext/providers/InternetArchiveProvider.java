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
 * Provider for Internet Archive (archive.org).
 * Internet Archive provides free access to millions of books, including out-of-print books.
 *
 * API documentation: https://archive.org/developers/internetarchive/
 */
@Component
@Slf4j
public class InternetArchiveProvider implements FreeTextProvider {

    private static final String API_BASE = "https://archive.org/advancedsearch.php";
    private static final String DETAILS_URL_TEMPLATE = "https://archive.org/details/%s";

    @Autowired
    @Qualifier("providerRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Internet Archive";
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public List<String> getExpectedDomains() {
        return List.of("archive.org");
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
                    .queryParam("fl[]", "lending___status") // Check if it's a lending item
                    .queryParam("output", "json")
                    .queryParam("rows", 20)
                    .build()
                    .toUriString();

            ArchiveResponse response = restTemplate.getForObject(url, ArchiveResponse.class);

            if (response == null || response.getResponse() == null ||
                response.getResponse().getDocs() == null ||
                response.getResponse().getDocs().isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No results found");
            }

            // Find best match - only return if title AND author actually match
            // Do NOT fall back to first result (causes false positives for copyrighted works)
            for (ArchiveDoc doc : response.getResponse().getDocs()) {
                String docTitle = doc.getTitle();
                if (docTitle != null && TitleMatcher.titleMatches(docTitle, title)) {
                    // Check if this is a freely readable item (not lending-only)
                    if (!doc.isFreelyReadable()) {
                        log.debug("Skipping lending-only item: {}", doc.getIdentifier());
                        continue;
                    }

                    // If author was provided, verify it matches
                    if (authorName != null && !authorName.isBlank()) {
                        String docCreator = doc.getCreatorString();
                        if (docCreator == null || !TitleMatcher.authorMatches(docCreator, authorName)) {
                            log.debug("Skipping item with non-matching author: {} vs {}", docCreator, authorName);
                            continue;
                        }
                    }

                    String detailsUrl = String.format(DETAILS_URL_TEMPLATE, doc.getIdentifier());
                    return FreeTextLookupResult.success(getProviderName(), detailsUrl);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "No matching title found");

        } catch (Exception e) {
            // Get root cause for better error messages (e.g., SocketTimeoutException)
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
            log.warn("Internet Archive search failed: {}", rootMessage);
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + rootMessage);
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
        @JsonProperty("lending___status")
        private Object lendingStatus; // Can be String or List<String>

        public String getTitle() {
            return title;
        }

        /**
         * Get creator as a string (handles both String and List<String> from API)
         */
        public String getCreatorString() {
            if (creator == null) {
                return null;
            }
            if (creator instanceof String) {
                return (String) creator;
            }
            if (creator instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> creatorList = (List<String>) creator;
                return creatorList.isEmpty() ? null : creatorList.get(0);
            }
            return creator.toString();
        }

        /**
         * Check if this item is freely readable (not lending-only or user upload).
         *
         * Internet Archive items come from two sources:
         * 1. Official library scanning programs - have "is_readable" status for public domain
         * 2. User uploads - no lending status, may include unauthorized copyrighted content
         *
         * Lending status values:
         * - "is_readable": freely readable, official public domain scan
         * - "is_printdisabled"/"is_lendable"/"is_borrowable": lending only (copyrighted)
         * - null/empty: potentially unauthorized user upload - DO NOT TRUST
         *
         * We ONLY trust items that have explicit "is_readable" status.
         * This avoids returning links to unauthorized uploads of copyrighted works.
         *
         * @return true if the item is officially marked as freely readable
         */
        public boolean isFreelyReadable() {
            // Only trust items with explicit "is_readable" status
            // This filters out:
            // - Lending-only items (is_printdisabled, etc.)
            // - User uploads without proper status (potential copyright violations)
            return hasExplicitReadableStatus();
        }

        private boolean hasExplicitReadableStatus() {
            if (lendingStatus == null) {
                return false;
            }
            if (lendingStatus instanceof String) {
                return ((String) lendingStatus).toLowerCase().contains("readable");
            }
            if (lendingStatus instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> statusList = (List<Object>) lendingStatus;
                for (Object status : statusList) {
                    if (status != null && status.toString().toLowerCase().contains("readable")) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
