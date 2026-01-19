# Table Column Width Standardization Checklist

## Objective
Convert all tables from content-based column widths to percentage-based widths with text cutoff (ellipsis) instead of wrapping.

## Code Changes

### Core Component Changes

- [ ] **DataTable.tsx** - Update shared DataTable component
  - [ ] Modify column interface to accept width percentage
  - [ ] Add table layout fixed CSS
  - [ ] Add text overflow/ellipsis handling for cells
  - [ ] Update header cells to use percentage widths
  - [ ] Update body cells to use percentage widths with overflow
  - File: `frontend/src/components/table/DataTable.tsx`

### Tables Using DataTable Component (Column Config Updates)

- [ ] **AuthorTable.tsx** - Define column width percentages
  - [ ] Name column width
  - [ ] Religious Affiliation column width
  - [ ] Biography column width
  - [ ] Book Count column width
  - [ ] Actions column width
  - File: `frontend/src/pages/authors/components/AuthorTable.tsx`

- [ ] **BookTable.tsx** - Define column width percentages
  - [ ] Title column width
  - [ ] Author column width
  - [ ] LOC Number column width
  - [ ] Status column width
  - [ ] Actions column width
  - File: `frontend/src/pages/books/components/BookTable.tsx`

- [ ] **UserTable.tsx** - Define column width percentages
  - [ ] Username column width
  - [ ] Authority column width
  - [ ] Active Loans column width
  - [ ] ID column width
  - [ ] Actions column width
  - File: `frontend/src/pages/users/components/UserTable.tsx`

- [ ] **LibrariesPage.tsx** - Define column width percentages
  - [ ] Name column width
  - [ ] Hostname column width
  - [ ] Books Count column width
  - [ ] Active Loans column width
  - [ ] Actions column width
  - File: `frontend/src/pages/libraries/LibrariesPage.tsx`

- [ ] **LoansPage.tsx** - Define column width percentages
  - [ ] Book Title column width
  - [ ] Checkout Date column width
  - [ ] Due Date column width
  - [ ] Return Date column width
  - [ ] Status column width
  - [ ] Actions column width
  - File: `frontend/src/pages/loans/LoansPage.tsx`

- [ ] **ApplicationsPage.tsx** - Define column width percentages
  - [ ] ID column width
  - [ ] Applicant Name column width
  - [ ] Actions column width
  - File: `frontend/src/pages/library-cards/ApplicationsPage.tsx`

### Custom Table Implementations (Full Rewrite)

- [ ] **SavedBooksTable.tsx** - Apply percentage widths and text cutoff
  - [ ] Add table-layout-fixed class
  - [ ] Define width for Title column
  - [ ] Define width for Author column
  - [ ] Define width for Library column
  - [ ] Define width for Photos column
  - [ ] Define width for Status column
  - [ ] Define width for Actions column
  - [ ] Add overflow/ellipsis to all cells
  - File: `frontend/src/pages/books-from-feed/components/SavedBooksTable.tsx`

- [ ] **LocLookupResultsModal.tsx** - Apply percentage widths and text cutoff
  - [ ] Add table-layout-fixed class
  - [ ] Define width for Status icon column
  - [ ] Define width for Book Title column
  - [ ] Define width for LOC Call Number column
  - [ ] Define width for Message column
  - [ ] Add overflow/ellipsis to all cells
  - File: `frontend/src/pages/books/components/LocLookupResultsModal.tsx`

## Test Changes

### UI Tests (Playwright)

- [ ] **authors.spec.ts** - Verify author table displays correctly
  - [ ] Test that author names are visible (even if truncated)
  - [ ] Test that table columns don't wrap
  - File: `frontend/playwright/tests/authors.spec.ts`

- [ ] **books.spec.ts** - Verify book table displays correctly
  - [ ] Test that book titles are visible (even if truncated)
  - [ ] Test that table columns don't wrap
  - [ ] Test LOC lookup results modal table
  - File: `frontend/playwright/tests/books.spec.ts`

- [ ] **users.spec.ts** - Verify user table displays correctly
  - [ ] Test that usernames are visible (even if truncated)
  - [ ] Test that table columns don't wrap
  - File: `frontend/playwright/tests/users.spec.ts`

- [ ] **libraries.spec.ts** - Verify library table displays correctly
  - [ ] Test that library names are visible (even if truncated)
  - [ ] Test that table columns don't wrap
  - File: `frontend/playwright/tests/libraries.spec.ts`

- [ ] **loans.spec.ts** - Verify loans table displays correctly
  - [ ] Test that book titles in loans are visible (even if truncated)
  - [ ] Test that table columns don't wrap
  - File: `frontend/playwright/tests/loans.spec.ts`

- [ ] **library-card-applications.spec.ts** - Verify applications table displays correctly
  - [ ] Test that applicant names are visible (even if truncated)
  - [ ] Test that table columns don't wrap
  - File: `frontend/playwright/tests/library-card-applications.spec.ts`

- [ ] **books-from-feed.spec.ts** - Verify saved books table displays correctly
  - [ ] Test that saved book titles are visible (even if truncated)
  - [ ] Test that table columns don't wrap
  - File: `frontend/playwright/tests/books-from-feed.spec.ts`

### Manual Visual Testing

- [ ] **Visual inspection** - Check all tables in running app
  - [ ] Authors page table layout
  - [ ] Books page table layout
  - [ ] Users page table layout
  - [ ] Libraries page table layout
  - [ ] Loans page table layout
  - [ ] Library card applications page table layout
  - [ ] Books from feed page (saved books table)
  - [ ] LOC lookup results modal table
  - [ ] Test with long content (titles, names, etc.)
  - [ ] Test with short content
  - [ ] Verify ellipsis appears on truncated content
  - [ ] Verify no text wrapping occurs

## Documentation Changes

- [ ] **feature-design-frontend.md** - Document table styling pattern
  - [ ] Add section on table column width standards
  - [ ] Document percentage-based width approach
  - [ ] Document text truncation pattern
  - File: `feature-design-frontend.md`

- [ ] **lessons-learned.md** - Add lesson about table layouts
  - [ ] Document why percentage widths are preferred
  - [ ] Document text-overflow pattern
  - File: `lessons-learned.md` (if appropriate)

## Summary

**Total items: 71**
- Code changes: 9 files
- Test changes: 7 Playwright test files + manual testing
- Documentation: 2 files

## Notes

- Start with DataTable.tsx as it affects 6 of the 9 tables
- Percentage allocations should consider:
  - Checkbox column (if present): ~5%
  - Actions column: ~15-20%
  - Content columns: distribute remaining 75-80%
  - Longer text fields (title, name, biography) should get more space
  - Short fields (count, status, date) should get less space
- Use Tailwind classes: `table-fixed`, `truncate`, `overflow-hidden`
- Ensure all changes maintain `data-test` attributes for Playwright
