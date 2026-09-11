# Book Prices (AbeBooks)

## Overview
Librarians can look up used-book prices on AbeBooks for hardcover and softcover copies in **good condition or better**, store the cheapest listing per cover, browse them on a Prices page, and round-trip them through JSON import/export.

## Lookup
- Trigger: **Lookup Prices** on the Books page bulk-action carousel (`data-test="bulk-lookup-prices"`).
- For each selected book the backend searches AbeBooks **once per query** (not once per cover). Binding is read from each listing instead of the `bi` filter, so unknown-binding copies are not dropped.
- Search URL: `https://www.abebooks.com/servlet/SearchResults`
  - `tn` = title (copy-number and format suffixes stripped; slashes kept)
  - `an` = first author's last name (semicolons, `et al.`, honorifics, generational and religious-order suffixes stripped)
  - no `bi` parameter
  - `sortby=17` = lowest total price (item + shipping)
  - `cond=new an fine nf vg good` = good or better on the first search (excludes Fair, Poor, As Described). Title-only fallback omits `cond` so ungraded `Used` listings still appear.
  - `ds=30`; further pages use `p` / `spo` when the first page does not yet have both covers
- Query strategy:
  1. Title + author last name, paging until a typed hardcover **and** typed softcover are found, or the pager has no next page (cap 5 pages).
  2. If both covers still cannot be filled and the cleaned title has **more than 7 letter-bearing words**, retry title-only (`tn`, no `an`, no `cond`) with the same paging.
- Each listing's Attributes row (`aria-label="Hardcover"` / `"Softcover"`) sets the cover. Listings with no binding attribute are **unknown** and fill any cover that still lacks a typed listing.
- The cheapest remaining listing per cover is saved. The HTML parser keeps Good/Very Good/New **and** ungraded `Used`, and still rejects Fair, Poor, Acceptable, and As Described.

## Saved fields (`book_price`)
One row per book per cover (`uk_book_price_book_cover`). Latest lookup overwrites.

| Field | Meaning |
| --- | --- |
| book | Catalog book |
| cover | `HARDCOVER` or `SOFTCOVER` |
| priceDollars | Item price |
| shippingDollars | Shipping; `0` for free shipping |
| condition | AbeBooks condition text (e.g. `Used - Good`) |
| lookedUpAt | When the lookup ran |
| detailsUrl | Listing URL |
| lookupError | Set when no listing was found or the request failed |

Deleting a book cascades to its prices.

## Prices page
- Route: `/prices` (librarian only)
- Nav: **Prices** (`data-test="nav-prices"`)
- Filters:
  - **Total less than $X** (`data-test="prices-max-total"`) — keeps rows whose `price + shipping` is strictly less than X
  - Hardcover / Softcover
  - Has listing / Lookup failed
  - Looked up recently (last 30 days)
  - The same book chips, labels, reading difficulty, favorite lists, and title/author query as Books
- Open in Prices (`data-test="open-in-prices"`) on Books copies the current Books filters onto `/prices?...` (one-way handoff, not live sync). Visiting `/prices` from the nav with no query shows every saved price.

## API
See `endpoints/endpoints-prices.md`.

- `GET /api/prices` — all saved listings
- `POST /api/prices/lookup/{bookId}` — lookup both covers for one book

Librarian only.

## Import/export
JSON export includes a `prices` array keyed by `bookTitle` + `bookAuthorName` + `cover`. Import merges on that key. Database stats include `priceCount`.
