# LibraryCard

Controller: `LibraryCardController`.

Base path: `/api/library-card`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/library-card/designs

Auth: Public. no `@PreAuthorize`; HTTP `/api/library-card/designs` is permitAll.

Parameters:

None.

Response: `200 OK` body Array of `LibraryCardDesignDto` fields: `name`, `displayName`, `description`, `imageUrl`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/library-card/print-all

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `byte[]`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Generate and download a multi-page PDF containing all library card designs.

@return PDF file as byte array with appropriate headers

## GET /api/library-card/print

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `byte[]`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Generate and download a wallet-sized library card PDF for the current user.

@return PDF file as byte array with appropriate headers
