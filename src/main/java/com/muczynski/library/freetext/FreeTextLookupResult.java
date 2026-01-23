/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result from a single provider's search for free online text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreeTextLookupResult {

    /**
     * Name of the provider that performed the search
     */
    private String providerName;

    /**
     * Whether a free text version was found
     */
    private boolean found;

    /**
     * URL to the free text version (set if found=true)
     */
    private String url;

    /**
     * Error message explaining why the search failed (set if found=false)
     * Examples: "Title not found", "Author not found", "Not found in provider"
     */
    private String errorMessage;

    /**
     * Create an error result with the given message.
     */
    public static FreeTextLookupResult error(String providerName, String errorMessage) {
        return FreeTextLookupResult.builder()
                .providerName(providerName)
                .found(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Create a success result with the found URL.
     */
    public static FreeTextLookupResult success(String providerName, String url) {
        return FreeTextLookupResult.builder()
                .providerName(providerName)
                .found(true)
                .url(url)
                .build();
    }
}
