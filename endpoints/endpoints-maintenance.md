# Maintenance Endpoints

Librarian tools on the Data Management page. Illegal-genre cleanup and duplicate-title actions are on `MaintenanceController`. The catalog tools below are on `CatalogToolMaintenanceController` (Issue #386).

## Illegal genres

### GET /api/maintenance/illegal-genres/count

Recalc. Saves nothing. `booksAffected` is how many books would change. `changes` is the table, with drops first. A row names the titles when one, two, or three books share that change. A larger group is a count, for example `101 titles get children changed to childrens`.

### POST /api/maintenance/illegal-genres/cleanup

Rewrites every book onto the standard genre list. Drops tags that are not on the list. A book is cleaned once, with all of its tags. The result says how many books were looked at. A removed tag is not also counted as a spelling correction. Records the previous tags on `catalog_maintenance_action` and returns `actionId` for Undo. That table is not part of JSON import/export.

### POST /api/maintenance/illegal-genres/undo

Body: `{ "actionId" }`

Puts those genre lists back when the book still has the tags Clean Genres wrote. A second undo of the same action is refused.

### GET /api/maintenance/illegal-genres/undoable

The latest Clean Genres that has not been undone. `actionId` is null when there is nothing to undo.

**Authentication:** Librarian (`hasAuthority('LIBRARIAN')`)

## Catalog tools

Each book tool covers every book, including withdrawn copies. It does not use the Books page filters. The author Grokipedia tool covers every author.

Chicago and both Grokipedia tools: Recalc lists the changes and saves nothing. Do It writes that list. Undo puts the previous values back. An undo is a new row in `catalog_maintenance_action` that points at the Do It row. Rows are not updated, deleted, or included in JSON import/export.

Free text does not use Recalc, Do It, or Undo. It searches 100, 200, 400, or 800 books, never-searched first and then oldest `freeTextLastSearched`. Each request searches at most 10 books and stores every finished book before the next one, including a miss. A new search does not write a `catalog_maintenance_action` row. Older free-text rows already in that table are left as they are.

Grokipedia Recalc talks to the network, so the page sends ids in batches of at most 25. Chicago title case is one request.

### POST /api/maintenance/chicago-titles/preview

Rewrites nothing. Returns every title that is not already in Chicago title case, with `before` and `after`. A title whose Chicago form is already used by another book is returned in `skipped` and is not a change.

### POST /api/maintenance/chicago-titles/apply

Body: `{ "changes": [ { "bookId", "before", "after" } ] }`

Writes a change only when the book title is still `before`, `after` is still the Chicago form of that title, and `after` is not already used. Returns `actionId` for Undo.

### POST /api/maintenance/chicago-titles/undo

Body: `{ "actionId" }`

Puts those titles back. A title is left alone when it is no longer the value Do It saved, or when the previous wording is now used by another book. A second undo of the same action is refused.

### GET /api/maintenance/chicago-titles/undoable

The latest Chicago Do It that has not been undone. `actionId` is null when there is nothing to undo.

### GET /api/maintenance/grokipedia/candidates

Every book id, in id order. Books that already have a Grokipedia URL are included so alternate titles can still be checked.

### POST /api/maintenance/grokipedia/preview

Body: `{ "bookIds": [1, 2] }`

Quick lookup only: the usual `grokipedia.com/page/...` URL for the primary title and each alternate title. Returns a change when a page answers 2xx and that URL is not already stored. A book that already has a URL can gain another one. Does not save, does not replace or remove a URL, and does not store `"-"`. `added` lists the new URLs. `after` is the field Do It would store. Several URLs are separated by spaces.

### POST /api/maintenance/grokipedia/apply

Body: `{ "changes": [ { "bookId", "before", "after" } ] }`

Writes `after` only when the stored field is still `before` and `after` only adds `grokipedia.com/page/` URLs. A value that would drop or replace a link is skipped. A blank value and `"-"` count as no stored URL.

### POST /api/maintenance/grokipedia/undo

Body: `{ "actionId" }`

Removes only the URLs that Do It added, or puts back the `"-"` that was there before. Leaves a field that has changed since.

### GET /api/maintenance/grokipedia/undoable

Same shape as the Chicago undoable endpoint.

### GET /api/maintenance/grokipedia-authors/candidates

Every author id, in id order.

### POST /api/maintenance/grokipedia-authors/preview

Body: `{ "authorIds": [1, 2] }`

Same quick lookup as the book tool, using the primary name and each alternate name. Adds pages. Does not replace or remove a stored URL, and does not store `"-"`.

### POST /api/maintenance/grokipedia-authors/apply

Body: `{ "changes": [ { "authorId", "before", "after" } ] }`

Same add-only rule as the book tool.

### POST /api/maintenance/grokipedia-authors/undo

Body: `{ "actionId" }`

Puts the previous author URLs back.

### GET /api/maintenance/grokipedia-authors/undoable

The latest author Grokipedia Do It that has not been undone.

### GET /api/maintenance/free-text/candidates?limit=

`limit` must be 100, 200, 400, or 800. Anything else is HTTP 400 with `Search 100, 200, 400, or 800 books.`

Returns that many book ids. Never-searched books come first, then the oldest `freeTextLastSearched`, then the lowest id. Blank and temporary primary titles are left out before the limit is applied. Alternate titles do not change the order. The response is `{ "bookIds": [ ... ] }`.

### POST /api/maintenance/free-text/search

Body: `{ "bookIds": [1, 2] }`

Searches at most 10 books. Each book is loaded as id, primary title, alternate titles, free-text URL, author name, and author alternate names. The lookup uses those titles and names. A new URL is merged onto the URLs already stored. The merged URL and `freeTextLastSearched` commit together. A miss or a per-book error commits the timestamp and leaves the URL unchanged. A blank or temporary primary title is skipped and not stamped. The response lists added links in `changes` and failed books in `skipped`. Links in that response are already stored.

The Books page still looks up the selected books one id at a time and stamps `freeTextLastSearched` the same way. It has no 100/200/400/800 control.
