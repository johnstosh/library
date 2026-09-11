# Book Price Endpoints

Librarian-only AbeBooks used-book price lookup and listing.

## GET /api/prices

Returns every saved price row, including book title and author.

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

Looks up the cheapest AbeBooks hardcover and softcover listings in good condition or better and upserts `book_price` rows for that book. Search uses title + author last name (then title-only without the condition filter for long titles), without a hardcover/softcover URL filter; binding is parsed from each result. Unknown-binding listings fill a cover that has no typed match. Ungraded `Used` listings are kept; Fair/Poor/As Described are not.

Pauses 500ms between AbeBooks HTTP calls (5s on every 10th call). On HTTP 403/429/503, a captcha/block page, or a no-listing response faster than 250ms, retries with backoff then returns `rateLimited: true` so the bulk carousel can cancel remaining books.

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
  "errorMessage": null
}
```

`success` is true when at least one cover found a listing. Temporary photo-intake titles are skipped with `Not Ready - Temporary title`.
