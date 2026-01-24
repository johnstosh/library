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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provider for Catholic Planet eLibrary.
 * Hosts free Catholic ebooks and documents.
 *
 * Website: https://www.catholicplanet.com/ebooks/
 */
@Component
@Slf4j
public class CatholicPlanetProvider implements FreeTextProvider {

    private static final String INDEX_URL = "https://www.catholicplanet.com/ebooks/";

    @Autowired
    @Qualifier("providerRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public String getProviderName() {
        return "Catholic Planet eLibrary";
    }

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String html = restTemplate.getForObject(INDEX_URL, String.class);

            if (html == null || html.isEmpty()) {
                return FreeTextLookupResult.error(getProviderName(), "Unable to fetch index page");
            }

            // Find all links and check if any match the title
            Pattern linkPattern = Pattern.compile(
                    "<a[^>]+href=\"([^\"]+)\"[^>]*>([^<]+)</a>",
                    Pattern.CASE_INSENSITIVE);

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String href = matcher.group(1);
                String linkText = matcher.group(2);

                // Skip external links (only accept catholicplanet.com content)
                if (href.startsWith("http") && !href.contains("catholicplanet.com")) {
                    continue;
                }

                // Skip non-ebook links (must be .pdf, .htm, .html, or relative path)
                String hrefLower = href.toLowerCase();
                if (href.startsWith("http") && !hrefLower.endsWith(".pdf") &&
                        !hrefLower.endsWith(".htm") && !hrefLower.endsWith(".html")) {
                    continue;
                }

                // Use TitleMatcher for accurate title matching
                if (TitleMatcher.titleMatches(linkText, title)) {
                    String fullUrl = href;
                    if (!href.startsWith("http")) {
                        fullUrl = INDEX_URL + href;
                    }
                    log.debug("Catholic Planet: Found match '{}' -> {}", linkText, fullUrl);
                    return FreeTextLookupResult.success(getProviderName(), fullUrl);
                }
            }

            return FreeTextLookupResult.error(getProviderName(), "Title not found in Catholic Planet eLibrary");

        } catch (Exception e) {
            // Get root cause for better error messages (e.g., SocketTimeoutException)
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String rootMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
            log.warn("Catholic Planet search failed: {}", rootMessage);
            return FreeTextLookupResult.error(getProviderName(), "Search error: " + rootMessage);
        }
    }
}
