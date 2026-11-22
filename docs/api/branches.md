# Branch

Controller: `BranchController`.

Base path: `/api/branches`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/branches

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/branches/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `BranchDto` fields: `id`, `branchName`, `librarySystemName`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/branches/{id}

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/branches/**` is permitAll.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `BranchDto` fields: `id`, `branchName`, `librarySystemName`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/branches

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `BranchDto` (required).

Response: `201 Created` body `BranchDto` fields: `id`, `branchName`, `librarySystemName`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/branches/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).
- Body `BranchDto` (required).

Response: `200 OK` body `BranchDto` fields: `id`, `branchName`, `librarySystemName`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/branches/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: Empty body (`204 No Content` when the method calls `noContent()`, otherwise `200` with `ok().build()`).

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/branches/statistics

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/branches/**` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `BranchStatisticsDto` fields: `branchId`, `branchName`, `bookCount`, `activeLoansCount`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
