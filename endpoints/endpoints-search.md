# Search Endpoints

## GET /api/search
Returns search results for books and authors matching the query.

**Authentication:** Public (permitAll)

**Query Parameters:**
- `query` (string, optional, default `""`) - Search term to match against book titles and author names
- `page` (int, optional) - Legacy zero-based page used for both lists when `bookPage`/`authorPage` are omitted
- `bookPage` (int, optional, default `0`) - Zero-based page number for book results
- `authorPage` (int, optional, default `0`) - Zero-based page number for author results
- `size` (int, required) - Number of results per page
- `filterInLibrary` (boolean, optional, default `false`) - Books with a non-blank LOC call number
- `filterElectronic` (boolean, optional, default `false`) - Books marked as electronic resources
- `filterFreeText` (boolean, optional, default `false`) - Books with a non-blank free text URL
- `filterAudio` (boolean, optional, default `false`) - Books whose free text URL contains `librivox`
- `filterMostRecent` (boolean, optional, default `false`) - Books added on the most recent day UTC, or with a temporary `YYYY-M-D` title
- `filterWithoutLoc` (boolean, optional, default `false`) - Books with no LOC call number
- `filterThreeLetterLoc` (boolean, optional, default `false`) - LOC call number starts with three uppercase letters
- `filterWithoutGrokipedia` (boolean, optional, default `false`) - Books with no Grokipedia URL
- `filterWithGrokipedia` (boolean, optional, default `false`) - Books with a Grokipedia URL
- `filterWithoutGenres` (boolean, optional, default `false`) - Books with no genre tags
- `filterNotActiveStatus` (boolean, optional, default `false`) - When `false`, hide WITHDRAWN books. When `true`, only non-ACTIVE statuses (LOST, WITHDRAWN, ON_ORDER, …)
- `filterWithoutFreeTextUrls` (boolean, optional, default `false`) - Books with no free text URL
- `filterYdlAudio` (boolean, optional, default `false`) - Books with YDL audio
- `filterYdlBook` (boolean, optional, default `false`) - Books with YDL paper
- `filterYdlEbook` (boolean, optional, default `false`) - Books with YDL ebook
- `filterEmuAudio` (boolean, optional, default `false`) - Books with EMU audio
- `filterEmuBook` (boolean, optional, default `false`) - Books with EMU paper
- `filterEmuEbook` (boolean, optional, default `false`) - Books with EMU ebook
- `labels` (string, optional) - Comma-separated genre tags; book must have ALL of them

All active boolean chips AND labels AND together. Conflicting chips may yield empty results.

**Response:** SearchResponseDto containing:
```json
{
  "books": [
    {
      "id": 1,
      "title": "The Great Gatsby",
      "author": "F. Scott Fitzgerald",
      "firstPhotoId": 12,
      "firstPhotoChecksum": "abc123",
      ...
    }
  ],
  "authors": [
    {
      "id": 1,
      "name": "F. Scott Fitzgerald",
      "firstPhotoId": 44,
      "firstPhotoChecksum": "def456",
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
- Filter books by AND-combined chip filters (Search UI shows discovery chips; Books keeps cataloger chips)
- Independently paginated book and author results (`bookPage` / `authorPage`)
- Powers `/search` page with real-time search and filter chips

---

**Related:** SearchController.java, SearchService.java, SearchResponseDto.java, feature-design-search.md
