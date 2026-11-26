# Spa

Controller: `SpaController`.

Base path: `(none)`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /

Auth: Public. no `@PreAuthorize`; HTTP `/` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /books

Auth: Public. no `@PreAuthorize`; HTTP `/books/**` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /books/**

Auth: Public. no `@PreAuthorize`; HTTP `/books/**` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /authors

Auth: Public. no `@PreAuthorize`; HTTP `/authors/**` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /authors/**

Auth: Public. no `@PreAuthorize`; HTTP `/authors/**` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /libraries

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /libraries/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /branches

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /branches/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /data-management

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /photos-management

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /photos-management/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /loans

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /loans/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /users

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /users/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /search

Auth: Public. no `@PreAuthorize`; HTTP `/search` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /search/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /mailing-lists

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

The signup page for a logged-in user. Unsubscribe stays on `MailingListController` and is not forwarded here.

## GET /my-card

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /applications

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /applications/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /labels

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /labels/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /books-from-feed

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /books-from-feed/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /settings

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /settings/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /global-settings

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /global-settings/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /test-data

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /test-data/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /login

Auth: Public. no `@PreAuthorize`; HTTP `/login` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /apply

Auth: Public. no `@PreAuthorize`; HTTP `/apply` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /photos

Auth: Public. no `@PreAuthorize`; HTTP `/photos/**` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /photos/**

Auth: Public. no `@PreAuthorize`; HTTP `/photos/**` is permitAll.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /prices

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /prices/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /sellers

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /sellers/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /savings

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.

## GET /savings/**

Auth: Authenticated. no `@PreAuthorize`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: View name string. `SpaController` returns `forward:/index.html`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

) are not affected by this mapping.
