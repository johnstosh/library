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
- Books page "Recent Arrivals" filter
- Cache validation: compare lastModified with cached data, fetch only changed books

---

### GET /api/books/without-loc
Returns book summaries for **ACTIVE** books without a Library of Congress call number, excluding electronic resources and non-ACTIVE statuses (lost, withdrawn, on-order, requested). See issue #337.

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
- Identify books that need LOC number assignment
- Status chip "Without LOC" filters the in-memory book list; this endpoint is the summary query for the same set

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
- Includes books whose `grokipediaUrl` is `"-"` (N/A after a slow lookup found no working URL)

---

### POST /api/books/grokipedia-lookup-bulk
Looks up Grokipedia URLs for selected books.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Query Parameters:**
- `slow` (boolean, default `false`) — `false` is quick lookup (generated URL only). `true` is slow lookup (generated URL, then Grok candidates, then HTTP checks).

**Request Body:** Array of book IDs
```json
[1, 2, 3]
```

**Response:** Array of `GrokipediaLookupResultDto`

**Behavior:**
- Quick: HEAD-check `https://grokipedia.com/page/{Title_With_Underscores}` after stripping a trailing copy suffix (`", c. 3"`). Save on 2xx; save nothing on 4xx.
- Slow: same first. If that is not 2xx, ask Grok to search Grokipedia by title only (author is context, not in `q=`). Keep 2xx `grokipedia.com/page/...` URLs; discard 4xx. Save `"-"` only when no working URL is found.

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

**Request Body:** Array of Long (book IDs), at most 100. A larger array is rejected with 400. The browser loads a longer id list as one batch after another.
```json
[1, 2, 3]
```

**Response:** Array of BookDto (full book objects). Includes `authorGrokipediaUrl` copied from the related author so book tables and search can show the author's Grokipedia link. Includes `binding` (`HARDCOVER`, `SOFTCOVER`, `LIBRARY_BINDING`, `OTHER`, `UNKNOWN`; missing on legacy rows is treated as `UNKNOWN`).

**Use Case:**
- Frontend fetches summaries to check what's changed
- Only requests full data for books that are new or modified
- Reduces bandwidth and improves performance

---

### GET /api/books/filtered-summaries
Returns the same `BookSummaryDto` rows as `/summaries`, after every Books and Prices catalog filter. The browser still loads full rows with `/by-ids`, but only for these ids.

**Authentication:** Public (permitAll)

**Query Parameters:** All optional. Boolean chips default to `false` and are AND-combined. Send `true` only for chips that are on.
- `q` — title, alternate title, or author name
- `labels` — comma-separated; the book must have every label
- `status` — comma-separated OR group (`in-library`, `electronic-resource`, `without-loc`, `lost`, `withdrawn`, `on-order`, `requested`). Empty hides WITHDRAWN and REQUESTED
- `readingDifficulty` — comma-separated OR group; `unset` also matches a null difficulty
- `binding` — comma-separated OR group (`HARDCOVER`, `SOFTCOVER`, `LIBRARY_BINDING`, `OTHER`, `UNKNOWN`); `UNKNOWN` also matches a null binding
- `favoriteLists` — comma-separated list names for the signed-in user. Applied only when those lists resolve to at least one book
- `desireToPurchase` — comma-separated `0`–`10` and/or `unset` (Prices)
- `priceOlderDays` — days for `priceOlder` (default 90)
- Boolean chips: `freeText`, `audio`, `mostRecent`, `withoutGrokipedia`, `withGrokipedia`, `withoutGenres`, `withoutFreeTextUrls`, `withoutProperPlotOrDescription`, `hasYdlAudio`, `hasYdlBook`, `hasYdlEbook`, `hasEmuAudio`, `hasEmuBook`, `hasEmuEbook`, `hasAclaAudio`, `hasAclaBook`, `hasAclaEbook`, `withPrices`, `noPrices`, `priceOlder`, `lookupErrors`

**Response:** Array of BookSummaryDto

**Behavior:**
- `mostRecent` uses the catalog-wide recent window (the latest add date, the day before it, and temporary date titles), AND the other filters.
- Patrons never receive REQUESTED books from this list.
- `withoutProperPlotOrDescription` keeps a book unless both the plot and the detailed description are at least 400 characters after trim.
- Each row includes `dateAddedToLibrary`. The list is most-recent first, with missing dates last.
- The Books page loads full rows for the first 100 matches, then the next 100 when the user scrolls to the bottom or clicks Load more. Each `/by-ids` request stays at most 100 ids.

### GET /api/books/checkout-matches
Active books for the checkout form. Returns at most 10 rows, best match first. A title, author, or call number shorter than 3 characters is ignored. The response is empty when every field is shorter than that. A book matches when any searched field contains the text. A containing match scores 10 and an exact match scores 20 more. Call numbers are compared with whitespace removed. Plot and description are not included.

**Authentication:** Public (permitAll)

**Query Parameters:** `title`, `author`, `locNumber`. All optional.

**Response:** Array of `{ id, title, author, locNumber, status }`

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
- 400: Book has no photos. The book is left unchanged (no placeholder title or random author).
  Body: `{"error": "This book has no photos, so Book from Image has nothing to read.", "message": "...", "code": "BOOK_HAS_NO_PHOTOS"}`
- 402: Grok is out of credits (see "Grok out of credits" below). The book is left unchanged.
- 500: Book not found, xAI API key not configured, or other API failure
  - Returns error message as plain text body (e.g., "xAI API key not configured for user ID: 1")
- `PUT /api/books/{id}/book-from-first-photo` behaves the same (400 message: "This book has no photos, so Book from First Photo has nothing to read.")

**Use Case:**
- Bulk process books from photos selected in the Books page
- Extracts title, author, publication year, publisher, and descriptions
- Creates new author if not found in database

---

### PUT /api/books/{id}/title-author-from-photo
Uses Grok AI vision to extract only the book title and author name from the book's first photo. Returns a preview BookDto for the edit form. **Does not persist the book.** The user must click Update to save, or Cancel to discard. A new Author record may be created if the extracted name is not already in the catalog.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameters:**
- `id` - Book ID

**Response:** BookDto with extracted `title` and `authorId` (other fields unchanged from the current book):
```json
{
  "id": 1,
  "title": "Extracted title",
  "authorId": 123,
  "author": "Extracted Author"
}
```

**Requirements:**
- Book must have at least one photo
- User must have xAI API key configured in user settings
- Uses Grok flagship vision model
- 10-minute timeout for API calls

**Error Responses:**
- 402: Grok is out of credits (see "Grok out of credits" below)
- 500: Book not found, no photos found, xAI API key not configured, or API call failed

**Use Case:**
- Book edit page "Title & Author from First Photo" button
- Preview extracted catalog fields in the form before saving

---

### Background Grok jobs (start + poll)
A Grok call can take minutes (grok-4.7 ~200s). One fetch held open that long can fail in the browser
("Failed to fetch") even when the server succeeds, so the frontend uses start-and-poll endpoints.
The synchronous endpoints remain for other callers with unchanged response shapes.

| Start endpoint (POST, LIBRARIAN) | Same work / result as |
|---|---|
| `/api/books/{id}/book-by-photo/start` | `PUT /api/books/{id}/book-by-photo` |
| `/api/books/{id}/book-from-first-photo/start` | `PUT /api/books/{id}/book-from-first-photo` |
| `/api/books/{id}/title-author-from-photo/start` | `PUT /api/books/{id}/title-author-from-photo` |
| `/api/books/{id}/book-from-title-author/start` (body `{"title", "authorName"}`) | `PUT /api/books/{id}/book-from-title-author` |
| `/api/authors/{id}/generate-missing/start` | `PUT /api/authors/{id}/generate-missing` |
| `/api/books-from-feed/process-single/{bookId}/start` | `POST /api/books-from-feed/process-single/{bookId}` |

**Start response:** `202 Accepted`, `Location: /api/grok-jobs/{jobId}`
```json
{"jobId": "3f2c...", "kind": "book-from-title-author", "status": "RUNNING"}
```

### GET /api/grok-jobs/{jobId}
Poll every few seconds (frontend: 3s). Authenticated; a job is visible only to the user who started it.
- `{"status": "RUNNING"}` - keep polling
- `{"status": "SUCCEEDED", "result": {...}}` - `result` is exactly what the synchronous endpoint returns
- `{"status": "FAILED", "httpStatus": 402, "error": "Grok is out of credits..."}` - `error`/`httpStatus`
  match the synchronous endpoint (402 out of credits, 400 no photos, 422 business rule, 500 other)
- `404 {"error": "This Grok request is no longer available..."}` - unknown, expired, or another user's job

**Limits (in memory, 512Mi container):** at most 50 jobs kept, 3 run at once with a queue of 20
(`503 {"code": "GROK_JOBS_BUSY"}` when full); finished jobs expire after 15 minutes. Only the result
DTO / error text is kept, never photo bytes. Relies on a single Cloud Run instance with CPU always
allocated (deploy.sh `--max-instances 1 --no-cpu-throttling`); a restart drops running jobs (poll returns 404).
The Grok call holds no DB transaction: photo/book data is read and results are saved in short transactions.

---

### Grok out of credits (all Grok-backed endpoints)
When xAI refuses a Grok call because the team is out of credits or hit its monthly spending limit
(HTTP 402, or HTTP 403/429 whose body mentions credits or a spending limit), `AskGrok` throws
`GrokCreditsExhaustedException`. A plain 429 rate limit without that wording is not treated this way.

- Single-book endpoints (`book-from-title-author`, `book-by-photo`, `book-from-first-photo`,
  `title-author-from-photo`, `suggest-loc`, `/api/loans/transcribe-checkout-card`) return **HTTP 402**:
  ```json
  {"error": "Grok is out of credits. Add credits or raise the spending limit at console.x.ai, then try again.",
   "message": "Grok is out of credits. Add credits or raise the spending limit at console.x.ai, then try again.",
   "code": "GROK_CREDITS_EXHAUSTED"}
  ```
- Per-item result endpoints (lookup genres, reading difficulty, author generate-missing, Grokipedia slow
  lookup, LOC bulk AI fallback) return 200 with the same message in each failed item's `errorMessage`.

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

### POST /api/books/lookup-reading-difficulty-bulk
Fills reading difficulty on selected books using Grok AI. The frontend sends up to 10 IDs per request; the backend also batches unset books into groups of 10 per Grok prompt.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** Array of Long (book IDs)
```json
[1, 2, 3]
```

**Response:** Array of ReadingDifficultyLookupResultDto
```json
[
  {
    "bookId": 1,
    "title": "Little Women",
    "success": true,
    "suggestedDifficulty": "children",
    "updatedBook": { "id": 1, "title": "Little Women", "readingDifficulty": "children" }
  },
  {
    "bookId": 2,
    "title": "Summa Theologica",
    "success": false,
    "errorMessage": "Already has a reading difficulty"
  }
]
```

**Behavior:**
- Books that already have a non-`unset` difficulty are skipped
- Missing IDs are reported as `"Book not found"`
- Grok is asked for a JSON array of candidate difficulty keys per book (most appropriate first); the first assignable key is stored
- Valid keys: `children`, `accessible`, `moderate`, `demanding`, `advanced`
- On success, `updatedBook` is returned so the frontend can seed its cache without a follow-up fetch

---

## POST /api/acla-lookup/lookup/{bookId}
Looks up paper/ebook/audio holdings for one book at the Allegheny County Library Association catalog (`acl.bibliocommons.com`) and stores the result on the book.

**Authentication:** Public (`permitAll`)

**Path Parameters:**
- `bookId` - Book ID to look up

**Response:** `AclaLookupResultDto`
```json
{
  "bookId": 1,
  "success": true,
  "audioAvailable": true,
  "paperAvailable": true,
  "ebookAvailable": false,
  "matchedTitle": "Pride and Prejudice"
}
```

**Behavior:**
- Temporary date-format titles are skipped (`Not Ready - Temporary title`)
- Title matching is exact after normalization; short titles (4 words or fewer) also require an author last-name match
- A completed search that finds no match clears stale holdings and returns `Not held by ACLA`
- HTTP and other lookup failures are stored on the book as `aclaLookupError` (max 255 characters) and returned as `success: false`. HTTP errors are recorded as `Error: HTTP {status}` without the response body, so a block/error page cannot overflow the column.
- BiblioCommons 403/429/502/503/504 and I/O timeouts are retried with backoff (`acla.lookup.retries`, `acla.lookup.backoff-ms`). A short pause (`acla.lookup.request-delay-ms`) is inserted between outbound searches so the Books-page bulk carousel does not trip the catalog CDN.
- Follow-up format searches that still fail after retries are skipped: holdings already found on the unfiltered title search are kept instead of recording `Error: HTTP 403 Forbidden` for the whole book.
- Same pattern as `/api/ydl-lookup/lookup/{bookId}` and `/api/emu-lookup/lookup/{bookId}`

**Use Case:**
- Books page bulk-action carousel "Lookup ACLA Availability"
- Book edit/view page "Lookup ACLA Availability"

---

**Related:** BookController.java, BookService.java, AskGrok.java, BookDto.java, BookSummaryDto.java, BulkDeleteResultDto.java, ReadingDifficultyLookupResultDto.java
