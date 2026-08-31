# Library of Congress (LOC) Integration

## Overview
The application integrates with the Library of Congress to look up and manage call numbers for books.

## Technology Stack
- **Marc4J 2.9.5**: Library for reading MARC records from Library of Congress
- **BYU CallNumber library 1.1.0**: Proper LOC call number sorting and parsing

## Features

### Single Book Lookup
- Individual "Lookup" button for each book in the Books table
- Endpoint: `POST /api/loc-bulk-lookup/lookup/{bookId}`
- Updates book's `locNumber` field
- Updates `lastModified` timestamp for cache invalidation
- In-place UI update (no page reload)

### Bulk Lookup
- Bulk LOC call number lookup for multiple books
- Import/export functionality for batch processing
- Section: `loc-bulk-lookup` (currently hidden from menu, but functionality exists)

### Lookup Strategies
The service attempts multiple strategies to find LOC call numbers:

1. **Title + Author lookup**: Primary strategy (uses book title and author name if available)
2. **Title-only fallback**: If author is not available or title + author fails
3. **Truncated title + author**: For long titles (truncates to first 50 characters)
4. **Truncated title-only**: Last resort (truncated title without author)
5. **AI Suggest fallback**: If all LOC catalog strategies fail, automatically calls `AskGrok.suggestLocNumber()` to get an AI-generated LOC call number. Results are marked with `aiSuggested=true` in the response DTO. Wrapped in try-catch so failures (e.g., no API key) silently fall back to returning the original LOC failure message.

Each strategy updates the book's `locNumber` and `lastModified` fields on success.

**Note**: The Book entity does not have an ISBN field. All lookups are based on title and author information.

## LOC Number Formatting

### Display Format
- Use `formatLocForSpine()` in `utils.js` for displaying LOC call numbers
- Formats call numbers appropriately for book pocket labels
- Handles multi-line formatting for PDF labels

### Sorting
- BYU CallNumber library provides proper sorting
- LOC call numbers are complex alphanumeric codes that require specialized sorting
- Example: "PS3545.H16" should sort correctly relative to "PS3545.A1"

## PDF Label Generation
- Book pocket labels for books with LOC call numbers
- Service: `LabelsPdfService` using iText 8
- Custom formatting for LOC numbers on labels
- Endpoint: `GET /api/labels/generate?bookIds={ids}` (librarian only)
- Accessible via "Generate Labels" button in Books page bulk actions toolbar

## Book Filtering

### Books Without LOC
- Endpoint: `GET /api/books/without-loc`
- Authorization: Public (permitAll)
- Filter button in Books page bulk actions toolbar
- Shows all books missing LOC call numbers
- Returns: Full `BookDto` objects
- Query: `SELECT b WHERE b.locNumber IS NULL OR b.locNumber = ''`

### Most Recent Day (+ Temporary Titles)
- Endpoint: `GET /api/books/most-recent-day`
- Also accessible via: `GET /api/books-from-feed/saved-books` (requires LIBRARIAN)
- Authorization: Public (permitAll)
- Filter button in Books page bulk actions toolbar
- Shows books added in the most recent 2 days OR books with temporary titles
- Temporary titles match pattern: YYYY-M-D or YYYY-MM-DD at start (e.g., "2025-01-10_14:30:00")
- Returns: `SavedBookDto` objects (efficient projection, no N+1 queries)
  - Includes: id, title, author, library, photoCount, needsProcessing, locNumber, status, grokipediaUrl
  - `needsProcessing` is true for temporary title books
- Useful for processing newly imported books from Google Photos feed
- Includes 2 days to handle timezone differences and ensure recently added books are not missed

### Books Without Grokipedia URL
- Endpoint: `GET /api/books/without-grokipedia`
- Authorization: Public (permitAll)
- Radio button filter in Books page
- Shows all books missing Grokipedia URL
- Returns: Full `BookDto` objects
- Query: `SELECT b WHERE b.grokipediaUrl IS NULL OR b.grokipediaUrl = ''`
- Useful for identifying books that need Grokipedia article links

### All Books with LOC Status
- Endpoint: `GET /api/loc-bulk-lookup/books`
- Authorization: LIBRARIAN only
- Shows all books with their current LOC status
- Returns: `BookLocStatusDto` objects (specialized for LOC bulk lookup operations)

## Implementation Details

### Service Layer
- `LocBulkLookupService` - LOC lookup business logic
- Updates `book.lastModified` on successful lookup for cache invalidation
- Logs all lookup attempts and results

### Repository Layer
- `BookRepository.findBooksWithoutLocNumber()` - Find books needing LOC numbers
- `BookRepository.findBooksWithoutGrokipediaUrl()` - Find books without Grokipedia URL
- Uses LEFT JOIN FETCH for performance

### Frontend
- `frontend/src/pages/books/components/BookTable.tsx` - Individual lookup buttons in table
- `frontend/src/pages/books/components/BulkActionsToolbar.tsx` - Bulk lookup UI and "Generate Labels" button
- `frontend/src/utils/formatters.ts` - `formatLocForSpine()` utility function

## UI Integration

### Books Table
- Individual "Lookup" button per book (librarian only) - purple magnifying glass icon
- Shows LOC number in table column
- Results displayed in modal dialog after lookup
- Filter buttons available in bulk actions toolbar

### Menu Items
- Individual LOC lookup buttons integrated into Books table (librarian only)
- Bulk lookup functionality available in Books page bulk actions toolbar
- "Generate Labels" button in Books page bulk actions toolbar (for PDF generation, librarian only)

## Related Files
- `src/main/java/com/muczynski/library/service/LocBulkLookupService.java` - Core lookup logic
- `src/main/java/com/muczynski/library/service/LabelsPdfService.java` - PDF generation for book pocket labels
- `src/main/java/com/muczynski/library/repository/BookRepository.java` - Queries for books without LOC
- `frontend/src/pages/books/components/BookTable.tsx` - UI for individual lookup buttons
- `frontend/src/pages/books/components/BulkActionsToolbar.tsx` - UI for bulk lookup and "Generate Labels" button

---

# Grokipedia URL Lookup

## Overview
The application can automatically discover Grokipedia article URLs for books and authors. This helps populate the `grokipediaUrl` field by checking if a page exists on grokipedia.com. Librarians choose **quick lookup** (generated URL only) or **slow lookup** (generated URL, then Grok, then HTTP checks of Grok's candidates).

## How It Works

### URL Generation
- Grokipedia URLs follow the pattern: `https://grokipedia.com/page/{Name_With_Underscores}`
- Spaces in book titles or author names are converted to underscores
- Book titles use `Book.stripCopySuffix` first so catalog copy numbers (`", c. 3"`) are not part of the URL
- Example: "Little Women" → `https://grokipedia.com/page/Little_Women`
- Example: "Catechism of the Catholic Church, c. 3" → `https://grokipedia.com/page/Catechism_of_the_Catholic_Church`
- Example: "Louisa May Alcott" → `https://grokipedia.com/page/Louisa_May_Alcott`

### Quick lookup
- Checks the generated URL with a HEAD request
- If the response is 2xx, the URL is saved and lookup stops
- If the response is 4xx/5xx, nothing is saved

### Slow lookup
1. Same generated-URL check as quick lookup. A 2xx result is saved and lookup stops (Grok is not called).
2. Ask Grok for candidate Grokipedia URLs. Author prompts treat Grokipedia search as the source of truth, prefer a biographical person page, warn that slugs are inconsistent (`C._S._Lewis`, `First_Last_(writer)`, etc.), and ask for a JSON array of URL strings (most relevant first) with nothing before or after the JSON. Book prompts include the corresponding author and a disambiguated example such as `(novel)`. Search URLs in the reply are ignored; only `grokipedia.com/page/...` URLs are HEAD-checked.
3. HEAD-check each candidate. Keep 2xx URLs and save the first working URL. Discard 4xx URLs.
4. If no working URL remains, save `"-"` in the database to mean N/A. `"-"` is only saved when there are no working URLs; it is not saved for individual 4xx candidates when another URL worked, and it is not saved if the Grok call itself failed.

`"-"` is treated as missing by without-Grokipedia filters and is not shown as a link.

## Endpoints

### Books Bulk Lookup
- Endpoint: `POST /api/books/grokipedia-lookup-bulk`
- Query: `slow=true` for slow lookup (default `false` = quick)
- Authorization: LIBRARIAN only
- Request Body: `List<Long>` (book IDs to look up)
- Response: `List<GrokipediaLookupResultDto>` with success/failure for each book
- Looks up selected books regardless of existing `grokipediaUrl` value

### Authors Bulk Lookup
- Endpoint: `POST /api/authors/grokipedia-lookup-bulk`
- Query: `slow=true` for slow lookup (default `false` = quick)
- Authorization: LIBRARIAN only
- Request Body: `List<Long>` (author IDs to look up)
- Response: `List<GrokipediaLookupResultDto>` with success/failure for each author
- Looks up selected authors regardless of existing `grokipediaUrl` value

## Frontend Integration

### Books Page
- "Quick Grokipedia lookup" and "Slow Grokipedia lookup" buttons in `BulkActionsToolbar`
- Visible when books are selected
- Shows results in `GrokipediaLookupResultsModal`
- Results show success/failure count and individual results with URLs

### Authors Page
- "Quick Grokipedia lookup" and "Slow Grokipedia lookup" buttons in bulk actions bar
- Visible when authors are selected
- Shows results in `GrokipediaLookupResultsModal`
- Results show success/failure count and individual results with URLs

## Implementation Files
- `src/main/java/com/muczynski/library/service/GrokipediaLookupService.java` - Core lookup logic
- `src/main/java/com/muczynski/library/service/AskGrok.java` - Grok prompt for candidate URLs
- `src/main/java/com/muczynski/library/dto/GrokipediaLookupResultDto.java` - Result DTO
- `frontend/src/api/grokipedia-lookup.ts` - API hooks
- `frontend/src/components/GrokipediaLookupResultsModal.tsx` - Results modal
- `frontend/src/pages/books/components/BulkActionsToolbar.tsx` - Books bulk action buttons
- `frontend/src/pages/authors/components/AuthorBulkActionsToolbar.tsx` - Authors bulk action buttons
