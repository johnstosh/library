# Auth

Controller: `AuthController`.

Base path: `/api/auth`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## POST /api/auth/login

Auth: Public. no `@PreAuthorize`; HTTP `/api/auth/**` is permitAll.

Parameters:

- Body `LoginRequestDto` (required).

Response: `200 OK` body `CurrentUserDto` fields: `id`, `username`, `authority`, `ssoSubjectId`, `createdAt`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Login endpoint for React frontend. Accepts username and SHA-256 hashed password.

Public. JSON body is `LoginRequestDto`: `username`, and `password` as the hex SHA-256 of the plaintext password. The database stores BCrypt of that hash. Success sets the HTTP session and returns `CurrentUserDto` (`id`, `username`, `authority`, `ssoSubjectId`, `createdAt`). The session principal name is the user id, not the username. Unknown user or a bad password fails the request. Duplicate usernames use the lowest id.

## POST /api/auth/logout

Auth: Public. no `@PreAuthorize`; HTTP `/api/auth/**` is permitAll.

Parameters:

- Raw request body. The controller reads the servlet input stream.

Response: `200 OK` body `Void`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Logout endpoint for React frontend.

Clears the security context and invalidates the session. Response body is empty.

## GET /api/auth/me

Auth: Public. no `@PreAuthorize`; HTTP `/api/auth/**` is permitAll.

Parameters:

None.

Response: `200 OK` body `CurrentUserDto` fields: `id`, `username`, `authority`, `ssoSubjectId`, `createdAt`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Get current authenticated user info. Works for both local (username/password) and OAuth (Google SSO) authentication. The authentication principal name is always the database user ID.

HTTP allows the call without a session. With no authentication the response is 401 and an empty body. With a session, the principal name is the user id and the body is `CurrentUserDto`.
