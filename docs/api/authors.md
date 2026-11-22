# Author

Controller: `AuthorController`.

Base path: `/api/authors`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/authors

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `AuthorDto` fields: `id`, `name`, `alternateNames`, `dateOfBirth`, `dateOfDeath`, `religiousAffiliation`, `birthCountry`, `nationality`, `briefBiography`, `grokipediaUrl`, `invalidGrokipediaUrls`, `firstPhotoId`, `firstPhotoChecksum`, `bookCount`, `lastModified`, `books`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Full `AuthorDto` rows, including biographical essays. No paging. Ordered by the last word of the name, case-insensitive, nulls last.

## GET /api/authors/count

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

None.

Response: `200 OK` body `CountDto` fields: `count`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/authors/name-matches

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

- Query `name` (`String`, optional).

Response: `200 OK` body Array of `AuthorNameMatchDto` fields: `id`, `name`.

Paging: At most 10 rows.

Ordering: Not specified by this endpoint.

Notes:

Authors for the book form. At most ten matches. A name shorter than three characters returns an empty list. Canonical and alternate names match.

A name shorter than 3 characters returns an empty list. Canonical and alternate names match.

## GET /api/authors/summaries

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `AuthorSummaryDto` fields: `id`, `lastModified`, `name`. `id` and `lastModified` only. `name` is left unset. Biography columns are not loaded.

Paging: None. Every author is one row.

Ordering: None. The projection query has no `ORDER BY`.

Notes:

Load full rows with `POST /api/authors/by-ids`.

## GET /api/authors/filtered-summaries

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

- Query `mostRecent` (`boolean`, optional, default `false`).
- Query `withoutDescription` (`boolean`, optional, default `false`).
- Query `withoutGrokipedia` (`boolean`, optional, default `false`).
- Query `withGrokipedia` (`boolean`, optional, default `false`).
- Query `zeroBooks` (`boolean`, optional, default `false`).
- Query `withoutPhotos` (`boolean`, optional, default `false`).
- Query `withPhotos` (`boolean`, optional, default `false`).
- Query `withoutBirthDate` (`boolean`, optional, default `false`).
- Query `withoutDeathDate` (`boolean`, optional, default `false`).
- Query `notCanonical` (`boolean`, optional, default `false`).
- Query `hasYdlBook` (`boolean`, optional, default `false`).
- Query `hasYdlEbook` (`boolean`, optional, default `false`).
- Query `hasYdlAudio` (`boolean`, optional, default `false`).
- Query `hasEmuBook` (`boolean`, optional, default `false`).
- Query `hasEmuEbook` (`boolean`, optional, default `false`).
- Query `hasEmuAudio` (`boolean`, optional, default `false`).
- Query `hasAclaBook` (`boolean`, optional, default `false`).
- Query `hasAclaEbook` (`boolean`, optional, default `false`).
- Query `hasAclaAudio` (`boolean`, optional, default `false`).
- Query `withoutYdlBook` (`boolean`, optional, default `false`).
- Query `withoutYdlEbook` (`boolean`, optional, default `false`).
- Query `withoutYdlAudio` (`boolean`, optional, default `false`).
- Query `withoutEmuBook` (`boolean`, optional, default `false`).
- Query `withoutEmuEbook` (`boolean`, optional, default `false`).
- Query `withoutEmuAudio` (`boolean`, optional, default `false`).
- Query `withoutAclaBook` (`boolean`, optional, default `false`).
- Query `withoutAclaEbook` (`boolean`, optional, default `false`).
- Query `withoutAclaAudio` (`boolean`, optional, default `false`).
- Query `hasWccBook` (`boolean`, optional, default `false`).
- Query `hasWccEbook` (`boolean`, optional, default `false`).
- Query `hasWccAudio` (`boolean`, optional, default `false`).
- Query `withoutWccBook` (`boolean`, optional, default `false`).
- Query `withoutWccEbook` (`boolean`, optional, default `false`).
- Query `withoutWccAudio` (`boolean`, optional, default `false`).
- Query `hasMsuBook` (`boolean`, optional, default `false`).
- Query `hasMsuEbook` (`boolean`, optional, default `false`).
- Query `hasMsuAudio` (`boolean`, optional, default `false`).
- Query `withoutMsuBook` (`boolean`, optional, default `false`).
- Query `withoutMsuEbook` (`boolean`, optional, default `false`).
- Query `withoutMsuAudio` (`boolean`, optional, default `false`).
- Query `favoriteLists` (`String`, optional).
- Query `q` (`String`, optional).

Response: `200 OK` body Array of `AuthorSummaryDto` fields: `id`, `lastModified`, `name`. `id`, `lastModified`, and `name` are set.

Paging: None. Every matching author id is returned.

Ordering: None. The criteria query does not set an `ORDER BY`.

Notes:

Summaries for the Authors page after every active filter. The browser then loads full rows with /by-ids for these ids only.

Boolean flags default to false. Within one of YDL, EMU, ACLA, WCC, and MSU, Audio, Book, and Ebook OR. The libraries AND. A positive flag means at least one of the author's books has that format. A Without flag means no book has that flag true. An author with no books matches every Without flag. `favoriteLists` is comma-separated and applies only when the session principal name parses as a numeric user id. `q` matches the author name and related text. The Authors page loads full rows with `POST /api/authors/by-ids`.

## POST /api/authors/by-ids

Auth: Authenticated. method `@PreAuthorize("permitAll()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `AuthorDto` fields: `id`, `name`, `alternateNames`, `dateOfBirth`, `dateOfDeath`, `religiousAffiliation`, `birthCountry`, `nationality`, `briefBiography`, `grokipediaUrl`, `invalidGrokipediaUrls`, `firstPhotoId`, `firstPhotoChecksum`, `bookCount`, `lastModified`, `books`.

Paging: None. One request accepts at most 100 ids. A larger array is HTTP 400 with an empty body.

Ordering: Not guaranteed to follow the request body.

Notes:

Full `AuthorDto` rows. An empty array returns an empty list.

## GET /api/authors/without-description

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `AuthorSummaryDto` fields: `id`, `lastModified`, `name`. Summary rows for authors whose biographical essay is null. The column is a PostgreSQL OID, so the query uses `IS NULL` only.

Paging: None.

Ordering: None.

Notes:

None.

## GET /api/authors/zero-books

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `AuthorSummaryDto` fields: `id`, `lastModified`, `name`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/authors/most-recent-day

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `AuthorSummaryDto` fields: `id`, `lastModified`, `name`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/authors/availability

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `AuthorAvailabilityDto` fields: `authorId`, `hasYdlBook`, `hasYdlEbook`, `hasYdlAudio`, `hasEmuBook`, `hasEmuEbook`, `hasEmuAudio`, `hasAclaBook`, `hasAclaEbook`, `hasAclaAudio`, `hasWccBook`, `hasWccEbook`, `hasWccAudio`, `hasMsuBook`, `hasMsuEbook`, `hasMsuAudio`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Per-author YDL/EMU/ACLA holdings (book/ebook/audio) rolled up from books. Used by the Authors page filter chips. Separate from author lastModified caching so lookups that change book flags show up without bumping the author row.

## GET /api/authors/without-grokipedia

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `AuthorSummaryDto` fields: `id`, `lastModified`, `name`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/authors/{id}/photos

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body Array of `PhotoDto` fields: `id`, `contentType`, `caption`, `bookId`, `authorId`, `loanId`, `imageChecksum`, `dateTaken`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/authors/{id}/books

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body Array of `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/authors/{id}

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/authors/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `AuthorDto` fields: `id`, `name`, `alternateNames`, `dateOfBirth`, `dateOfDeath`, `religiousAffiliation`, `birthCountry`, `nationality`, `briefBiography`, `grokipediaUrl`, `invalidGrokipediaUrls`, `firstPhotoId`, `firstPhotoChecksum`, `bookCount`, `lastModified`, `books`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/authors/{id}/photos

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).
- Part `file` (`MultipartFile`, required).

Response: `201 Created` body `PhotoDto` fields: `id`, `contentType`, `caption`, `bookId`, `authorId`, `loanId`, `imageChecksum`, `dateTaken`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/authors

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `AuthorDto` (required).

Response: `201 Created` body `AuthorDto` fields: `id`, `name`, `alternateNames`, `dateOfBirth`, `dateOfDeath`, `religiousAffiliation`, `birthCountry`, `nationality`, `briefBiography`, `grokipediaUrl`, `invalidGrokipediaUrls`, `firstPhotoId`, `firstPhotoChecksum`, `bookCount`, `lastModified`, `books`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/authors/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).
- Body `AuthorDto` (required).

Response: `200 OK` body `AuthorDto` fields: `id`, `name`, `alternateNames`, `dateOfBirth`, `dateOfDeath`, `religiousAffiliation`, `birthCountry`, `nationality`, `briefBiography`, `grokipediaUrl`, `invalidGrokipediaUrls`, `firstPhotoId`, `firstPhotoChecksum`, `bookCount`, `lastModified`, `books`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/authors/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: Empty body (`204 No Content` when the method calls `noContent()`, otherwise `200` with `ok().build()`).

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/authors/delete-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body `BulkDeleteResultDto` fields: `deletedCount`, `failedCount`, `deletedIds`, `failures`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/authors/{id}/generate-missing

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `AuthorEnrichmentResultDto` fields: `authorId`, `name`, `success`, `skipped`, `filledFields`, `errorMessage`, `updatedAuthor`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/authors/{id}/generate-missing/start

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `GrokJobDto` fields: `jobId`, `kind`, `status`, `result`, `error`, `httpStatus`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Background version of `generateMissingData`: returns 202 with a job id at once; poll GET /api/grok-jobs/{jobId}. The job result is the same AuthorEnrichmentResultDto.

## POST /api/authors/normalize-names-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `AuthorNameNormalizationResultDto` fields: `authorId`, `name`, `before`, `after`, `changed`, `success`, `errorMessage`, `mergedIntoAuthorId`, `updatedAuthor`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Rewrites selected authors into canonical given-name-then-family-name form. The author row is renamed in place. When that name is already used, the selected author is merged into the existing one and deleted.

## POST /api/authors/grokipedia-lookup-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `List<Long>` (required).
- Query `slow` (`boolean`, optional, default `false`).

Response: `200 OK` body Array of `GrokipediaLookupResultDto` fields: `bookId`, `authorId`, `name`, `success`, `grokipediaUrl`, `errorMessage`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/authors/{authorId}/photos/{photoId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `authorId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/authors/{authorId}/photos/{photoId}/rotate-cw

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `authorId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/authors/{authorId}/photos/{photoId}/rotate-ccw

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `authorId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/authors/{authorId}/photos/{photoId}/move-left

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `authorId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/authors/{authorId}/photos/{photoId}/move-right

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `authorId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
