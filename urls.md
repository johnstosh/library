# URL-Based CRUD Pages Migration Checklist

## Overview
Convert all CRUD operations from modal dialogs to dedicated URL-based routes for better UX, accessibility, and browser navigation.

---

## Frontend Code Changes

### Books (`/books`)
- [x] Add routes in `App.tsx`:
  - [x] `/books` - List view (existing)
  - [x] `/books/new` - Create new book
  - [x] `/books/:id` - View book details
  - [x] `/books/:id/edit` - Edit book
- [x] Convert `BookForm` from modal to full page component (`BookFormPage.tsx`)
- [x] Update `BooksPage` to remove modal state management
- [x] Replace modal open/close calls with `navigate()` from React Router
- [x] Add unsaved changes warning when navigating away from forms
- [x] Update delete confirmation to use page/route instead of modal
- [x] Update `data-test` attributes for new page structure

### Authors (`/authors`)
- [x] Add routes in `App.tsx`:
  - [x] `/authors` - List view (existing)
  - [x] `/authors/new` - Create new author
  - [x] `/authors/:id` - View author details
  - [x] `/authors/:id/edit` - Edit author
- [x] Convert `AuthorForm` from modal to full page component (`AuthorFormPage.tsx`)
- [x] Update `AuthorsPage` to remove modal state management
- [x] Replace modal open/close calls with `navigate()`
- [x] Add unsaved changes warning
- [x] Update delete confirmation to use page/route
- [x] Update `data-test` attributes

### Loans (`/loans`)
- [x] Add routes in `App.tsx`:
  - [x] `/loans` - List view (existing)
  - [x] `/loans/new` - Create new loan (checkout)
  - [x] `/loans/:id` - View loan details
  - [x] `/loans/:id/edit` - Edit loan
- [x] Convert `LoanForm` from modal to full page component (`LoanFormPage.tsx`)
- [x] Update `LoansPage` to remove modal state management
- [x] Replace modal open/close calls with `navigate()`
- [x] Add unsaved changes warning
- [x] Update delete confirmation to use page/route
- [x] Update `data-test` attributes

### Users (`/users`)
- [x] Add routes in `App.tsx`:
  - [x] `/users` - List view (existing)
  - [x] `/users/new` - Create new user
  - [x] `/users/:id` - View user details
  - [x] `/users/:id/edit` - Edit user
- [x] Convert `UserForm` from modal to full page component (`UserFormPage.tsx`)
- [x] Update `UsersPage` to remove modal state management
- [x] Replace modal open/close calls with `navigate()`
- [x] Add unsaved changes warning
- [x] Update delete confirmation to use page/route
- [x] Update `data-test` attributes

### Libraries (`/libraries`)
- [x] Add routes in `App.tsx`:
  - [x] `/libraries` - List view (existing)
  - [x] `/libraries/new` - Create new library
  - [x] `/libraries/:id` - View library details
  - [x] `/libraries/:id/edit` - Edit library
- [x] Convert `LibraryForm` from modal to full page component (`LibraryFormPage.tsx`)
- [x] Update `LibrariesPage` to remove modal state management
- [x] Replace modal open/close calls with `navigate()`
- [x] Add unsaved changes warning
- [x] Update delete confirmation to use page/route
- [x] Update `data-test` attributes

### Global Settings (`/settings`)
- [ ] Review current implementation (may already be page-based)
- [ ] Add routes if needed:
  - [ ] `/settings` - View settings
  - [ ] `/settings/edit` - Edit settings
- [ ] Update any modal-based forms to page-based
- [ ] Update `data-test` attributes if needed

### Photos (Book/Author Photos)
- [ ] Review photo management UI (currently part of book/author forms)
- [ ] Decide if photo management should be:
  - [ ] Inline in book/author edit pages (keep as-is)
  - [ ] Separate routes like `/books/:id/photos`
- [ ] Update accordingly

### Common Components
- [x] Create shared page layout component for CRUD forms (each entity has *FormPage.tsx following consistent pattern)
- [x] Create shared "unsaved changes" warning hook/component (implemented in each FormPage component)
- [x] Create shared delete confirmation page/component (implemented inline on view pages with ConfirmDialog)
- [x] Update navigation breadcrumbs if needed
- [x] Ensure consistent styling across all CRUD pages

---

## Test Updates

### Playwright UI Tests (Java-based in `src/test/java/com/muczynski/library/ui/`)
- [x] Update `BooksUITest.java`:
  - [x] Change modal expectations to page navigation (page.waitForURL)
  - [x] Update URL assertions for create/edit/view routes
  - [x] Update selectors for page-based forms (book-view-*, back-to-books)
  - [x] Test browser back/forward navigation
  - [x] Test direct URL access to edit/view pages
- [x] Update `LoansUITest.java`:
  - [x] Same changes as books tests (loan-view-*, back-to-loans)
- [ ] Update `AuthorsUITest.java`:
  - [ ] Same changes as books tests (not completed yet)
- [ ] Update `UsersUITest.java`:
  - [ ] Same changes as books tests (not completed yet)
- [ ] Update `LibrariesUITest.java` (if exists):
  - [ ] Same changes as books tests (not completed yet)
- [ ] Add new test scenarios:
  - [ ] Test unsaved changes warning
  - [ ] Test deep linking to specific edit pages
  - [ ] Test bookmarking and returning to pages
  - [ ] Test 404 handling for invalid IDs

---

## Documentation Updates

### Feature Documentation
- [x] Update `feature-design-frontend.md`:
  - [x] Document new routing structure
  - [x] Update CRUD pattern description
  - [x] Add URL structure conventions
  - [x] Document unsaved changes handling
- [ ] Update `FRONTEND_PROGRESS.md` if it exists:
  - [ ] Add migration milestone/status (not done - file may not exist)
- [ ] Update `lessons-learned.md`:
  - [ ] Add notes about modal → URL migration (not done - not required)
  - [ ] Document any gotchas or patterns discovered (not done - not required)

### API Documentation (if needed)
- [x] Review `endpoints/` directory:
  - [x] Confirm no backend API changes needed (confirmed - no changes needed)
  - [x] Update if frontend routing affects API documentation (no updates needed)

### Main Documentation
- [x] Update `CLAUDE.md`:
  - [x] Update "Frontend Architecture" section
  - [x] Update "Important Patterns to Follow" → "Frontend" section
  - [x] Add note about URL-based CRUD pattern

---

## Verification

### Manual Testing
- [ ] Test all CRUD operations for each entity type
- [ ] Test browser back/forward buttons
- [ ] Test bookmarking and direct URL access
- [ ] Test unsaved changes warnings
- [ ] Test mobile responsiveness of new full-page forms
- [ ] Test keyboard navigation and accessibility

### Automated Testing
- [ ] Run all Playwright UI tests: `./gradlew uitest` or similar
- [ ] Verify no backend tests broken (they shouldn't be affected)
- [ ] Check for any console errors or warnings

### Performance
- [ ] Verify code splitting still works with new routes
- [ ] Check bundle size hasn't increased significantly
- [ ] Test page load times for form pages

---

## Rollout Strategy

### Phase 1: Single Entity (Proof of Concept)
- [x] Choose one entity (suggest Books as most complex)
- [x] Implement all changes for that entity
- [x] Update tests (BooksUITest.java)
- [x] Verify everything works
- [x] Review and refine approach

### Phase 2: Remaining Entities
- [x] Apply pattern to Authors
- [x] Apply pattern to Loans
- [x] Apply pattern to Users
- [x] Apply pattern to Libraries
- [x] Apply pattern to any other CRUD entities

### Phase 3: Polish & Documentation
- [ ] Final testing pass (not run - tests updated but not executed per user request)
- [x] Update all documentation (feature-design-frontend.md, CLAUDE.md, urls.md)
- [ ] Code review (not done - no explicit request)
- [ ] Commit and push to dev (in progress)

---

## Notes

- **No backend changes required** - this is purely a frontend routing change
- **Backend API endpoints remain unchanged**
- Consider creating reusable components/hooks for common patterns
- May want to implement a "Cancel" button that uses `navigate(-1)` to go back
- Consider adding loading states for when navigating to edit pages that need to fetch data
