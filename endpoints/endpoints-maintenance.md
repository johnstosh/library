# Maintenance Endpoints

Librarian tools on the Data Management page. Illegal-genre cleanup and duplicate-title actions are on `MaintenanceController`. The catalog tools below are on `CatalogToolMaintenanceController` (Issue #386).

**Authentication:** Librarian (`hasAuthority('LIBRARIAN')`)

## Catalog tools

Each book tool covers every book, including withdrawn copies. It does not use the Books page filters. The author Grokipedia tool covers every author.

Recalc lists the changes and saves nothing. Do It writes that list. Undo puts the previous values back. An undo is a new row in `catalog_maintenance_action` that points at the Do It row. Rows are not updated, deleted, or included in JSON import/export.

Grokipedia and free-text Recalc talk to the network, so the page sends book ids in batches (at most 25 Grokipedia books, or 10 free-text books, per request). Chicago title case is one request.

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

### GET /api/maintenance/free-text/candidates

Every book id, in id order.

### POST /api/maintenance/free-text/preview

Body: `{ "bookIds": [1, 2] }`

Same search as Find Links to Free Online Text, without saving. A change is returned only when the merge would add at least one new URL. Existing links stay. `added` lists the new URLs. `after` is the field Do It would store.

### POST /api/maintenance/free-text/apply

Body: `{ "changes": [ { "bookId", "before", "after" } ] }`

Writes `after` only when the stored field is still `before` and `after` is that field plus new links. A value that would drop or replace a link is skipped.

### POST /api/maintenance/free-text/undo

Body: `{ "actionId" }`

Puts the previous free-text field back, which removes only the links Do It added. Leaves a field that has changed since.

### GET /api/maintenance/free-text/undoable

Same shape as the Chicago undoable endpoint.

The Books page carousel still runs these tools on the selected books and saves as it goes. These maintenance endpoints are the whole-catalog preview.
