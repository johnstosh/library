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

1. **ISBN + Author lookup**: Primary strategy
2. **Title-only fallback**: If ISBN + author fails
3. **Truncated title + author**: For long titles
4. **Truncated title-only**: Last resort

Each strategy updates the book's `locNumber` and `lastModified` fields on success.

## LOC Number Formatting

### Display Format
- Use `formatLocForSpine()` in `utils.js` for displaying LOC call numbers
- Formats call numbers appropriately for spine labels
- Handles multi-line formatting for PDF labels

### Sorting
- BYU CallNumber library provides proper sorting
- LOC call numbers are complex alphanumeric codes that require specialized sorting
- Example: "PS3545.H16" should sort correctly relative to "PS3545.A1"

## PDF Label Generation
- Spine labels for books with LOC call numbers
- Service: `LabelsPdfService` using iText 8
- Custom formatting for LOC numbers on labels
- Endpoint: `/api/labels/pdf` (librarian only)

## Book Filtering

### Books Without LOC
- Endpoint: `GET /api/books/without-loc`
- Filter button in Books table UI
- Shows all books missing LOC call numbers
- Query: `SELECT b WHERE b.locNumber IS NULL OR b.locNumber = ''`

### Most Recent Day
- Endpoint: `GET /api/books/most-recent-day`
- Filter button in Books table UI
- Shows books added on the most recent date
- Useful for processing newly imported books

## Implementation Details

### Service Layer
- `LocBulkLookupService` - LOC lookup business logic
- Updates `book.lastModified` on successful lookup for cache invalidation
- Logs all lookup attempts and results

### Repository Layer
- `BookRepository.findBooksWithoutLocNumber()` - Find books needing LOC numbers
- Uses LEFT JOIN FETCH for performance

### Frontend
- `js/loc-bulk-lookup.js` - Bulk lookup UI (currently hidden from menu)
- `js/books-table.js` - Individual lookup buttons in table
- `js/labels.js` - PDF label generation UI
- `js/utils.js` - `formatLocForSpine()` utility function

## UI Integration

### Books Table
- Individual "Lookup" button per book (librarian only)
- Shows LOC number in table with special formatting
- Filter buttons: "Books Without LOC", "Books from Most Recent Day", "All Books"
- LOC numbers displayed as green `<code>` blocks when present

### Menu Items
- LOC Lookup section currently hidden from main menu
  - Functionality replaced by individual lookup buttons
  - Can be re-enabled by uncommenting in `index.html`
- Labels section visible in menu (for PDF generation)

## Related Files
- `LocBulkLookupService.java` - Core lookup logic
- `LabelsPdfService.java` - PDF generation for spine labels
- `BookRepository.java` - Queries for books without LOC
- `books-table.js` - UI for individual lookup buttons
- `loc-bulk-lookup.js` - UI for bulk lookup (hidden)
- `labels.js` - UI for PDF label generation
