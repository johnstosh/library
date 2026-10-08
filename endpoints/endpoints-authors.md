# Author Endpoints

## GET /api/authors/filtered-summaries
Returns author summaries (id, name, and lastModified) after every Authors page filter. The browser sorts by last name and loads full rows for the first 100, then the next 100 when the user scrolls to the bottom or clicks Load more. Each `/by-ids` request is at most 100 ids. A larger body is rejected with 400. The older single-chip endpoints below remain.

**Authentication:** Public (`permitAll()`)

**Query Parameters:** All optional. Boolean chips default to `false` and are AND-combined.
- `mostRecent` — authors of books added on the calendar day of the latest `dateAddedToLibrary`
- `withoutDescription` — biographical essay is null
- `withoutGrokipedia` / `withGrokipedia` — missing (`null`, blank, or `"-"`) or present
- `zeroBooks` — no books
- `withoutPhotos` / `withPhotos` — author photos (photos not attached to a book)
- `withoutBirthDate` / `withoutDeathDate`
- `notCanonical` — the name is not already given-name then family-name (comma-inverted, appended years, initials with a parenthetical expansion, or an editor or translator credit). A comma inside a phrase is not an inversion. Blank names are not flagged.
- Availability: `hasYdlBook`, `hasYdlEbook`, `hasYdlAudio`, `hasEmuBook`, `hasEmuEbook`, `hasEmuAudio`, `hasAclaBook`, `hasAclaEbook`, `hasAclaAudio` — the author has at least one book with that flag
- `favoriteLists` — comma-separated list names for the signed-in user. Applied only when those lists resolve to at least one author

**Response:** Array of AuthorSummaryDto

---

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

## POST /api/authors/normalize-names-bulk
Rewrites selected authors into canonical given-name-then-family-name form.

**Authentication:** Requires `LIBRARIAN` authority

**Request Body:** JSON array of author IDs, e.g. `[1, 2, 3]`

**Response:** Array of `AuthorNameNormalizationResultDto`
- `authorId`, `name` (name after the operation), `before`, `after`
- `changed` — true when the stored name changed
- `success` — false when the author is missing or the canonical name is already used
- `errorMessage` — `Author not found`, or `Name "..." is already used by another author`
- `updatedAuthor` — set only when the name changed

**Behavior:**
- Comma inversion (`Simpson, Richard` → `Richard Simpson`), birth and death years, and initials with a parenthetical expansion (`Johnson, B. J.-P. (Barney John-Paul)` → `Barney John Paul Johnson`) are cleaned. Initials without an expansion stay initials. A comma inside a phrase (`Sisters of Charity of Our Lady, Mother of the Church`, `Ignatius, of Loyola`) stays, and those words are not reordered.
- Editor and translator credits are removed (`Gasquet (ed.)`, `Bagshawe (tr.)`, `translated by …`, `, editor`). The credit is not saved. A fuller name in parentheses (`Almedingen, E. M. (Edith Martha)`) and an edition note (`(Benziger ed.)`, `(English edition)`) stay.
- A suffix stays after the name and loses its comma. The letters and periods stay as stored. A no-space run of 2–6 capital letters is a suffix (`SJ`, `S.J.`, `OCD`, `O.S.B.`, `D.D.`), as are the mixed forms `CSSp` and `O.Praem.` Spaced initials in the given-name slot (`B. J.`, `C. L`, `T. E`) stay given names and are turned around. `Inc` stays at the end and loses the comma (`Japan Travel Bureau, Inc` becomes `Japan Travel Bureau Inc`).
- The managed author row is renamed in place, so portraits stay attached. The old form is not stored as an alternate name.
- When two selected authors canonicalize to a name that is already used, or to the same new name, the lowest id is renamed and the other is skipped.
- Authors that already conform are left unchanged.

**Use Case:** Authors page filter "Not canonical" and bulk-action carousel "Canonical Author Names"

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
- The author edit form uses this endpoint with a single ID for the Quick/Slow lookup buttons next to the Grokipedia URL field.

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

## Alternate names

`AuthorDto.alternateNames` is a list of other catalog forms of the same person (a pen name, a saint's name, a Latin name, or a spelling catalogs use). It is stored as a PostgreSQL `text[]` column, `author.alternate_names`.

The canonical `name` stays the display name. Blank lines, case-insensitive duplicates, and a repeat of the canonical name are dropped on create, update, and import.

Search, checkout matching, free-text lookup, and ACLA, YDL, EMU, and LOC lookups try each name. Library catalogs match the last word of each name.

JSON import and export use the same `alternateNames` array on each author. An empty list is omitted from export.

**Related:** AuthorController.java, AuthorService.java, AuthorDto.java, AuthorEnrichmentResultDto.java
