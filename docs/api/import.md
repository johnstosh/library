# Import

Controller: `ImportController`.

Base path: `/api/import`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## POST /api/import/json

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/import/**` requires LIBRARIAN.

Consumes: `MediaType.APPLICATION_JSON_VALUE`.

Parameters:

- Raw request body. The controller reads the servlet input stream.

Response: `200 OK` body `ImportResponseDto` fields: `success`, `message`, `counts`, `errors`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Librarian only. The body is the export document (`libraries`, `authors`, `users`, `books`, `loans`, and the other sections the exporter writes). Import merges. It does not delete rows that are absent from the file. Books match on title only (`uk_book_title`). If more than one row shares a title, the lowest id is updated. Fields present in the book object overwrite the stored book. `readingDifficulty` and `desireToPurchase` are stored when the book object includes them. A missing value leaves the stored value unchanged. This version does not drop those fields. Authors and branches are resolved by name before the book is saved. A missing author or branch fails that book. Loans upsert on book, user, and loan date. Photos in this JSON import are not fully upserted. HTTP 200 returns `ImportResponseDto` even when individual rows recorded errors. A failure before that result is HTTP 400.

## GET /api/import/json

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/import/**` requires LIBRARIAN.

Parameters:

None.

Response: No `ResponseEntity`. The method writes the HTTP response itself.

Paging: None. The whole catalog is streamed as one JSON document.

Ordering: Not specified by this endpoint.

Notes:

Response body is `application/json` written to the servlet output stream, not a `ResponseEntity`. Photos are included as metadata in this export.

## GET /api/import/json/chunk-plan

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/import/**` requires LIBRARIAN.

Parameters:

None.

Response: `200 OK` body `ExportChunkPlanDto` fields: `targetChunks`, `pageSize`, `totalItems`, `sections`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Plan for chunked JSON export used by Data Management (~33 sequential GETs). Full streaming export remains at GET /api/import/json.

Tells the client how to walk `GET /api/import/json/chunk`. The full-document export remains `GET /api/import/json`.

## GET /api/import/json/chunk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/import/**` requires LIBRARIAN.

Parameters:

- Query `section` (`String`, required).
- Query `afterId` (`long`, optional, default `0`).
- Query `limit` (`int`, optional, default `50`).

Response: `200 OK` body `ExportChunkDto` fields: `section`, `afterId`, `limit`, `nextAfterId`, `total`, `count`, `hasMore`, `items`.

Paging: Keyset page. `afterId` is an exclusive id cursor (default 0). `limit` defaults to 50 and is capped at 500. A limit of 0 or less uses 50. Rows are `id > afterId` ordered by `id` ascending. Pass the returned `nextAfterId` as the next `afterId` until `hasMore` is false.

Ordering: `id` ascending within the section.

Notes:

One keyset page of a catalog section for chunked JSON export. Query params: section (libraries|authors|users|books|loans|photos|favorites|prices), afterId (exclusive, default 0), limit (page size).

`section` is required: `libraries` (alias `branches`), `authors`, `users`, `books`, `loans`, `photos`, `favorites`, `prices`, `sellers`, or `listings`. An unknown section is an error. Book pages load ids first, then the rows, so large text columns are not read for the whole table at once.

## GET /api/import/stats

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/import/**` requires LIBRARIAN.

Parameters:

None.

Response: `200 OK` body `DatabaseStatsDto` fields: `branchCount`, `bookCount`, `authorCount`, `userCount`, `loanCount`, `favoriteCount`, `priceCount`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Database statistics for the Data Management page. `favoriteCount` is the raw favorites row count. `priceCount` is unique books with a usable AbeBooks listing.

## GET /api/import/label-counts

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/import/**` requires LIBRARIAN.

Parameters:

None.

Response: `200 OK` body Array of `LabelCountDto` fields: `label`, `count`.

Paging: None.

Ordering: Count descending, then label name.

Notes:

Returns book counts per label, sorted by count descending then alphabetically.

## GET /api/import/availability-stats

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/import/**` requires LIBRARIAN.

Parameters:

None.

Response: `200 OK` body `BookAvailabilityStatsDto` fields: `electronicResource`, `hasCallNumber`, `hasFreeOnlineText`, `hasFreeOnlineAudio`, `withdrawn`, `requested`, `availableAtYdl`, `ydlPaper`, `ydlEbook`, `ydlAudio`, `availableAtEmu`, `emuPaper`, `emuEbook`, `emuAudio`, `availableAtAcla`, `aclaPaper`, `aclaEbook`, `aclaAudio`, `availableAtWcc`, `wccPaper`, `wccEbook`, `wccAudio`, `availableAtMsu`, `msuPaper`, `msuEbook`, `msuAudio`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Returns named book-count statistics for electronic, call number, withdrawn, YDL, EMU, ACLA, WCC, and MSU. hasCallNumber excludes WITHDRAWN and REQUESTED even when a call number is present.

## GET /api/import/favorite-stats

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/import/**` requires LIBRARIAN.

Parameters:

None.

Response: `200 OK` body Array of `FavoriteListCountDto` fields: `listName`, `bookCount`, `authorCount`.

Paging: None.

Ordering: Total count descending, then list name.

Notes:

Returns favorite-list membership counts across all users, split by books and authors. Sorted by total count descending, then list name.
