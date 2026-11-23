# Search

Controller: `SearchController`.

Base path: `/api/search`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/search

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/search/**` is permitAll.

Parameters:

- Query `query` (`String`, optional, default empty).
- Query `page` (`Integer`, optional).
- Query `bookPage` (`Integer`, optional).
- Query `authorPage` (`Integer`, optional).
- Query `size` (`int`, required).
- Query `filterInLibrary` (`boolean`, optional, default `false`).
- Query `filterElectronic` (`boolean`, optional, default `false`).
- Query `filterFreeText` (`boolean`, optional, default `false`).
- Query `filterAudio` (`boolean`, optional, default `false`).
- Query `filterMostRecent` (`boolean`, optional, default `false`).
- Query `filterWithoutLoc` (`boolean`, optional, default `false`).
- Query `filterThreeLetterLoc` (`boolean`, optional, default `false`).
- Query `filterWithoutGrokipedia` (`boolean`, optional, default `false`).
- Query `filterWithGrokipedia` (`boolean`, optional, default `false`).
- Query `filterWithoutGenres` (`boolean`, optional, default `false`). Titles with no genre tags. The Search page shows this chip only to librarians.
- Query `filterNotActiveStatus` (`boolean`, optional, default `false`).
- Query `filterWithoutFreeTextUrls` (`boolean`, optional, default `false`).
- Query `filterYdlAudio` (`boolean`, optional, default `false`).
- Query `filterYdlBook` (`boolean`, optional, default `false`).
- Query `filterYdlEbook` (`boolean`, optional, default `false`).
- Query `filterEmuAudio` (`boolean`, optional, default `false`).
- Query `filterEmuBook` (`boolean`, optional, default `false`).
- Query `filterEmuEbook` (`boolean`, optional, default `false`).
- Query `filterAclaAudio` (`boolean`, optional, default `false`).
- Query `filterAclaBook` (`boolean`, optional, default `false`).
- Query `filterAclaEbook` (`boolean`, optional, default `false`).
- Query `filterWithoutYdlAudio` (`boolean`, optional, default `false`).
- Query `filterWithoutYdlBook` (`boolean`, optional, default `false`).
- Query `filterWithoutYdlEbook` (`boolean`, optional, default `false`).
- Query `filterWithoutEmuAudio` (`boolean`, optional, default `false`).
- Query `filterWithoutEmuBook` (`boolean`, optional, default `false`).
- Query `filterWithoutEmuEbook` (`boolean`, optional, default `false`).
- Query `filterWithoutAclaAudio` (`boolean`, optional, default `false`).
- Query `filterWithoutAclaBook` (`boolean`, optional, default `false`).
- Query `filterWithoutAclaEbook` (`boolean`, optional, default `false`).
- Query `filterWccAudio` (`boolean`, optional, default `false`).
- Query `filterWccBook` (`boolean`, optional, default `false`).
- Query `filterWccEbook` (`boolean`, optional, default `false`).
- Query `filterWithoutWccAudio` (`boolean`, optional, default `false`).
- Query `filterWithoutWccBook` (`boolean`, optional, default `false`).
- Query `filterWithoutWccEbook` (`boolean`, optional, default `false`).
- Query `filterMsuAudio` (`boolean`, optional, default `false`).
- Query `filterMsuBook` (`boolean`, optional, default `false`).
- Query `filterMsuEbook` (`boolean`, optional, default `false`).
- Query `filterWithoutMsuAudio` (`boolean`, optional, default `false`).
- Query `filterWithoutMsuBook` (`boolean`, optional, default `false`).
- Query `filterWithoutMsuEbook` (`boolean`, optional, default `false`).
- Query `labels` (`String`, optional).
- Query `status` (`String`, optional).
- Query `readingDifficulty` (`String`, optional).
- Query `favoriteLists` (`String`, optional).
- Query `libraryCheckedOlder` (`String`, optional).
- Query `libraryCheckedOlderDays` (`int`, optional, default `90`).
- Query `libraryLookupError` (`String`, optional).

Response: `200 OK` body `SearchResponseDto` fields: `books`, `authors`, `bookPage`, `authorPage`.

Paging: `size` is required and is the page size for both lists. `bookPage` overrides `page` for books, and `authorPage` overrides `page` for authors. When those are omitted, `page` is used, and it defaults to 0. The two lists page independently.

Ordering: Each list is a Spring Data page of size `size`.

Notes:

Public. `query` defaults to an empty string, which does not require a title match. Boolean filters default to false. `labels`, `status`, `readingDifficulty`, and `favoriteLists` are comma-separated. Status and reading-difficulty tokens are the same keys as `GET /api/books/filtered-summaries`. `libraryCheckedOlder` and `libraryLookupError` use `YDL`, `EMU`, `ACLA`, `WCC`, or `MSU`. `libraryCheckedOlderDays` defaults to 90. Response is `SearchResponseDto`: `books` (`BookDto` list), `authors` (`AuthorDto` list), `bookPage` and `authorPage` (`totalPages`, `totalElements`, `currentPage`, `pageSize`). Favorite filters use the session principal when its name parses as a numeric user id.
