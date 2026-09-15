# Favorites Endpoints

**Authentication:** all `/api/favorites/**` endpoints require a logged-in user (`isAuthenticated()`).

### GET /api/favorites/summary
Returns the current user’s favorited item IDs (any list), per-list book and author IDs for filter chips, and `availableLists` for the star editor (built-in lists for the caller’s role, then custom names).

The star editor uses this payload so opening a star does not wait on `GET /api/favorites/item`. Reads use membership projections (`list_name`, `book_id`, `author_id`) rather than loading Book/Author rows.

```json
{
  "favoriteBookIds": [1, 9],
  "favoriteAuthorIds": [4],
  "lists": [
    { "listName": "Have Read", "bookIds": [1], "authorIds": [4] }
  ],
  "availableLists": ["Have Read", "Want to Read", "Want to Recommend"]
}
```

### GET /api/favorites/item?itemType=BOOK&itemId=1
Returns selected list names for that book or author, plus the lists available to this user (built-in for their role, then their custom lists).

Existence is checked with `existsById`; selected names come from a list-name query on `(user_id, book_id)` / `(user_id, author_id)`. Book and Author entities are not loaded.

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
