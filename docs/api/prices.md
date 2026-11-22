# BookPrice

Controller: `BookPriceController`.

Base path: `/api/prices`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/prices

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/prices/**` requires LIBRARIAN.

Parameters:

None.

Response: `200 OK` body Array of `BookPriceDto` fields: `id`, `bookId`, `bookTitle`, `author`, `sellerId`, `sellerName`, `cover`, `priceDollars`, `shippingDollars`, `totalDollars`, `condition`, `lookedUpAt`, `detailsUrl`, `lookupError`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/prices/summaries

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/prices/**` requires LIBRARIAN.

Parameters:

None.

Response: `200 OK` body Array of `BookSummaryDto` fields: `id`, `lastModified`, `dateAddedToLibrary`. The query selects `BookPrice.id` and `BookPrice.lastModified` into a `BookSummaryDto`. `dateAddedToLibrary` is not a price column and is left unset.

Paging: None.

Ordering: None.

Notes:

Ids are price-row ids. Load full price rows with `POST /api/prices/by-ids`. There is no 100-id cap on the prices batch endpoints.

## POST /api/prices/by-ids

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/prices/**` requires LIBRARIAN.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `BookPriceDto` fields: `id`, `bookId`, `bookTitle`, `author`, `sellerId`, `sellerName`, `cover`, `priceDollars`, `shippingDollars`, `totalDollars`, `condition`, `lookedUpAt`, `detailsUrl`, `lookupError`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Body is a JSON array of price-row ids. Empty or null returns an empty list. No controller batch cap. Full `BookPriceDto` rows.

## POST /api/prices/by-book-ids

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/prices/**` requires LIBRARIAN.

Parameters:

- Body `List<Long>` (required).

Response: `200 OK` body Array of `BookPriceDto` fields: `id`, `bookId`, `bookTitle`, `author`, `sellerId`, `sellerName`, `cover`, `priceDollars`, `shippingDollars`, `totalDollars`, `condition`, `lookedUpAt`, `detailsUrl`, `lookupError`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Librarian-only endpoint to fetch BookPriceDtos scoped to specific book IDs. Used by PricesPage and BooksPage to avoid loading the full catalog when non-price filters (search, status, labels, etc.) have narrowed the set. Empty body or empty list returns empty list. Reuses the same DTOs and JOIN FETCH as /by-ids.

Body is a JSON array of book ids. Empty or null returns an empty list. No controller batch cap. Used when a filtered book id list should not load every price.

## POST /api/prices/lookup/{bookId}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `/api/prices/**` requires LIBRARIAN.

Parameters:

- Path `bookId` (`Long`).

Response: `200 OK` body `BookPriceLookupResultDto` fields: `bookId`, `bookTitle`, `success`, `rateLimited`, `cancelled`, `hardcover`, `softcover`, `libraryBinding`, `errorMessage`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Not `@Transactional`: AbeBooks HTTP must not hold a pool connection.

Not transactional. The AbeBooks call must not hold a database connection.
