# Labels

Controller: `LabelsController`.

Base path: `/api/labels`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/labels/generate

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Query `bookIds` (`List<Long>`, required).

Response: `200 OK` body `byte[]`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Generate labels PDF for specified books (librarian only)
