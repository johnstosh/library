# Photo

Controller: `PhotoController`.

Base path: `/api/photos`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/photos/{id}/image

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/photos/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: Raw bytes, not JSON. Image responses use the photo media type. A missing photo is HTTP 404.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Public image bytes for one photo.

## GET /api/photos/{id}/thumbnail

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/photos/**` is permitAll.

Parameters:

- Path `id` (`Long`).
- Query `width` (`Integer`, required).

Response: Raw bytes, not JSON. Image responses use the photo media type. A missing photo is HTTP 404.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Public. `width` is a required integer. The response is thumbnail bytes, not JSON.

## DELETE /api/photos/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `?`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/photos/{id}/restore

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `?`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/photos/{id}/crop

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).
- Part `file` (`MultipartFile`, required).

Response: `200 OK` body `?`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/photos/import-zip-chunk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Consumes: `application/octet-stream`.

Parameters:

- Raw request body. The controller reads the servlet input stream.
- Header `X-Upload-Id` (`String`, required).
- Header `X-Chunk-Index` (`int`, required).
- Header `X-Is-Last-Chunk` (`boolean`, required).
- Header `X-Resume-From-Processed` (`int`, optional, default `-1`).
- Header `X-Bytes-To-Skip` (`long`, optional, default `0`).

Response: `200 OK` body `ChunkUploadResultDto` fields: `uploadId`, `chunkIndex`, `processedPhotos`, `totalProcessedSoFar`, `totalSuccessSoFar`, `totalFailureSoFar`, `totalSkippedSoFar`, `complete`, `finalResult`, `errorMessage`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Import photos from a ZIP file using chunked upload. Each chunk is a raw byte slice of the ZIP file, sent sequentially. Headers provide chunk metadata; body is raw octet-stream bytes.

## GET /api/photos/import-zip-chunk-resume/{uploadId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/photos/**` is permitAll.

Parameters:

- Path `uploadId` (`String`).

Response: `200 OK` body `ResumeInfoDto` fields: `uploadId`, `resumeFromChunkIndex`, `bytesToSkipInChunk`, `totalProcessed`, `successCount`, `failureCount`, `skippedCount`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Get resume info for a chunked upload that was interrupted (e.g., by a Cloud Run reboot). Returns the chunk index and byte offset to resume from.

## DELETE /api/photos/import-zip-chunk/{uploadId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `uploadId` (`String`).

Response: Empty body (`204 No Content` when the method calls `noContent()`, otherwise `200` with `ok().build()`).

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Remove a completed or failed chunked upload session. Called by the frontend after it has read the final stats.
