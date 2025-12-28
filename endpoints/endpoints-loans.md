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

**Related:** LoanController.java, LoanService.java, LoanDto.java
