# Book Price Endpoints

Librarian-only AbeBooks used-book price lookup and listing. Uses the same caching pattern as books: `GET /summaries` (id + lastModified) for cache validation + `POST /by-ids` for only changed rows. Details are persisted in IndexedDB.

## GET /api/prices/summaries

Returns lightweight summaries for frontend lastModified cache validation.

**Authentication:** Librarian (`hasAuthority('LIBRARIAN')`)

**Response:** array of `BookSummaryDto` (id + lastModified)

```json
[
  {
    "id": 1,
    "lastModified": "2026-09-10T12:00:00"
  }
]
```

## POST /api/prices/by-ids

Batch fetch full price rows for given price IDs (with book/author JOINs). Used by caching hook after summaries diff.

**Authentication:** Librarian (`hasAuthority('LIBRARIAN')`)

**Request body:** array of price IDs, e.g. `[1, 2]`

**Response:** array of `BookPriceDto`

## POST /api/prices/by-book-ids

Batch fetch full price rows for all prices belonging to the given *book* IDs (with book/author JOINs). Used by PricesPage and BooksPage after non-price filters narrow the candidate books. Empty list returns `[]`.

**Authentication:** Librarian (`hasAuthority('LIBRARIAN')`)

**Request body:** array of book IDs, e.g. `[42]`

**Response:** array of `BookPriceDto` (may contain 0-3+ rows per book)

## GET /api/prices

Returns every saved price row, including book title and author (legacy; caching path preferred).

**Authentication:** Librarian (`hasAuthority('LIBRARIAN')`)

**Response:** array of `BookPriceDto`

```json
[
  {
    "id": 1,
    "bookId": 42,
    "bookTitle": "Pride and Prejudice",
    "author": "Jane Austen",
    "cover": "HARDCOVER",
    "priceDollars": 4.86,
    "shippingDollars": 0,
    "totalDollars": 4.86,
    "condition": "Used - Good",
    "lookedUpAt": "2026-09-10T12:00:00",
    "detailsUrl": "https://www.abebooks.com/Pride-Prejudice/32512951271/bd",
    "lookupError": null,
    "lastModified": "2026-09-10T12:00:00"
  }
]
```

## POST /api/prices/lookup/{bookId}

Looks up the cheapest AbeBooks hardcover, softcover, and library-binding listings in good condition or better and upserts `book_price` rows for that book. Search uses title + author last name (then title-only without the condition filter for long titles), without a hardcover/softcover URL filter; binding is parsed from each result. Unknown-binding listings fill hardcover/softcover that have no typed match during search and are saved as `UNKNOWN`. Library-binding and other named bindings are saved when parsed from a listing. Ungraded `Used` listings are kept; Fair/Poor/As Described are not.

Pauses 8s between AbeBooks HTTP calls (30s on every 10th call). On HTTP 403/429/502/503/504, I/O timeout, or a captcha/block page, retries with capped exponential backoff then returns `rateLimited: true` so the bulk carousel can cancel remaining books. A real SearchResults page with zero listings is not treated as rate-limited. HTTP 500 and other non-rate-limit errors are stored as `lookupError` `AbeBooks HTTP {code}` with the last SearchResults URL in `detailsUrl`. When no listing is found, `lookupError` is `No matching listing` and `detailsUrl` is that search URL. AbeBooks HTTP runs outside a DB transaction so other librarian tabs stay usable. `book_price.book_id` is indexed.

**Authentication:** Librarian

**Response:** `BookPriceLookupResultDto`

```json
{
  "bookId": 42,
  "bookTitle": "Pride and Prejudice",
  "success": true,
  "rateLimited": false,
  "cancelled": false,
  "hardcover": { "cover": "HARDCOVER", "priceDollars": 4.86, "shippingDollars": 0, "totalDollars": 4.86 },
  "softcover": { "cover": "SOFTCOVER", "priceDollars": 3.00, "shippingDollars": 4.00, "totalDollars": 7.00 },
  "libraryBinding": { "cover": "LIBRARY_BINDING", "priceDollars": 12.00, "shippingDollars": 0, "totalDollars": 12.00 },
  "errorMessage": null
}
```

`success` is true when at least one cover found a listing. Temporary photo-intake titles are skipped with `Not Ready - Temporary title`.

See also `feature-design-prices.md` and the caching design in `design-caching.md`. The new endpoints keep the existing `listAll` behavior intact.
