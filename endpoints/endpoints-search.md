# Search Endpoints

## GET /api/search
Returns search results for books and authors matching the query.

**Authentication:** Public (permitAll)

**Query Parameters:**
- `query` (string, required) - Search term to match against book titles and author names
- `page` (int, required) - Zero-based page number
- `size` (int, required) - Number of results per page

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
- Paginated results for both books and authors
- Powers `/search` page with real-time search

---

**Related:** SearchController.java, SearchService.java, SearchResponseDto.java, feature-design-search.md
