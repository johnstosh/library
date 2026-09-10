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

Looks up the cheapest AbeBooks hardcover and softcover listings in good condition or better and upserts `book_price` rows for that book.

**Authentication:** Librarian

**Response:** `BookPriceLookupResultDto`

```json
{
  "bookId": 42,
  "bookTitle": "Pride and Prejudice",
  "success": true,
  "hardcover": { "cover": "HARDCOVER", "priceDollars": 4.86, "shippingDollars": 0, "totalDollars": 4.86 },
  "softcover": { "cover": "SOFTCOVER", "priceDollars": 3.00, "shippingDollars": 4.00, "totalDollars": 7.00 },
  "errorMessage": null
}
```

`success` is true when at least one cover found a listing. Temporary photo-intake titles are skipped with `Not Ready - Temporary title`.
