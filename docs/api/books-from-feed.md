# BooksFromFeed

Controller: `BooksFromFeedController`.

Base path: `/api/books-from-feed`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/books-from-feed/saved-books

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body Array of `SavedBookDto` fields: `id`, `title`, `authorId`, `author`, `library`, `photoCount`, `needsProcessing`, `locNumber`, `status`, `grokipediaUrl`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Get books from most recent day OR with temporary titles (date-pattern titles). Uses efficient projection query - no N+1 queries. Delegates to BookService.getBooksFromMostRecentDay().

@return List of SavedBookDto with id, title, author, library, photoCount, needsProcessing

## POST /api/books-from-feed/process-single/{bookId}

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `bookId` (`Long`).

Response: `200 OK` body `Map<String, Object>`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Process a single saved book using AI book-by-photo workflow @param bookId The ID of the book to process @return Processing result for this book

## POST /api/books-from-feed/process-single/{bookId}/start

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `bookId` (`Long`).

Response: `200 OK` body `GrokJobDto` fields: `jobId`, `kind`, `status`, `result`, `error`, `httpStatus`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Background version of `processSingleBook`: returns 202 with a job id at once; poll GET /api/grok-jobs/{jobId}. The job result is the same map process-single returns (success / error fields), so a Grok failure still arrives as a SUCCEEDED job with `success=false`.

## POST /api/books-from-feed/save-single-from-picker

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `Map<String, Object>` (required).

Response: `200 OK` body `Map<String, Object>`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Save a single photo selected via Google Photos Picker API to database (Phase 1 - No AI). Each request runs in its own transaction for per-photo isolation. Frontend iterates photos one at a time, showing progress bar.

@param photo Single photo metadata from the Picker @return Result map: {success, photoId, photoName, bookId, title} or {success: false, error}

## POST /api/books-from-feed/save-from-picker

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `Map<String, Object>` (required).

Response: `200 OK` body `Map<String, Object>`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Save photos selected via Google Photos Picker API to database (Phase 1 - No AI) @deprecated Use /save-single-from-picker instead for per-photo transaction isolation. @param request Contains 'photos' array with photo metadata from Picker @return Save results

## POST /api/books-from-feed/picker-session

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `Map<String, Object>`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Create a new Google Photos Picker session (server-side to handle token refresh) @return Session info with id and pickerUri

## GET /api/books-from-feed/picker-session/{sessionId}

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `sessionId` (`String`).

Response: `200 OK` body `Map<String, Object>`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Poll a Google Photos Picker session for completion status @param sessionId The session ID from the Picker API @return Session status with mediaItemsSet field

## GET /api/books-from-feed/picker-session/{sessionId}/media-items

Auth: Librarian. class `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `sessionId` (`String`).

Response: `200 OK` body `Map<String, Object>`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Fetch media items from a Google Photos Picker session (server-side to avoid CORS) @param sessionId The session ID from the Picker API @return Media items list
