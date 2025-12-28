
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Documentation Structure

**This is the main overview file.** Detailed subsystem documentation is in separate files:
- `feature-design-security.md` - Security, authentication, OAuth, authorities
- `feature-design-frontend.md` - Frontend architecture, JavaScript modules, UI patterns
- `feature-design-photos.md` - Photo storage, Google Photos integration, caching
- `feature-design-loc.md` - Library of Congress integration, call number lookup
- `feature-design-import-export.md` - JSON import/export, photo backup
- `feature-design-libraries.md` - Library management, library statistics
- `feature-design-library-cards.md` - Library card applications, PDF generation, card designs
- `backend-requirements.md` - Backend development requirements and patterns
- `backend-development-requirements.md` - Detailed backend guidelines
- `uitest-requirements.md` - UI testing with Playwright
- `backend-lessons-learned.md` - Important backend lessons
- `lessons-learned.md` - General best practices
- `endpoints.md` - API endpoint documentation
- `sso.md` - Google OAuth SSO configuration
- `photos-design.md` - Photo storage design details
- `wipe-instructions.md` - Database reset instructions

## Technology Stack

**Backend:**
- Java 17 with Spring Boot 3.5+
- Spring Data JPA with Hibernate ORM
- PostgreSQL (production) with Cloud SQL / H2 (testing)
- Spring Security with OAuth2 (Google SSO)
- MapStruct 1.5.5 for DTO mapping
- iText 8 for PDF generation (library cards, labels)
- Lombok for boilerplate reduction
- Google Photos API for photo storage
- Marc4J 2.9.5 for Library of Congress lookup
- BYU CallNumber library for LOC sorting

**Frontend:**
- Modern React 18+ SPA with TypeScript (migrated Dec 2024)
- Tailwind CSS v4 for utility-first styling
- TanStack Query v5 for server state management
- React Router v6 for client-side routing
- Zustand for client state
- Vite for fast builds and dev server
- **See `feature-design-frontend.md` for complete architecture**
- **See `FRONTEND_PROGRESS.md` for migration status**

**Testing:**
- JUnit 5 with Spring Boot Test
- Playwright for UI testing
- REST Assured for API testing
- H2 in-memory database for tests

## Common Development Commands

```bash
# Run with H2 database (default for development)
./gradlew bootRun

# Run tests (shows only failures for cleaner output)
./gradlew test

# Run a single test class
./gradlew test --tests "com.muczynski.library.controller.BookControllerTest"

# Clean build
./gradlew clean build

# Run with production profile (PostgreSQL)
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Test Output Configuration
The build.gradle test configuration only shows detailed output for failed tests to reduce noise.

## Architecture Overview

### Domain Model
**Core Entities:**
- `Library` - Library branch information (name, hostname)
- `Book` - Book inventory with title, publication year, publisher, author, library, LOC number, status
- `Author` - Authors with biographical information
- `Loan` - Book checkout tracking
- `User` - Library patron/staff accounts with authority-based access (includes `libraryCardDesign` enum field)
- `Photo` - Book and author photos with Google Photos integration
- `Applied` - Library card application tracking
- `LibraryCardDesign` - Enum with 5 predefined card design options (per-user preference)
- `GlobalSettings` - Application-wide settings (SSO, etc.)
- `RandomBook`, `RandomAuthor`, `RandomLoan` - Test data generation templates

**Key Relationships:**
- `Library` has many `Book`s
- `Author` has many `Book`s
- `Book` has many `Photo`s (cascade delete)
- `Author` has many `Photo`s (cascade delete)
- `Book` has many `Loan`s
- `User` has many `Loan`s

### Layered Architecture

**Controller Layer:**
- REST controllers use `@RestController` with `/api/*` paths
- Use DTOs (never expose entities directly)
- `@PreAuthorize("hasAuthority('LIBRARIAN')")` for librarian-only endpoints
- `@PreAuthorize("isAuthenticated()")` for authenticated endpoints
- Returns `ResponseEntity` with appropriate HTTP status codes

**Service Layer:**
- Transactional business logic
- **MapStruct** for all entity-DTO conversions
  - Always use MapStruct mappers, never manual field copying
  - If mapper doesn't exist, create it following patterns in `mapper/` package
  - Use `@Mapper(componentModel = "spring")` annotation
- Services throw `LibraryException` for errors
- OAuth user lookup: Services must handle both username and SSO subject ID

**Repository Layer:**
- Spring Data JPA repositories
- Custom queries use JPQL with LEFT JOIN FETCH for performance
- Entity relationships managed by JPA

### Frontend Architecture
See `feature-design-frontend.md` for complete details.

**Key Points:**
- React 18 single-page application with React Router
- TypeScript strict mode for type safety
- TanStack Query for server state management and caching
- Tailwind CSS for styling
- Protected routes with role-based access control
- All API calls use TanStack Query hooks
- `data-test` attributes on all interactive elements for Playwright tests
- Consistent CRUD pattern across all features
- Code splitting with lazy-loaded page components (49% bundle reduction)
- Error boundaries for graceful error handling

## Key Configuration

### Database
- **Production**: PostgreSQL via Google Cloud SQL (application-prod.properties)
  - Socket factory: `com.google.cloud.sql.postgres.SocketFactory`
- **Testing**: H2 in-memory database (application-test.properties)
- Schema auto-updated via `spring.jpa.hibernate.ddl-auto=update`

### Jackson / JSON Serialization
- Custom `ObjectMapper` bean in `AppConfig.java`
- All datetime types serialized as ISO-8601 strings
  - Example: `"2025-12-06T14:30:00"` instead of `[2025,12,6,14,30,0]`
- Ensures consistent datetime format for frontend caching
- `JavaTimeModule` registered for Java 8+ date/time support
- `WRITE_DATES_AS_TIMESTAMPS` disabled

### CORS
- Configured to allow specific origins for security
- Credentials allowed for authenticated requests

## Security
See `feature-design-security.md` for complete details.

**Critical Points:**
- **Use authorities, NOT roles**: `hasAuthority('LIBRARIAN')` not `hasRole('LIBRARIAN')`
- Two-tier system: `LIBRARIAN` (full access) and `USER` (limited access)
- Form-based login with client-side SHA-256 password hashing
- Google OAuth2 SSO with dynamic configuration
- OAuth users identified by subject ID, not username

## Major Features

### Photo Storage
See `feature-design-photos.md` for complete details.
- Google Photos API integration for book and author photos
- Browser-based caching (IndexedDB) for thumbnails and book data
- Photo cropping, rotation, ordering
- **Photos NOT included in JSON export** (use separate Photo Export)

### Library of Congress Integration
See `feature-design-loc.md` for complete details.
- Marc4J for LOC call number lookup from ISBN
- BYU CallNumber for proper LOC sorting
- Individual "Lookup" button per book in table
- Books can be filtered by "Without LOC" or "Most Recent Day"

### Import/Export
See `feature-design-import-export.md` for complete details.
- JSON database export/import (libraries, authors, users, books, loans)
- **Photos excluded from JSON export** - use separate Photo Export
- Import merges data (doesn't delete existing)
- Books can be imported from Google Photos feed

### PDF Generation
- **Library Cards**: Wallet-sized PDF cards via `LibraryCardPdfService` using iText
- **Book Labels**: Spine labels for books with LOC call numbers via `LabelsPdfService`

### Test Data Generation
- `/api/test-data/**` endpoints (no auth required)
- Generates sample books, authors, libraries, users, loans
- Menu visibility controlled by `app.show-test-data-page` property
- Default: hidden in production, visible in development

## Git Workflow

**This project uses direct commits to the `dev` branch - no feature branches.**

```bash
# Before starting work
git checkout dev
git pull

# After completing work
git push origin dev
```

## Definition of Done

For any request to be considered complete, ALL of these steps must be accomplished:

1. **Checkout dev branch**: `git checkout dev`
2. **Complete the requested work**: Implement the feature/fix
3. **Update documentation**:
   - Update .md files in root directory if architecture or APIs changed
   - **Do NOT update CLAUDE.md** (it's for AI context only)
   - Update inline code comments and JavaDoc where appropriate
   - **Ensure copyright header** exists at top of every source file:
     - `// (c) Copyright 2025 by Muczynski` or `/* (c) Copyright 2025 by Muczynski */`
4. **Update ALL tests** - Always check and update all three test types:
   - **Unit tests**: Test individual classes and methods in isolation
   - **Integration tests**: Test API endpoints and service interactions
   - **UI tests**: Test Playwright end-to-end user flows
   - **Why all three?** Other programmers make errors that need to be caught
5. **Run verifier**: Use the appropriate verifier to confirm the request was accomplished
   - **IMPORTANT**: If you don't know what verifier to use, don't start work
6. **Push to dev**: `git push origin dev`

## Development Workflow

### Backend Development
1. **Adding a new feature**: Create/modify entities → services → controllers → DTOs → mappers
2. **API changes**: Update controller, ensure `@PreAuthorize` is correct, add/update DTO types
3. **Tests**: Write controller tests with `@SpringBootTest`, integration tests
4. **Security**: Always use `@PreAuthorize` for librarian-only operations, test with different authorities

### Frontend Development (React)
1. **Adding a new feature**:
   - Create API functions in `frontend/src/api/`
   - Add React Query hooks (useFeatures, useCreateFeature, etc.)
   - Create page component in `frontend/src/pages/feature/`
   - Add child components (filters, table, form, modals)
   - Add route in `App.tsx` (lazy-loaded)
   - Add navigation link in `Navigation.tsx`
   - Add TypeScript interfaces in `frontend/src/types/dtos.ts`
2. **UI changes**: Edit React components with Tailwind CSS classes
3. **Tests**: Write Playwright UI tests with `data-test` attributes

## Important Patterns to Follow

### Backend
- Never expose JPA entities directly in controllers - always use DTOs
- Use MapStruct for all entity-DTO conversions
- All dates serialized as ISO strings, not timestamp arrays
- OAuth user handling: Services must check both username and SSO subject ID
- Password hashing: Always use SHA-256 client-side via `hashPassword()` before sending to server

### Frontend (React + TypeScript)
- Use `data-test` attributes for all testable elements
- TypeScript strict mode - all props and state must be typed
- Use TanStack Query hooks for all API calls (never raw fetch)
- Protected routes: Use `<ProtectedRoute />` and `<LibrarianRoute />` wrappers
- Lazy load all page components for code splitting
- Forms: Use controlled components with useState
- State management: TanStack Query (server) + Zustand (client) + useState (local)
- Styling: Use Tailwind CSS utility classes, not custom CSS
- LOC formatting: Use `formatLocForSpine()` in `utils/formatters.ts`
- Copyright header: `// (c) Copyright 2025 by Muczynski` on all new files

### General
- **Copyright headers**: Every source file must have copyright at top
- Don't run all tests (too slow) - instead run the fast-tests.sh script
