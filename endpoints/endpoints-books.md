# Book Endpoints

## Book Filtering Endpoints

### GET /api/books/most-recent-day
Returns books from the most recent day OR books with temporary titles (date-pattern titles like "2025-01-10_14:30:00").

**Note:** This endpoint uses efficient database projections to avoid N+1 query issues. Returns the same data as `/api/books-from-feed/saved-books`.

**Authentication:** Public (permitAll)

**Response:** Array of SavedBookDto
```json
[
  {
    "id": 123,
    "title": "Book Title",
    "author": "Author Name",
    "library": "Library Name",
    "photoCount": 2,
    "needsProcessing": false,
    "locNumber": "PS3566.O5",
    "status": "ACTIVE",
    "grokipediaUrl": "https://..."
  },
  {
    "id": 124,
    "title": "2025-01-10_14:30:00",
    "author": null,
    "library": "Library Name",
    "photoCount": 1,
    "needsProcessing": true,
    "locNumber": null,
    "status": "ACTIVE",
    "grokipediaUrl": null
  }
]
```

**Fields:**
- `id` - Book ID
- `title` - Book title (temporary titles start with date pattern YYYY-MM-DD)
- `author` - Author name (optional)
- `library` - Library name (optional)
- `photoCount` - Number of photos associated with book
- `needsProcessing` - true if title matches temporary date pattern (YYYY-M-D or YYYY-MM-DD at start)
- `locNumber` - Library of Congress call number (optional)
- `status` - Book status (ACTIVE, ON_ORDER, LOST)
- `grokipediaUrl` - Grokipedia URL (optional)

**Use Case:**
- View recently added books
- Identify books needing AI processing (needsProcessing=true)
- Books page "Most Recent Day" filter

---

## Book Caching Endpoints

### GET /api/books/summaries
Returns lightweight book summaries (ID and lastModified timestamp) for browser caching.

**Authentication:** Public (permitAll)

**Response:** Array of BookSummaryDto
```json
[
  {
    "id": 1,
    "lastModified": "2025-01-01T12:00:00"
  },
  {
    "id": 2,
    "lastModified": "2025-01-02T12:00:00"
  }
]
```

---

### POST /api/books/by-ids
Fetches full book data for a list of book IDs.

**Authentication:** Public (permitAll)

**Request Body:** Array of Long (book IDs)
```json
[1, 2, 3]
```

**Response:** Array of BookDto (full book objects)

**Use Case:**
- Frontend fetches summaries to check what's changed
- Only requests full data for books that are new or modified
- Reduces bandwidth and improves performance

---

## AI-Assisted Cataloging

### POST /api/books/suggest-loc
Uses Grok AI to suggest a Library of Congress call number for a book.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:**
```json
{
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald"
}
```

**Response:**
```json
{
  "suggestion": "PS3511.I9 G7"
}
```

**Requirements:**
- User must have xAI API key configured in user settings
- Uses Grok-3-latest model
- 10-minute timeout for API calls

**Error Responses:**
- 400: Title is required
- 500: xAI API key not configured or API call failed

**Use Case:**
- AI-powered assistance for cataloging books
- Suggests LOC call numbers when title/author lookup fails

---

### PUT /api/books/{id}/book-by-photo
Uses Grok AI to extract book metadata from the book's first photo (cover image).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `id` - Book ID

**Response:** BookDto with AI-populated fields:
```json
{
  "id": 1,
  "title": "AI-extracted title",
  "authorId": 123,
  "publicationYear": 2020,
  "publisher": "AI-extracted publisher",
  "plotSummary": "AI-generated plot summary",
  "detailedDescription": "AI-generated detailed description",
  "relatedWorks": "Other works by the same author"
}
```

**Requirements:**
- Book must have at least one photo
- User must have xAI API key configured in user settings
- Uses Grok-4 model for vision
- 10-minute timeout for API calls

**Error Responses:**
- 404: Book not found
- 500: No photos found, xAI API key not configured, or API call failed
  - Returns error message as plain text body (e.g., "xAI API key not configured for user ID: 1")

**Use Case:**
- Bulk process books from photos selected in the Books page
- Extracts title, author, publication year, publisher, and descriptions
- Creates new author if not found in database

---

## Bulk Operations

### POST /api/books/delete-bulk
Deletes multiple books with partial success handling. Books that can be deleted are deleted; books with active loans are skipped.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** Array of Long (book IDs)
```json
[1, 2, 3]
```

**Response:** BulkDeleteResultDto
```json
{
  "deletedCount": 2,
  "failedCount": 1,
  "deletedIds": [1, 3],
  "failures": [
    {
      "id": 2,
      "title": "Book Title",
      "errorMessage": "Cannot delete book because it is currently checked out with 1 loan(s)."
    }
  ]
}
```

**Fields:**
- `deletedCount` - Number of books successfully deleted
- `failedCount` - Number of books that could not be deleted
- `deletedIds` - Array of IDs for successfully deleted books
- `failures` - Array of failure details for books that couldn't be deleted
  - `id` - Book ID
  - `title` - Book title
  - `errorMessage` - Reason deletion failed

**Behavior:**
- Deletes books in order, skipping any that have active loans
- Returns 200 OK even if some deletions fail (partial success)
- Frontend shows results modal with success/failure details

**Use Case:**
- Bulk delete books from the Books page
- Safe for books with loans - they are skipped, not blocking other deletions

---

**Related:** BookController.java, BookService.java, AskGrok.java, BookDto.java, BookSummaryDto.java, BulkDeleteResultDto.java
