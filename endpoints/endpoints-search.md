# Search Endpoints

## GET /api/search
Returns search results for books and authors matching the query.

**Authentication:** Public (permitAll)

**Query Parameters:**
- `query` (string, required) - Search term to match against book titles and author names
- `page` (int, required) - Zero-based page number
- `size` (int, required) - Number of results per page
- `searchType` (string, optional) - Filter for book search scope (default: `IN_LIBRARY`):
  - `ONLINE` - Only books with a free text URL
  - `ALL` - All books (no filtering)
  - `IN_LIBRARY` - Only books with a LOC call number

**Response:** SearchResponseDto containing:
```json
{
  "books": [
    {
      "id": 1,
      "title": "The Great Gatsby",
      "author": "F. Scott Fitzgerald",
      ...
    }
  ],
  "authors": [
    {
      "id": 1,
      "name": "F. Scott Fitzgerald",
      ...
    }
  ],
  "bookPage": {
    "totalPages": 5,
    "totalElements": 42,
    "currentPage": 0,
    "pageSize": 20
  },
  "authorPage": {
    "totalPages": 2,
    "totalElements": 15,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

**Use Case:**
- Public search across library catalog
- Case-insensitive partial matching on book titles and author names
- Filter books by availability: online only, all, or in-library materials (default)
- Paginated results for both books and authors
- Powers `/search` page with real-time search and search type radio buttons

---

**Related:** SearchController.java, SearchService.java, SearchResponseDto.java, feature-design-search.md
