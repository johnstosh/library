# Author Endpoints

## GET /api/authors/without-description
Returns authors that are missing brief biographies.

**Authentication:** Public (`permitAll()`)

**Response:** Array of AuthorDto with `bookCount` and `lastModified`

**Use Case:**
- Filter to find authors needing biographical information
- Matches Books page filtering functionality

---

## GET /api/authors/zero-books
Returns authors that have no associated books.

**Authentication:** Public (`permitAll()`)

**Response:** Array of AuthorDto with `bookCount` set to 0

**Use Case:**
- Identify orphaned author records
- Clean up database by removing unused authors

---

## GET /api/authors/without-grokipedia
Returns authors that are missing a Grokipedia URL.

**Authentication:** Public (`permitAll()`)

**Response:** Array of AuthorDto with `bookCount`

**Use Case:**
- Filter to find authors needing Grokipedia links
- Systematic data enrichment workflow

---

## DELETE /api/authors/{id}
Deletes an author by ID.

**Authentication:** Requires `LIBRARIAN` authority

**Path Parameters:**
- `id` - Author ID to delete

**Response:**
- `204 No Content` - Author deleted successfully
- `409 Conflict` - Author has associated books and cannot be deleted
  - Body: `{ "message": "Cannot delete author because it has N associated books." }`

**Use Case:**
- Remove authors from the system
- Authors with books must have their books reassigned or deleted first

---

**Related:** AuthorController.java, AuthorService.java, AuthorDto.java
