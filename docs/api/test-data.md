# TestData

Controller: `TestDataController`.

Base path: `/api/test-data`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## POST /api/test-data/generate

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/test-data/**` is permitAll.

Parameters:

- Body `Map<String, Integer>` (required).

Response: `200 OK` body `TestDataResponseDto` fields: `success`, `message`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/test-data/generate-loans

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/test-data/**` is permitAll.

Parameters:

- Body `Map<String, Integer>` (required).

Response: `200 OK` body `TestDataResponseDto` fields: `success`, `message`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/test-data/generate-users

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/test-data/**` is permitAll.

Parameters:

- Body `Map<String, Integer>` (required).

Response: `200 OK` body `TestDataResponseDto` fields: `success`, `message`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/test-data/delete-all

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/test-data/**` is permitAll.

Parameters:

None.

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/test-data/total-purge

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/test-data/**` is permitAll.

Parameters:

None.

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/test-data/stats

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/test-data/**` is permitAll.

Parameters:

None.

Response: `200 OK` body `TestDataStatsDto` fields: `books`, `authors`, `loans`, `users`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
