# UserSettings

Controller: `UserSettingsController`.

Base path: `/api/user-settings`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/user-settings

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `/api/user-settings` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googleApiKey`, `useXaiApiKey`, `useGoogleApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## PUT /api/user-settings

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `/api/user-settings` requires a logged-in user.

Parameters:

- Body `UserSettingsDto` (required).

Response: `200 OK` body `UserDto` fields: `id`, `username`, `authorities`, `xaiApiKey`, `googleApiKey`, `useXaiApiKey`, `useGoogleApiKey`, `googlePhotosApiKey`, `googlePhotosRefreshToken`, `googlePhotosTokenExpiry`, `googleClientSecret`, `googlePhotosAlbumId`, `lastPhotoTimestamp`, `ssoProvider`, `ssoSubjectId`, `email`, `phone`, `receiveEmails`, `libraryCardDesign`, `activeLoansCount`, `createdAt`, `lastModified`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

An SSO user (`ssoProvider` set and not `local`, or `ssoSubjectId` set) cannot change `email`. Sending the same email, or omitting it, still saves. A local user can change email. A changed address moves that user's `mailing_list_subscription` rows from the old address to the new one. Clearing the address turns those rows off so mail does not keep going to the old address. OAuth user creation may still set the email from the provider.

## DELETE /api/user-settings

Auth: Authenticated. method `@PreAuthorize("isAuthenticated()")`; HTTP `/api/user-settings` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

None.

## GET /notifications/unsubscribe/{token}

Auth: Public. no `@PreAuthorize`; HTTP `/notifications/unsubscribe/**` is permitAll. No login and no confirmation step.

Parameters:

- Path `token` (`String`, required). The user's unguessable notification-unsubscribe secret. It is not a mailing-list token.

Response: `200 OK` `text/html`. The page says email notifications are turned off, or were already off. `404 Not Found` `text/html` when the token matches no user.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Sets `receiveEmails` false for that user and does nothing else. A second GET with the same token is idempotent. The token stays on the user so the link still resolves. The token is not written to the log. This is not `/mailing-lists/unsubscribe/{token}`.

## POST /notifications/unsubscribe/{token}

Auth: Public. no `@PreAuthorize`; HTTP `/notifications/unsubscribe/**` is permitAll. No login and no confirmation step.

Parameters:

- Path `token` (`String`, required). The same secret as the GET link. A body is not required. Mail clients may POST `List-Unsubscribe=One-Click`.

Response: `200 OK` `text/plain` with the same sentence as the HTML page. `404 Not Found` `text/plain` when the token matches no user.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

One-click unsubscribe for loan and application notices. A repeated POST stays unsubscribed. The token is not written to the log.
