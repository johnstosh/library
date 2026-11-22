# Book

Controller: `BookController`.

Base path: `/api/books`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/books

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Returns every `BookDto`, including plot and description text. There is no paging. A caller who is not a librarian does not receive books whose status is `REQUESTED`.

## GET /api/books/without-loc

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `BookSummaryDto` fields: `id`, `lastModified`, `dateAddedToLibrary`. Summary rows (`id`, `lastModified` only). Active books with no call number, excluding electronic resources and non-active statuses.

Paging: None.

Ordering: None.

Notes:

None.

## GET /api/books/most-recent-day

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `BookSummaryDto` fields: `id`, `lastModified`, `dateAddedToLibrary`. Summary rows (`id`, `lastModified` only). Books whose `dateAddedToLibrary` falls on the newest calendar day in the catalog or the day before, plus titles that look like `YYYY-M-D` (temporary titles).

Paging: None.

Ordering: None.

Notes:

Get book summaries (id + lastModified) from most recent 2 days OR with temporary titles. Returns BookSummaryDto for cache validation - use /books/by-ids to fetch full data.

## GET /api/books/without-grokipedia

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `BookSummaryDto` fields: `id`, `lastModified`, `dateAddedToLibrary`. Summary rows (`id`, `lastModified` only). `grokipediaUrl` is null, empty, or `-`.

Paging: None.

Ordering: None.

Notes:

None.

## GET /api/books/{id}

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

One full `BookDto`. A non-librarian who asks for a `REQUESTED` book does not receive it.

## POST /api/books

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Body `BookDto` (required).

Response: `201 Created` body `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/books/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).
- Body `BookDto` (required).

Response: `200 OK` body `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/books/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: Empty body (`204 No Content` when the method calls `noContent()`, otherwise `200` with `ok().build()`).

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/books/delete-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body `BulkDeleteResultDto` fields: `deletedCount`, `failedCount`, `deletedIds`, `failures`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/books/grokipedia-lookup-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Body `List<Long>` (required).
- Query `slow` (`boolean`, optional, default `false`).

Response: `200 OK` body Array of `GrokipediaLookupResultDto` fields: `bookId`, `authorId`, `name`, `success`, `grokipediaUrl`, `errorMessage`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Body is a JSON array of book ids. `slow` defaults to false.

## POST /api/books/{id}/clone

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `201 Created` body `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/books/{bookId}/photos

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `bookId` (`Long`).
- Part `file` (`MultipartFile`, required).

Response: `201 Created` body `PhotoDto` fields: `id`, `contentType`, `caption`, `bookId`, `authorId`, `loanId`, `imageChecksum`, `dateTaken`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/books/{bookId}/photos

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `bookId` (`Long`).

Response: `200 OK` body Array of `PhotoDto` fields: `id`, `contentType`, `caption`, `bookId`, `authorId`, `loanId`, `imageChecksum`, `dateTaken`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/books/{bookId}/photos/{photoId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `bookId` (`Long`).
- Path `photoId` (`Long`).
- Body `PhotoDto` (required).

Response: `200 OK` body `PhotoDto` fields: `id`, `contentType`, `caption`, `bookId`, `authorId`, `loanId`, `imageChecksum`, `dateTaken`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/books/{bookId}/photos/{photoId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `bookId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/books/{bookId}/photos/{photoId}/rotate-cw

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `bookId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/books/{bookId}/photos/{photoId}/rotate-ccw

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `bookId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/books/{id}/book-by-photo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/books/{id}/book-from-first-photo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/books/{id}/title-author-from-photo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Preview title and author extracted from the book's first photo. Does not persist the book; the edit form applies the values until Update or Cancel.

## PUT /api/books/{id}/book-from-title-author

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).
- Body `Map<String, String>` (required).

Response: `200 OK` body `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Not `@Transactional`: Grok HTTP must not hold a pool connection, and catch-and-return must not leave a controller txn rollback-only (#362).

## POST /api/books/{id}/book-by-photo/start

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `GrokJobDto` fields: `jobId`, `kind`, `status`, `result`, `error`, `httpStatus`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/books/{id}/book-from-first-photo/start

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `GrokJobDto` fields: `jobId`, `kind`, `status`, `result`, `error`, `httpStatus`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/books/{id}/title-author-from-photo/start

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `GrokJobDto` fields: `jobId`, `kind`, `status`, `result`, `error`, `httpStatus`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/books/{id}/book-from-title-author/start

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).
- Body `Map<String, String>` (required).

Response: `200 OK` body `GrokJobDto` fields: `jobId`, `kind`, `status`, `result`, `error`, `httpStatus`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/books/{bookId}/photos/{photoId}/move-left

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `bookId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/books/{bookId}/photos/{photoId}/move-right

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `bookId` (`Long`).
- Path `photoId` (`Long`).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/books/count

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

None.

Response: `200 OK` body `CountDto` fields: `count`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

A non-librarian count omits `REQUESTED` books.

## GET /api/books/summaries

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `BookSummaryDto` fields: `id`, `lastModified`, `dateAddedToLibrary`. This is the ids-only list. The query sets `id` and `lastModified` only. `dateAddedToLibrary` is left unset. It does not return titles, authors, or essays, and it does not hide `REQUESTED` books.

Paging: None. Every book is one row in a single response.

Ordering: None. `SELECT b.id, b.lastModified FROM Book b` has no `ORDER BY`.

Notes:

Use this for cache checks. Load full rows with `POST /api/books/by-ids`.

## GET /api/books/filtered-summaries

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Query `q` (`String`, optional).
- Query `labels` (`String`, optional).
- Query `status` (`String`, optional).
- Query `readingDifficulty` (`String`, optional).
- Query `binding` (`String`, optional).
- Query `favoriteLists` (`String`, optional).
- Query `desireToPurchase` (`String`, optional).
- Query `priceOlderDays` (`int`, optional, default `90`).
- Query `freeText` (`boolean`, optional, default `false`).
- Query `audio` (`boolean`, optional, default `false`).
- Query `mostRecent` (`boolean`, optional, default `false`).
- Query `withoutGrokipedia` (`boolean`, optional, default `false`).
- Query `withGrokipedia` (`boolean`, optional, default `false`).
- Query `withoutGenres` (`boolean`, optional, default `false`).
- Query `withoutFreeTextUrls` (`boolean`, optional, default `false`).
- Query `withoutProperPlotOrDescription` (`boolean`, optional, default `false`).
- Query `titleNotChicago` (`boolean`, optional, default `false`).
- Query `authorNotCanonical` (`boolean`, optional, default `false`).
- Query `hasYdlAudio` (`boolean`, optional, default `false`).
- Query `hasYdlBook` (`boolean`, optional, default `false`).
- Query `hasYdlEbook` (`boolean`, optional, default `false`).
- Query `hasEmuAudio` (`boolean`, optional, default `false`).
- Query `hasEmuBook` (`boolean`, optional, default `false`).
- Query `hasEmuEbook` (`boolean`, optional, default `false`).
- Query `hasAclaAudio` (`boolean`, optional, default `false`).
- Query `hasAclaBook` (`boolean`, optional, default `false`).
- Query `hasAclaEbook` (`boolean`, optional, default `false`).
- Query `withoutYdlAudio` (`boolean`, optional, default `false`).
- Query `withoutYdlBook` (`boolean`, optional, default `false`).
- Query `withoutYdlEbook` (`boolean`, optional, default `false`).
- Query `withoutEmuAudio` (`boolean`, optional, default `false`).
- Query `withoutEmuBook` (`boolean`, optional, default `false`).
- Query `withoutEmuEbook` (`boolean`, optional, default `false`).
- Query `withoutAclaAudio` (`boolean`, optional, default `false`).
- Query `withoutAclaBook` (`boolean`, optional, default `false`).
- Query `withoutAclaEbook` (`boolean`, optional, default `false`).
- Query `hasWccAudio` (`boolean`, optional, default `false`).
- Query `hasWccBook` (`boolean`, optional, default `false`).
- Query `hasWccEbook` (`boolean`, optional, default `false`).
- Query `withoutWccAudio` (`boolean`, optional, default `false`).
- Query `withoutWccBook` (`boolean`, optional, default `false`).
- Query `withoutWccEbook` (`boolean`, optional, default `false`).
- Query `hasMsuAudio` (`boolean`, optional, default `false`).
- Query `hasMsuBook` (`boolean`, optional, default `false`).
- Query `hasMsuEbook` (`boolean`, optional, default `false`).
- Query `withoutMsuAudio` (`boolean`, optional, default `false`).
- Query `withoutMsuBook` (`boolean`, optional, default `false`).
- Query `withoutMsuEbook` (`boolean`, optional, default `false`).
- Query `withPrices` (`boolean`, optional, default `false`).
- Query `noPrices` (`boolean`, optional, default `false`).
- Query `priceOlder` (`boolean`, optional, default `false`).
- Query `lookupErrors` (`boolean`, optional, default `false`).
- Query `libraryCheckedOlder` (`String`, optional).
- Query `libraryCheckedOlderDays` (`int`, optional, default `90`).
- Query `libraryLookupError` (`String`, optional).
- Query `freeTextSearchedOlderDays` (`Integer`, optional).
- Query `descriptionMinChars` (`Integer`, optional).
- Query `descriptionMaxChars` (`Integer`, optional).

Response: `200 OK` body Array of `BookSummaryDto` fields: `id`, `lastModified`, `dateAddedToLibrary`. `id`, `lastModified`, and `dateAddedToLibrary` are all set.

Paging: None. Every id that matches the filters is returned.

Ordering: `dateAddedToLibrary` descending, nulls last, then `id` ascending.

Notes:

Summaries for the Books and Prices pages after every active filter. The browser then loads full rows with /by-ids for these ids only.

Boolean flags default to false. Omitted string filters are off. `q` matches title and author text. `labels`, `status`, `readingDifficulty`, `binding`, `favoriteLists`, and `desireToPurchase` are comma-separated. `status` tokens: `in-library`, `electronic-resource`, `without-loc`, `lost`, `withdrawn`, `on-order`, `requested`. Selected status tokens OR together. With no status token, `WITHDRAWN` and `REQUESTED` stay hidden. `in-library` is active with a call number. `without-loc` is active, no call number, and not electronic. `readingDifficulty` tokens: `children`, `accessible`, `moderate`, `demanding`, `advanced`, `unset`. `binding` tokens are `BookCoverType` names: `HARDCOVER`, `SOFTCOVER`, `LIBRARY_BINDING`, `OTHER`, `UNKNOWN`. Unknown tokens are ignored. `desireToPurchase` tokens are integers 0 through 10, plus `unset`. `favoriteLists` applies only when the session principal name parses as a numeric user id. Within one of YDL, EMU, ACLA, WCC, and MSU, the Audio, Book, and Ebook flags OR. The five libraries AND. Each `without*` flag matches false or null, and the three Without flags for one library AND. `libraryCheckedOlder` is `YDL`, `EMU`, `ACLA`, `WCC`, or `MSU`. Anything else turns it off. A missing last-check timestamp matches. A stored timestamp matches only when it is strictly before now (UTC) minus `libraryCheckedOlderDays` (default 90). The same codes and age rule apply to `libraryLookupError`, which keeps rows whose stored message for that library starts with `Error:`. `freeTextSearchedOlderDays` is off unless it is greater than 0. A missing free-text search timestamp then matches. A stored timestamp must be strictly older than the cutoff. `priceOlderDays` defaults to 90 and is used when `priceOlder` is true. `titleNotChicago` and `authorNotCanonical` are applied in Java after the summary query. `descriptionMinChars` and `descriptionMaxChars` are optional inclusive bounds on the character length of `detailedDescription` after trim. A null, empty, or whitespace-only description counts as 0. Spaces, tabs, and newlines at either end are not counted. The plot is not measured. Omit a bound to leave that side off. Zero is allowed. A negative value is HTTP 400 with the text `descriptionMinChars and descriptionMaxChars must be zero or greater`. The two bounds AND with each other and with the other filters, including `freeText`. The check is SQL (`btrim` and `length`) and does not load the essay. Search does not accept these parameters. Books, Prices, and Savings do not send them and have no control for them. Callers can sort the returned ids and load full rows 10 at a time with `POST /api/books/by-ids` (the batch cap is 100). A non-librarian does not receive `REQUESTED` books. The Books page loads full rows for these ids with `POST /api/books/by-ids`.

## POST /api/books/by-ids

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `BookDto` fields: `id`, `title`, `alternateTitles`, `publicationYear`, `publisher`, `plotSummary`, `relatedWorks`, `detailedDescription`, `grokipediaUrl`, `invalidGrokipediaUrls`, `freeTextUrl`, `invalidFreeTextUrls`, `invalidFreeAudioUrls`, `invalidHoldings`, `freeTextLastSearched`, `dateAddedToLibrary`, `lastModified`, `status`, `authorId`, `author`, `authorGrokipediaUrl`, `libraryId`, `library`, `firstPhotoId`, `firstPhotoChecksum`, `loanCount`, `locNumber`, `electronicResource`, `statusReason`, `ydlAudioAvailable`, `ydlPaperAvailable`, `ydlEbookAvailable`, `ydlLastChecked`, `ydlLookupError`, `emuAudioAvailable`, `emuPaperAvailable`, `emuEbookAvailable`, `emuLastChecked`, `emuLookupError`, `aclaAudioAvailable`, `aclaPaperAvailable`, `aclaEbookAvailable`, `aclaLastChecked`, `aclaLookupError`, `wccAudioAvailable`, `wccPaperAvailable`, `wccEbookAvailable`, `wccLastChecked`, `wccLookupError`, `msuAudioAvailable`, `msuPaperAvailable`, `msuEbookAvailable`, `msuLastChecked`, `msuLookupError`, `readingDifficulty`, `binding`, `desireToPurchase`, `tagsList`. Full `BookDto` rows, not summaries. An empty array returns an empty list.

Paging: None. One request accepts at most 100 ids (`ByIds.MAX_BATCH`). A larger array is HTTP 400 with the text body `At most 100 book ids`.

Ordering: Not the order of the request body. Ids the caller cannot see are omitted.

Notes:

A non-librarian does not receive `REQUESTED` books. This is the batch read that follows `/summaries` or `/filtered-summaries`.

## GET /api/books/checkout-matches

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Query `title` (`String`, optional).
- Query `author` (`String`, optional).
- Query `locNumber` (`String`, optional).

Response: `200 OK` body Array of `CheckoutMatchDto` fields: `id`, `title`, `author`, `locNumber`, `status`.

Paging: At most 10 rows.

Ordering: Best score first, then title, then id. A field shorter than 3 characters is ignored. The list is empty when every field is shorter than that.

Notes:

Active books for the checkout form. At most ten matches. A title, author, or call number shorter than three characters is ignored, and the response is empty when every field is shorter than that.

Active books only. Title, author (including alternate names), and call number are scored separately.

## POST /api/books/suggest-loc

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Body `Map<String, String>` (required).

Response: `200 OK` body `Map<String, String>`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

JSON object with `title` (required) and `author`. Response JSON is `{ "suggestion": "..." }`. A blank title is HTTP 400 `{ "error": "Title is required" }`.

## POST /api/books/{id}/lookup-genres

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `GenreLookupResultDto` fields: `bookId`, `title`, `success`, `suggestedGenres`, `errorMessage`, `updatedBook`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Not `@Transactional`: Grok HTTP must not hold a pool connection, and service catch-and-return must not leave a controller txn rollback-only (#361).

Not transactional. The Grok call must not hold a database connection. The prompt is the title, a description excerpt, a short plot excerpt, and the author name. The bulk Lookup Genres button retries that book when the connection drops or Cloud Run returns 502, 503, or 504.

## POST /api/books/lookup-genres-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `GenreLookupResultDto` fields: `bookId`, `title`, `success`, `suggestedGenres`, `errorMessage`, `updatedBook`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Not `@Transactional`: same isolation as `lookupGenresForBook`.

Not transactional. Body is a JSON array of book ids. No batch cap in the controller. The Books page sends one id at a time and retries that id when the connection drops or Cloud Run returns 502, 503, or 504.

## POST /api/books/lookup-reading-difficulty-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `ReadingDifficultyLookupResultDto` fields: `bookId`, `title`, `success`, `suggestedDifficulty`, `errorMessage`, `updatedBook`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/books/normalize-titles-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/books/**` is permitAll.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `NameNormalizationResultDto` fields: `bookId`, `title`, `before`, `after`, `changed`, `success`, `errorMessage`, `updatedBook`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Rewrites selected books' titles into Chicago title case. A title that already conforms is left unchanged. A title that would collide with another book is reported and skipped.
