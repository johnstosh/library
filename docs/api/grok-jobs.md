# GrokJob

Controller: `GrokJobController`.

Base path: `/api/grok-jobs`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/grok-jobs/{jobId}

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `jobId` (`String`).

Response: `200 OK` body `GrokJobDto` fields: `jobId`, `kind`, `status`, `result`, `error`, `httpStatus`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Poll a background Grok job started by a `.../start` endpoint. A known job returns `GrokJobDto`. An expired or unknown id is HTTP 404 with `{ "error", "message" }` both set to the not-found text. `202` responses from the start endpoints send `Location: /api/grok-jobs/{jobId}`.
