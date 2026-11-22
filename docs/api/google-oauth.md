# GoogleOAuth

Controller: `GoogleOAuthController`.

Base path: `/api/oauth/google`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/oauth/google/authorize

Auth: Public. no `@PreAuthorize`; HTTP `/api/oauth/google/authorize` is permitAll.

Parameters:

- Query `origin` (`String`, required).

Response: `RedirectView`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Initiate OAuth flow - redirects user to Google consent screen

## GET /api/oauth/google/callback

Auth: Public. no `@PreAuthorize`; HTTP `/api/oauth/google/callback` is permitAll.

Parameters:

- Query `code` (`String`, required).
- Query `state` (`String`, required).
- Query `error` (`String`, optional).

Response: `RedirectView`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Handle OAuth callback from Google

## POST /api/oauth/google/revoke

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `Map<String, String>`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Revoke Google Photos access
