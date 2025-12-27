# Endpoint Annotations Guide

## Key Rules

1. **Always use `hasAuthority()` NOT `hasRole()`**
   - Database stores: `"LIBRARIAN"` and `"USER"` (no ROLE_ prefix)
   - `hasRole('X')` looks for `"ROLE_X"` ❌
   - `hasAuthority('X')` looks for `"X"` ✅

2. **Add `@Transactional` to endpoints that access lazy-loaded fields**
   - Use `@Transactional(readOnly = true)` for GET endpoints
   - Use `@Transactional` for POST/PUT/DELETE endpoints

3. **Add new endpoint paths to SecurityConfig.java**
   - Every new controller path must be added to SecurityConfig
   - SecurityConfig controls which paths require authentication
   - Use `hasAuthority("LIBRARIAN")` in SecurityConfig to match controller annotations

## Quick Reference

```java
// Public endpoint - no auth needed
@GetMapping("/public")
public ResponseEntity<String> publicEndpoint() { }

// Any authenticated user
@GetMapping("/user-data")
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true)
public ResponseEntity<String> userEndpoint() { }

// Librarian only
@PostMapping("/admin-action")
@PreAuthorize("hasAuthority('LIBRARIAN')")
@Transactional
public ResponseEntity<String> adminEndpoint() { }
```

## Common Mistakes

| Wrong | Right |
|-------|-------|
| `hasRole('LIBRARIAN')` | `hasAuthority('LIBRARIAN')` |
| No `@Transactional` on LOB access | `@Transactional(readOnly = true)` |
| Mixing hasRole and hasAuthority | Use hasAuthority everywhere |

## Troubleshooting

**403 Forbidden?**
- Check annotation: `hasRole` → `hasAuthority`
- Verify user has role in database
- **Check SecurityConfig.java** - ensure path is configured (e.g., `.requestMatchers("/api/your-path/**")`)
- Verify SecurityConfig and @PreAuthorize both use `hasAuthority()`

**Large Objects error?**
- Add `@Transactional` to controller method
- Use `readOnly = true` for GET endpoints

---

## New Endpoints for Book Caching

### GET /api/books/summaries
Returns lightweight book summaries (ID and lastModified timestamp) for browser caching.

**Authentication:** Public (permitAll)

**Response:** Array of BookSummaryDto
```json
[
  {
    "id": 1,
    "lastModified": "2025-01-01T12:00:00"
  },
  {
    "id": 2,
    "lastModified": "2025-01-02T12:00:00"
  }
]
```

### POST /api/books/by-ids
Fetches full book data for a list of book IDs.

**Authentication:** Public (permitAll)

**Request Body:** Array of Long (book IDs)
```json
[1, 2, 3]
```

**Response:** Array of BookDto (full book objects)

**Use Case:**
- Frontend fetches summaries to check what's changed
- Only requests full data for books that are new or modified
- Reduces bandwidth and improves performance

---

## JSON Import/Export Endpoints

### GET /api/import/json
Exports database to JSON format for backup/migration.

**Authentication:** Librarian only

**Response:** ImportRequestDto containing:
- Libraries
- Authors
- Users (including hashed passwords)
- Books
- Loans
- **Photos:** NOT INCLUDED - photos are excluded due to size

**Important Notes:**
- Photos are intentionally excluded from JSON export to prevent response size issues
- Photo data should be managed separately via the Photo Export feature (`/api/photo-export`)
- The JSON export is designed for backing up metadata and structural data only
- Photo files are backed up to Google Photos via the separate photo export functionality

### POST /api/import/json
Imports database from JSON format.

**Authentication:** Librarian only

**Request Body:** ImportRequestDto (same structure as export)

**Behavior:**
- Merges data with existing records (doesn't delete existing data)
- Matches entities by natural keys (e.g., library name, author name, book title+author)
- Photos in the import file are processed but image bytes are not expected

---

## Phase 2 Backend Endpoints (Added Dec 2024)

### GET /api/libraries/statistics
Returns statistics for all libraries including book count and active loans.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of LibraryStatisticsDto
```json
[
  {
    "libraryId": 1,
    "libraryName": "St. Martin de Porres",
    "bookCount": 150,
    "activeLoansCount": 12
  }
]
```

**Use Case:**
- Powers Libraries page statistics display
- Shows book inventory and circulation at a glance

---

### GET /api/authors/without-description
Returns authors that are missing brief biographies.

**Authentication:** Public (`permitAll()`)

**Response:** Array of AuthorDto with `bookCount` and `lastModified`

**Use Case:**
- Filter to find authors needing biographical information
- Matches Books page filtering functionality

---

### GET /api/authors/zero-books
Returns authors that have no associated books.

**Authentication:** Public (`permitAll()`)

**Response:** Array of AuthorDto with `bookCount` set to 0

**Use Case:**
- Identify orphaned author records
- Clean up database by removing unused authors

---

### POST /api/books/suggest-loc
Uses Grok AI to suggest a Library of Congress call number for a book.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:**
```json
{
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald"
}
```

**Response:**
```json
{
  "suggestion": "PS3511.I9 G7"
}
```

**Requirements:**
- User must have xAI API key configured in user settings
- Uses Grok-3-latest model
- 10-minute timeout for API calls

**Error Responses:**
- 400: Title is required
- 500: xAI API key not configured or API call failed

**Use Case:**
- AI-powered assistance for cataloging books
- Suggests LOC call numbers when title/author lookup fails

---

## lastModified Timestamps (Added Dec 2024)

All DTOs now include `lastModified` timestamp for cache invalidation:
- **AuthorDto** - `lastModified: LocalDateTime`
- **UserDto** - `lastModified: LocalDateTime`
- **LoanDto** - `lastModified: LocalDateTime`
- **BookDto** - `lastModified: LocalDateTime` (already included)

**Entities with @PreUpdate:**
- Author, User, Loan, Book - automatically update `lastModified` on save

**Use Case:**
- Frontend cache invalidation strategy
- `/api/books/summaries` returns minimal data for cache comparison
- Only fetch full data for changed items

---

---

## User Management Endpoints

### GET /api/users/me
Returns the current authenticated user's information.

**Authentication:** Authenticated users only (`isAuthenticated()`)

**Response:** UserDto with user details including authorities and API keys

---

### GET /api/users
Returns all users in the system.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of UserDto with `activeLoansCount` for each user

---

### GET /api/users/{id}
Returns a specific user by ID.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** UserDto or 404 if not found

---

### POST /api/users
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

### POST /api/users/public/register
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

### PUT /api/users/{id}
Updates an existing user.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** CreateUserDto (password optional for update)

**Response:** Updated UserDto

---

### PUT /api/users/{id}/apikey
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

### DELETE /api/users/{id}
Deletes a user.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** 204 No Content, or 409 Conflict if user has active loans

---

### POST /api/users/delete-bulk
Deletes multiple users in a single request.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** Array of user IDs
```json
[1, 2, 3]
```

**Response:** 204 No Content, or 409 Conflict if any user has active loans

---

---

## Loan Management Endpoints

### POST /api/loans/checkout
Checkout a book (create a new loan).

**Authentication:** Authenticated users (`isAuthenticated()`)

**Authorization:**
- Librarians can checkout books to any user
- Regular users can only checkout books to themselves

**Request Body:** LoanDto
```json
{
  "bookId": 1,
  "userId": 2,
  "loanDate": "2025-01-01",  // Optional, defaults to today
  "dueDate": "2025-01-15"    // Optional, defaults to 2 weeks from loan date
}
```

**Response:** 201 Created with LoanDto

**Error Responses:**
- 400: Missing bookId or userId
- 403: Regular user attempting to checkout to different user
- 409: Book is already on loan (BOOK_ALREADY_LOANED)

---

### GET /api/loans
Returns loans based on user role.

**Authentication:** Authenticated users (`isAuthenticated()`)

**Query Parameters:**
- `showAll` (boolean, default: false) - Include returned loans

**Behavior:**
- Librarians see all loans
- Regular users see only their own loans

**Response:** Array of LoanDto

---

### GET /api/loans/{id}
Returns a specific loan by ID.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** LoanDto or 404 if not found

---

### PUT /api/loans/{id}
Updates an existing loan.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** LoanDto with fields to update

**Response:** Updated LoanDto

---

### PUT /api/loans/return/{id}
Return a book (set return date on loan).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Updated LoanDto with returnDate set, or 404 if loan not found

---

### DELETE /api/loans/{id}
Deletes a loan record.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** 204 No Content

---

## Library Card Application Endpoints

### POST /api/application/public/register
Submit a library card application (public endpoint).

**Authentication:** Public (no auth required)

**Request Body:** RegistrationRequest
```json
{
  "username": "john_doe",
  "password": "sha256-hashed-password",
  "authority": "USER"
}
```

**Password Requirements:**
- Must be SHA-256 hashed client-side (64 hex characters)
- Server validates hash format before accepting
- Server then hashes with bcrypt for storage

**Response:** 204 No Content on success

**Error Responses:**
- 400: Invalid password format (not SHA-256 hash)
- 500: Internal server error

**Use Case:**
- Public-facing library card application form
- Creates `Applied` entity with status `PENDING`
- Awaits librarian approval

---

### GET /api/applied
Returns all library card applications.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of AppliedDto
```json
[
  {
    "id": 1,
    "name": "john_doe",
    "status": "PENDING"
  }
]
```

**Note:** Password field is NOT included in response for security.

---

### POST /api/applied
Creates a library card application (librarian manual entry).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** Applied entity
```json
{
  "name": "john_doe",
  "password": "sha256-hashed-password"
}
```

**Response:** 201 Created with AppliedDto

---

### PUT /api/applied/{id}
Updates an application's status.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** Applied entity with updated fields
```json
{
  "status": "REJECTED"
}
```

**Response:** Updated AppliedDto or 404 if not found

---

### DELETE /api/applied/{id}
Deletes a library card application.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** 204 No Content or 404 if not found

**Use Case:**
- Reject an application by deleting it
- Clean up old approved applications

---

### POST /api/applied/{id}/approve
Approves a library card application and creates a user account.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Process:**
1. Retrieves application by ID
2. Creates new User account with:
   - username from application
   - password from application (already bcrypt-hashed)
   - authority: `USER`
3. Updates application status to `APPROVED`
4. User can now log in

**Response:** 200 OK or 404 if application not found

**Error Responses:**
- 404: Application not found
- 500: User creation failed (e.g., username already exists)

---

## Library Card PDF Endpoints

### GET /api/library-card/print
Generates and downloads a wallet-sized library card PDF for the current user.

**Authentication:** Authenticated users only (`isAuthenticated()`)

**Response:** PDF file (application/pdf) with download headers
- Content-Type: application/pdf
- Content-Disposition: attachment; filename="library-card.pdf"

**Card Details:**
- Wallet size: 2.125" x 3.375"
- Contains: Library name, patron name, member ID
- Design: Custom colors and logo from LibraryCardDesign settings
- Generated using iText 8

**Error Responses:**
- 401: User not authenticated
- 404: User not found
- 500: PDF generation failed

**Use Case:**
- Users print their library card for physical wallet
- PDF can be saved or printed

---

**Related:** SecurityConfig.java, CustomUserDetailsService.java, Photo.java (@Lob fields), ImportService.java, AskGrok.java, LibraryStatisticsDto.java, AppliedController.java, LibraryCardController.java, LibraryCardPdfService.java
