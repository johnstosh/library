# Free Text Lookup Caching

This document describes the caching mechanism for free text lookup URLs.

## Overview

The `FreeTextLookupCache` class provides a global in-memory cache of known free text URLs for books. This cache is consulted before querying external providers, improving lookup speed and reducing external API calls.

## Data Source

The cache data was extracted from provider lookup logs in `book-lookup-reference-backup/*.log`. These logs contain the results of running the free text lookup against the library's book collection, recording which books were successfully found on each provider.

### Log Format

Each log entry follows this format:
```
========================================
Book: <title>
Author: <author>
Timestamp: <timestamp>
Duration: <ms>
----------------------------------------
Status: HIT|MISS
URL: <url>  (only present for HITs)
Error: <message>  (only present for MISSes)
========================================
```

### Providers Included

- Project Gutenberg
- Internet Archive
- LibriVox (Audiobooks)
- Library of Congress (LOC) Open Access Books
- Library of Congress Online Catalog
- Online Books Page (LOC Index)
- Christian Classics Ethereal Library (CCEL)
- Catholic Planet eLibrary
- Vatican.va

## Cache Structure

The cache uses a two-level map structure:

```java
Map<normalizedAuthor, Map<normalizedTitle, space-separated-URLs>>
```

Multiple URLs from different providers are stored as space-separated strings, allowing retrieval of all known sources for a book.

## Normalization Algorithms

The cache uses the same normalization algorithms as `TitleMatcher` to ensure consistent matching.

### Title Normalization

1. Convert to lowercase
2. Remove trailing parenthetical content (e.g., "(1877)", "(Complete Edition)")
3. Remove leading articles ("the", "a", "an")
4. Remove all punctuation except spaces
5. Collapse multiple spaces to single space
6. Trim whitespace

Example:
- "The Spiritual Exercises (1548)" -> "spiritual exercises"
- "A Tale of Two Cities" -> "tale of two cities"

### Author Normalization

1. Convert to lowercase
2. Remove all punctuation except spaces
3. Collapse multiple spaces to single space
4. Trim whitespace

Example:
- "C. A. Chardenal" -> "c a chardenal"
- "Oscar Fingal O'Flahertie Wills Wilde" -> "oscar fingal oflahertie wills wilde"

## Lookup Behavior

The `lookup(author, title)` method performs matching in this order:

1. **Exact author match**: Normalized author must match exactly
2. **Last name match**: Extract and compare last names only
3. **Title-only fallback**: Search all cached books for matching title

This allows flexible matching while prioritizing author-specific results.

## Usage

The cache is automatically consulted by `FreeTextLookupService` before querying external providers:

```java
// In FreeTextLookupService.lookupBook()
String cachedUrl = FreeTextLookupCache.lookupFirstUrl(authorName, book.getTitle());
if (cachedUrl != null) {
    // Use cached URL, skip provider queries
}
```

## Adding New Cache Entries

To add new entries to the cache, edit `FreeTextLookupCache.java` and add calls to the `add()` method in the static initializer block:

```java
add("Author Name", "Book Title",
    "https://provider1.org/url1",
    "https://provider2.org/url2");
```

Multiple URLs can be provided as additional arguments.

### Special Entries

The following entry was manually added per user request:

```java
// Catholic Church - Enchiridion of Indulgences
add("Catholic Church", "The Handbook of Indulgences",
    "https://www.vatican.va/roman_curia/tribunals/apost_penit/documents/rc_trib_appen_doc_20020826_enchiridion-indulgentiarum_lt.html");
```

## Cache Statistics

The cache provides methods for monitoring:

- `getAuthorCount()`: Number of unique authors
- `getBookCount()`: Total number of author/title combinations

These are logged at startup:
```
Free text cache contains 45 authors with 60 books
```

## Performance Impact

- **Cache hit**: Instant return, no external API calls
- **Cache miss**: Falls through to normal provider queries

The cache reduces redundant lookups for commonly searched books and provides instant results for the library's existing collection.

## Files

- `src/main/java/com/muczynski/library/freetext/FreeTextLookupCache.java` - Cache implementation
- `src/test/java/com/muczynski/library/freetext/FreeTextLookupCacheTest.java` - Unit tests
- `src/main/java/com/muczynski/library/freetext/FreeTextLookupService.java` - Service using the cache
