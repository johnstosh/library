/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.freetext.FreeTextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for EWTN Catholic Library.
 * EWTN hosts a large collection of Catholic documents, books, and articles.
 *
 * Website: https://www.ewtn.com/catholicism/library
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EwtnLibraryProvider implements FreeTextProvider {

    private static final String SEARCH_URL = "https://www.ewtn.com/search";

    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "EWTN Catholic Library";
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String query = title;
            if (authorName != null && !authorName.isBlank()) {
                query = title + " " + authorName;
            }

            String searchUrl = SEARCH_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            String html = restTemplate.getForObject(searchUrl, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "No response from EWTN search");
            }

            // Look for library links in search results
            // EWTN library URLs contain "/catholicism/library/"
            Pattern libraryPattern = Pattern.compile(
                    "<a[^>]+href=\"(https?://www\\.ewtn\\.com/catholicism/library/[^\"]+)\"",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = libraryPattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.success(getProviderName(), matcher.group(1));
            }

            // Also check for teachings/documents
            Pattern teachingsPattern = Pattern.compile(
                    "<a[^>]+href=\"(https?://www\\.ewtn\\.com/catholicism/teachings/[^\"]+)\"",
                    Pattern.CASE_INSENSITIVE);

            matcher = teachingsPattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.success(getProviderName(), matcher.group(1));
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in EWTN library");

        } catch (Exception e) {
            log.warn("EWTN Library search failed: {}", e.getMessage());
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + e.getMessage());
        }
    }
}
