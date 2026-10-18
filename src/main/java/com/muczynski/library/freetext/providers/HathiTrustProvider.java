/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.freetext.FreeTextProvider;
import com.muczynski.library.freetext.TitleMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provider for HathiTrust full-view volumes.
 * <p>
 * HathiTrust's catalog search page sits behind a browser challenge, but its Bib API
 * answers by OCLC or LCCN number without one. This provider:
 * <ol>
 *   <li>asks Open Library for editions with the same title and author and collects
 *       their OCLC and LCCN numbers,</li>
 *   <li>asks the HathiTrust Bib API which volumes carry those numbers, and</li>
 *   <li>returns the first volume whose catalog title matches and whose US rights are
 *       "Full view" (public domain, or public domain in the US, since the library is
 *       in the US).</li>
 * </ol>
 * The catalog title is checked again because an OCLC number on Open Library is
 * occasionally attached to an unrelated record.
 * <p>
 * Bib API: https://www.hathitrust.org/member-libraries/resources-for-librarians/data-resources/bibliographic-api/
 */
@Component
@Slf4j
public class HathiTrustProvider implements FreeTextProvider {

    static final String OPEN_LIBRARY_SEARCH = "https://openlibrary.org/search.json";
    static final String BIB_API = "https://catalog.hathitrust.org/api/volumes/brief/json/";
    static final String HANDLE_URL = "https://hdl.handle.net/2027/";
    static final String FULL_VIEW = "Full view";

    /** Identifiers sent to the Bib API in one request. */
    private static final int BATCH = 20;
    /** At most this many identifiers are checked per search. */
    private static final int MAX_IDS = 60;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("providerRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "HathiTrust";
    }

    @Override
    public int getPriority() {
        return 18;
    }

    @Override
    public List<String> getExpectedDomains() {
        return List.of("hdl.handle.net", "hathitrust.org");
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String mainTitle = TitleMatcher.mainTitle(title);
            if (mainTitle.isBlank()) {
                return FreeTextLookupResult.error(getProviderName(), "No title");
            }

            List<String> ids = editionIdentifiers(title, mainTitle, authorName);
            if (ids.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No matching catalog editions");
            }

            for (int i = 0; i < ids.size(); i += BATCH) {
                List<String> batch = ids.subList(i, Math.min(ids.size(), i + BATCH));
                String htid = fullViewVolume(batch, title);
                if (htid != null) {
                    return FreeTextLookupResult.success(getProviderName(), HANDLE_URL + htid);
                }
            }
            return FreeTextLookupResult.error(getProviderName(), "No full-view volume");

        } catch (Exception e) {
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
            log.warn("HathiTrust search failed: {}", rootMessage);
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + rootMessage);
        }
    }

    /**
     * OCLC and LCCN numbers ("oclc:123", "lccn:456") of Open Library editions whose
     * title and author match the book.
     */
    private List<String> editionIdentifiers(String title, String mainTitle, String authorName) throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(OPEN_LIBRARY_SEARCH)
                .queryParam("title", mainTitle)
                .queryParam("fields", "title,subtitle,author_name,oclc,lccn")
                .queryParam("limit", 20);
        if (authorName != null && !authorName.isBlank()) {
            builder.queryParam("author", authorName);
        }
        URI uri = builder.encode().build().toUri();
        String body = restTemplate.getForObject(uri, String.class);
        if (body == null || body.isBlank()) {
            return List.of();
        }

        Set<String> ids = new LinkedHashSet<>();
        for (JsonNode doc : objectMapper.readTree(body).path("docs")) {
            String docTitle = doc.path("title").asText("");
            String docSubtitle = doc.path("subtitle").asText("");
            String fullDocTitle = docSubtitle.isBlank() ? docTitle : docTitle + ": " + docSubtitle;
            if (!TitleMatcher.titleMatches(fullDocTitle, title) && !TitleMatcher.titleMatches(docTitle, title)) {
                continue;
            }
            if (authorName != null && !authorName.isBlank() && !anyAuthorMatches(doc.path("author_name"), authorName)) {
                continue;
            }
            for (JsonNode oclc : doc.path("oclc")) {
                ids.add("oclc:" + oclc.asText().trim());
            }
            for (JsonNode lccn : doc.path("lccn")) {
                ids.add("lccn:" + lccn.asText().trim());
            }
        }
        List<String> list = new ArrayList<>(ids);
        return list.size() > MAX_IDS ? list.subList(0, MAX_IDS) : list;
    }

    private static boolean anyAuthorMatches(JsonNode authors, String authorName) {
        for (JsonNode author : authors) {
            if (TitleMatcher.authorMatches(author.asText(""), authorName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ask the Bib API about a batch of identifiers and return the HathiTrust id of a
     * full-view volume whose record title matches, preferring public domain ("pd")
     * over public domain in the US only ("pdus").
     */
    private String fullViewVolume(List<String> batch, String title) throws Exception {
        // The Bib API separates identifiers with "|", which must be sent encoded.
        URI uri = URI.create(BIB_API + String.join("%7C", batch.stream()
                .map(id -> id.replaceAll("[^A-Za-z0-9:]", ""))
                .toList()));
        String body = restTemplate.getForObject(uri, String.class);
        if (body == null || body.isBlank()) {
            return null;
        }

        String usOnly = null;
        JsonNode root = objectMapper.readTree(body);
        for (JsonNode answer : root) {
            JsonNode records = answer.path("records");
            for (JsonNode item : answer.path("items")) {
                if (!FULL_VIEW.equalsIgnoreCase(item.path("usRightsString").asText(""))) {
                    continue;
                }
                JsonNode record = records.path(item.path("fromRecord").asText(""));
                if (!recordTitleMatches(record, title)) {
                    continue;
                }
                String htid = item.path("htid").asText("");
                if (htid.isBlank() || !htid.matches("[A-Za-z0-9.$:/_-]+")) {
                    continue;
                }
                if (!"pdus".equalsIgnoreCase(item.path("rightsCode").asText(""))) {
                    return htid;
                }
                if (usOnly == null) {
                    usOnly = htid;
                }
            }
        }
        return usOnly;
    }

    private static boolean recordTitleMatches(JsonNode record, String title) {
        for (JsonNode recordTitle : record.path("titles")) {
            if (TitleMatcher.titleMatches(recordTitle.asText(""), title)) {
                return true;
            }
        }
        return false;
    }
}
