# REST API

These pages document the HTTP API implemented by the controllers in
`src/main/java/com/muczynski/library/controller`. One file per controller.
They describe this version of the source tree. Older versions are not documented here.

Field lists live in [types.md](types.md).

## Auth

A call is allowed only when both checks allow it:

- `SecurityConfig` picks the first matching rule. Unmatched paths require a logged-in user.
- `@PreAuthorize` on the method, or on the class when the method has none. `hasAuthority('LIBRARIAN')` still applies when the HTTP rule is `permitAll`. `permitAll()` on a method does not open a path whose HTTP rule requires a login.

Authorities are `LIBRARIAN` and `USER` with no `ROLE_` prefix. CSRF is disabled.
`POST /api/auth/login` expects the password as hex SHA-256. The session principal name is the numeric user id.

## Keeping this accurate

When a controller gains, loses, or renames a `@GetMapping`, `@PostMapping`, `@PutMapping`,
`@DeleteMapping`, or `@PatchMapping`, update the matching `## METHOD /path` section.
`ApiDocumentationCoverageTest` fails when a mapping has no heading in this folder.
The heading line is the method, a space, and the full path, for example `## GET /api/books/summaries`.

## Controllers

| File | Controller | Base path | Mappings |
| --- | --- | --- | --- |
| [sellers.md](sellers.md) | `AbeSellerController` | `/api` | 5 |
| [acla-lookup.md](acla-lookup.md) | `AclaLookupController` | `/api/acla-lookup` | 1 |
| [applications.md](applications.md) | `AppliedController` | `/api` | 6 |
| [auth.md](auth.md) | `AuthController` | `/api/auth` | 3 |
| [authors.md](authors.md) | `AuthorController` | `/api/authors` | 28 |
| [books.md](books.md) | `BookController` | `/api/books` | 37 |
| [prices.md](prices.md) | `BookPriceController` | `/api/prices` | 5 |
| [books-from-feed.md](books-from-feed.md) | `BooksFromFeedController` | `/api/books-from-feed` | 8 |
| [branches.md](branches.md) | `BranchController` | `/api/branches` | 6 |
| [catalog-tools.md](catalog-tools.md) | `CatalogToolMaintenanceController` | `/api/maintenance` | 16 |
| [emu-lookup.md](emu-lookup.md) | `EmuLookupController` | `/api/emu-lookup` | 1 |
| [favorites.md](favorites.md) | `FavoriteController` | `/api/favorites` | 3 |
| [free-text.md](free-text.md) | `FreeTextLookupController` | `/api/free-text` | 3 |
| [global-settings.md](global-settings.md) | `GlobalSettingsController` | `/api/global-settings` | 4 |
| [google-oauth.md](google-oauth.md) | `GoogleOAuthController` | `/api/oauth/google` | 3 |
| [grok-jobs.md](grok-jobs.md) | `GrokJobController` | `/api/grok-jobs` | 1 |
| [import.md](import.md) | `ImportController` | `/api/import` | 8 |
| [labels.md](labels.md) | `LabelsController` | `/api/labels` | 1 |
| [library-card.md](library-card.md) | `LibraryCardController` | `/api/library-card` | 3 |
| [loans.md](loans.md) | `LoanController` | `/api/loans` | 10 |
| [loc-bulk-lookup.md](loc-bulk-lookup.md) | `LocBulkLookupController` | `/api/loc-bulk-lookup` | 4 |
| [mailing-lists.md](mailing-lists.md) | `MailingListController` | `(none)` | 3 |
| [maintenance.md](maintenance.md) | `MaintenanceController` | `/api/maintenance` | 13 |
| [msu-lookup.md](msu-lookup.md) | `MsuLookupController` | `/api/msu-lookup` | 1 |
| [photos.md](photos.md) | `PhotoController` | `/api/photos` | 8 |
| [photo-export.md](photo-export.md) | `PhotoExportController` | `/api/photo-export` | 14 |
| [search.md](search.md) | `SearchController` | `/api/search` | 1 |
| [spa.md](spa.md) | `SpaController` | `(none)` | 42 |
| [test-data.md](test-data.md) | `TestDataController` | `/api/test-data` | 6 |
| [users.md](users.md) | `UserController` | `/api/users` | 8 |
| [user-settings.md](user-settings.md) | `UserSettingsController` | `/api/user-settings` | 3 |
| [wcc-lookup.md](wcc-lookup.md) | `WccLookupController` | `/api/wcc-lookup` | 1 |
| [ydl-lookup.md](ydl-lookup.md) | `YdlLookupController` | `/api/ydl-lookup` | 1 |

257 mappings.
