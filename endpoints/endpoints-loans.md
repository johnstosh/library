# Loan Management Endpoints

## POST /api/loans/checkout
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

## GET /api/loans
Returns loans based on user role.

**Authentication:** Authenticated users (`isAuthenticated()`)

**Query Parameters:**
- `showAll` (boolean, default: false) - Include returned loans

**Behavior:**
- Librarians see all loans
- Regular users see only their own loans

**Response:** Array of LoanDto

---

## GET /api/loans/{id}
Returns a specific loan by ID.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** LoanDto or 404 if not found

---

## PUT /api/loans/{id}
Updates an existing loan.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** LoanDto with fields to update

**Response:** Updated LoanDto

---

## PUT /api/loans/return/{id}
Return a book (set return date on loan).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** Updated LoanDto with returnDate set, or 404 if loan not found

---

## DELETE /api/loans/{id}
Deletes a loan record.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** 204 No Content

---

## POST /api/loans/transcribe-checkout-card
Transcribes a photo of a library checkout card using Grok AI to extract book information.

**Authentication:** Authenticated users (`isAuthenticated()`)

**Request:** Multipart form data with photo file
- Parameter name: `photo`
- Accepted types: image/jpeg, image/png, image/gif, etc.
- Maximum size: 50MB (configured in application.properties)

**Response:** CheckoutCardTranscriptionDto
```json
{
  "title": "The Pushcart War",
  "author": "Jean Merrill",
  "call_number": "PZ 7 .M5453 5",
  "last_date": "1-17-26",
  "last_issued_to": "John",
  "last_due": "1-31-26"
}
```

**Error Responses:**
- 400: Empty file or non-image file
- 401: User not authenticated
- 500: Transcription failed (xAI API error, no API key configured, invalid JSON response, etc.)

**Notes:**
- Requires user to have xAI API key configured in User Settings
- Uses Grok-4 vision model for image analysis
- Fields may contain "N/A" if information is missing or unreadable from photo
- Response uses snake_case JSON field names (call_number, last_date, last_issued_to, last_due)

---

**Related:** LoanController.java, LoanService.java, LoanDto.java, CheckoutCardTranscriptionService.java, CheckoutCardTranscriptionDto.java, AskGrok.java
