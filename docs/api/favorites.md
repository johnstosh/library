# Favorite

Controller: `FavoriteController`.

Base path: `/api/favorites`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/favorites/summary

Auth: Authenticated. class `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `FavoriteSummaryDto` fields: `favoriteBookIds`, `favoriteAuthorIds`, `lists`, `availableLists`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/favorites/item

Auth: Authenticated. class `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Query `itemType` (`FavoriteItemType`, required).
- Query `itemId` (`Long`, required).

Response: `200 OK` body `FavoriteItemDto` fields: `itemType`, `itemId`, `selectedLists`, `availableLists`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/favorites/item

Auth: Authenticated. class `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `FavoriteUpdateDto` (required).

Response: `200 OK` body `FavoriteItemDto` fields: `itemType`, `itemId`, `selectedLists`, `availableLists`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
