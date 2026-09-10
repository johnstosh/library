# Favorites Endpoints

**Authentication:** all `/api/favorites/**` endpoints require a logged-in user (`isAuthenticated()`).

### GET /api/favorites/summary
Returns the current user’s favorited item IDs (any list) plus per-list book and author IDs for filter chips.

```json
{
  "favoriteBookIds": [1, 9],
  "favoriteAuthorIds": [4],
  "lists": [
    { "listName": "Have Read", "bookIds": [1], "authorIds": [4] }
  ]
}
```

### GET /api/favorites/item?itemType=BOOK&itemId=1
Returns selected list names for that book or author, plus the lists available to this user (built-in for their role, then their custom lists).

`itemType` is `BOOK` or `AUTHOR`.

### PUT /api/favorites/item
Replaces the current user’s lists for one item. Empty `listNames` removes all memberships (star outline).

```json
{
  "itemType": "BOOK",
  "itemId": 1,
  "listNames": ["Have Read", "Nightstand"]
}
```

Patrons cannot assign librarian-only lists (`Needs Review`, `Need to Locate`).

### GET /api/import/favorite-stats
**Authentication:** Librarian only.

Global membership counts per list name, with separate `bookCount` and `authorCount`. Sorted by total descending, then name.
