# Test Data Management Endpoints

**Design Decision:** All test data endpoints use `@PreAuthorize("permitAll()")` to allow unauthenticated access. This is intentional for development convenience and testing purposes. The feature should be disabled in production by setting `app.show-test-data-page=false`.

## POST /api/test-data/generate
Generates random test books and authors.

**Authentication:** Public (permitAll)

**Request Body:**
```json
{
  "numBooks": 10
}
```

**Response:** TestDataResponseDto
```json
{
  "success": true,
  "message": "Test data generated successfully for 10 books"
}
```

**Error Responses:**
- 500: Internal server error with TestDataResponseDto containing error message

**Use Case:**
- Development and testing environments
- Generates random books with associated authors
- Creates library if none exists
- Books marked with `publisher="test-data"` for easy identification
- Authors marked with `religiousAffiliation="test-data"` for easy identification

---

## POST /api/test-data/generate-loans
Generates random loan records for existing books and users.

**Authentication:** Public (permitAll)

**Request Body:**
```json
{
  "numLoans": 5
}
```

**Response:** TestDataResponseDto
```json
{
  "success": true,
  "message": "Test data generated successfully for 5 loans"
}
```

**Error Responses:**
- 500: Internal server error with TestDataResponseDto containing error message

**Behavior:**
- Requires existing books and users in database
- Creates loans with `loanDate=2099-01-01` for easy identification
- Randomly assigns books to users

---

## POST /api/test-data/generate-users
Generates random test user accounts.

**Authentication:** Public (permitAll)

**Request Body:**
```json
{
  "numUsers": 5
}
```

**Response:** TestDataResponseDto
```json
{
  "success": true,
  "message": "Test data generated successfully for 5 users"
}
```

**Error Responses:**
- 500: Internal server error with TestDataResponseDto containing error message

**Behavior:**
- Creates users with usernames starting with `test-data-` for easy identification
- 20% chance of assigning LIBRARIAN authority, 80% USER authority
- Sets default password (SHA-256 hash of "password123")
- Assigns default library card design
- Marks as local (non-SSO) users

---

## DELETE /api/test-data/delete-all
Deletes all generated test data (books, authors, loans).

**Authentication:** Public (permitAll)

**Response:** 204 No Content

**Behavior:**
- Deletes loans with `loanDate=2099-01-01`
- Deletes books with `publisher="test-data"`
- Deletes authors with `religiousAffiliation="test-data"`
- Deletes users with usernames starting with `test-data-`
- Handles photo cleanup for deleted authors

**Warning:** Does not delete libraries.

---

## DELETE /api/test-data/total-purge
⚠️ **DANGEROUS:** Drops all database tables and destroys all data.

**Authentication:** Public (permitAll)

**Response:** 200 OK (no body)

**Error Responses:**
- 500: Internal server error

**Behavior:**
- Drops all tables with CASCADE (PostgreSQL)
- **Destroys ALL data** including users, libraries, books, authors, loans, photos, etc.
- Cannot be undone
- Database schema will be recreated on next application startup

**Warning:** This is a destructive operation for development use only!

---

## GET /api/test-data/stats
Returns current counts of books, authors, loans, and users.

**Authentication:** Public (permitAll)

**Response:** TestDataStatsDto
```json
{
  "books": 42,
  "authors": 15,
  "loans": 7,
  "users": 10
}
```

**Use Case:**
- Display current statistics on test data page
- Frontend polls this endpoint every 3 seconds when test data page is active

---

## GET /api/global-properties/test-data-page-visibility
Returns whether the test data page should be shown in navigation.

**Authentication:** Public (permitAll)

**Response:** TestDataPageVisibilityDto
```json
{
  "showTestDataPage": false
}
```

**Behavior:**
- Reads `app.show-test-data-page` property from application.properties
- Defaults to `true` if property not set
- Frontend should use this to conditionally show/hide test data navigation link

**Note:** Now properly enforced by frontend via `useTestDataPageVisibility()` hook.

---

**Related:** TestDataController.java, TestDataService.java, GlobalPropertiesController.java, RandomBook.java, RandomAuthor.java, RandomLoan.java, RandomUser.java
