# PhotoExport

Controller: `PhotoExportController`.

Base path: `/api/photo-export`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/photo-export/zip-parts

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body Array of `PhotoZipPartDto` fields: `partNumber`, `totalParts`, `rangeLabel`, `photoCount`, `estimatedMb`, `startKey`, `endKey`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Returns how many ZIP parts the full photo collection is split into, with the letter range and estimated size for each part. The frontend uses this to render one download button per part.

## GET /api/photo-export/zip/{partNumber}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

- Path `partNumber` (`int`).

Response: No `ResponseEntity`. The method writes the HTTP response itself.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Downloads a single alphabetically-bounded ZIP part (1-based). Call GET /zip-parts first to discover how many parts exist.

## GET /api/photo-export/stats

Auth: Authenticated. no `@PreAuthorize`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `PhotoExportStatsDto` fields: `total`, `exported`, `imported`, `pendingExport`, `pendingImport`, `failed`, `inProgress`, `completed`, `pending`, `albumName`, `albumId`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Get export statistics

No method `@PreAuthorize`. Any logged-in user can call it.

## GET /api/photo-export

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

None.

Response: No `ResponseEntity`. The method writes the HTTP response itself.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Download all photos as a ZIP file using true streaming. Memory-efficient - photos are streamed directly to the response, never buffered entirely in memory. The ZIP uses the same filename format as the import feature: - book-{sanitized-title}[-{n}].{ext} - author-{sanitized-name}[-{n}].{ext} - loan-{sanitized-title}-{sanitized-username}[-{n}].{ext}

Librarian. Streams a ZIP of photo files. No `Content-Length`. An empty catalog is HTTP 400 JSON `{ "error": "Export Failed", "message": "No photos available for export" }`.

## GET /api/photo-export/photos

Auth: Authenticated. no `@PreAuthorize`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body Array of `PhotoExportInfoDto` fields: `id`, `lastModified`, `caption`, `exportStatus`, `exportedAt`, `permanentId`, `exportErrorMessage`, `contentType`, `hasImage`, `checksum`, `bookTitle`, `bookId`, `bookLocNumber`, `bookDateAdded`, `bookAuthorId`, `bookAuthorName`, `authorName`, `authorId`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Get all photos with export status

No method `@PreAuthorize`. Any logged-in user can call it. Metadata for every photo. Does not return image bytes.

## GET /api/photo-export/summaries

Auth: Authenticated. no `@PreAuthorize`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body Array of `PhotoSummaryDto` fields: `id`, `lastModified`. `PhotoSummaryDto` is `id` and `lastModified`. Image bytes are not included.

Paging: None.

Ordering: None.

Notes:

Get photo summaries (id + lastModified) for cache validation. Same auth as GET /photos. Never returns image bytes.

No method `@PreAuthorize`. Any logged-in user can call it. The ZIP download at `GET /api/photo-export` is librarian-only. Image bytes are not in this response.

## POST /api/photo-export/by-ids

Auth: Authenticated. no `@PreAuthorize`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `PhotoExportInfoDto` fields: `id`, `lastModified`, `caption`, `exportStatus`, `exportedAt`, `permanentId`, `exportErrorMessage`, `contentType`, `hasImage`, `checksum`, `bookTitle`, `bookId`, `bookLocNumber`, `bookDateAdded`, `bookAuthorId`, `bookAuthorName`, `authorName`, `authorId`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Batch-fetch photo export info by IDs. Same mapping as GET /photos. Same auth as GET /photos. Never returns image bytes.

No method `@PreAuthorize`. Any logged-in user can call it. Body is a JSON array of photo ids. No controller batch cap. Does not return image bytes.

## GET /api/photo-export/photos/{photoId}

Auth: Authenticated. no `@PreAuthorize`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

- Path `photoId` (`Long`).

Response: `200 OK` body `PhotoExportInfoDto` fields: `id`, `lastModified`, `caption`, `exportStatus`, `exportedAt`, `permanentId`, `exportErrorMessage`, `contentType`, `hasImage`, `checksum`, `bookTitle`, `bookId`, `bookLocNumber`, `bookDateAdded`, `bookAuthorId`, `bookAuthorName`, `authorName`, `authorId`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Get export info for a single photo by ID. Used by the frontend to refresh only the changed row after a single-photo export/import.

## POST /api/photo-export/export-all

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `PhotoExportResponseDto` fields: `message`, `photoId`, `stats`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Manually trigger export for all pending photos

## POST /api/photo-export/export/{photoId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

- Path `photoId` (`Long`).

Response: `200 OK` body `PhotoExportInfoDto` fields: `id`, `lastModified`, `caption`, `exportStatus`, `exportedAt`, `permanentId`, `exportErrorMessage`, `contentType`, `hasImage`, `checksum`, `bookTitle`, `bookId`, `bookLocNumber`, `bookDateAdded`, `bookAuthorId`, `bookAuthorName`, `authorName`, `authorId`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Manually trigger export for a specific photo. Returns the updated PhotoExportInfoDto so the frontend can patch just that row in its cache.

## POST /api/photo-export/import/{photoId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

- Path `photoId` (`Long`).

Response: `200 OK` body `PhotoExportInfoDto` fields: `id`, `lastModified`, `caption`, `exportStatus`, `exportedAt`, `permanentId`, `exportErrorMessage`, `contentType`, `hasImage`, `checksum`, `bookTitle`, `bookId`, `bookLocNumber`, `bookDateAdded`, `bookAuthorId`, `bookAuthorName`, `authorName`, `authorId`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Import a photo from Google Photos using its permanent ID. Returns the updated PhotoExportInfoDto so the frontend can patch just that row in its cache.

## POST /api/photo-export/import-all

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `PhotoImportResultDto` fields: `message`, `imported`, `failed`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Import all photos that have permanent IDs but no image data

## POST /api/photo-export/verify/{photoId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

- Path `photoId` (`Long`).

Response: `200 OK` body `PhotoVerifyResultDto` fields: `photoId`, `permanentId`, `valid`, `message`, `filename`, `mimeType`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Verify that a photo's permanent ID still works in Google Photos

## POST /api/photo-export/unlink/{photoId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photo-export/**` requires a logged-in user.

Parameters:

- Path `photoId` (`Long`).

Response: `200 OK` body `PhotoExportInfoDto` fields: `id`, `lastModified`, `caption`, `exportStatus`, `exportedAt`, `permanentId`, `exportErrorMessage`, `contentType`, `hasImage`, `checksum`, `bookTitle`, `bookId`, `bookLocNumber`, `bookDateAdded`, `bookAuthorId`, `bookAuthorName`, `authorName`, `authorId`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Unlink a photo by removing its permanent ID. Returns the updated PhotoExportInfoDto so the frontend can patch just that row in its cache.
