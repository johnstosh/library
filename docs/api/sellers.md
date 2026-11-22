# AbeSeller

Controller: `AbeSellerController`.

Base path: `/api`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/sellers

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/sellers/**` requires LIBRARIAN.

Parameters:

None.

Response: `200 OK` body Array of `AbeSellerDto`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/sellers/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/sellers/**` requires LIBRARIAN.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `AbeSellerDto`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/sellers/reparse

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/sellers/**` requires LIBRARIAN.

Parameters:

None.

Response: `200 OK` body `SellerReparseResultDto` fields: `examined`, `updated`, `unchanged`, `keptLibrarian`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/sellers/{id}/additional-shipping

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/sellers/**` requires LIBRARIAN.

Parameters:

- Path `id` (`Long`).
- Body `AdditionalShippingUpdateDto` (required).

Response: `200 OK` body `AbeSellerDto`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/listings/by-book-ids

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/listings/**` requires LIBRARIAN.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `BookListingDto`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
