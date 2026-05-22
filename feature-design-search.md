# Search Feature Design

## Overview

The Search page provides global search functionality across books and authors in the library catalog. Users can search by entering a query that matches against book titles and author names using case-insensitive partial matching. Filter chips narrow book results by physical presence, resource type, or online availability. A blank search (empty query) is valid and returns all books (subject to active filters).

## Purpose

- **Catalog Discovery**: Find books and authors across the entire library collection
- **Public Access**: No authentication required - anyone can search the catalog
- **Filter-Based Refinement**: Filter chips let users narrow results without requiring a text query
- **Paginated Results**: Efficient handling of large result sets with separate pagination for books and authors

## Domain Model

### Entities

Search operates on existing domain entities:

- **Book**: Searches the `title` field; filtered by `locNumber`, `electronicResource`, and `freeTextUrl`
- **Author**: Searches the `name` field

### DTOs

**SearchResponseDto** - Search results wrapper
```java
public class SearchResponseDto {
    private List<BookDto> books;           // Matching books
    private List<AuthorDto> authors;       // Matching authors
    private PageInfoDto bookPage;          // Book pagination info
    private PageInfoDto authorPage;        // Author pagination info
}
```

**PageInfoDto** - Reusable pagination metadata
```java
public class PageInfoDto {
    private int totalPages;       // Total number of pages
    private long totalElements;   // Total number of matching results
    private int currentPage;      // Current page number (zero-based)
    private int pageSize;         // Results per page
}
```

## API Endpoint

**Base Path**: `/api/search`

### GET /api/search

Returns paginated search results for books and authors.

**Authentication**: Public (no authentication required)

**Query Parameters**:
- `query` (string, optional, default `""`) - Search term to match against book titles and author names. Empty query returns all results.
- `page` (int, required) - Zero-based page number
- `size` (int, required) - Number of results per page (default: 20)
- `filterInLibrary` (boolean, optional, default `false`) - Limit books to those with a LOC call number (physical collection)
- `filterElectronic` (boolean, optional, default `false`) - Limit books to those marked as electronic resources (`electronicResource = true`)
- `filterFreeText` (boolean, optional, default `false`) - Limit books to those with a free online text URL (`freeTextUrl IS NOT NULL`)
- `filterAudio` (boolean, optional, default `false`) - Limit books to those with a LibriVox audio recording (`freeTextUrl LIKE '%librivox%'`)
- `labels` (string, optional, multi-value) - Limit books to those tagged with all specified labels

Multiple boolean filters use OR logic: a book is included if it matches **any** active filter.

**Response**: `SearchResponseDto` containing books, authors, and pagination info for each

**HTTP Status Codes**:
- `200 OK` - Search completed successfully
- `400 Bad Request` - Missing required `page` parameter
- `500 Internal Server Error` - Search failed (e.g., database error)

**Controller**: `src/main/java/com/muczynski/library/controller/SearchController.java`
**Service**: `src/main/java/com/muczynski/library/service/SearchService.java`

## Search Algorithm

### Search Strategy

1. **Case-Insensitive Matching**: Uses JPQL `LOWER(...) LIKE LOWER(CONCAT('%', :query, '%'))`
2. **Partial Matching**: Searches for the query string anywhere within the field
3. **Separate Queries**: Books and authors are searched independently with separate pagination
4. **Blank Query Allowed**: An empty or missing `query` param returns all books (subject to filters)
5. **Filter Logic (OR)**: When any filter is active, books must match at least one active filter. When no filter is active, all books are eligible.
6. **Author Query**: Non-empty query → `findByNameContainingIgnoreCase`; empty query → `findAll`

### Repository Methods

- `BookRepository.findWithFilters(query, filterInLibrary, filterElectronic, filterFreeText, filterAudio, pageable)` — standard book search
- `BookRepository.findWithFiltersAndLabels(query, filterInLibrary, filterElectronic, filterFreeText, filterAudio, labels, labelCount, pageable)` — additionally filters by label tags

### Filter Semantics

| Filter | Condition |
|--------|-----------|
| In-library materials | `locNumber IS NOT NULL AND locNumber <> ''` |
| Electronic resource | `electronicResource = true` |
| Has free online text | `freeTextUrl IS NOT NULL` |
| Has free online audio | `freeTextUrl IS NOT NULL AND LOWER(freeTextUrl) LIKE '%librivox%'` |

### Fields Searched

- **Books**: `title` field only (NOT publisher, NOT description)
- **Authors**: `name` field only

## Pagination

- **Independent Pagination**: Books and authors have separate page counts and totals
- **Default Page Size**: 20 results per page
- **Zero-Based Pages**: Page numbers start at 0
- **Page Metadata**: Each result type includes total pages, total elements, current page, and page size

## User Interface

**Location**: `frontend/src/pages/search/SearchPage.tsx`
**Technology**: React 18+ with TypeScript, TanStack Query, Tailwind CSS

### URL-Based Search State

Search state is persisted in the URL for better UX and shareability:

**URL Pattern**: `/search?q=<query>&page=<page>&inLib=<bool>&elec=<bool>&freeText=<bool>&audio=<bool>`

**Parameters**:
- `q` (string) - Search query text
- `page` (number, optional) - Zero-based page number (omitted when 0)
- `inLib` (boolean, optional) - In-library filter active (`true`/`false`; omitted when false)
- `elec` (boolean, optional) - Electronic resource filter active
- `freeText` (boolean, optional) - Free online text filter active
- `audio` (boolean, optional) - Free online audio filter active

**Examples**:
- `/search?q=Augustine` - Search for "Augustine" (no filter)
- `/search?inLib=true` - All in-library books (blank query with filter)
- `/search?q=Augustine&inLib=true&elec=true` - "Augustine" in in-library OR electronic books
- `/search?audio=true` - All LibriVox audio books

**Benefits**:
- Bookmarkable search URLs
- Browser back/forward navigation works correctly
- Shareable search links
- Page refresh preserves search state

**Implementation**:
- Uses `useSearchParams` hook from React Router
- Filter chips read/write URL params directly, triggering immediate search
- Search executes whenever `hasSearched || hasFilters` is true

### Page Structure

#### 1. Search Input

- Text input field synced with URL `q` parameter
- Search button is **always enabled** — blank search is valid and returns all books
- Search executes on form submit (Enter key or Search button click)
- Clear button resets query text AND all filter chips, then clears URL parameters

#### 2. Filter Chips

Four toggle chips displayed below the search input. Clicking a chip immediately updates the URL and triggers a search. Each chip shows:
- **Inactive**: Funnel (filter) icon + label + ⓘ indicator
- **Active**: Checkmark icon + label + ⓘ indicator + blue highlight

| Chip | `data-test` | Tooltip |
|------|-------------|---------|
| In-library materials | `filter-in-library` | "Limit results to books with a Library of Congress call number — books physically in the collection" |
| Electronic resource | `filter-electronic` | "Limit results to books marked as electronic resources" |
| Has free online text | `filter-free-text` | "Limit results to books that have a free online text URL (e.g., Project Gutenberg, Internet Archive)" |
| Has free online audio | `filter-audio` | "Limit results to books with a free LibriVox audio recording" |

Note: Filter chips affect only book results; author search is unaffected by filters.

#### 3. Results Display

**Books Section**:
- Table showing matching books with columns: Title, Author name, Library name, Publication year, LOC number
- Book count and pagination controls
- "No books found" message when empty

**Authors Section**:
- Table showing matching authors with columns: Name, Brief biography, Birth/death dates, Book count
- Author count and pagination controls
- "No authors found" message when empty

#### 4. Empty State

- Displayed when no query has been entered and no filter chip is active
- Search button click (or filter chip click) triggers first search

#### 5. Clear Button

Visible when `hasSearched || hasFilters`. Resets all state: clears input, deactivates all filter chips, clears URL params.

### React Query Integration

**API Function**: `frontend/src/api/search.ts`

```typescript
export interface SearchFilters {
  inLib: boolean
  elec: boolean
  freeText: boolean
  audio: boolean
}

export const defaultSearchFilters: SearchFilters = {
  inLib: false, elec: false, freeText: false, audio: false
}

export const useSearch = (query: string, page: number, size: number, filters: SearchFilters, enabled: boolean)
```

**Query Key**: `['search', query, page, size, filters]` — cache is keyed by all filter values

## Security

- **Public Access**: `/api/search` endpoint has `@PreAuthorize("permitAll()")`
- **No Authentication Required**: Anyone can search the catalog
- **Read-Only**: Search endpoint is a read-only operation with no side effects
- **Action-Based Security**:
  - **View action** (eye icon) - Available to all users (public)
  - **Edit action** (pencil icon) - Librarian only (checked via `useIsLibrarian()` hook)

## Testing

### Backend Tests

**SearchServiceTest.java** - Service layer unit tests
- `searchWithResultsFound()` - Basic search returns matching results
- `searchWithNoResults()` - Empty results when nothing matches
- `searchEmptyQueryReturnsAllBooks()` - Empty query returns everything
- `searchWithInLibraryFilterPassesTrueToRepository()` - filterInLibrary flag forwarded
- `searchWithElectronicFilterPassesTrueToRepository()` - filterElectronic flag forwarded
- `searchWithFreeTextFilterPassesTrueToRepository()` - filterFreeText flag forwarded
- `searchWithAudioFilterPassesTrueToRepository()` - filterAudio flag forwarded
- `searchWithMultipleFiltersPassesAllTrueToRepository()` - multiple flags forwarded correctly

**SearchControllerTest.java** - Controller integration tests
- Tests HTTP endpoint behavior with all four filter boolean params
- Mocks SearchService for isolation
- Verifies HTTP status codes and response structure
- `testSearch_WithInLibraryFilter()`, `testSearch_WithElectronicFilter()`, etc.
- `testSearch_DefaultFiltersAreFalse()` - no filter params → all booleans default to false

### UI Tests

**Location**: `src/test/java/com/muczynski/library/ui/SearchUITest.java`

Playwright UI test coverage:
- `testSearchPageLayout()` - Search form displays correctly; all four filter chips present; button enabled
- `testSearchForBooks()` - Search returns book results
- `testSearchForAuthors()` - Search returns author results
- `testSearchForBooksAndAuthors()` - Search returns both types
- `testNoResultsFound()` - No results message displayed
- `testClearSearch()` - Clear button resets results
- `testSearchButtonAlwaysEnabled()` - Button enabled with empty, filled, or cleared input
- `testBlankSearchReturnsResults()` - Clicking search with empty input returns all books
- `testBookResultDetails()` - Book details displayed correctly
- `testAuthorResultDetails()` - Author details displayed correctly
- `testSearchUpdatesUrl()` - Search updates URL with `?q=` parameter
- `testSearchFromUrlParameter()` - URL with `?q=` loads search results
- `testClearSearchUpdatesUrl()` - Clear removes URL parameters
- `testViewBookNavigatesToPage()` - View navigates to `/books/{id}`
- `testViewAuthorNavigatesToPage()` - View navigates to `/authors/{id}`
- `testFilterChipsVisible()` - All 4 chips visible with correct text and tooltip
- `testInLibraryFilterChipUpdatesUrl()` - Clicking chip sets `inLib=true` in URL and returns books
- `testAudioFilterReturnsLibriVoxBooks()` - Audio filter returns only LibriVox book
- `testFreeTextFilterReturnsOnlineTextBooks()` - Free-text filter returns 2 books with URLs
- `testFilterChipStateRestoredFromUrl()` - Navigating with filter params shows active chips
- `testClearRemovesFilterChips()` - Clear button removes filter params from URL

## Performance Considerations

1. **Database Indexes**: Ensure indexes on `book.title` and `author.name` for fast searching
2. **Pagination**: Limits result size to prevent large data transfers
3. **Query Caching**: TanStack Query reduces redundant API calls keyed by query + page + filters
4. **Case-Insensitive Search**: Uses JPQL LOWER() for database-portable case folding

## Limitations

- **Search Fields**: Only searches title and name fields (NOT publisher, NOT description, NOT other metadata)
- **No Advanced Search**: No boolean operators (AND/OR), no phrase matching, no wildcards
- **No Fuzzy Matching**: Exact substring matching only (no typo tolerance)
- **No Full-Text Search**: Not using PostgreSQL full-text search capabilities
- **Single Query**: Books and authors searched with same query (can't search different terms)
- **Filter OR Logic**: Multiple filters are ORed — cannot require a book to satisfy all filters simultaneously

## Future Enhancements (Not Implemented)

- ❌ **Publisher Search**: Searching books by publisher name
- ❌ **Advanced Search**: Boolean operators, phrase matching
- ❌ **Fuzzy Search**: Typo tolerance and similarity matching
- ❌ **Full-Text Search**: PostgreSQL `tsvector` for better relevance ranking
- ❌ **Search Suggestions**: Autocomplete based on popular searches
- ❌ **Search History**: User search history and recent searches
- ❌ **Filter AND Logic**: Require all selected filters to be satisfied simultaneously

## CRUD Operations in Search Results

### Actions Column

Each search result (book or author) includes an Actions column on the right side with navigation links:

#### All Users
- **View** (Eye icon, gray color) - Navigates to `/books/{id}` or `/authors/{id}`

#### Librarian Only
- **Edit** (Pencil icon, blue color) - Navigates to `/books/{id}/edit` or `/authors/{id}/edit`

**Note**: Delete functionality is intentionally NOT available from search results.

## Related Documentation

- **API Endpoints**: See `endpoints/` for complete endpoint documentation
- **CLAUDE.md**: Main project overview and architecture
- **feature-design-frontend.md**: React architecture and URL-based CRUD pattern
