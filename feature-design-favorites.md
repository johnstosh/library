# Favorites Feature Design

## Overview
Logged-in patrons and librarians can mark books and authors as favorites using a star icon. Favorites are stored per user as named lists. Guests do not see the star.

## Star UI
- **Not a favorite** (no lists selected): black outline star (`PiStar`, `w-5 h-5`)
- **Favorite** (one or more lists): red filled star (`PiStarFill`, `w-6 h-6`)
- Clicking the star opens a modal. There is no Cancel/Save; the modal closes with the header **(X)** (`data-test="modal-close"`). Checkbox changes persist immediately.

Stars appear for authenticated users on:
- Book and author view pages (next to the title/name)
- Books table, Authors table, and an author’s book list
- Search result rows for books and authors

`data-test`: `favorite-star-book-{id}`, `favorite-star-author-{id}`

## Lists
Built-in patron lists:
- Have Read
- Want to Read
- Want to Recommend

Librarian-only lists (in addition to the patron lists):
- Needs Review
- Need to Locate

**Add Favorite List** creates a custom list name for that user. The new list is added to the checkbox list and **automatically checked**, then saved immediately.

This version does not delete or rename lists. Unchecking a list removes the item from it. A custom name remains in the user’s available lists while they still have memberships on it.

## Data model
Runtime storage uses **book ID** or **author ID** plus **user ID** and **list name**. Exactly one of book or author is set.

JSON export/import uses natural keys:
- `username`
- `listName`
- book favorites: `bookTitle` + `bookAuthorName`
- author favorites: `authorName`

## API
- `GET /api/favorites/summary` — current user’s favorited book/author IDs
- `GET /api/favorites/item?itemType=BOOK|AUTHOR&itemId=` — selected lists and available lists
- `PUT /api/favorites/item` — replace selected lists for that item (immediate)
- `GET /api/import/favorite-stats` — librarian; global counts per list name, books and authors separate, sorted by total descending

Authentication: summary/item require login. Stats require `LIBRARIAN`.

## Data Management
**Favorites Statistics** shows each list name with book count and author count, highest total first.
