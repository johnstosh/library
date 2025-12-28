# Code Review: Search Feature

## Overview
Review of the Search feature (`/search` page) including backend API endpoints, frontend UI, and documentation.

**Status:** Search feature is functional but has documentation gaps and architectural issues.

---

## Bugs Found

### Bug #1: SearchController uses Map<String, Object> instead of DTO
**Location:** `SearchController.java:31-34`

**Issue:** The search endpoint returns `Map<String, Object>` instead of a proper DTO.

```java
public ResponseEntity<?> search(@RequestParam String query, @RequestParam int page, @RequestParam int size) {
    try {
        Map<String, Object> results = searchService.search(query, page, size);
        return ResponseEntity.ok(results);
```

**Why it's wrong:**
- Violates CLAUDE.md rule: "Never expose JPA entities directly in controllers - always use DTOs"
- Map<String, Object> provides no type safety
- Makes API contract unclear
- Harder to maintain and document

**Expected:**
Create `SearchResponseDto.java` with proper fields:
```java
public class SearchResponseDto {
    private List<BookDto> books;
    private List<AuthorDto> authors;
    private PageInfoDto bookPage;
    private PageInfoDto authorPage;
}
```

**Related files:**
- `SearchService.java:42` - Also returns Map<String, Object>
- Frontend correctly defines `SearchResponse` interface in `frontend/src/api/search.ts:6-21`

---

### Bug #2: SearchService returns Map<String, Object> instead of DTO
**Location:** `SearchService.java:42`

**Issue:** Service layer returns untyped Map instead of DTO.

```java
public Map<String, Object> search(String query, int page, int size) {
    // ...
    Map<String, Object> results = new HashMap<>();
    results.put("books", books);
    results.put("authors", authors);
```

**Why it's wrong:**
- Service layer should work with domain types and DTOs, not Maps
- No compile-time type checking
- Error-prone (typos in map keys won't be caught)
- Inconsistent with other services (BookService, AuthorService use DTOs)

**Expected:**
Return `SearchResponseDto` with properly typed fields.

---

### Bug #3: SearchController uses ResponseEntity<?> wildcard
**Location:** `SearchController.java:31`

**Issue:** Return type uses wildcard instead of specific DTO type.

```java
public ResponseEntity<?> search(@RequestParam String query, @RequestParam int page, @RequestParam int size) {
```

**Why it's wrong:**
- No type safety at compile time
- API contract is unclear to consumers
- Makes automated API documentation generation harder
- Inconsistent with best practices

**Expected:**
```java
public ResponseEntity<SearchResponseDto> search(@RequestParam String query, @RequestParam int page, @RequestParam int size) {
```

**Note:** This is a systemic issue across multiple controllers:
- `AppliedController` - all 6 methods use `ResponseEntity<?>`
- `AuthorController` - 16 out of 18 methods use `ResponseEntity<?>`
- `GooglePhotosDiagnosticController` - all 5 methods use `ResponseEntity<?>`

---

### Bug #4: Missing endpoint documentation
**Location:** `endpoints.md`

**Issue:** The `/api/search` endpoint is not documented in `endpoints.md`.

**Expected:** Add documentation following the pattern in `endpoints.md`:

```markdown
### GET /api/search
Returns search results for books and authors matching the query.

**Authentication:** Public (permitAll)

**Query Parameters:**
- `query` (string, required) - Search term to match against book titles and author names
- `page` (int, required) - Zero-based page number
- `size` (int, required) - Number of results per page

**Response:** SearchResponseDto containing books and authors with pagination info

**Use Case:**
- Public search across library catalog
- Search by book title or author name
- Paginated results for both books and authors
```

---

### Bug #5: Missing feature documentation
**Location:** Root directory

**Issue:** No `feature-design-search.md` file exists to document the Search feature.

**Why it's wrong:**
- All other major features have dedicated documentation files:
  - `feature-design-security.md`
  - `feature-design-frontend.md`
  - `feature-design-photos.md`
  - `feature-design-loc.md`
  - `feature-design-libraries.md`
  - `feature-design-library-cards.md`
- CLAUDE.md references these files but Search is missing

**Expected:**
Create `feature-design-search.md` documenting:
- Search algorithm (case-insensitive partial match)
- Fields searched (book title, author name)
- Pagination strategy
- Public accessibility (no auth required)
- Frontend caching strategy

---

### Bug #6: CLAUDE.md doesn't mention Search feature
**Location:** `CLAUDE.md`

**Issue:** The Search feature is not mentioned in CLAUDE.md under "Major Features" or anywhere else.

**Expected:**
Add Search to the "Major Features" section:

```markdown
### Search
See `feature-design-search.md` for complete details.
- Global search across books and authors
- Case-insensitive partial matching on title and name
- Paginated results (20 per page default)
- Public access (no authentication required)
```

---

### Bug #7: Checklist documentation mismatch
**Location:** `checklist-code-review.md:151-154`

**Issue:** Checklist claims search includes publisher, but code doesn't implement it.

**Checklist says:**
```markdown
- [ ] **Search** (`/search`)
  - Global search across books and authors
  - Search by title, author, publisher  ← This is not implemented
  - Search filters
```

**Reality:**
- `BookRepository.findByTitleContainingIgnoreCase()` - only searches title
- `AuthorRepository.findByNameContainingIgnoreCase()` - only searches name
- No publisher search implemented
- No search filters implemented

**Expected:**
Either:
1. Update checklist to match implementation (remove "publisher" and "Search filters")
2. Or implement publisher search and filters as documented

---

### Bug #8: No SearchServiceTest
**Location:** `src/test/java/com/muczynski/library/service/`

**Issue:** No unit tests for `SearchService`.

**Why it's wrong:**
- `SearchControllerTest.java` exists with 7 tests, but it mocks the service
- Service logic is not tested in isolation
- Other services have dedicated test classes (BookService, AuthorService, etc.)

**Expected:**
Create `SearchServiceTest.java` with tests for:
- Search with results found
- Search with no results
- Empty query handling
- Pagination behavior
- Case-insensitive matching

---

### Bug #9: No Playwright UI tests
**Location:** UI test directory

**Issue:** No end-to-end tests for the Search page.

**Why it's wrong:**
- Other major features should have Playwright tests per `uitest-requirements.md`
- Search page has `data-test` attributes but no tests using them
- Critical user flow is untested

**Expected:**
Create Playwright tests for:
- Search form submission
- Results display for books
- Results display for authors
- No results state
- Pagination controls
- Clear search functionality

---

### Bug #10: No PageInfoDto or PageDto class
**Location:** `src/main/java/com/muczynski/library/dto/`

**Issue:** SearchService manually creates pagination info as `Map<String, Object>` instead of using a reusable DTO.

```java
Map<String, Object> bookPageInfo = new HashMap<>();
bookPageInfo.put("totalPages", bookPage.getTotalPages());
bookPageInfo.put("totalElements", bookPage.getTotalElements());
bookPageInfo.put("currentPage", bookPage.getNumber());
bookPageInfo.put("pageSize", bookPage.getSize());
```

**Why it's wrong:**
- Duplicated code (same structure for bookPage and authorPage)
- No type safety
- Should be a reusable DTO class

**Expected:**
Create `PageInfoDto.java`:
```java
public class PageInfoDto {
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;
}
```

Then use it in `SearchResponseDto`.

---

## Similar Bugs in Other Features

### Systemic Issue: ResponseEntity<?> wildcard usage
**Affected controllers:**
- `SearchController` - 1 method
- `AppliedController` - 6 methods (despite being marked as "fixed" in previous review)
- `AuthorController` - 16 methods
- `GooglePhotosDiagnosticController` - 5 methods

**Recommendation:**
Consider adding a coding standard to prohibit `ResponseEntity<?>` and require specific DTO types.

### Systemic Issue: Map<String, Object> in service layer
**Affected services:**
- `SearchService` - returns Map for search results
- `PhotoExportService` - returns Map for stats and photo info
- `GooglePhotosService` - uses Map for external API interactions (acceptable for third-party APIs)

**Recommendation:**
Replace Map returns with proper DTOs in `SearchService` and `PhotoExportService`.
Exception: External API interactions can use Map if they're internal implementation details.

---

## Summary

**Total Bugs Found:** 10

**Categories:**
- Architecture/Design: 3 (Map usage, ResponseEntity wildcard, missing DTO)
- Documentation: 4 (endpoints.md, feature docs, CLAUDE.md, checklist mismatch)
- Testing: 2 (missing service tests, missing UI tests)
- Feature completeness: 1 (publisher search not implemented)

**Severity:**
- High: Bugs #1, #2, #3 (architectural issues affecting maintainability)
- Medium: Bugs #4, #5, #6, #8, #9 (documentation and testing gaps)
- Low: Bugs #7, #10 (minor inconsistencies)

---

## Files Reviewed

### Backend
- `src/main/java/com/muczynski/library/controller/SearchController.java`
- `src/main/java/com/muczynski/library/service/SearchService.java`
- `src/main/java/com/muczynski/library/repository/BookRepository.java`
- `src/main/java/com/muczynski/library/repository/AuthorRepository.java`
- `src/test/java/com/muczynski/library/controller/SearchControllerTest.java`
- `src/main/java/com/muczynski/library/config/SecurityConfig.java`

### Frontend
- `frontend/src/pages/search/SearchPage.tsx`
- `frontend/src/api/search.ts`
- `frontend/src/components/layout/Navigation.tsx`
- `frontend/src/App.tsx`

### Documentation
- `CLAUDE.md`
- `endpoints.md`
- `checklist-code-review.md`

---

## Notes

The Search feature is **functionally complete** and working correctly. The bugs are primarily:
1. **Architectural** - should use DTOs instead of Map
2. **Documentation** - missing from key documentation files
3. **Testing** - missing service and UI tests

The frontend implementation is well-structured with proper TypeScript types, TanStack Query integration, and `data-test` attributes for testing.
