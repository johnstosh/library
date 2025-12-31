# Grokipedia URL Integration Checklist

## Overview
Add a `grokipediaUrl` field to both Author and Book entities to store links to grokipedia (reference encyclopedia).

## Backend Changes

### Database Schema & Entities
- [x] Add `grokipediaUrl` column to `Author` entity (String, nullable)
- [x] Add `grokipediaUrl` column to `Book` entity (String, nullable)

### DTOs
- [x] Add `grokipediaUrl` field to `AuthorDto`
- [x] Add `grokipediaUrl` field to `BookDto`
- [x] Add `grokipediaUrl` field to `RandomAuthor` (test data entity)
- [x] Add `grokipediaUrl` field to `RandomBook` (test data entity)

### Mappers
- [x] Verify `AuthorMapper` includes `grokipediaUrl` mapping (MapStruct should auto-handle)
- [x] Verify `BookMapper` includes `grokipediaUrl` mapping (MapStruct should auto-handle)

### Services
- [x] Update `AuthorService` create/update operations to handle `grokipediaUrl`
- [x] Update `BookService` create/update operations to handle `grokipediaUrl`
- [x] Update test data generation services to populate `grokipediaUrl` with sample data

### Import/Export
- [x] Verify JSON export includes `grokipediaUrl` for Authors
- [x] Verify JSON export includes `grokipediaUrl` for Books
- [x] Verify JSON import handles `grokipediaUrl` for Authors
- [x] Verify JSON import handles `grokipediaUrl` for Books

## Frontend Changes

### TypeScript Types
- [x] Add `grokipediaUrl?: string` to `Author` interface in `types/dtos.ts`
- [x] Add `grokipediaUrl?: string` to `Book` interface in `types/dtos.ts`

### Author Pages
- [x] Add `grokipediaUrl` input field to `AuthorFormPage.tsx` (used by New and Edit)
- [x] Display `grokipediaUrl` as clickable link in `AuthorViewPage.tsx`
- [ ] Add `grokipediaUrl` column to authors table (optional - may be too wide)

### Book Pages
- [x] Add `grokipediaUrl` input field to `BookFormPage.tsx` (used by New and Edit)
- [x] Display `grokipediaUrl` as clickable link in `BookViewPage.tsx`
- [ ] Add `grokipediaUrl` column to books table (optional - may be too wide)

## Testing

### Backend Unit Tests
- [x] Update `AuthorControllerTest` to test `grokipediaUrl` in create/update
- [x] Update `BookControllerTest` to test `grokipediaUrl` in create/update
- [x] Update `AuthorServiceTest` (if exists) to test `grokipediaUrl` handling
- [x] Update `BookServiceTest` (if exists) to test `grokipediaUrl` handling

### Backend Integration Tests
- [ ] Test Author API endpoints with `grokipediaUrl` field
- [ ] Test Book API endpoints with `grokipediaUrl` field
- [ ] Test import/export with `grokipediaUrl` field

### Frontend UI Tests (Playwright)
- [ ] Update `authors.spec.ts` to test `grokipediaUrl` input in create/edit
- [ ] Update `authors.spec.ts` to verify `grokipediaUrl` display in view page
- [ ] Update `books.spec.ts` to test `grokipediaUrl` input in create/edit
- [ ] Update `books.spec.ts` to verify `grokipediaUrl` display in view page

## Documentation
- [ ] Update `feature-design-frontend.md` if CRUD patterns are affected
- [ ] Update `endpoints/` documentation for Author and Book endpoints
- [ ] Update `backend-requirements.md` or `backend-development-requirements.md` if needed
- [x] Add copyright headers to any new files created

## Verification
- [x] Run backend tests: `./gradlew test`
- [x] Run frontend build: `cd frontend && npm run build`
- [ ] Run Playwright tests: `cd frontend && npm run test:ui`
- [x] Manual testing:
  - [x] Create new Author with grokipedia URL
  - [x] Edit existing Author to add grokipedia URL
  - [x] Verify URL displays as clickable link in Author view
  - [x] Create new Book with grokipedia URL
  - [x] Edit existing Book to add grokipedia URL
  - [x] Verify URL displays as clickable link in Book view
  - [ ] Export database to JSON and verify `grokipediaUrl` is included
  - [ ] Import JSON and verify `grokipediaUrl` is restored

## Git Workflow
- [ ] Ensure on `dev` branch: `git checkout dev`
- [ ] Pull latest changes: `git pull`
- [ ] Commit all changes with descriptive message
- [ ] Push to dev: `git push origin dev`

## Notes
- Field is optional (nullable) - not all books/authors may have grokipedia entries
- URLs should be displayed as clickable links in view pages
- Input validation: Basic URL format validation in frontend (optional)
- MapStruct should automatically handle the new fields in existing mappers
