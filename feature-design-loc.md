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

Each strategy updates the book's `locNumber` and `lastModified` fields on success.

**Note**: The Book entity does not have an ISBN field. All lookups are based on title and author information.

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
- Endpoint: `GET /api/loc-bulk-lookup/books/missing-loc`
- Filter button in Books page bulk actions toolbar
- Shows all books missing LOC call numbers
- Query: `SELECT b WHERE b.locNumber IS NULL OR b.locNumber = ''`

### Most Recent Day
- Endpoint: `GET /api/loc-bulk-lookup/books/most-recent`
- Filter button in Books page bulk actions toolbar
- Shows books added on the most recent date
- Useful for processing newly imported books

### All Books with LOC Status
- Endpoint: `GET /api/loc-bulk-lookup/books`
- Shows all books with their current LOC status

## Implementation Details

### Service Layer
- `LocBulkLookupService` - LOC lookup business logic
- Updates `book.lastModified` on successful lookup for cache invalidation
- Logs all lookup attempts and results

### Repository Layer
- `BookRepository.findBooksWithoutLocNumber()` - Find books needing LOC numbers
- Uses LEFT JOIN FETCH for performance

### Frontend
- `frontend/src/pages/books/components/BookTable.tsx` - Individual lookup buttons in table
- `frontend/src/pages/books/components/BulkActionsToolbar.tsx` - Bulk lookup UI
- `frontend/src/pages/labels/LabelsPage.tsx` - PDF label generation UI
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
- Labels section visible in menu (for PDF generation)

## Related Files
- `src/main/java/com/muczynski/library/service/LocBulkLookupService.java` - Core lookup logic
- `src/main/java/com/muczynski/library/service/LabelsPdfService.java` - PDF generation for spine labels
- `src/main/java/com/muczynski/library/repository/BookRepository.java` - Queries for books without LOC
- `frontend/src/pages/books/components/BookTable.tsx` - UI for individual lookup buttons
- `frontend/src/pages/books/components/BulkActionsToolbar.tsx` - UI for bulk lookup
- `frontend/src/pages/labels/LabelsPage.tsx` - UI for PDF label generation
