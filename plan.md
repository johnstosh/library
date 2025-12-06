# Plan: Add LOC Lookup Button to Books Table

## Overview
Add a "Lookup" button in the Actions column of the books table (books-table.js) that performs LOC (Library of Congress) call number lookup for individual books, similar to the functionality in the LOC Bulk Lookup page.

## Current State Analysis

### Existing LOC Lookup Feature (loc-bulk-lookup.js)
- **Function**: `lookupSingleBook(bookId)` (lines 208-355)
- **Backend API**: `POST /api/loc-bulk-lookup/lookup/{bookId}`
- **Authentication**: Requires `LIBRARIAN` authority
- **Behavior**:
  1. Shows spinner in actions cell during lookup
  2. Calls API endpoint to lookup LOC number via ISBN
  3. Updates LOC number cell with formatted result
  4. Restores action buttons after completion
  5. Shows success/error messages
  6. Uses `window.formatLocForSpine()` for LOC number display

### Current Books Table (books-table.js)
- Displays books with: Photo, Title/Author, LOC Number, Loans, Actions
- Actions column includes: View (eye icon), Edit (pencil icon), Delete (trash icon)
- Edit and Delete buttons are librarian-only
- Uses `window.loadBooksWithCache()` for data loading
- Book data includes: `id`, `title`, `author`, `locNumber`, `firstPhotoId`, `firstPhotoChecksum`, `status`, `loanCount`

## Implementation Plan

### 1. Frontend Changes (books-table.js)

#### 1.1 Add Lookup Button to Actions Column
- **Location**: `loadBooks()` function, actions cell creation (around line 70-100)
- **Changes**:
  - Add "Lookup" button before the View/Edit/Delete icons
  - Style: `btn btn-sm btn-primary me-2` (consistent with loc-bulk-lookup.js)
  - Text: "Lookup"
  - Only visible for librarians (wrapped in `if (window.isLibrarian)` check)
  - Add `data-test="lookup-book-btn"` attribute for testing
  - onclick handler: `() => lookupSingleBookFromTable(book.id)`

#### 1.2 Implement lookupSingleBookFromTable() Function
- **Pattern**: Follow `lookupSingleBook()` from loc-bulk-lookup.js (lines 208-355)
- **Important**: Duplicate the code rather than creating helper functions
  - The LOC Lookup page will be deleted later, so code duplication is acceptable
  - Inline all button restoration logic within the function
- **Key differences**:
  - Use 'books' section for error/success messages instead of 'loc-lookup'
  - Update the row in the books table (not loc-lookup table)
  - Preserve the books table structure (different from loc-lookup table)

- **Implementation steps**:
  1. Clear existing error/success messages for 'books' section
  2. Find the row by `data-entity-id` attribute (books table uses this, not `data-book-id`)
  3. Replace actions cell content with spinner: `<span class="spinner-border spinner-border-sm"></span> Looking up...`
  4. Call API: `POST /api/loc-bulk-lookup/lookup/${bookId}`
  5. On success:
     - Update LOC number cell with formatted result using `window.formatLocForSpine()`
     - Apply styling: `text-success fw-bold` class
     - Show success message
  6. On failure:
     - Show error message with failure reason
  7. Always restore action buttons (Lookup, View, Edit, Delete) after completion
     - **Duplicate button restoration code** in success, failure, and error paths
     - Do NOT create a helper function
  8. Handle errors gracefully and restore buttons

### 2. Error/Success Message Display

#### 2.1 Use Existing Books Section Messages
- Books section already has error/success divs (check index.html for confirmation)
- Use existing `showError('books', message)` and `showSuccess('books', message)` functions
- These functions may be in utils.js or defined globally

### 3. Testing Requirements

**IMPORTANT: UI Tests (Playwright) should NOT be run at this time.**

#### 3.1 Manual Testing Checklist
- [ ] Lookup button appears only for librarians in books table
- [ ] Lookup button triggers API call when clicked
- [ ] Spinner appears in actions cell during lookup
- [ ] LOC number cell updates on successful lookup
- [ ] Action buttons restore after lookup completes
- [ ] Error message displays if lookup fails
- [ ] Success message displays if lookup succeeds
- [ ] Works for books with existing LOC numbers (re-lookup)
- [ ] Works for books without LOC numbers

#### 3.2 UI Test Updates (Playwright) - DEFERRED
- **Status**: DO NOT RUN UI TESTS
- **Future test file**: `src/test/java/com/muczynski/library/ui/BookUITest.java` (or similar)
- **Future test**: `testLocLookupFromBooksTable()`
- **Note**: UI test can be added later when UI testing is enabled

#### 3.3 Integration Test Updates
- **File**: `src/test/java/com/muczynski/library/controller/LocBulkLookupControllerTest.java`
- **Note**: API endpoint already exists and should already have tests
- **Verification**: Confirm `/api/loc-bulk-lookup/lookup/{bookId}` is tested
- If not tested, add integration test for single book lookup

### 4. Documentation Updates

#### 4.1 CLAUDE.md
- Update "Frontend Architecture" → "Feature Modules" → "js/books-table.js" description
- Add note: "Book listing with LOC lookup functionality"

#### 4.2 lessons-learned.md (if applicable)
- Add note about intentional code duplication between books-table.js and loc-bulk-lookup.js
- Document that LOC Lookup page will be deprecated, so duplication is acceptable
- Document the pattern for adding action buttons to tables

## Dependencies & Risks

### Dependencies
- Existing API endpoint: `/api/loc-bulk-lookup/lookup/{bookId}` (already implemented)
- Existing utility: `window.formatLocForSpine()` (in utils.js)
- Existing authentication: Librarian authority check (already in place)
- Existing error/success message infrastructure for 'books' section

### Risks
- **Code duplication**: `lookupSingleBook()` logic is duplicated between two files
  - **Accepted**: LOC Lookup page will be deleted later, so duplication is intentional and acceptable
  - Do NOT create helper functions or extract to utils.js
- **Performance**: Multiple rapid clicks could trigger multiple API calls
  - Mitigation: Disable button during lookup (add to implementation)
- **Row updates**: Books table row structure must match expectations
  - Mitigation: Use existing selectors and test thoroughly

## Files to Modify

### Primary Changes
1. **src/main/resources/static/js/books-table.js**
   - Add Lookup button to actions column
   - Implement `lookupSingleBookFromTable()` function (with duplicated button restoration logic)
   - Export functions globally if needed

### Testing Changes (DEFERRED)
2. **src/test/java/com/muczynski/library/ui/BookUITest.java**
   - UI tests should NOT be run at this time
   - Future: Add UI test for LOC lookup from books table

### Documentation Changes
3. **CLAUDE.md**
   - Update books-table.js description in Frontend Architecture section

## Implementation Order

1. **Step 1**: Modify books-table.js
   - Add Lookup button to loadBooks() function
   - Implement lookupSingleBookFromTable() function with inlined button restoration logic
   - Test manually in browser

2. **Step 2**: Update documentation
   - Update CLAUDE.md with new functionality

3. **Step 3**: Verification
   - Run manual testing checklist
   - **DO NOT RUN UI TESTS** (Playwright tests are deferred)
   - Push to main branch

## Success Criteria

- ✅ Lookup button appears in books table Actions column for librarians only
- ✅ Clicking Lookup button triggers LOC lookup API call
- ✅ LOC number updates in table after successful lookup
- ✅ Appropriate success/error messages display
- ✅ Action buttons restore after lookup completes
- ✅ Code follows existing patterns from loc-bulk-lookup.js (with intentional duplication)
- ✅ Documentation updated
- ✅ Manual testing checklist completed
- ✅ Changes pushed to main branch
- ⚠️ UI tests NOT run (deferred)

## Estimated Complexity
**Medium** - Straightforward implementation following existing patterns, but requires careful attention to:
- Table row selector differences
- Error/success message section naming
- Button restoration logic
- Testing in multiple scenarios
