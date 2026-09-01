/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext.providers;

import com.muczynski.library.freetext.FreeTextLookupResult;
import com.muczynski.library.freetext.FreeTextProvider;
import com.muczynski.library.freetext.TitleMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for the EWTN Catholic Library.
 * EWTN site search is a JavaScript Google CSE widget that does not expose library
 * results in HTML, so this provider matches titles against the public sitemap.
 *
 * Website: https://www.ewtn.com/catholicism/library
 */
@Component
@Slf4j
public class EwtnLibraryProvider implements FreeTextProvider {

    static final String SITEMAP_URL = "https://www.ewtn.com/sitemap.xml";
    private static final String LIBRARY_PREFIX = "https://www.ewtn.com/catholicism/library/";

    private static final Pattern LIBRARY_LOC = Pattern.compile(
            "<loc>(https://www\\.ewtn\\.com/catholicism/library/[a-z0-9-]+-\\d+)</loc>",
            Pattern.CASE_INSENSITIVE);

    @Autowired
    @Qualifier("providerRestTemplate")
    private RestTemplate restTemplate;

    /**
     * Cached library document URLs from the sitemap. Loaded on first search.
     */
    private volatile List<String> libraryUrls;

    @Override
    public String getProviderName() {
        return "EWTN Catholic Library";
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    public List<String> getExpectedDomains() {
        return List.of("ewtn.com");
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            List<String> urls = loadLibraryUrls();
            if (urls.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "Unable to load EWTN library sitemap");
            }

            String fuzzyMatch = null;
            for (String url : urls) {
                String slugTitle = slugToTitle(url);
                if (slugTitle.isBlank() || !TitleMatcher.titleMatches(slugTitle, title)) {
                    continue;
                }
                if (TitleMatcher.normalizeForComparison(slugTitle)
                        .equals(TitleMatcher.normalizeForComparison(title))) {
                    log.debug("EWTN: Exact match '{}' -> {}", slugTitle, url);
                    return FreeTextLookupResult.success(getProviderName(), url);
                }
                if (fuzzyMatch == null) {
                    fuzzyMatch = url;
                }
            }

            if (fuzzyMatch != null) {
                log.debug("EWTN: Fuzzy match -> {}", fuzzyMatch);
                return FreeTextLookupResult.success(getProviderName(), fuzzyMatch);
            }

            return FreeTextLookupResult.error(getProviderName(), "Not found in EWTN library");

        } catch (Exception e) {
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
            log.warn("EWTN search failed: {}", rootMessage);
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + rootMessage);
        }
    }

    private List<String> loadLibraryUrls() {
        List<String> cached = libraryUrls;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (libraryUrls != null) {
                return libraryUrls;
            }
            log.info("EWTN: Fetching sitemap {}", SITEMAP_URL);
            long startTime = System.currentTimeMillis();
            String xml = restTemplate.getForObject(SITEMAP_URL, String.class);
            long elapsed = System.currentTimeMillis() - startTime;
            if (xml == null || xml.isBlank()) {
                log.warn("EWTN: Empty sitemap response after {}ms", elapsed);
                return List.of();
            }

            List<String> urls = new ArrayList<>();
            Matcher matcher = LIBRARY_LOC.matcher(xml);
            while (matcher.find()) {
                urls.add(matcher.group(1));
            }
            log.info("EWTN: Sitemap parsed in {}ms, {} library URLs", elapsed, urls.size());
            libraryUrls = List.copyOf(urls);
            return libraryUrls;
        }
    }

    /**
     * Convert an EWTN library URL slug into a title-like string.
     * {@code .../wisdom-of-fr-brown-10889} becomes {@code wisdom of fr brown}.
     */
    static String slugToTitle(String url) {
        if (url == null || !url.startsWith(LIBRARY_PREFIX)) {
            return "";
        }
        String slug = url.substring(LIBRARY_PREFIX.length());
        slug = slug.replaceFirst("-\\d+$", "");
        return slug.replace('-', ' ').trim();
    }
}
