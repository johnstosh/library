# User Management Endpoints

## GET /api/users/me
Returns the current authenticated user's information.

**Authentication:** Authenticated users only (`isAuthenticated()`)

**Response:** UserDto with user details including authorities and API keys

---

## GET /api/users
Returns all users in the system.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of UserDto with `activeLoansCount` for each user

---

## GET /api/users/{id}
Returns a specific user by ID.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** UserDto or 404 if not found

---

## POST /api/users
Creates a new user (librarian-created).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** CreateUserDto
```json
{
  "username": "newuser",
  "password": "sha256-hashed-password",
  "authority": "USER"
}
```

**Response:** 201 Created with UserDto

---

## POST /api/users/public/register
Public user self-registration endpoint.

**Authentication:** Public (no auth required)

**Request Body:** CreateUserDto (authority must be "USER")
```json
{
  "username": "newuser",
  "password": "sha256-hashed-password",
  "authority": "USER"
}
```

**Response:** 201 Created with UserDto, or 400 if authority is not "USER"

---

## PUT /api/users/{id}
Updates an existing user.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** CreateUserDto (password optional for update)

**Response:** Updated UserDto

---

## PUT /api/users/{id}/apikey
Updates a user's xAI API key.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** UserDto with `xaiApiKey` field
```json
{
  "xaiApiKey": "xai-api-key-at-least-32-characters"
}
```

**Response:** Updated UserDto, or 500 if key is too short

---

## DELETE /api/users/{id}
Deletes a user.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** 204 No Content, or 409 Conflict if user has active loans

---

## POST /api/users/delete-bulk
Deletes multiple users in a single request.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** Array of user IDs
```json
[1, 2, 3]
```

**Response:** 204 No Content, or 409 Conflict if any user has active loans

---

**Related:** UserController.java, UserService.java, UserDto.java, CreateUserDto.java, feature-design-security.md
