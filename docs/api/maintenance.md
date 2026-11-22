# Maintenance

Controller: `MaintenanceController`.

Base path: `/api/maintenance`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/maintenance/illegal-genres/count

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `IllegalGenresMaintenanceDto` fields: `booksAffected`, `booksScanned`, `booksUpdated`, `pluralCorrections`, `illegalRemoved`, `message`, `error`, `actionId`, `changes`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Recalculates count of books affected by illegal or mismatched genre tags. Used to populate the Count column (shows "-" until clicked).

## POST /api/maintenance/illegal-genres/cleanup

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `IllegalGenresMaintenanceDto` fields: `booksAffected`, `booksScanned`, `booksUpdated`, `pluralCorrections`, `illegalRemoved`, `message`, `error`, `actionId`, `changes`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Performs cleanup of illegal genres: normalizes plurals/variants and removes any remaining non-canonical tags. Updates books in place. Returns detailed summary for the Results column.

## POST /api/maintenance/illegal-genres/undo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolUndoRequestDto` (required).

Response: `200 OK` body `IllegalGenresMaintenanceDto` fields: `booksAffected`, `booksScanned`, `booksUpdated`, `pluralCorrections`, `illegalRemoved`, `message`, `error`, `actionId`, `changes`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Restores the genre tags from a Clean Genres action (Issue #385).

## GET /api/maintenance/illegal-genres/undoable

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `IllegalGenresMaintenanceDto` fields: `booksAffected`, `booksScanned`, `booksUpdated`, `pluralCorrections`, `illegalRemoved`, `message`, `error`, `actionId`, `changes`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Latest Clean Genres that has not been undone.

## POST /api/maintenance/duplicate-titles/find

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `DuplicateTitlesResultDto` fields: `pairs`, `booksScanned`, `representativesCompared`, `message`, `error`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Find near-duplicate catalog titles (read-only). Uses titles + alternate titles, keyword indexing, and Jaro–Winkler; returns top 100 pairs. WITHDRAWN books are excluded from the scan.

## POST /api/maintenance/duplicate-titles/dismissals

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `DuplicateTitleDismissalRequestDto` (required).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Remember that the two works on a result row are not duplicates. Stored by folded title and author, so every extra copy stays hidden. The current result table is left unchanged until the next find.

## POST /api/maintenance/duplicate-titles/dismissals/undo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `DuplicateTitleDismissalRequestDto` (required).

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Remove a not-duplicate mark. Used by Undo while the row is still on screen.

## POST /api/maintenance/duplicate-titles/author-merges

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `DuplicateTitlePairActionRequestDto` (required).

Response: `200 OK` body `DuplicateTitleActionResultDto` fields: `actionId`, `actionKind`, `summary`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Merge the two authors on one Duplicate Titles row. Not a general author merge. The change is recorded in duplicate_title_action.

## POST /api/maintenance/duplicate-titles/author-merges/undo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `DuplicateTitleUndoRequestDto` (required).

Response: `200 OK` body `DuplicateTitleActionResultDto` fields: `actionId`, `actionKind`, `summary`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Reverse a Duplicate Titles author merge. Inserts an undo row and leaves the original row in place.

## POST /api/maintenance/duplicate-titles/title-merges

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `DuplicateTitlePairActionRequestDto` (required).

Response: `200 OK` body `DuplicateTitleActionResultDto` fields: `actionId`, `actionKind`, `summary`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Merge the two books on one Duplicate Titles row by combining free-text URLs. Refuses when both books have call numbers. Not a general book merge.

## POST /api/maintenance/duplicate-titles/title-merges/undo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `DuplicateTitleUndoRequestDto` (required).

Response: `200 OK` body `DuplicateTitleActionResultDto` fields: `actionId`, `actionKind`, `summary`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Reverse a Duplicate Titles title merge. Inserts an undo row and leaves the original row in place.

## POST /api/maintenance/duplicate-titles/title-fixes

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `DuplicateTitlePairActionRequestDto` (required).

Response: `200 OK` body `DuplicateTitleActionResultDto` fields: `actionId`, `actionKind`, `summary`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Add the next ", c. N" copy suffix on a Duplicate Titles row whose books both have call numbers. Uses the save-title copy-number rule, so existing ", c. 2", ", c. 3", and ", c. 4" copies make the next title ", c. 5". Does not delete either book.

## POST /api/maintenance/duplicate-titles/title-fixes/undo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `DuplicateTitleUndoRequestDto` (required).

Response: `200 OK` body `DuplicateTitleActionResultDto` fields: `actionId`, `actionKind`, `summary`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Reverse a Duplicate Titles fix. Inserts an undo row and leaves the original row in place.
