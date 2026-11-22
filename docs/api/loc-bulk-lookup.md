# LocBulkLookup

Controller: `LocBulkLookupController`.

Base path: `/api/loc-bulk-lookup`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/loc-bulk-lookup/books

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body Array of `BookLocStatusDto` fields: `id`, `title`, `authorName`, `currentLocNumber`, `hasLocNumber`, `publicationYear`, `firstPhotoId`, `firstPhotoChecksum`, `dateAdded`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Get all books with their LOC status

## POST /api/loc-bulk-lookup/lookup/{bookId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `bookId` (`Long`).

Response: `200 OK` body `LocLookupResultDto` fields: `bookId`, `success`, `locNumber`, `errorMessage`, `matchCount`, `aiSuggested`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Lookup LOC number for a single book

## POST /api/loc-bulk-lookup/lookup-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `LocLookupResultDto` fields: `bookId`, `success`, `locNumber`, `errorMessage`, `matchCount`, `aiSuggested`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Lookup LOC numbers for multiple books

## POST /api/loc-bulk-lookup/lookup-all-missing

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body Array of `LocLookupResultDto` fields: `bookId`, `success`, `locNumber`, `errorMessage`, `matchCount`, `aiSuggested`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Lookup LOC numbers for all books missing LOC numbers
