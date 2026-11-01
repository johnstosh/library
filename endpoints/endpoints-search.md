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
- `filterInLibrary` (boolean, optional, default `false`) - Legacy: Active books with a non-blank LOC call number. Prefer `status`.
- `filterElectronic` (boolean, optional, default `false`) - Legacy: Active electronic resources. Prefer `status`.
- `filterFreeText` (boolean, optional, default `false`) - Books with a non-blank free text URL
- `filterAudio` (boolean, optional, default `false`) - Books whose free text URL contains `librivox`
- `filterMostRecent` (boolean, optional, default `false`) - Books added on the most recent day UTC, or with a temporary `YYYY-M-D` title
- `filterWithoutLoc` (boolean, optional, default `false`) - Legacy: same as `status=without-loc` (ACTIVE, no LOC, not electronic) when `status` is omitted. See issue #337.
- `filterThreeLetterLoc` (boolean, optional, default `false`) - LOC call number starts with three uppercase letters
- `filterWithoutGrokipedia` (boolean, optional, default `false`) - Books with no Grokipedia URL
- `filterWithGrokipedia` (boolean, optional, default `false`) - Books with a Grokipedia URL
- `filterWithoutGenres` (boolean, optional, default `false`) - Books with no genre tags
- `filterNotActiveStatus` (boolean, optional, default `false`) - Legacy: Lost, Withdrawn, On Order, and Requested when `status` is omitted
- `filterWithoutFreeTextUrls` (boolean, optional, default `false`) - Books with no free text URL
- `filterYdlAudio` (boolean, optional, default `false`) - Books with YDL audio
- `filterYdlBook` (boolean, optional, default `false`) - Books with YDL paper
- `filterYdlEbook` (boolean, optional, default `false`) - Books with YDL ebook
- `filterEmuAudio` (boolean, optional, default `false`) - Books with EMU audio
- `filterEmuBook` (boolean, optional, default `false`) - Books with EMU paper
- `filterEmuEbook` (boolean, optional, default `false`) - Books with EMU ebook
- `filterAclaAudio` (boolean, optional, default `false`) - Books with ACLA audio
- `filterAclaBook` (boolean, optional, default `false`) - Books with ACLA paper
- `filterAclaEbook` (boolean, optional, default `false`) - Books with ACLA ebook
- `filterWithoutYdlAudio`, `filterWithoutYdlBook`, `filterWithoutYdlEbook`, `filterWithoutEmuAudio`, `filterWithoutEmuBook`, `filterWithoutEmuEbook`, `filterWithoutAclaAudio`, `filterWithoutAclaBook`, `filterWithoutAclaEbook` (boolean, optional, default `false`) - That format flag is false or unset

Audio, Book, and Ebook OR within one public library (YDL, EMU, or ACLA) and AND across libraries. Without chips AND with that group. Search authors are authors of titles that match.
- `labels` (string, optional) - Comma-separated genre tags; book must have ALL of them
- `favoriteLists` (string, optional) - Comma-separated favorite list names for the logged-in user. A name that contains a comma is `~` plus a percent-encoded name. Lists are ORed; the clause is ANDed with other filters. Ignored when anonymous.
- `readingDifficulty` (string, optional) - Comma-separated reading-difficulty keys (`children`, `accessible`, `moderate`, `demanding`, `advanced`, `unset`); book must match ANY of them. `unset` also matches null or blank stored values
- `status` (string, optional) - Comma-separated status-filter keys (`in-library`, `electronic-resource`, `without-loc`, `lost`, `withdrawn`, `on-order`, `requested`); book must match ANY of them. `in-library`/`electronic-resource`/`without-loc` are Active-only (`without-loc` also requires no LOC and not electronicResource). When omitted, WITHDRAWN and REQUESTED stay hidden. When every key is selected, status is unconstrained. See issue #337.

Boolean chips AND labels AND status (OR within that list) AND reading-difficulty (OR within that list) AND together, except Audio, Book, and Ebook within one public library, which OR. A Without chip matches a false or unset flag. Conflicting chips, such as YDL Audio together with Without YDL Audio, yield empty results.

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
- Filter books by AND-combined chip filters (Search UI shows discovery chips including Recent Arrivals; Books keeps cataloger chips)
- Independently paginated book and author results (`bookPage` / `authorPage`)
- Powers `/search` page with real-time search and filter chips

---

**Related:** SearchController.java, SearchService.java, SearchResponseDto.java, feature-design-search.md
