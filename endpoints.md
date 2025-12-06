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

**Related:** SecurityConfig.java, CustomUserDetailsService.java, Photo.java (@Lob fields), ImportService.java
