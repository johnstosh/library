# Book Endpoints

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

**Related:** BookController.java, BookService.java, AskGrok.java, BookDto.java, BookSummaryDto.java
