# Applied

Controller: `AppliedController`.

Base path: `/api`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## POST /api/application/public/register

Auth: Public. no `@PreAuthorize`; HTTP `/api/application/public/**` is permitAll.

Parameters:

- Body `RegistrationRequest` (required).

Response: Empty body (`204 No Content` when the method calls `noContent()`, otherwise `200` with `ok().build()`).

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/applied

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body Array of `AppliedDto` fields: `id`, `name`, `email`, `phone`, `status`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/applied

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `Applied` (required).

Response: `201 Created` body `AppliedDto` fields: `id`, `name`, `email`, `phone`, `status`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/applied/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).
- Body `Applied` (required).

Response: `200 OK` body `AppliedDto` fields: `id`, `name`, `email`, `phone`, `status`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/applied/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: Empty body (`204 No Content` when the method calls `noContent()`, otherwise `200` with `ok().build()`).

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/applied/{id}/approve

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `?`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
