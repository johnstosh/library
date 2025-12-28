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

## Search Endpoint

### GET /api/search
Returns search results for books and authors matching the query.

**Authentication:** Public (permitAll)

**Query Parameters:**
- `query` (string, required) - Search term to match against book titles and author names
- `page` (int, required) - Zero-based page number
- `size` (int, required) - Number of results per page

**Response:** SearchResponseDto containing:
```json
{
  "books": [
    {
      "id": 1,
      "title": "The Great Gatsby",
      "author": "F. Scott Fitzgerald",
      ...
    }
  ],
  "authors": [
    {
      "id": 1,
      "name": "F. Scott Fitzgerald",
      ...
    }
  ],
  "bookPage": {
    "totalPages": 5,
    "totalElements": 42,
    "currentPage": 0,
    "pageSize": 20
  },
  "authorPage": {
    "totalPages": 2,
    "totalElements": 15,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

**Use Case:**
- Public search across library catalog
- Case-insensitive partial matching on book titles and author names
- Paginated results for both books and authors
- Powers `/search` page with real-time search

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

## Photo Export Endpoints (Google Photos Sync)

### GET /api/photo-export/stats
Returns statistics about photo export status.

**Authentication:** Public (permitAll) ⚠️ **Should be librarian-only**

**Response:** Map containing:
```json
{
  "totalPhotos": 150,
  "exportedPhotos": 120,
  "notExportedPhotos": 30
}
```

**Use Case:**
- Display photo sync status on Data Management page
- Shows how many photos have been backed up to Google Photos

---

### GET /api/photo-export/photos
Returns all photos with their export status.

**Authentication:** Public (permitAll) ⚠️ **Should be librarian-only**

**Response:** Array of photo information maps:
```json
[
  {
    "id": 1,
    "caption": "Book cover",
    "permanentId": "abc123...",
    "hasBeenUploaded": true,
    "bookTitle": "The Great Gatsby",
    "authorName": null
  }
]
```

**Use Case:**
- View which photos have been synced to Google Photos
- Identify photos that need backup

---

### POST /api/photo-export/export-all
Uploads all photos to Google Photos for backup.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Success message or error

**Behavior:**
- Uploads all local photos to Google Photos
- Updates permanent ID for each photo
- Skips photos already uploaded

**Use Case:**
- Backup all library photos to Google Photos cloud storage
- One-click photo backup operation

---

### POST /api/photo-export/export/{photoId}
Uploads a single photo to Google Photos.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `photoId` - Photo ID to export

**Response:** Success message or 404 if photo not found

**Use Case:**
- Selective photo backup to Google Photos
- Re-upload modified photo

---

### POST /api/photo-export/import/{photoId}
Downloads a single photo from Google Photos.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `photoId` - Photo ID to import

**Response:** Success message or error

**Behavior:**
- Fetches photo from Google Photos using permanent ID
- Updates local photo data with downloaded bytes
- Requires photo to have valid permanent ID

**Use Case:**
- Restore photo from Google Photos backup
- Download updated photo from cloud

---

### POST /api/photo-export/import-all
Downloads all photos from Google Photos.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Success message or error

**Behavior:**
- Fetches all photos with permanent IDs from Google Photos
- Updates local database with downloaded photo bytes
- Skips photos without permanent IDs

**Use Case:**
- Restore all photos from Google Photos backup
- Sync photos after database restore from JSON

---

### POST /api/photo-export/verify/{photoId}
Verifies that a photo's permanent ID is valid in Google Photos.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `photoId` - Photo ID to verify

**Response:** Verification result with photo metadata

**Use Case:**
- Check if photo still exists in Google Photos
- Troubleshoot sync issues

---

### POST /api/photo-export/unlink/{photoId}
Removes the Google Photos permanent ID from a photo.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `photoId` - Photo ID to unlink

**Response:** Success message or 404 if photo not found

**Behavior:**
- Clears permanent ID field
- Marks photo as not uploaded
- Does NOT delete photo from Google Photos (only removes link)

**Use Case:**
- Prepare photo for re-upload
- Fix sync issues

---

## Books from Feed Endpoints

### GET /api/books-from-feed/saved-books
Returns books saved from Google Photos that need processing.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of saved book data including:
- Photo information
- Processing status
- AI-generated metadata (if processed)

**Use Case:**
- View books imported from Google Photos feed
- Identify books needing AI processing

---

### POST /api/books-from-feed/process-single/{bookId}
Processes a single saved book with AI to extract metadata.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `bookId` - Saved book ID to process

**Response:** Processed book data with AI-extracted metadata:
- Title
- Author
- Publication year
- Publisher
- Other metadata

**Requirements:**
- User must have xAI API key configured
- Book must have associated photo

**Use Case:**
- Extract book metadata from photo using AI
- Convert saved photos into catalog entries

---

### POST /api/books-from-feed/process-saved
Processes all saved books with AI in batch.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Array of processed books with AI-extracted metadata

**Behavior:**
- Processes each saved book sequentially
- Uses AI to extract metadata from photos
- Updates processing status for each book

**Use Case:**
- Bulk process imported photos from Google Photos feed
- Automated book cataloging from photos

---

### POST /api/books-from-feed/save-from-picker
Saves photos selected from Google Photos Picker.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** Array of Google Photos media item IDs
```json
{
  "mediaItemIds": ["abc123", "def456", "ghi789"]
}
```

**Response:** Success message with count of saved books

**Behavior:**
- Downloads photos from Google Photos
- Creates saved book entries
- Stores photos for later processing

**Use Case:**
- Import book photos from Google Photos using picker UI
- First step in Books from Feed workflow

---

### POST /api/books-from-feed/picker-session
Creates a new Google Photos Picker session.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Picker session data including:
```json
{
  "sessionId": "session-uuid",
  "pickerUrl": "https://photos.google.com/picker/...",
  "expiresAt": "2025-01-15T12:00:00"
}
```

**Use Case:**
- Initialize Google Photos Picker for selecting photos
- Get picker URL to embed in UI

---

### GET /api/books-from-feed/picker-session/{sessionId}
Returns the status of a Google Photos Picker session.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `sessionId` - Session UUID

**Response:** Session status including:
```json
{
  "sessionId": "session-uuid",
  "status": "COMPLETED",
  "selectedCount": 5,
  "expiresAt": "2025-01-15T12:00:00"
}
```

**Use Case:**
- Check if user has finished selecting photos
- Monitor picker session progress

---

### GET /api/books-from-feed/picker-session/{sessionId}/media-items
Returns media items selected in a picker session.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Path Parameter:** `sessionId` - Session UUID

**Response:** Array of selected media item IDs
```json
{
  "mediaItemIds": ["abc123", "def456", "ghi789"]
}
```

**Use Case:**
- Retrieve photos selected by user in picker
- Process selected photos into saved books

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

## Book Label PDF Endpoints

### GET /api/labels/generate
Generates and downloads book pocket labels PDF for selected books.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Query Parameters:**
- `bookIds` (array of Long, required) - Book IDs to generate labels for

**Example:**
```
GET /api/labels/generate?bookIds=1&bookIds=2&bookIds=3
```

**Response:** PDF file (application/pdf) with download headers
- Content-Type: application/pdf
- Content-Disposition: attachment; filename="book-labels.pdf"

**Label Details:**
- Format: Avery 6572 book pocket labels
- Contains: Book title, author, LOC call number formatted for spine
- Only books with LOC call numbers are included
- Generated using iText 8
- Custom LOC formatting via `formatLocForSpine()` utility

**Error Responses:**
- 401: User not authenticated
- 403: User does not have LIBRARIAN authority
- 400: No bookIds provided or invalid bookIds
- 500: PDF generation failed

**Use Case:**
- Librarians generate labels for newly cataloged books
- Accessible via "Generate Labels" button in Books page bulk actions toolbar
- Select multiple books and generate labels in one PDF

---

**Related:** SecurityConfig.java, CustomUserDetailsService.java, Photo.java (@Lob fields), ImportService.java, AskGrok.java, LibraryStatisticsDto.java, AppliedController.java, LibraryCardController.java, LibraryCardPdfService.java, LabelsController.java, LabelsPdfService.java
