/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

/**
 * Interface for providers that search for free online text versions of books.
 * Each implementation handles a specific website with its own API or scraping approach.
 */
public interface FreeTextProvider {

    /**
     * Get the display name of this provider.
     *
     * @return provider name for display and logging
     */
    String getProviderName();

    /**
     * Search for a book by title and author.
     *
     * @param title      the book title
     * @param authorName the author name (may be null)
     * @return lookup result with either URL or error message
     */
    FreeTextLookupResult search(String title, String authorName);

    /**
     * Get the priority order for this provider.
     * Lower numbers are searched first (0-100).
     * Providers with comprehensive catalogs and reliable APIs should have lower priorities.
     *
     * @return priority number (lower = higher priority)
     */
    int getPriority();
}
