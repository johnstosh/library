# Search Feature Design

## Overview

The Search page provides global search functionality across books and authors in the library catalog. Users can search by entering a query that matches against book titles and author names using case-insensitive partial matching.

## Purpose

- **Catalog Discovery**: Find books and authors across the entire library collection
- **Public Access**: No authentication required - anyone can search the catalog
- **Real-Time Search**: Immediate results as users type their search query
- **Paginated Results**: Efficient handling of large result sets with separate pagination for books and authors

## Domain Model

### Entities

Search operates on existing domain entities:

- **Book**: Searches the `title` field
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
- `query` (string, required) - Search term to match against book titles and author names
- `page` (int, required) - Zero-based page number
- `size` (int, required) - Number of results per page (default: 20)

**Response**: `SearchResponseDto` containing books, authors, and pagination info for each

**HTTP Status Codes**:
- `200 OK` - Search completed successfully
- `500 Internal Server Error` - Search failed (e.g., database error)

**Validation**:
- Empty or null query throws `IllegalArgumentException`
- Whitespace-only query throws `IllegalArgumentException`

**Controller**: `src/main/java/com/muczynski/library/controller/SearchController.java`
**Service**: `src/main/java/com/muczynski/library/service/SearchService.java`

## Search Algorithm

### Search Strategy

1. **Case-Insensitive Matching**: Uses SQL `ILIKE` (PostgreSQL) or case-insensitive matching (H2)
2. **Partial Matching**: Searches for the query string anywhere within the field
3. **Separate Queries**: Books and authors are searched independently with separate pagination
4. **Repository Methods**:
   - `BookRepository.findByTitleContainingIgnoreCase(query, pageable)`
   - `AuthorRepository.findByNameContainingIgnoreCase(query, pageable)`

### Fields Searched

- **Books**: `title` field only (NOT publisher, NOT description)
- **Authors**: `name` field only

### Query Examples

| Query | Matches Book Titles | Matches Author Names |
|-------|---------------------|---------------------|
| "gatsby" | "The Great Gatsby" | - |
| "fitzgerald" | - | "F. Scott Fitzgerald" |
| "great" | "The Great Gatsby", "Great Expectations" | - |

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

**URL Pattern**: `/search?q=<query>&page=<page>`

**Parameters**:
- `q` (string) - Search query text
- `page` (number, optional) - Zero-based page number (omitted when 0)

**Examples**:
- `/search?q=Augustine` - Search for "Augustine" on page 1
- `/search?q=City%20of%20God&page=2` - Search for "City of God" on page 3

**Benefits**:
- Bookmarkable search URLs
- Browser back/forward navigation works correctly
- Shareable search links
- Page refresh preserves search state

**Implementation**:
- Uses `useSearchParams` hook from React Router
- Input field syncs with URL on browser navigation
- Search button updates URL instead of local state

### Page Structure

#### 1. Search Input
- Text input field synced with URL `q` parameter
- Clear button resets search and clears URL parameters
- Search executes on form submit (Enter key or Search button click)

#### 2. Results Display

**Books Section**:
- Table showing matching books with columns:
  - Title
  - Author name
  - Library name
  - Publication year
  - LOC number
- Book count and pagination controls
- Click on book title to view book details
- "No books found" message when empty

**Authors Section**:
- Table showing matching authors with columns:
  - Name
  - Brief biography
  - Birth/death dates
  - Book count
- Author count and pagination controls
- Click on author name to view author details
- "No authors found" message when empty

#### 3. Empty State
- Displayed when no query has been entered yet
- Prompts user to enter a search term
- No API calls until user searches

### React Query Integration

**API Function**: `frontend/src/api/search.ts`

```typescript
export interface SearchResponse {
  books: BookDto[];
  authors: AuthorDto[];
  bookPage: PageInfo;
  authorPage: PageInfo;
}

export const searchLibrary = async (query: string, page: number, size: number): Promise<SearchResponse> => {
  const response = await fetch(`${API_BASE_URL}/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`);
  return response.json();
};
```

**Query Hook**: `useSearch(query, page, size)`
- Fetches search results using TanStack Query
- Caches results by query + page + size
- Automatic refetching on parameter change

### Caching Strategy

**Browser Caching**:
- TanStack Query caches search results by unique query/page/size combination
- Cache time: Default TanStack Query cache time (5 minutes)
- Stale time: Results become stale immediately, refetch on navigation back
- No IndexedDB caching (search is fast enough without it)

**Benefits**:
- Instant results when navigating back/forward
- Reduced server load for repeated searches
- Automatic cache invalidation

## Security

- **Public Access**: `/api/search` endpoint has `@PreAuthorize("permitAll()")`
- **No Authentication Required**: Anyone can search the catalog
- **Read-Only**: Search endpoint is a read-only operation with no side effects
- **No Sensitive Data**: Search results only include public book/author information
- **Action-Based Security**:
  - **View action** (eye icon) - Available to all users (public)
  - **Edit action** (pencil icon) - Librarian only (checked via `useIsLibrarian()` hook)
  - **Delete action** (trash icon) - Librarian only (checked via `useIsLibrarian()` hook)

## Testing

### Backend Tests

**SearchServiceTest.java** - Service layer unit tests
- `searchWithResultsFound()` - Verifies search returns matching results
- `searchWithNoResults()` - Verifies empty results when nothing matches
- `searchWithEmptyQueryThrowsException()` - Validates empty query handling
- `searchWithNullQueryThrowsException()` - Validates null query handling
- `searchWithWhitespaceQueryThrowsException()` - Validates whitespace query handling
- `searchPaginationBehavior()` - Verifies pagination metadata is correct

**SearchControllerTest.java** - Controller integration tests
- Tests HTTP endpoint behavior
- Mocks SearchService for isolation
- Verifies HTTP status codes and response structure

### Frontend Tests

TanStack Query integration tests verify:
- API calls are made with correct parameters
- Results are cached properly
- Error states are handled

### UI Tests

**Location**: `src/test/java/com/muczynski/library/ui/SearchUITest.java`

Playwright UI test coverage:
- `testSearchPageLayout()` - Search form displays correctly
- `testSearchForBooks()` - Search returns book results
- `testSearchForAuthors()` - Search returns author results
- `testSearchForBooksAndAuthors()` - Search returns both types
- `testNoResultsFound()` - No results message displayed
- `testClearSearch()` - Clear button works correctly
- `testSearchButtonState()` - Button enabled/disabled state
- `testBookResultDetails()` - Book details display correctly
- `testAuthorResultDetails()` - Author details display correctly
- `testSearchUpdatesUrl()` - Search updates URL with `?q=` parameter
- `testSearchFromUrlParameter()` - URL with `?q=` loads search results
- `testClearSearchUpdatesUrl()` - Clear search clears URL parameter
- `testViewBookNavigatesToPage()` - View navigates to `/books/{id}`
- `testViewAuthorNavigatesToPage()` - View navigates to `/authors/{id}`

## Performance Considerations

1. **Database Indexes**: Ensure indexes on `book.title` and `author.name` for fast searching
2. **Pagination**: Limits result size to prevent large data transfers
3. **Query Caching**: TanStack Query reduces redundant API calls
4. **Case-Insensitive Search**: Uses database-native case-insensitive search (ILIKE)

## Limitations

- **Search Fields**: Only searches title and name fields (NOT publisher, NOT description, NOT other metadata)
- **No Advanced Search**: No boolean operators (AND/OR), no phrase matching, no wildcards
- **No Fuzzy Matching**: Exact substring matching only (no typo tolerance)
- **No Full-Text Search**: Not using PostgreSQL full-text search capabilities
- **Single Query**: Books and authors searched with same query (can't search different terms)

## Future Enhancements (Not Implemented)

The following features are documented in code review but NOT implemented:

- ❌ **Publisher Search**: Searching books by publisher name
- ❌ **Search Filters**: Filter results by status, library, date, etc.
- ❌ **Advanced Search**: Boolean operators, phrase matching
- ❌ **Fuzzy Search**: Typo tolerance and similarity matching
- ❌ **Full-Text Search**: PostgreSQL `tsvector` for better relevance ranking
- ❌ **Search Suggestions**: Autocomplete based on popular searches
- ❌ **Search History**: User search history and recent searches

## CRUD Operations in Search Results

### Actions Column

Each search result (book or author) includes an Actions column on the right side with navigation links:

#### All Users
- **View** (Eye icon, gray color)
  - Navigates to book/author view page (`/books/{id}` or `/authors/{id}`)
  - `data-test="book-result-view-{id}"` or `data-test="author-result-view-{id}"`
  - Uses React Router `Link` component for client-side navigation

#### Librarian Only
- **Edit** (Pencil icon, blue color)
  - Navigates to book/author edit page (`/books/{id}/edit` or `/authors/{id}/edit`)
  - `data-test="book-result-edit-{id}"` or `data-test="author-result-edit-{id}"`
  - Uses React Router `Link` component for client-side navigation
  - Only visible if `useIsLibrarian()` returns true

**Note**: Delete functionality is intentionally NOT available from search results. Users should navigate to the respective Books or Authors pages for delete operations.

### Implementation Details

**Search Results Display**:
- Book results use `BookResult` component with navigation links
- Author results use `AuthorResult` component with navigation links
- Icons use inline SVG for consistency with Books and Authors pages
- Hover effects change icon color for better UX

**Page-Based Navigation (not modals)**:
- View actions navigate to `/books/{id}` or `/authors/{id}`
- Edit actions navigate to `/books/{id}/edit` or `/authors/{id}/edit`
- Uses React Router `Link` component for declarative navigation
- Browser back button returns to search results (preserves search URL state)

## Related Documentation

- **API Endpoints**: See `endpoints.md` for complete endpoint documentation
- **CLAUDE.md**: Main project overview and architecture
- **checklist-code-review.md**: Feature checklist and review status
