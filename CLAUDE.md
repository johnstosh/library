

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Technology Stack

**Backend:**
- Java 17 with Spring Boot 3.5+
- Spring Data JPA with Hibernate ORM
- PostgreSQL database (production) with Cloud SQL integration
- H2 in-memory database (testing)
- Spring Security with OAuth2 (Google SSO)
- MapStruct 1.5.5 for DTO mapping
- iText 8 for PDF generation (library cards)
- Lombok for boilerplate reduction
- Google Photos API for photo storage
- Marc4J 2.9.5 for Library of Congress (LOC) call number lookup
- BYU CallNumber library for LOC call number sorting

**Frontend:**
- Single-page application (SPA) using vanilla JavaScript
- Bootstrap 5.3.8 for UI
- Cropper.js for image manipulation
- **CRITICAL: Uses ES6 modules for most files, but NOT all**
  - Core files (auth.js, sections.js, utils.js, init.js) use ES6 `import`/`export`
  - Feature modules may use traditional global scope
  - Check existing file patterns before adding new JavaScript
  - **Script load order is critical** - the order in index.html must be preserved

**Testing:**
- JUnit 5 with Spring Boot Test
- Playwright for UI testing
- REST Assured for API testing
- H2 in-memory database for tests

## Additional Documentation

This repository contains several markdown files with specific requirements and context:
- `backend-requirements.md` - Backend development requirements and patterns
- `backend-development-requirements.md` - Detailed backend development guidelines
- `uitest-requirements.md` - UI testing requirements and Playwright patterns
- `backend-lessons-learned.md` - Important lessons from backend development
- `lessons-learned.md` - General lessons and best practices
- `endpoints.md` - API endpoint documentation
- `sso.md` - Google OAuth SSO configuration and implementation
- `photos-design.md` - Photo storage and Google Photos integration design
- `wipe-instructions.md` - Database reset instructions

Refer to these files for specific task requirements and context.

## Common Development Commands

### Build and Run
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
The build.gradle test configuration only shows detailed output for failed tests to reduce noise. Standard streams are disabled by default.

## Architecture Overview

### Security & Authentication
- **Two-tier role system**: `USER` and `LIBRARIAN`
- **LIBRARIAN role**: Has full CRUD access to all resources (books, authors, libraries, users, settings, etc.)
- **USER role**: Can view books/authors/libraries, check out books to themselves, view their own loans
  - Users cannot create/edit/delete books, authors, or libraries
  - Users cannot view other users' data or access admin features
- **Public endpoints**: Test data generation, book/author/library listings, search are unauthenticated
- **Authentication methods**:
  - Form-based login with SHA-256 password hashing (client-side before transmission to avoid BCrypt 72-byte limit)
  - Google OAuth2 SSO (dynamically configured via database settings)
- **OAuth subject ID handling**: OAuth users are identified by their subject ID (not username) in authentication.getName()
  - Services must handle lookups by both username and SSO subject ID

### Domain Model Architecture
The application follows a standard Spring Boot layered architecture:

**Core Domain Entities:**
- `Library` - Root entity for library branch information (name, hostname)
- `Book` - Book inventory with title, publication year, publisher, author, library, LOC number, status (ACTIVE/INACTIVE/LOST/DAMAGED)
- `Author` - Book authors with biographical information (name, dates, nationality, religious affiliation)
- `Loan` - Book checkout tracking with loan date, due date, return date
- `User` - Library patron/staff accounts with role-based access
- `Photo` - Book and author photos with Google Photos integration
- `Applied` - Library card application tracking
- `LibraryCardDesign` - Customizable library card PDF design settings
- `GlobalSettings` - Application-wide settings (SSO configuration, etc.)
- `RandomBook`, `RandomAuthor`, `RandomLoan` - Test data generation templates

**Key Relationships:**
- `Library` has many `Book`s
- `Author` has many `Book`s
- `Book` has many `Photo`s (one-to-many, cascade delete)
- `Author` has many `Photo`s (one-to-many, cascade delete)
- `Book` has many `Loan`s
- `User` has many `Loan`s
- All entities use JPA with Lombok for getters/setters

### Service Layer Patterns
- Services are transactional and handle business logic
- **MapStruct** is used for all entity-to-DTO conversions (see `mapper/` package)
- **Important**: Always use MapStruct mappers, never manual field copying
  - **If a mapper doesn't exist, create it** following the existing patterns in `mapper/` package
  - Use `@Mapper(componentModel = "spring")` annotation
  - Define methods for entity ↔ DTO conversion
- Services throw `LibraryException` for errors
- **OAuth user lookup**: Services must handle both username and SSO subject ID lookups
  - Example: `LoanService.getLoansByUsername()` checks username first, then SSO subject ID

### Controller Layer
- All REST controllers use `@RestController` with `/api/*` paths
- Controllers delegate to services and use DTOs (never expose entities directly)
- `@PreAuthorize("hasAuthority('LIBRARIAN')")` for librarian-only endpoints
- `@PreAuthorize("isAuthenticated()")` for authenticated endpoints (both roles)
- Global exception handling returns consistent error responses
- Returns `ResponseEntity` with appropriate HTTP status codes

### Frontend Architecture
The frontend is a single-page application in `/src/main/resources/static/`:

**Core Structure:**
- `index.html` - Main SPA shell with navigation and section containers
- `js/init.js` - Application initialization and authentication check
- `js/sections.js` - Section navigation and visibility management
- `js/app.js` - Main application orchestration
- `js/auth.js` - Login/logout handling, role-based UI visibility

**Feature Modules** (each manages a specific domain):
- `js/books-table.js` - Book listing
- `js/books-edit.js` - Book CRUD operations
- `js/books-photo.js` - Book photo management
- `js/books-from-feed.js` - Import books from Google Photos feed
- `js/authors-table.js` - Author listing
- `js/authors-edit.js` - Author CRUD operations
- `js/authors-photo.js` - Author photo management
- `js/libraries.js` - Library management
- `js/loans.js` - Loan checkout and tracking
- `js/users.js` - User management
- `js/settings.js` - User settings (password, API keys)
- `js/global-settings.js` - Global application settings (SSO, etc.)
- `js/search.js` - Book/author search functionality
- `js/photos.js` - Google Photos integration
- `js/labels.js` - PDF label generation
- `js/librarycard.js` - Library card PDF generation
- `js/loc-bulk-lookup.js` - Bulk LOC call number lookup
- `js/test-data.js` - Test data generation UI

**Utilities:**
- `js/utils.js` - Common utilities (date formatting, API helpers, LOC formatting)
- `js/thumbnail-cache.js` - Photo thumbnail caching
- `js/photo-crop.js` - Photo cropping utilities

**UI Patterns:**
- Sections are shown/hidden via `showSection()` function
- Elements with class `librarian-only` are hidden for non-librarian users (ROLE_USER and unauthenticated)
- Elements with class `public-item` are visible to unauthenticated users
- Elements with class `librarian-or-unauthenticated` are visible only to ROLE_LIBRARIAN and unauthenticated users (hidden from ROLE_USER)
  - Example: "Apply for Library Card" section - users who already have cards (ROLE_USER) don't need to see it
- `data-test` attributes on all interactive elements for Playwright tests
- All API calls use `fetch()` with proper error handling
- Forms use Bootstrap styling with validation feedback

**CRUD UI Pattern:**
CRUD operations in the UI follow a consistent pattern:
- **Create**: "Add" button/section
- **Read**: List/table view with "View" option
- **Update**: "Edit" button with pencil icon (✏️)
- **Delete**: "Delete" button with trash can icon (🗑️)

### Photo Storage Integration
- Google Photos API integration for book and author photo storage
- OAuth2 flow for Google Photos authorization at `/api/oauth/google/authorize`
- `GooglePhotosService` handles uploads, downloads, and album management
- Photo entities store Google Photos media item IDs and URLs
- Supports batch operations via Google Photos Library API
- Photos can be imported from Google Photos feed and associated with books
- Cropping and rotation features available

### PDF Generation
- **Library Cards**: Wallet-sized PDF cards via `LibraryCardPdfService` using iText
  - Customizable design through `LibraryCardDesign` entity
  - Front and back card generation
- **Book Labels**: Spine labels for books with LOC call numbers via `LabelsPdfService`
  - Custom formatting for LOC numbers on spine labels

### Library of Congress Integration
- **Marc4J**: Used for LOC call number lookup from ISBN
- **BYU CallNumber**: Used for proper LOC call number sorting
- Bulk lookup functionality for batch processing
- LOC numbers stored per book and displayed on labels

### Test Data Generation
- `/api/test-data/**` endpoints (no auth required) for generating sample data
- Useful for development and demos
- Can generate books, authors, libraries, users, and loans
- **Test Data Menu Visibility**: The Test Data menu item can be hidden in production
  - Controlled by `app.show-test-data-page` property in application.properties
  - Default: `true` (visible) in development/test environments
  - Default: `false` (hidden) in production (application-prod.properties)
  - The backend API endpoints remain accessible even when the menu is hidden
  - Visibility is checked via `/api/global-properties/test-data-page-visibility` endpoint
- **IMPORTANT**: Test data records should be distinguishable from real data in the database

### Import/Export System
- Photo metadata export at `/api/photo-export/**` (authenticated users)
- LOC bulk lookup import/export functionality
- Import books from Google Photos feed

## Key Configuration Notes

### Database
- **Production**: PostgreSQL via Google Cloud SQL
  - Connection uses socket factory: `com.google.cloud.sql.postgres.SocketFactory`
  - Configured in `application-prod.properties`
- **Testing**: H2 in-memory database
  - Configured in `application-test.properties`
- Schema auto-updated via `spring.jpa.hibernate.ddl-auto=update`

### CORS
- Configured to allow specific origins for security
- Credentials allowed for authenticated requests

### Google Integration
- OAuth client configuration stored in database (`GlobalSettings`) and dynamically loaded
- Falls back to environment variables if not set in database
- Scopes include full Google Photos Library access
- OAuth subject ID used as primary identifier for OAuth users

### Password Security
- Client-side SHA-256 hashing before transmission (avoids BCrypt 72-byte limit)
- Hashed passwords stored with BCrypt in database
- Implementation in `utils.js` `hashPassword()` function

## Git Workflow

**This project uses direct commits to the `main` branch - no feature branches.**

Before starting any work:
```bash
git checkout main
git pull
```

When pushing changes:
```bash
git push origin main
```

## Definition of Done

For any request to be considered complete, ALL of the following steps must be accomplished:

1. **Checkout main branch**: `git checkout main`
2. **Complete the requested work**: Implement the feature/fix
3. **Update documentation**:
   - Update .md files in the root directory if architecture or APIs changed
   - **Do NOT update CLAUDE.md** (it's for AI context only)
   - Update inline code comments and JavaDoc where appropriate
   - **Ensure copyright header** exists at the top of every source file: `// (c) Copyright 2025 by Muczynski` or `/* (c) Copyright 2025 by Muczynski */`
4. **Update ALL tests** - Always check and update all three test types:
   - **Unit tests**: Test individual classes and methods in isolation
   - **Integration tests**: Test API endpoints and service interactions
   - **UI tests**: Test Playwright end-to-end user flows
   - **Why all three?** Other programmers make errors that need to be caught and rectified
5. **Run verifier**: Use the appropriate verifier to confirm the request was accomplished
   - **IMPORTANT**: If you don't know what verifier to use, don't start work on the request
6. **Push to main**: `git push origin main`

## Development Workflow

1. **Adding a new feature**: Create/modify entities → services → controllers → DTOs → mappers → frontend JS
2. **UI changes**: Edit `index.html` for structure, `js/*.js` for behavior, `css/style.css` for styling
3. **API changes**: Update controller, ensure `@PreAuthorize` is correct, update corresponding frontend JS
4. **Tests**: Write controller tests with `@SpringBootTest`, UI tests with Playwright in `ui/` package
5. **Security**: Always use `@PreAuthorize` for librarian-only operations, test with different roles

## Important Patterns to Follow

- Never expose JPA entities directly in controllers - always use DTOs
- Use MapStruct for all entity-DTO conversions
- Frontend uses `data-test` attributes for all testable elements
- All dates serialized as ISO strings, not timestamp arrays
- Librarian-only UI elements must have `librarian-only` CSS class
- Public UI elements (visible to unauthenticated users) must have `public-item` CSS class
- **JavaScript modules**: Core infrastructure uses ES6 modules, but check existing patterns
  - Don't blindly convert all files to modules or vice versa
  - Maintain consistency with the existing file's pattern
- **Copyright headers**: Every source file must have `// (c) Copyright 2025 by Muczynski` or `/* (c) Copyright 2025 by Muczynski */` at the top
- **OAuth user handling**: Services must check both username and SSO subject ID for user lookups
- **Password hashing**: Always use SHA-256 client-side hashing via `hashPassword()` before sending passwords to server
- **LOC formatting**: Use `formatLocForSpine()` in utils.js for displaying LOC call numbers on spine labels
