# Book Endpoints

## Book Filtering Endpoints

All book filter endpoints return **BookSummaryDto** (id + lastModified) for cache validation. Use `/api/books/by-ids` to fetch full book data for the IDs you need.

### GET /api/books/most-recent-day
Returns book summaries for books from the most recent 2 days OR books with temporary titles (date-pattern titles like "2025-01-10_14:30:00").

**Note:** This endpoint returns lightweight summaries for cache validation. Use `/api/books/by-ids` to fetch full book data. Includes 2 days to handle timezone differences.

**Authentication:** Public (permitAll)

**Response:** Array of BookSummaryDto
```json
[
  {
    "id": 123,
    "lastModified": "2025-01-10T14:30:00"
  },
  {
    "id": 124,
    "lastModified": "2025-01-10T15:00:00"
  }
]
```

**Use Case:**
- Books page "Most Recent Day" filter
- Cache validation: compare lastModified with cached data, fetch only changed books

---

### GET /api/books/without-loc
Returns book summaries for books without a Library of Congress call number.

**Authentication:** Public (permitAll)

**Response:** Array of BookSummaryDto
```json
[
  {
    "id": 123,
    "lastModified": "2025-01-10T14:30:00"
  }
]
```

**Use Case:**
- Books page "Without LOC" filter
- Identify books that need LOC number assignment

---

### GET /api/books/by-3letter-loc
Returns book summaries for books with 3-letter LOC call number prefixes (e.g., "ABC 123.45").

**Authentication:** Public (permitAll)

**Response:** Array of BookSummaryDto
```json
[
  {
    "id": 123,
    "lastModified": "2025-01-10T14:30:00"
  }
]
```

**Use Case:**
- Books page "3-Letter LOC" filter
- Identify books with potentially incorrect 3-letter LOC prefixes

---

### GET /api/books/without-grokipedia
Returns book summaries for books without a Grokipedia URL.

**Authentication:** Public (permitAll)

**Response:** Array of BookSummaryDto
```json
[
  {
    "id": 123,
    "lastModified": "2025-01-10T14:30:00"
  }
]
```

**Use Case:**
- Books page "Without Grokipedia" filter
- Identify books that need Grokipedia lookup

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
Uses Grok AI to extract book metadata from all of the book's photos (cover, spine, back cover, table of contents, etc.).

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

**Photo Analysis:**
- **All photos** associated with the book are analyzed together by AI (not just the first photo)
- This provides more comprehensive information from cover, spine, back cover, table of contents, etc.
- AI receives all images in a single request for better context
- Photos with missing or null image data are automatically skipped without causing errors

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
