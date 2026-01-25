/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FreeTextLookupCache.
 */
class FreeTextLookupCacheTest {

    @Test
    void testNormalizeTitle() {
        // Basic normalization
        assertThat(FreeTextLookupCache.normalizeTitle("The Spiritual Exercises"))
                .isEqualTo("spiritual exercises");

        // Remove leading article
        assertThat(FreeTextLookupCache.normalizeTitle("A Tale of Two Cities"))
                .isEqualTo("tale of two cities");

        // Remove punctuation
        assertThat(FreeTextLookupCache.normalizeTitle("Pilgrim's Progress"))
                .isEqualTo("pilgrims progress");

        // Remove trailing parenthetical content
        assertThat(FreeTextLookupCache.normalizeTitle("Anna Karenina (1877)"))
                .isEqualTo("anna karenina");

        // Handle subtitles (colon becomes nothing after punctuation removal)
        assertThat(FreeTextLookupCache.normalizeTitle("The Handbook of Indulgences: Norms and Grants"))
                .isEqualTo("handbook of indulgences norms and grants");
    }

    @Test
    void testNormalizeAuthor() {
        // Basic normalization
        assertThat(FreeTextLookupCache.normalizeAuthor("Louisa May Alcott"))
                .isEqualTo("louisa may alcott");

        // Remove punctuation (periods)
        assertThat(FreeTextLookupCache.normalizeAuthor("C. A. Chardenal"))
                .isEqualTo("c a chardenal");

        // Remove punctuation (commas)
        assertThat(FreeTextLookupCache.normalizeAuthor("Dickens, Charles"))
                .isEqualTo("dickens charles");
    }

    @Test
    void testLookupExactMatch() {
        // Exact author and title match
        String urls = FreeTextLookupCache.lookup("Louisa May Alcott", "Eight Cousins");
        assertThat(urls).isNotNull();
        assertThat(urls).contains("gutenberg.org");
        assertThat(urls).contains("archive.org");
    }

    @Test
    void testLookupFirstUrl() {
        // Should return first URL only
        String url = FreeTextLookupCache.lookupFirstUrl("Louisa May Alcott", "Eight Cousins");
        assertThat(url).isNotNull();
        assertThat(url).doesNotContain(" ");
        assertThat(url).startsWith("https://");
    }

    @Test
    void testLookupWithLeadingArticle() {
        // Title with "The" should match
        String url = FreeTextLookupCache.lookupFirstUrl("Ignatius of Loyola", "The Spiritual Exercises");
        assertThat(url).isNotNull();

        // Title without "The" should also match
        String url2 = FreeTextLookupCache.lookupFirstUrl("Ignatius of Loyola", "Spiritual Exercises");
        assertThat(url2).isNotNull();
        assertThat(url).isEqualTo(url2);
    }

    @Test
    void testLookupByLastNameOnly() {
        // Should find by last name match
        String url = FreeTextLookupCache.lookupFirstUrl("Stevenson", "Kidnapped");
        assertThat(url).isNotNull();
    }

    @Test
    void testLookupTitleOnlyFallback() {
        // Should find by title alone when author doesn't match
        String url = FreeTextLookupCache.lookupFirstUrl("Unknown Author", "Eight Cousins");
        assertThat(url).isNotNull();
    }

    @Test
    void testLookupNotFound() {
        // Non-existent book
        String url = FreeTextLookupCache.lookup("Nobody", "This Book Does Not Exist");
        assertThat(url).isNull();
    }

    @Test
    void testLookupNullTitle() {
        String url = FreeTextLookupCache.lookup("Author", null);
        assertThat(url).isNull();
    }

    @Test
    void testLookupBlankTitle() {
        String url = FreeTextLookupCache.lookup("Author", "   ");
        assertThat(url).isNull();
    }

    @Test
    void testLookupNullAuthor() {
        // Should still work with null author (title-only search)
        String url = FreeTextLookupCache.lookupFirstUrl(null, "Little Women");
        assertThat(url).isNotNull();
    }

    @Test
    void testEnchiridionOfIndulgences() {
        // User-requested Vatican.va entry
        String url = FreeTextLookupCache.lookupFirstUrl("Catholic Church", "The Handbook of Indulgences");
        assertThat(url).isNotNull();
        assertThat(url).contains("vatican.va");
        assertThat(url).contains("enchiridion-indulgentiarum");
    }

    @Test
    void testEnchiridionWithSubtitle() {
        // Should also match with subtitle
        String url = FreeTextLookupCache.lookupFirstUrl("Catholic Church", "The Handbook of Indulgences: Norms and Grants");
        assertThat(url).isNotNull();
        assertThat(url).contains("vatican.va");
    }

    @Test
    void testCacheStatistics() {
        assertThat(FreeTextLookupCache.getAuthorCount()).isGreaterThan(0);
        assertThat(FreeTextLookupCache.getBookCount()).isGreaterThan(0);
        assertThat(FreeTextLookupCache.getBookCount()).isGreaterThanOrEqualTo(FreeTextLookupCache.getAuthorCount());
    }

    @Test
    void testMultipleAuthorsForSameTitle() {
        // Charles Dickens has variations in the cache
        String url1 = FreeTextLookupCache.lookupFirstUrl("Charles Dickens", "A Tale of Two Cities");
        String url2 = FreeTextLookupCache.lookupFirstUrl("Charles John Huffam Dickens", "A Tale of Two Cities");

        assertThat(url1).isNotNull();
        assertThat(url2).isNotNull();
        // Both should return valid URLs (may be same or different depending on cache structure)
    }

    @Test
    void testPopeFrancisLumenFidei() {
        String url = FreeTextLookupCache.lookupFirstUrl("Pope Francis", "Lumen Fidei");
        assertThat(url).isNotNull();
        assertThat(url).contains("vatican.va");
    }

    @Test
    void testBrotherLawrence() {
        String url = FreeTextLookupCache.lookupFirstUrl("Brother Lawrence", "The Practice of the Presence of God");
        assertThat(url).isNotNull();
    }

    /**
     * Test that books marked as "not found" return an empty string, not null.
     * This allows distinguishing between "never searched" (null) and "searched but not found" (empty string).
     */
    @Test
    void testNotFoundCaching() {
        // "My Treasured Catholic Prayers" was searched but has no free text - should return empty string
        String result = FreeTextLookupCache.lookup("Lawrence G. Lovasik", "My Treasured Catholic Prayers");
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testNotFoundCachingVsNeverSearched() {
        // Book that was searched but not found - returns empty string
        String notFound = FreeTextLookupCache.lookup("John Anthony Hardon", "The Catholic Catechism");
        assertThat(notFound).isNotNull().isEmpty();

        // Book that was never searched - returns null
        String neverSearched = FreeTextLookupCache.lookup("Unknown Author", "This Book Was Never Searched");
        assertThat(neverSearched).isNull();
    }

    @Test
    void testNotFoundCachingMultipleBooks() {
        // Verify several books from the 0-hits list return empty strings
        assertThat(FreeTextLookupCache.lookup("Marie McSwigan", "Snow Treasure"))
                .isNotNull().isEmpty();

        assertThat(FreeTextLookupCache.lookup("Catholic Church", "The Handbook of Indulgences: Norms and Grants"))
                .isNotNull(); // This one might have a URL depending on cache

        assertThat(FreeTextLookupCache.lookup("Gabriel Denis", "The Reign of Jesus Through Mary"))
                .isNotNull().isEmpty();
    }

    @Test
    void testCacheContainsBooksFromArchive() {
        // These books should be in the cache (either with URLs or as not-found)
        // based on the branch-archive.json lookup
        assertThat(FreeTextLookupCache.getBookCount()).isGreaterThan(250);
    }
}
