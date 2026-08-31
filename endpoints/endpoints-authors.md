# Author Endpoints

## GET /api/authors/without-description
Returns authors that are missing brief biographies.

**Authentication:** Public (`permitAll()`)

**Response:** Array of AuthorDto with `bookCount` and `lastModified`

**Use Case:**
- Filter to find authors needing biographical information
- Matches Books page filtering functionality

---

## GET /api/authors/zero-books
Returns authors that have no associated books.

**Authentication:** Public (`permitAll()`)

**Response:** Array of AuthorDto with `bookCount` set to 0

**Use Case:**
- Identify orphaned author records
- Clean up database by removing unused authors

---

## GET /api/authors/without-grokipedia
Returns authors that are missing a Grokipedia URL.

**Authentication:** Public (`permitAll()`)

**Response:** Array of AuthorDto with `bookCount`

**Use Case:**
- Filter to find authors needing Grokipedia links
- Systematic data enrichment workflow
- Includes authors whose `grokipediaUrl` is `"-"` (N/A after a slow lookup found no working URL)

---

### POST /api/authors/grokipedia-lookup-bulk
Looks up Grokipedia URLs for selected authors.

**Authentication:** Requires `LIBRARIAN` authority

**Query Parameters:**
- `slow` (boolean, default `false`) — `false` is quick lookup (generated URL only). `true` is slow lookup (generated URL, then Grok candidates, then HTTP checks).

**Request Body:** Array of author IDs
```json
[1, 2, 3]
```

**Response:** Array of `GrokipediaLookupResultDto`

**Behavior:**
- Quick: HEAD-check `https://grokipedia.com/page/{Name_With_Underscores}`. Save on 2xx; save nothing on 4xx.
- Slow: same first. If that is not 2xx, ask Grok to search Grokipedia by author name only (a corresponding book is context, not in `q=`). Keep 2xx `grokipedia.com/page/...` URLs; discard 4xx. Save `"-"` only when no working URL is found.

---

## DELETE /api/authors/{id}
Deletes an author by ID.

**Authentication:** Requires `LIBRARIAN` authority

**Path Parameters:**
- `id` - Author ID to delete

**Response:**
- `204 No Content` - Author deleted successfully
- `409 Conflict` - Author has associated books and cannot be deleted
  - Body: `{ "message": "Cannot delete author because it has N associated books." }`

**Use Case:**
- Remove authors from the system
- Authors with books must have their books reassigned or deleted first

---

## POST /api/authors/delete-bulk
Deletes multiple authors. Authors with associated books are skipped rather than aborting the rest of the batch.

**Authentication:** Requires `LIBRARIAN` authority

**Request Body:** JSON array of author IDs, e.g. `[1, 2, 3]`

**Response:** `BulkDeleteResultDto`
- `deletedCount` - number deleted
- `failedCount` - number that could not be deleted
- `deletedIds` - IDs that were deleted
- `failures` - `{ id, title, errorMessage }` for each skipped author (`title` is the author name)

---

## PUT /api/authors/{id}/generate-missing
Fills blank author catalog fields by prompting Grok. Existing values are never overwritten. Name and `grokipediaUrl` are never changed.

Fillable fields: `dateOfBirth`, `dateOfDeath`, `religiousAffiliation`, `birthCountry`, `nationality`, `biographicalEssay`.

**Authentication:** Requires `LIBRARIAN` authority

**Path Parameters:**
- `id` - Author ID

**Response:** `AuthorEnrichmentResultDto`
- `authorId`, `name`
- `success` - false when Grok/parse failed
- `skipped` - true when every fillable field already had a value (Grok is not called)
- `filledFields` - DTO/prompt field names that were written (uses `biographicalEssay`, not `briefBiography`)
- `errorMessage` - present on failure
- `updatedAuthor` - `AuthorDto` after the update (or the unchanged author on skip/failure)

**Use Case:**
- Multi-select authors on the Authors page and generate missing catalog data
- Pair with filters such as without-description or without-birth-date

---

**Related:** AuthorController.java, AuthorService.java, AuthorDto.java, AuthorEnrichmentResultDto.java
