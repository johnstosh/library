# Library Card Endpoints

## Library Card Applications

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

## Library Card PDF

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

**Related:** AppliedController.java, LibraryCardController.java, LibraryCardPdfService.java, Applied.java, AppliedDto.java, LibraryCardDesign.java, feature-design-library-cards.md
