# UserSettings

Controller: `UserSettingsController`.

Base path: `/api/user-settings`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/user-settings

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `/api/user-settings` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/user-settings

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `/api/user-settings` requires a logged-in user.

Parameters:

- Body `UserSettingsDto` (required).

Response: `200 OK` body `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## DELETE /api/user-settings

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `/api/user-settings` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.
