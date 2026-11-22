# CatalogToolMaintenance

Controller: `CatalogToolMaintenanceController`.

Base path: `/api/maintenance`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## POST /api/maintenance/chicago-titles/preview

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `CatalogToolPreviewDto` fields: `booksScanned`, `changeCount`, `message`, `error`, `changes`, `skipped`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/maintenance/chicago-titles/apply

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolApplyRequestDto` (optional).

Response: `200 OK` body `CatalogToolApplyResultDto` fields: `actionId`, `actionKind`, `applied`, `skipped`, `message`, `error`, `changes`, `skippedChanges`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/maintenance/chicago-titles/undo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolUndoRequestDto` (required).

Response: `200 OK` body `CatalogToolApplyResultDto` fields: `actionId`, `actionKind`, `applied`, `skipped`, `message`, `error`, `changes`, `skippedChanges`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/maintenance/chicago-titles/undoable

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `CatalogToolUndoableDto` fields: `actionId`, `changeCount`, `summary`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/maintenance/grokipedia/candidates

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `CatalogToolCandidatesDto` fields: `bookIds`, `authorIds`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/maintenance/grokipedia/preview

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolCandidatesDto` (optional).

Response: `200 OK` body `CatalogToolPreviewDto` fields: `booksScanned`, `changeCount`, `message`, `error`, `changes`, `skipped`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/maintenance/grokipedia/apply

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolApplyRequestDto` (optional).

Response: `200 OK` body `CatalogToolApplyResultDto` fields: `actionId`, `actionKind`, `applied`, `skipped`, `message`, `error`, `changes`, `skippedChanges`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/maintenance/grokipedia/undo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolUndoRequestDto` (required).

Response: `200 OK` body `CatalogToolApplyResultDto` fields: `actionId`, `actionKind`, `applied`, `skipped`, `message`, `error`, `changes`, `skippedChanges`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/maintenance/grokipedia/undoable

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `CatalogToolUndoableDto` fields: `actionId`, `changeCount`, `summary`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/maintenance/grokipedia-authors/candidates

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `CatalogToolCandidatesDto` fields: `bookIds`, `authorIds`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/maintenance/grokipedia-authors/preview

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolCandidatesDto` (optional).

Response: `200 OK` body `CatalogToolPreviewDto` fields: `booksScanned`, `changeCount`, `message`, `error`, `changes`, `skipped`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/maintenance/grokipedia-authors/apply

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolApplyRequestDto` (optional).

Response: `200 OK` body `CatalogToolApplyResultDto` fields: `actionId`, `actionKind`, `applied`, `skipped`, `message`, `error`, `changes`, `skippedChanges`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/maintenance/grokipedia-authors/undo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolUndoRequestDto` (required).

Response: `200 OK` body `CatalogToolApplyResultDto` fields: `actionId`, `actionKind`, `applied`, `skipped`, `message`, `error`, `changes`, `skippedChanges`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/maintenance/grokipedia-authors/undoable

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `CatalogToolUndoableDto` fields: `actionId`, `changeCount`, `summary`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/maintenance/free-text/candidates

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Query `limit` (`int`, required).

Response: `200 OK` body `CatalogToolCandidatesDto` fields: `bookIds`, `authorIds`.

Paging: Keyset page using `afterId` and `limit`.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/maintenance/free-text/search

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CatalogToolCandidatesDto` (optional).

Response: `200 OK` body `CatalogToolPreviewDto` fields: `booksScanned`, `changeCount`, `message`, `error`, `changes`, `skipped`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
