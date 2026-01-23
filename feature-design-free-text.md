# Feature Design: Free Online Text Finder

## Overview

This feature provides bulk processing of selected books to find links to free online text versions. Each website is handled by a dedicated provider class that uses the specific API or technique appropriate for that site.

## Architecture

### Class Hierarchy

```
FreeTextLookupService (orchestrator)
├── FreeTextProvider (interface)
│   ├── GutenbergProvider
│   ├── InternetArchiveProvider
│   ├── VaticanProvider
│   ├── LibriVoxProvider
│   ├── EwtnLibraryProvider
│   ├── CcelProvider (Christian Classics Ethereal Library)
│   ├── CatholicPlanetProvider
│   ├── GoodCatholicBooksProvider
│   ├── MyCatholicLifeProvider
│   ├── TraditionalCatholicProvider
│   ├── CatholicSatProvider
│   ├── LocOpenAccessProvider (LOC Open Access Books Collection)
│   ├── LocCatalogOnlineProvider (Library of Congress Online Catalog)
│   └── LocOnlineBooksPageProvider (Online Books Page - LOC index)
```

### Core Interface

```java
public interface FreeTextProvider {
    /**
     * Provider name for display and logging
     */
    String getProviderName();

    /**
     * Search for a book by title and author.
     * Returns a URL if definitively found, or an error message.
     *
     * @param title Book title
     * @param authorName Author name (may be null)
     * @return FreeTextLookupResult with either URL or error
     */
    FreeTextLookupResult search(String title, String authorName);

    /**
     * Priority order (lower = searched first, 0-100)
     * Providers with comprehensive catalogs get lower priority numbers.
     */
    int getPriority();
}
```

### DTOs

```java
@Data
@Builder
public class FreeTextLookupResult {
    private String providerName;
    private boolean found;
    private String url;           // Set if found=true
    private String errorMessage;  // Set if found=false (e.g., "Title not found", "Author not found")
}

@Data
@Builder
public class FreeTextBulkLookupResultDto {
    private Long bookId;
    private String bookTitle;
    private String authorName;
    private boolean success;
    private String freeTextUrl;      // Set if success=true
    private String providerName;     // Which provider found it
    private String errorMessage;     // Set if success=false
    private List<String> providersSearched;  // All providers that were tried
}
```

---

## Provider Implementations

### 1. GutenbergProvider

**Website:** https://www.gutenberg.org

**API Approach:** REST API with JSON response

**Search Strategy:**
1. Search by author name first: `GET https://gutendex.com/books/?search={author_name}`
2. Filter results by title match
3. Return HTML URL for best match: `https://www.gutenberg.org/ebooks/{book_id}`

**API Details:**
- Gutendex is the official API: https://gutendex.com/
- Returns JSON with book metadata including `formats` map with available file URLs
- Supports search across all fields including title and author
- Rate limit: None specified, but be respectful

```java
@Service
@Slf4j
public class GutenbergProvider implements FreeTextProvider {
    private static final String API_BASE = "https://gutendex.com/books/";
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "Project Gutenberg"; }

    @Override
    public int getPriority() { return 10; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        // Step 1: Search by author if available
        String searchQuery = authorName != null ? authorName : title;
        String url = API_BASE + "?search=" + URLEncoder.encode(searchQuery, UTF_8);

        GutendexResponse response = restTemplate.getForObject(url, GutendexResponse.class);

        if (response == null || response.getResults().isEmpty()) {
            return errorResult(authorName != null ? "Author not found" : "Title not found");
        }

        // Step 2: Find best title match among results
        for (GutendexBook book : response.getResults()) {
            if (titleMatches(book.getTitle(), title)) {
                return FreeTextLookupResult.builder()
                    .providerName(getProviderName())
                    .found(true)
                    .url("https://www.gutenberg.org/ebooks/" + book.getId())
                    .build();
            }
        }

        return errorResult("Title not found in author's works");
    }
}
```

---

### 2. InternetArchiveProvider

**Website:** https://archive.org

**API Approach:** Archive.org Advanced Search API (JSON)

**Search Strategy:**
1. Search by author AND title: `https://archive.org/advancedsearch.php?q=creator:{author}+AND+title:{title}&fl=identifier,title,creator&output=json`
2. Verify match and return item URL

**API Details:**
- Free API, no authentication required
- Supports fielded search: `creator`, `title`, `subject`, `mediatype`
- Filter by `mediatype:texts` to limit to books
- Returns `identifier` which maps to: `https://archive.org/details/{identifier}`

```java
@Service
@Slf4j
public class InternetArchiveProvider implements FreeTextProvider {
    private static final String API_BASE = "https://archive.org/advancedsearch.php";
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "Internet Archive"; }

    @Override
    public int getPriority() { return 5; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        StringBuilder query = new StringBuilder();
        query.append("mediatype:texts");

        if (authorName != null && !authorName.isBlank()) {
            query.append(" AND creator:\"").append(escapeQuery(authorName)).append("\"");
        }
        query.append(" AND title:\"").append(escapeQuery(title)).append("\"");

        String url = UriComponentsBuilder.fromHttpUrl(API_BASE)
            .queryParam("q", query.toString())
            .queryParam("fl", "identifier,title,creator")
            .queryParam("output", "json")
            .queryParam("rows", 5)
            .build().toUriString();

        ArchiveResponse response = restTemplate.getForObject(url, ArchiveResponse.class);

        if (response == null || response.getResponse().getDocs().isEmpty()) {
            return errorResult(authorName != null ? "Not found by author and title" : "Title not found");
        }

        ArchiveDoc doc = response.getResponse().getDocs().get(0);
        return FreeTextLookupResult.builder()
            .providerName(getProviderName())
            .found(true)
            .url("https://archive.org/details/" + doc.getIdentifier())
            .build();
    }
}
```

---

### 3. VaticanProvider

**Website:** https://www.vatican.va/content/vatican/en/the-holy-see.html

**Approach:** Static URL structure with HTML parsing

**Search Strategy:**
1. Vatican Library documents are organized by pope/author and document type
2. No public search API - use Google site search as fallback
3. URL pattern for papal documents: `https://www.vatican.va/content/{pope}/en/...`

**Implementation:**
```java
@Service
@Slf4j
public class VaticanProvider implements FreeTextProvider {
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "Vatican.va"; }

    @Override
    public int getPriority() { return 50; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        // Vatican site search via Google Custom Search API or direct URL check
        // For papal documents, try known URL patterns first

        // Strategy: Check if this appears to be a papal/church document
        // then construct likely URLs and verify they exist

        String searchUrl = "https://www.google.com/search?q=site:vatican.va+"
            + URLEncoder.encode("\"" + title + "\"", UTF_8);

        // Alternatively, use vatican.va's internal search if available
        // This is a simplified implementation - real implementation would
        // parse search results or check known document indices

        return errorResult("Vatican search requires manual verification");
    }
}
```

---

### 4. LibriVoxProvider

**Website:** https://librivox.org

**API Approach:** LibriVox API (JSON/XML)

**Search Strategy:**
1. Search by author: `https://librivox.org/api/feed/audiobooks/?author={name}&format=json`
2. Filter results by title match

**API Details:**
- Free API, no authentication
- Supports: `author`, `title`, `genre`, `extended` parameters
- Returns audiobook metadata with URL to LibriVox page

```java
@Service
@Slf4j
public class LibriVoxProvider implements FreeTextProvider {
    private static final String API_BASE = "https://librivox.org/api/feed/audiobooks/";
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "LibriVox (Audiobooks)"; }

    @Override
    public int getPriority() { return 30; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_BASE)
            .queryParam("format", "json");

        if (authorName != null && !authorName.isBlank()) {
            builder.queryParam("author", authorName);
        }
        builder.queryParam("title", title);

        LibriVoxResponse response = restTemplate.getForObject(
            builder.build().toUriString(), LibriVoxResponse.class);

        if (response == null || response.getBooks() == null || response.getBooks().isEmpty()) {
            return errorResult("Not found in LibriVox catalog");
        }

        // Find best match
        for (LibriVoxBook book : response.getBooks()) {
            if (titleMatches(book.getTitle(), title)) {
                return FreeTextLookupResult.builder()
                    .providerName(getProviderName())
                    .found(true)
                    .url(book.getUrlLibrivox())
                    .build();
            }
        }

        return errorResult("Title not found in author's audiobooks");
    }
}
```

---

### 5. EwtnLibraryProvider

**Website:** https://www.ewtn.com/catholicism/library

**Approach:** HTML scraping of EWTN library pages

**Search Strategy:**
1. EWTN library is organized by category and author
2. Use site search: `https://www.ewtn.com/search?q={title}+{author}`
3. Parse results for library links

```java
@Service
@Slf4j
public class EwtnLibraryProvider implements FreeTextProvider {
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "EWTN Catholic Library"; }

    @Override
    public int getPriority() { return 40; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        // EWTN search URL
        String query = title + (authorName != null ? " " + authorName : "");
        String searchUrl = "https://www.ewtn.com/search?q=" + URLEncoder.encode(query, UTF_8);

        try {
            String html = restTemplate.getForObject(searchUrl, String.class);

            // Parse HTML to find library links
            // Look for links containing "/catholicism/library/"
            Pattern pattern = Pattern.compile(
                "href=\"(https?://www\\.ewtn\\.com/catholicism/library/[^\"]+)\"[^>]*>([^<]*"
                + Pattern.quote(title.split(" ")[0]) + "[^<]*)",
                Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.builder()
                    .providerName(getProviderName())
                    .found(true)
                    .url(matcher.group(1))
                    .build();
            }
        } catch (Exception e) {
            log.warn("EWTN search failed: {}", e.getMessage());
        }

        return errorResult("Not found in EWTN library");
    }
}
```

---

### 6. CcelProvider (Christian Classics Ethereal Library)

**Website:** https://www.ccel.org

**API Approach:** CCEL has a structured URL pattern and basic search

**Search Strategy:**
1. Use CCEL search: `https://www.ccel.org/search?qu={query}`
2. Parse search results for book pages
3. Book URLs follow pattern: `https://www.ccel.org/ccel/{author_short}/{title_short}`

```java
@Service
@Slf4j
public class CcelProvider implements FreeTextProvider {
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "Christian Classics Ethereal Library"; }

    @Override
    public int getPriority() { return 35; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        String query = title + (authorName != null ? " " + authorName : "");
        String searchUrl = "https://www.ccel.org/search?qu=" + URLEncoder.encode(query, UTF_8);

        try {
            String html = restTemplate.getForObject(searchUrl, String.class);

            // Parse for book result links
            // CCEL book links follow pattern /ccel/{author}/{work}
            Pattern pattern = Pattern.compile(
                "href=\"(/ccel/[^\"]+)\"[^>]*class=\"[^\"]*book[^\"]*\"",
                Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                String bookPath = matcher.group(1);
                return FreeTextLookupResult.builder()
                    .providerName(getProviderName())
                    .found(true)
                    .url("https://www.ccel.org" + bookPath)
                    .build();
            }
        } catch (Exception e) {
            log.warn("CCEL search failed: {}", e.getMessage());
        }

        return errorResult("Not found in CCEL");
    }
}
```

---

### 7. CatholicPlanetProvider

**Website:** https://www.catholicplanet.com/ebooks/

**Approach:** Index page parsing

**Search Strategy:**
1. Fetch ebook index pages organized by category
2. Search for title/author matches in index
3. Return direct link to ebook

```java
@Service
@Slf4j
public class CatholicPlanetProvider implements FreeTextProvider {
    private static final String INDEX_URL = "https://www.catholicplanet.com/ebooks/";
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "Catholic Planet eLibrary"; }

    @Override
    public int getPriority() { return 60; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String html = restTemplate.getForObject(INDEX_URL, String.class);

            // Search for title in the page content
            String titleLower = title.toLowerCase();
            Pattern linkPattern = Pattern.compile(
                "<a[^>]+href=\"([^\"]+\\.htm[l]?)\"[^>]*>([^<]*)</a>",
                Pattern.CASE_INSENSITIVE);

            Matcher matcher = linkPattern.matcher(html);
            while (matcher.find()) {
                String linkText = matcher.group(2).toLowerCase();
                if (linkText.contains(titleLower.split(" ")[0])) {
                    String href = matcher.group(1);
                    if (!href.startsWith("http")) {
                        href = INDEX_URL + href;
                    }
                    return FreeTextLookupResult.builder()
                        .providerName(getProviderName())
                        .found(true)
                        .url(href)
                        .build();
                }
            }
        } catch (Exception e) {
            log.warn("Catholic Planet search failed: {}", e.getMessage());
        }

        return errorResult("Not found in Catholic Planet eLibrary");
    }
}
```

---

### 8. GoodCatholicBooksProvider

**Website:** https://www.goodcatholicbooks.org

**Approach:** Site search or index parsing

```java
@Service
@Slf4j
public class GoodCatholicBooksProvider implements FreeTextProvider {
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "Good Catholic Books"; }

    @Override
    public int getPriority() { return 65; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        // Good Catholic Books has an author-organized structure
        // Try Google site search as primary approach
        String query = "site:goodcatholicbooks.org \"" + title + "\"";
        if (authorName != null) {
            query += " \"" + authorName + "\"";
        }

        // Implementation would use Google Custom Search API or HTML parsing
        return errorResult("Not found in Good Catholic Books");
    }
}
```

---

### 9. MyCatholicLifeProvider

**Website:** https://mycatholic.life/books/

**Approach:** Direct page structure parsing

```java
@Service
@Slf4j
public class MyCatholicLifeProvider implements FreeTextProvider {
    private static final String BOOKS_URL = "https://mycatholic.life/books/";
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "My Catholic Life! Books"; }

    @Override
    public int getPriority() { return 70; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String html = restTemplate.getForObject(BOOKS_URL, String.class);

            // Parse for book links matching title
            String searchTerm = title.split(":")[0].trim().toLowerCase();
            Pattern pattern = Pattern.compile(
                "<a[^>]+href=\"([^\"]+)\"[^>]*>([^<]*" + Pattern.quote(searchTerm) + "[^<]*)</a>",
                Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.builder()
                    .providerName(getProviderName())
                    .found(true)
                    .url(matcher.group(1))
                    .build();
            }
        } catch (Exception e) {
            log.warn("MyCatholicLife search failed: {}", e.getMessage());
        }

        return errorResult("Not found in My Catholic Life! Books");
    }
}
```

---

### 10. TraditionalCatholicProvider

**Website:** https://www.traditionalcatholic.co/free-catholic-books/

**Approach:** Index page parsing

```java
@Service
@Slf4j
public class TraditionalCatholicProvider implements FreeTextProvider {
    private static final String INDEX_URL = "https://www.traditionalcatholic.co/free-catholic-books/";
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "TraditionalCatholic.co"; }

    @Override
    public int getPriority() { return 75; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String html = restTemplate.getForObject(INDEX_URL, String.class);

            String searchTerm = title.split(":")[0].trim().toLowerCase();
            if (html.toLowerCase().contains(searchTerm)) {
                // Find the specific link
                Pattern pattern = Pattern.compile(
                    "<a[^>]+href=\"([^\"]+)\"[^>]*>[^<]*" + Pattern.quote(searchTerm) + "[^<]*</a>",
                    Pattern.CASE_INSENSITIVE);

                Matcher matcher = pattern.matcher(html);
                if (matcher.find()) {
                    return FreeTextLookupResult.builder()
                        .providerName(getProviderName())
                        .found(true)
                        .url(matcher.group(1))
                        .build();
                }
            }
        } catch (Exception e) {
            log.warn("TraditionalCatholic search failed: {}", e.getMessage());
        }

        return errorResult("Not found in TraditionalCatholic.co");
    }
}
```

---

### 11. CatholicSatProvider

**Website:** https://www.catholicsat.com/e-books-list

**Approach:** E-books list parsing

```java
@Service
@Slf4j
public class CatholicSatProvider implements FreeTextProvider {
    private static final String EBOOKS_URL = "https://www.catholicsat.com/e-books-list";
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "CatholicSat.com E-books"; }

    @Override
    public int getPriority() { return 80; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            String html = restTemplate.getForObject(EBOOKS_URL, String.class);

            String searchTerm = title.split(":")[0].trim().toLowerCase();
            Pattern pattern = Pattern.compile(
                "<a[^>]+href=\"([^\"]+)\"[^>]*>[^<]*" + Pattern.quote(searchTerm) + "[^<]*</a>",
                Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                String href = matcher.group(1);
                if (!href.startsWith("http")) {
                    href = "https://www.catholicsat.com" + href;
                }
                return FreeTextLookupResult.builder()
                    .providerName(getProviderName())
                    .found(true)
                    .url(href)
                    .build();
            }
        } catch (Exception e) {
            log.warn("CatholicSat search failed: {}", e.getMessage());
        }

        return errorResult("Not found in CatholicSat E-books");
    }
}
```

---

### 12. LocOpenAccessProvider

**Website:** https://www.loc.gov/collections/open-access-books/

**API Approach:** Library of Congress JSON API

**Search Strategy:**
1. Use LOC search API: `https://www.loc.gov/books/?q={title}&fo=json`
2. Filter to open access collection
3. Return item permalink

```java
@Service
@Slf4j
public class LocOpenAccessProvider implements FreeTextProvider {
    private static final String API_BASE = "https://www.loc.gov/books/";
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "LOC Open Access Books"; }

    @Override
    public int getPriority() { return 15; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_BASE)
                .queryParam("q", title + (authorName != null ? " " + authorName : ""))
                .queryParam("fo", "json")
                .queryParam("fa", "online-format:online text")
                .queryParam("c", 5);

            LocSearchResponse response = restTemplate.getForObject(
                builder.build().toUriString(), LocSearchResponse.class);

            if (response != null && response.getResults() != null) {
                for (LocResult result : response.getResults()) {
                    if (titleMatches(result.getTitle(), title)) {
                        return FreeTextLookupResult.builder()
                            .providerName(getProviderName())
                            .found(true)
                            .url(result.getUrl())
                            .build();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("LOC Open Access search failed: {}", e.getMessage());
        }

        return errorResult("Not found in LOC Open Access collection");
    }
}
```

---

### 13. LocCatalogOnlineProvider

**Website:** https://catalog.loc.gov

**API Approach:** SRU API (similar to existing LocCatalogService)

**Search Strategy:**
1. Query LOC SRU API for the title/author
2. Check if record has online resource link (856 field)
3. Return online resource URL if available

```java
@Service
@Slf4j
public class LocCatalogOnlineProvider implements FreeTextProvider {
    private final LocCatalogService locCatalogService;
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "LOC Online Catalog"; }

    @Override
    public int getPriority() { return 20; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        // Use existing LOC SRU infrastructure but look for online resources
        // MARC field 856 contains electronic location/access info

        try {
            String cqlQuery = buildCqlQuery(title, authorName);
            String sruUrl = buildSruUrl(cqlQuery);

            String marcXml = restTemplate.getForObject(sruUrl, String.class);
            String onlineUrl = extractOnlineResourceUrl(marcXml, title);

            if (onlineUrl != null) {
                return FreeTextLookupResult.builder()
                    .providerName(getProviderName())
                    .found(true)
                    .url(onlineUrl)
                    .build();
            }
        } catch (Exception e) {
            log.warn("LOC Catalog search failed: {}", e.getMessage());
        }

        return errorResult("No online version in LOC catalog");
    }

    private String extractOnlineResourceUrl(String marcXml, String title) {
        // Parse MARC 856 field for electronic resource URLs
        // 856 $u contains the URL
        // Filter for records where 245 $a matches our title
        // Implementation uses XML parsing similar to existing LocCatalogService
        return null; // Placeholder
    }
}
```

---

### 14. LocOnlineBooksPageProvider

**Website:** http://onlinebooks.library.upenn.edu/ (Online Books Page, curated by LOC)

**API Approach:** Direct search interface

**Search Strategy:**
1. Use search form: `http://onlinebooks.library.upenn.edu/webbin/book/search?aession=...&tession={title}&aession={author}`
2. Parse results page for book links

```java
@Service
@Slf4j
public class LocOnlineBooksPageProvider implements FreeTextProvider {
    private static final String SEARCH_URL = "http://onlinebooks.library.upenn.edu/webbin/book/search";
    private final RestTemplate restTemplate;

    @Override
    public String getProviderName() { return "Online Books Page (LOC Index)"; }

    @Override
    public int getPriority() { return 12; }

    @Override
    public FreeTextLookupResult search(String title, String authorName) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("tession", title);

            if (authorName != null && !authorName.isBlank()) {
                builder.queryParam("aession", authorName);
            }

            String html = restTemplate.getForObject(builder.build().toUriString(), String.class);

            // Parse results for book links
            // Results contain links to actual book sources
            Pattern pattern = Pattern.compile(
                "<a[^>]+href=\"(https?://[^\"]+)\"[^>]*class=\"[^\"]*booklink[^\"]*\"",
                Pattern.CASE_INSENSITIVE);

            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return FreeTextLookupResult.builder()
                    .providerName(getProviderName())
                    .found(true)
                    .url(matcher.group(1))
                    .build();
            }
        } catch (Exception e) {
            log.warn("Online Books Page search failed: {}", e.getMessage());
        }

        return errorResult("Not found in Online Books Page");
    }
}
```

---

## Orchestrator Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FreeTextLookupService {

    private final BookRepository bookRepository;
    private final List<FreeTextProvider> providers;

    @PostConstruct
    public void init() {
        // Sort providers by priority
        providers.sort(Comparator.comparingInt(FreeTextProvider::getPriority));
        log.info("Initialized {} free text providers: {}",
            providers.size(),
            providers.stream().map(FreeTextProvider::getProviderName).toList());
    }

    /**
     * Look up free online text for a single book.
     * Tries all providers in priority order until one finds a match.
     */
    public FreeTextBulkLookupResultDto lookupBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new LibraryException("Book not found: " + bookId));

        // Skip temporary titles
        if (BooksFromFeedService.isTemporaryTitle(book.getTitle())) {
            return FreeTextBulkLookupResultDto.builder()
                .bookId(bookId)
                .bookTitle(book.getTitle())
                .success(false)
                .errorMessage("Temporary title - skipped")
                .providersSearched(List.of())
                .build();
        }

        String authorName = book.getAuthor() != null ? book.getAuthor().getName() : null;
        List<String> searchedProviders = new ArrayList<>();

        for (FreeTextProvider provider : providers) {
            searchedProviders.add(provider.getProviderName());

            try {
                FreeTextLookupResult result = provider.search(book.getTitle(), authorName);

                if (result.isFound()) {
                    // Update the book with the found URL
                    book.setFreeTextUrl(result.getUrl());
                    book.setLastModified(LocalDateTime.now());
                    bookRepository.save(book);

                    log.info("Found free text for book {}: {} via {}",
                        bookId, result.getUrl(), provider.getProviderName());

                    return FreeTextBulkLookupResultDto.builder()
                        .bookId(bookId)
                        .bookTitle(book.getTitle())
                        .authorName(authorName)
                        .success(true)
                        .freeTextUrl(result.getUrl())
                        .providerName(provider.getProviderName())
                        .providersSearched(searchedProviders)
                        .build();
                }
            } catch (Exception e) {
                log.warn("Provider {} failed for book {}: {}",
                    provider.getProviderName(), bookId, e.getMessage());
            }
        }

        // No provider found a match
        return FreeTextBulkLookupResultDto.builder()
            .bookId(bookId)
            .bookTitle(book.getTitle())
            .authorName(authorName)
            .success(false)
            .errorMessage("Not found in any provider")
            .providersSearched(searchedProviders)
            .build();
    }

    /**
     * Look up free online text for multiple books.
     */
    public List<FreeTextBulkLookupResultDto> lookupBooks(List<Long> bookIds) {
        List<FreeTextBulkLookupResultDto> results = new ArrayList<>();

        for (Long bookId : bookIds) {
            try {
                results.add(lookupBook(bookId));
            } catch (Exception e) {
                log.error("Error looking up free text for book {}: {}", bookId, e.getMessage());
                results.add(FreeTextBulkLookupResultDto.builder()
                    .bookId(bookId)
                    .success(false)
                    .errorMessage("Error: " + e.getMessage())
                    .providersSearched(List.of())
                    .build());
            }
        }

        return results;
    }
}
```

---

## Controller Endpoint

```java
@RestController
@RequestMapping("/api/free-text")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('LIBRARIAN')")
public class FreeTextLookupController {

    private final FreeTextLookupService freeTextLookupService;

    @PostMapping("/lookup/{bookId}")
    public ResponseEntity<FreeTextBulkLookupResultDto> lookupBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(freeTextLookupService.lookupBook(bookId));
    }

    @PostMapping("/lookup-bulk")
    public ResponseEntity<List<FreeTextBulkLookupResultDto>> lookupBooks(@RequestBody List<Long> bookIds) {
        return ResponseEntity.ok(freeTextLookupService.lookupBooks(bookIds));
    }
}
```

---

## Frontend Integration

### API Hook

```typescript
// api/free-text-lookup.ts
export interface FreeTextLookupResultDto {
  bookId: number
  bookTitle: string
  authorName?: string
  success: boolean
  freeTextUrl?: string
  providerName?: string
  errorMessage?: string
  providersSearched: string[]
}

export function useLookupBulkFreeText() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (ids: number[]) =>
      api.post<FreeTextLookupResultDto[]>('/free-text/lookup-bulk', ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
    },
  })
}
```

### Button in BulkActionsToolbar

```tsx
// Add to BulkActionsToolbar.tsx
const lookupFreeText = useLookupBulkFreeText()
const [freeTextResults, setFreeTextResults] = useState<FreeTextLookupResultDto[]>([])
const [showFreeTextResults, setShowFreeTextResults] = useState(false)

const handleFreeTextLookup = async () => {
  try {
    const results = await lookupFreeText.mutateAsync(Array.from(selectedIds))
    setFreeTextResults(results)
    setShowFreeTextResults(true)
  } catch (error) {
    console.error('Failed to lookup free text URLs:', error)
  }
}

// Button with progress indicator showing count completed
<Button
  variant="outline"
  size="sm"
  onClick={handleFreeTextLookup}
  isLoading={lookupFreeText.isPending}
  disabled={lookupFreeText.isPending}
  leftIcon={<PiBookOpen />}
  data-test="bulk-lookup-free-text"
>
  {lookupFreeText.isPending
    ? `Finding... (${freeTextProgress}/${selectedIds.size})`
    : 'Find links to free online text'}
</Button>
```

---

## Provider Priority Summary

| Priority | Provider | API Type |
|----------|----------|----------|
| 5 | Internet Archive | REST API (JSON) |
| 10 | Project Gutenberg | Gutendex API (JSON) |
| 12 | Online Books Page | HTML Search |
| 15 | LOC Open Access Books | LOC API (JSON) |
| 20 | LOC Online Catalog | SRU API (MARC XML) |
| 30 | LibriVox | REST API (JSON) |
| 35 | CCEL | HTML Search |
| 40 | EWTN Catholic Library | HTML Search |
| 50 | Vatican.va | Site Search |
| 60 | Catholic Planet | Index Parsing |
| 65 | Good Catholic Books | Site Search |
| 70 | My Catholic Life! | Page Parsing |
| 75 | TraditionalCatholic.co | Index Parsing |
| 80 | CatholicSat.com | Index Parsing |

---

## Title Matching Algorithm

```java
public class TitleMatcher {

    /**
     * Check if two titles match, accounting for:
     * - Case differences
     * - Subtitle variations (text after colon)
     * - Articles (The, A, An)
     * - Punctuation differences
     */
    public static boolean titleMatches(String candidate, String target) {
        if (candidate == null || target == null) return false;

        String normalizedCandidate = normalize(candidate);
        String normalizedTarget = normalize(target);

        // Exact match after normalization
        if (normalizedCandidate.equals(normalizedTarget)) return true;

        // Main title match (before colon)
        String candidateMain = getMainTitle(normalizedCandidate);
        String targetMain = getMainTitle(normalizedTarget);

        if (candidateMain.equals(targetMain)) return true;

        // Contains match for longer titles
        if (candidateMain.length() > 20 && targetMain.length() > 20) {
            return candidateMain.contains(targetMain) || targetMain.contains(candidateMain);
        }

        return false;
    }

    private static String normalize(String title) {
        return title.toLowerCase()
            .replaceAll("^(the|a|an)\\s+", "")
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String getMainTitle(String title) {
        int colonIndex = title.indexOf(':');
        return colonIndex > 0 ? title.substring(0, colonIndex).trim() : title;
    }
}
```

---

## Error Handling

Each provider returns specific error messages:
- `"Author not found"` - Author search yielded no results
- `"Title not found"` - Title search yielded no results
- `"Title not found in author's works"` - Author found but no matching title
- `"Not found in {provider name}"` - General not found
- `"Search requires manual verification"` - Cannot automate search

---

## Testing Strategy

1. **Unit Tests**: Test each provider with mock HTTP responses
2. **Integration Tests**: Test FreeTextLookupService with mock providers
3. **Controller Tests**: Test REST endpoints with @WebMvcTest
4. **UI Tests**: Playwright tests for button interaction and results display
