# FreeTextLookup

Controller: `FreeTextLookupController`.

Base path: `/api/free-text`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## POST /api/free-text/lookup/{bookId}

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `bookId` (`Long`).

Response: `200 OK` body `FreeTextBulkLookupResultDto` fields: `bookId`, `bookTitle`, `authorName`, `success`, `freeTextUrl`, `providerName`, `errorMessage`, `providersSearched`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Look up free online text for a single book.

@param bookId the book ID to look up @return lookup result with URL if found

## POST /api/free-text/lookup-bulk

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `FreeTextBulkLookupResultDto` fields: `bookId`, `bookTitle`, `authorName`, `success`, `freeTextUrl`, `providerName`, `errorMessage`, `providersSearched`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Look up free online text for multiple books (bulk operation).

@param bookIds list of book IDs to look up @return list of lookup results

## GET /api/free-text/providers

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body Array of `String`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Get the list of available free text providers.

@return list of provider names in priority order
