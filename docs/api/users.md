# User

Controller: `UserController`.

Base path: `/api/users`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/users/me

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `/api/users/me` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googleApiKey`, `useXaiApiKey`, `useGoogleApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/users

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body Array of `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googleApiKey`, `useXaiApiKey`, `useGoogleApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /api/users/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: `200 OK` body `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googleApiKey`, `useXaiApiKey`, `useGoogleApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/users

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `CreateUserDto` (required).

Response: `201 Created` body `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googleApiKey`, `useXaiApiKey`, `useGoogleApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/users/public/register

Auth: Public. no `@PreAuthorize`; HTTP `/api/users/public/**` is permitAll.

Parameters:

- Body `CreateUserDto` (required).

Response: `201 Created` body `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googleApiKey`, `useXaiApiKey`, `useGoogleApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

`authority` must be `USER`. Any other value is HTTP 400 with an empty body.

## PUT /api/users/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).
- Body `CreateUserDto` (required).

Response: `200 OK` body `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googleApiKey`, `useXaiApiKey`, `useGoogleApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

An SSO user (`ssoProvider` set and not `local`, or `ssoSubjectId` set) cannot change `email`. Sending the same email, or omitting it, still saves. A local user can change email. A changed address moves that user's mailing-list subscriptions onto the new address.

## DELETE /api/users/{id}

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Path `id` (`Long`).

Response: Empty body (`204 No Content` when the method calls `noContent()`, otherwise `200` with `ok().build()`).

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## POST /api/users/delete-bulk

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `List<Long>` (required).

Response: Empty body (`204 No Content` when the method calls `noContent()`, otherwise `200` with `ok().build()`).

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
