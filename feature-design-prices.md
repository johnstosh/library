# Book Prices (AbeBooks)

## Overview
Librarians can look up used-book prices on AbeBooks for hardcover, softcover, and library-binding copies in **good condition or better**, store the cheapest listing per cover, browse them on a Prices page, and round-trip them through JSON import/export. Catalog books also store a binding (`HARDCOVER`, `SOFTCOVER`, `LIBRARY_BINDING`, `OTHER`, `UNKNOWN`).

## Lookup
- Trigger: **Lookup Prices** on the Books page bulk-action carousel (`data-test="bulk-lookup-prices"`).
- For each selected book the backend searches AbeBooks **once per query** (not once per cover). Binding is read from each listing instead of the `bi` filter, so unknown-binding copies are not dropped. Library-binding and other named bindings (leather, spiral, board book) are kept when they appear on the same pages.
- Search URL: `https://www.abebooks.com/servlet/SearchResults`
  - `tn` = title (copy-number and format suffixes stripped; slashes kept)
  - `an` = first author's last name (semicolons, `et al.`, honorifics, generational and religious-order suffixes stripped)
  - no `bi` parameter
  - `sortby=17` = lowest total price (item + shipping)
  - `cond=new an fine nf vg good` = good or better on the first search (excludes Fair, Poor, As Described). Title-only fallback omits `cond` so ungraded `Used` listings still appear.
  - `ds=30`; further pages use `p` / `spo` when the first page does not yet have both covers
- Query strategy:
  1. Title + author last name, paging until a typed hardcover **and** typed softcover are found, or the pager has no next page (cap 5 pages). Library binding is saved when seen on those pages; paging does not wait for it.
  2. If both covers still cannot be filled and the cleaned title has **more than 7 letter-bearing words**, retry title-only (`tn`, no `an`, no `cond`) with the same paging.
- Each listing's Attributes row sets the cover (`Hardcover`, `Softcover`, `Library Binding`, leather/spiral/board book → `OTHER`). Listings with no binding attribute are **unknown**: they fill hardcover/softcover that still lack a typed listing during search, and are saved as `UNKNOWN`.
- The cheapest remaining listing per cover is saved. The HTML parser keeps Good/Very Good/New **and** ungraded `Used`, and still rejects Fair, Poor, Acceptable, and As Described.
- HTTP 500 (and other non-rate-limit errors) are stored as `lookupError` `AbeBooks HTTP {code}` — not swallowed, and not shown as a listing.
- Politeness / rate limits:
  - 8s pause before each AbeBooks HTTP call (`abebooks.request-delay-ms`); every 10th call waits 30s (`abebooks.tenth-request-delay-ms`)
  - 1s pause between books in the bulk carousel; every 10th book waits 10s
  - HTTP 403/429/502/503/504, I/O timeout, or a captcha/block page is treated as rate-limited. A real SearchResults page with zero listings is not, even when it arrives in under 250ms.
  - The current book is retried with exponential backoff capped at 5 minutes (`abebooks.rate-limit-retries`, `abebooks.rate-limit-backoff-ms`)
  - If still rate-limited, remaining selected books are **cancelled** (not recorded as "no listing") so the batch stops hammering AbeBooks
  - AbeBooks HTTP and throttling sleeps run **outside** a database transaction so the Hikari pool (size 3) stays available for other tabs (Data Management) during a long bulk lookup

## Saved fields (`book_price`)
One row per book per cover (`uk_book_price_book_cover`). Latest lookup overwrites.

| Field | Meaning |
| --- | --- |
| book | Catalog book |
| cover | `HARDCOVER`, `SOFTCOVER`, `LIBRARY_BINDING`, `OTHER`, or `UNKNOWN` |
| priceDollars | Item price |
| shippingDollars | Shipping; `0` for free shipping |
| condition | AbeBooks condition text (e.g. `Used - Good`) |
| lookedUpAt | When the lookup ran |
| detailsUrl | Listing URL when a price was found; otherwise the last AbeBooks SearchResults URL used |
| lookupError | Set when no listing was found (`No matching listing`) or the request failed (`AbeBooks rate limited`, `AbeBooks HTTP 500`, …) |

Deleting a book cascades to its prices.

`book_price.book_id` is indexed (`idx_book_price_book` and unique `uk_book_price_book_cover`). Lookup slowness is the AbeBooks politeness delay (8s / 30s), not missing Book ID indexes.

## Prices page
- Route: `/prices` (librarian only)
- Nav: **Prices** (`data-test="nav-prices"`)
- Table:
  - **Book** column is 20% of table width
  - **Status** (`data-test="price-status-{id}"`) shows `lookupError` (`No matching listing`, `AbeBooks rate limited`, `AbeBooks HTTP 500`, …); blank (`—`) when the row is a real listing
  - **Listing** shows an AbeBooks URL: the listing when a price was found (`AbeBooks`), or the last search used when no price was found (`Search`)
- Filters:
  - The same book chips, labels, reading difficulty, favorite lists, and title/author query as Books
  - **Desire to Purchase** (`data-test="desire-to-purchase-filters"`) — OR chips for 0–10 plus Unset, same pattern as Reading Difficulty (`desireToPurchase=0,10,unset`)
  - A bottom **Pricing** section (`data-test="book-price-filters"`) with:
    - **Looked up recently** chip (last **N hours**, default 24; `data-test="filter-price-recent"`) + hours input (FilterChip pattern)
    - **Books with Pricing** / **Books without Pricing** / **Lookup Errors** / **Price older than N days** (default 90) / **Total less than $X** (`data-test="prices-max-total"`) from BookPriceFilters
    - Binding filters (Hardcover/Softcover/Library Binding/Other/Unknown) are in the separate BindingFilters section (not in Pricing)
- Title/author filter submit is **Search** (`data-test="prices-search-button"`), matching Books and Search. Other list pages (Authors, Loans, Users, Applications) filter as you type and have no submit button. **Apply** is reserved for the library-card application form.
- Counts above the table (`data-test="prices-stats"`) match Books: unique books in the current rows (`table-count`), total books in the database (`database-count`), plus price rows in the table (`price-row-count`).
- Footer (`data-test="price-statistics"`) on Prices reports:
  - **Total cost** — sum of the cheaper hardcover/softcover/library-binding total (item + shipping) per book in the current book filters. Other/Unknown listings are used only when none of those typed covers has a usable price.
  - **Over $20 / Over $40 / Over $80** — counts of books whose cheapest total is strictly greater than that amount (cumulative).
  - **Total books** — books in the current book filters.
  - **Without prices** — books with no usable listing (missing rows, `No matching listing`, rate-limited, or other lookup errors).
- Open in Prices (`data-test="open-in-prices"`) on Books copies the current Books filters onto `/prices?...` (one-way handoff, not live sync). Open in Books (`data-test="open-in-books"`) on Prices copies the current Prices inventory filters onto `/books?...`. Both are React Router links (Button `to=`) so they can be opened in a new tab. Visiting `/prices` from the nav with no query shows every saved price. Direct loads and new-tab opens of `/prices` are forwarded to `index.html` by `SpaController` (same as `/books`).

## API
See `endpoints/endpoints-prices.md`.

- `GET /api/prices` — all saved listings
- `POST /api/prices/lookup/{bookId}` — lookup hardcover, softcover, and library binding for one book

Librarian only.

## Import/export
JSON export includes a `prices` array keyed by `bookTitle` + `bookAuthorName` + `cover`. Import merges on that key. Database stats `priceCount` is unique books with at least one usable listing (`priceDollars` present and no `lookupError`).
