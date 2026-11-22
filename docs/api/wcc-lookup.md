# WccLookup

Controller: `WccLookupController`.

Base path: `/api/wcc-lookup`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## POST /api/wcc-lookup/lookup/{bookId}

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/wcc-lookup/**` is permitAll.

Parameters:

- Path `bookId` (`Long`).

Response: `200 OK` body `WccLookupResultDto` fields: `bookId`, `success`, `audioAvailable`, `paperAvailable`, `ebookAvailable`, `matchedTitle`, `matches`, `errorMessage`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Lookup Washtenaw Community College availability for a single book. Public so patrons can refresh holdings from the book page.
