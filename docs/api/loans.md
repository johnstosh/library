# Loan

Controller: `LoanController`.

Base path: `/api/loans`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/loans/title-loaned

Auth: Public. method `@PreAuthorize("permitAll()")`; HTTP `/api/loans/title-loaned` is permitAll.

Parameters:

- Query `bookId` (`Long`, required).

Response: `200 OK` body `TitleLoanedDto` fields: `bookId`, `title`, `loaned`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Public catalog availability: whether this book title is currently on loan. Does not include borrower identity.

Public. `bookId` is required. Says whether that title is currently loaned.

## POST /api/loans/checkout

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `LoanDto` (required).

Response: `201 Created` body `LoanDto` fields: `id`, `bookId`, `bookTitle`, `userId`, `userName`, `loanDate`, `dueDate`, `returnDate`, `lastModified`, `photoId`, `photoChecksum`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/loans/return/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `LoanDto` fields: `id`, `bookId`, `bookTitle`, `userId`, `userName`, `loanDate`, `dueDate`, `returnDate`, `lastModified`, `photoId`, `photoChecksum`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/loans

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Query `showAll` (`boolean`, optional, default `false`).

Response: `200 OK` body Array of `LoanDto` fields: `id`, `bookId`, `bookTitle`, `userId`, `userName`, `loanDate`, `dueDate`, `returnDate`, `lastModified`, `photoId`, `photoChecksum`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Authenticated. A librarian sees every loan. Any other user sees only their own. `showAll` defaults to false and then returns active loans (not yet returned). `showAll=true` includes returned loans. No paging.

## GET /api/loans/{id}

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `LoanDto` fields: `id`, `bookId`, `bookTitle`, `userId`, `userName`, `loanDate`, `dueDate`, `returnDate`, `lastModified`, `photoId`, `photoChecksum`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/loans/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).
- Body `LoanDto` (required).

Response: `200 OK` body `LoanDto` fields: `id`, `bookId`, `bookTitle`, `userId`, `userName`, `loanDate`, `dueDate`, `returnDate`, `lastModified`, `photoId`, `photoChecksum`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/loans/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: Empty body (`204 No Content` when the method calls `noContent()`, otherwise `200` with `ok().build()`).

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/loans/checkout-with-photo

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Consumes: `multipart/form-data`.

Parameters:

- Query `bookId` (`Long`, required).
- Query `userId` (`Long`, required).
- Query `loanDate` (`String`, optional).
- Query `dueDate` (`String`, optional).
- Part `photo` (`MultipartFile`, optional).

Response: `201 Created` body `LoanDto` fields: `id`, `bookId`, `bookTitle`, `userId`, `userName`, `loanDate`, `dueDate`, `returnDate`, `lastModified`, `photoId`, `photoChecksum`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Checkout a book with an optional checkout card photo. Uses multipart form data to accept both loan data and photo.

## POST /api/loans/{id}/photo

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Consumes: `multipart/form-data`.

Parameters:

- Path `id` (`Long`).
- Part `photo` (`MultipartFile`, required).

Response: `200 OK` body `LoanDto` fields: `id`, `bookId`, `bookTitle`, `userId`, `userName`, `loanDate`, `dueDate`, `returnDate`, `lastModified`, `photoId`, `photoChecksum`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Add or replace the checkout card photo for an existing loan.

## POST /api/loans/transcribe-checkout-card

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Part `photo` (`MultipartFile`, required).

Response: `200 OK` body `CheckoutCardTranscriptionDto` fields: `title`, `author`, `callNumber`, `lastDate`, `lastIssuedTo`, `lastDue`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
