# Code Review Implementation Checklist

## Rules
1. **Make a code-review-xxxx.md file for one feature. Stop when you're done.**
2. **Don't report what's right about the subsystem, but rather just bugs.**
3. **Point out where the code doesn't match the documentation.**
4. **Point out where features in the documentation are missing from the code.**
5. **Don't commit.**
6. **When you find bugs in the production code report them, look for similar bugs in other features and report them.**
7. **Include both backend and frontend code in the code review.**


## Progress Tracking
- [x] LOC (Library of Congress Integration) - complete
- [x] Books - complete
- [x] Authors - complete
- [x] Libraries - complete
- [x] Users - complete
- [x] Loans - complete
- [x] Data Management - complete
- [x] Books from Feed - complete
- [x] Photo Upload - complete


## Features/Pages Checklist

### Core Entity Management
- [x] **Books** (`/books`) - **FIXES IMPLEMENTED**
  - Books table with filters
  - Add/Edit/Delete books
  - Book detail view with photos
  - LOC lookup integration
  - Book status management (ACTIVE, ON_ORDER, INACTIVE)
  - Bulk operations (select, delete, LOC lookup, generate labels)
  - **Table Columns:** Title (with author subtitle), LOC, Status (removed: Year, Publisher, Library)
  - **Fixes Applied:**
    - ✅ Added missing copyright headers to LocBulkLookupController, ImportController, UserSettingsController
    - ✅ Removed duplicate filter endpoints from LocBulkLookupController (missing-loc, most-recent)
    - ✅ Added missing fields to BookDto interface (plotSummary, relatedWorks, detailedDescription, statusReason, dateAddedToLibrary)
    - ✅ Added missing fields to BookForm component with textarea inputs
    - ✅ Added missing fields to BookDetailModal display
    - ✅ Added test for clone book endpoint
    - ✅ Added test for suggest LOC endpoint
    - ✅ Updated feature-design-loc.md to use correct /api/books endpoints
    - ✅ Removed Year, Publisher, Library columns from BookTable (Dec 2024)
    - ✅ Added "Generate Labels" bulk action button to BulkActionsToolbar (Dec 2024)

- [x] **Authors** (`/authors`) - **FIXES IMPLEMENTED**
  - Authors table
  - Add/Edit/Delete authors
  - Author photos
  - Author biographical information
  - **Table Columns:** Name, Religious Affiliation, Biography, Books (removed: Birth Date, Death Date)
  - **Fixes Applied:**
    - ✅ Added bulk delete endpoint to AuthorController (POST /api/authors/delete-bulk)
    - ✅ Added deleteBulkAuthors method to AuthorService
    - ✅ Added missing fields to frontend AuthorDto (firstPhotoId, firstPhotoChecksum)
    - ✅ Added missing fields to AuthorForm (religiousAffiliation, birthCountry, nationality)
    - ✅ Added missing fields to AuthorDetailModal display
    - ✅ Added 11 missing controller tests (update, delete, bulk delete, photo operations, etc.)
    - ✅ Removed Birth Date and Death Date columns from AuthorTable (Dec 2024)
    - ✅ Added Religious Affiliation column to AuthorTable (Dec 2024)

- [x] **Libraries** (`/libraries`) - **FIXES IMPLEMENTED**
  - Library branches management
  - Add/Edit/Delete libraries
  - Library hostname configuration
  - Library statistics (book count, active loans count)
  - **Fixes Applied:**
    - ✅ Fixed LibraryService.getLibraryStatistics() to count active loans per library instead of globally
    - ✅ Added countByBookLibraryIdAndReturnDateIsNull method to LoanRepository
    - ✅ Converted LibraryMapper from manual @Component to MapStruct @Mapper interface
    - ✅ Updated feature-design-libraries.md to reflect React implementation instead of obsolete vanilla JavaScript
    - ✅ Removed unused books field from Library entity (never referenced, better performance)

- [x] **Users** (`/users`) - **FIXES IMPLEMENTED**
  - User management (librarian only)
  - Add/Edit/Delete users
  - Authority assignment (LIBRARIAN vs USER)
  - Password hashing (SHA-256 client-side)
  - SSO user handling
  - **Fixes Applied:**
    - ✅ Converted UserMapper from manual @Component to MapStruct @Mapper interface
    - ✅ Added copyright header to UserRepository
    - ✅ Added bulk delete endpoint (POST /api/users/delete-bulk)
    - ✅ Added @PreAuthorize("isAuthenticated()") to /api/users/me endpoint
    - ✅ Added 8 missing controller tests (public register, API key, 404, active loans, bulk delete)
    - ✅ Added missing fields to frontend UserDto (xaiApiKey, ssoProvider, email, activeLoansCount, etc.)
    - ✅ Added activeLoansCount column to UserTable component
    - ✅ Updated frontend useDeleteUsers hook to use bulk delete endpoint
    - ✅ Documented all User endpoints in endpoints.md

- [x] **Loans** (`/loans`) - **FIXES IMPLEMENTED**
  - Book checkout tracking
  - Check out books
  - Return books
  - View active loans
  - Loan history
  - **Fixes Applied:**
    - ✅ Fixed frontend API endpoints to match backend (POST /loans/checkout, PUT /loans/return/{id})
    - ✅ Fixed security vulnerability: regular users can now only checkout books to themselves
    - ✅ Added book already loaned validation - throws BookAlreadyLoanedException (409 Conflict)
    - ✅ Converted LoanMapper from manual @Service to MapStruct @Mapper interface
    - ✅ Added copyright header to ImportLoanDto.java
    - ✅ Added 5 missing controller tests (404 on get/return, 400 on invalid input, 403 on checkout to other user)
    - ✅ Documented all Loan endpoints in endpoints.md

### Library Card Features
- [x] **My Library Card** (`/my-card`) - **FIXES IMPLEMENTED**
  - View personal library card
  - Download library card PDF
  - **Fixes Applied:**
    - ✅ Added @PreAuthorize("isAuthenticated()") to LibraryCardController.printLibraryCard()
    - ✅ Created feature-design-library-cards.md documentation
    - ✅ Added library card PDF endpoint to endpoints.md

- [x] **Apply for Card** (`/apply`) - **FIXES IMPLEMENTED**
  - Library card application form
  - Application submission
  - **Fixes Applied:**
    - ✅ Fixed AppliedController to use AppliedDto instead of exposing Applied entities (Bugs #1, #2)
    - ✅ Removed duplicate and unused imports from AppliedController (Bugs #4, #5)
    - ✅ Fixed frontend AppliedDto interface to match backend (removed password, added status)
    - ✅ Fixed AppliedService.getAppliedById to throw exception instead of returning null
    - ✅ Created feature-design-library-cards.md documentation
    - ✅ Added all application endpoints to endpoints.md

- [x] **Applications** (`/applications`, librarian only) - **FIXES IMPLEMENTED**
  - Review card applications
  - Approve/Reject applications
  - View application details
  - **Fixes Applied:**
    - ✅ Fixed AppliedController to use DTOs (prevents password exposure)
    - ✅ All endpoints properly return AppliedDto (id, name, status)
    - ✅ MapStruct AppliedMapper properly utilized
    - ✅ Created feature-design-library-cards.md documentation
    - ✅ Added all application management endpoints to endpoints.md

- [x] **Library Card Design** (in User Settings) - **ALL FIXES COMPLETE**
  - Customize library card PDF design
  - Select from 5 predefined card designs
  - PDF generation with iText 8
  - **Findings & Fixes:**
    - ✅ LibraryCardDesign is an enum (not an entity) with 5 predefined designs
    - ✅ Stored per-user (not global), field on User entity
    - ✅ **DOCUMENTATION FIXED**: Updated feature-design-library-cards.md to describe per-user design preference
    - ✅ **DOCUMENTATION FIXED**: Removed incorrect references to customizable logo/colors
    - ✅ **DOCUMENTATION FIXED**: Moved section from "Global Settings" to "User Settings"
    - ✅ **DOCUMENTATION FIXED**: Updated LibraryCardDesign section to describe enum with 5 designs
    - ✅ **DOCUMENTATION FIXED**: Updated CLAUDE.md to reflect per-user design preference
    - ✅ **FIXED**: Added frontend UI in UserSettingsPage for library card design selection
    - ✅ **FIXED**: Created LibraryCardDesign enum in frontend (TypeScript)
    - ✅ **FIXED**: Updated UserDto.libraryCardDesign to use enum type instead of string
    - ✅ **FIXED**: Removed unused LibraryCardDesignDto from frontend
    - ✅ **FIXED**: Added copyright header to UserSettingsService
    - ✅ **FIXED**: Created UserSettingsControllerTest.java with 14 comprehensive tests
    - ✅ **FIXED**: Added 5 SSO status tests to GlobalSettingsControllerTest

### Search & Discovery
- [x] **Search** (`/search`) - **FIXES IMPLEMENTED + CRUD ACTIONS ADDED (Dec 2024)**
  - Global search across books and authors
  - Search by title and author name (publisher search not implemented)
  - **Navigation menu now visible** on Search page (Dec 2024)
  - **Actions column with CRUD icons** for books and authors (Dec 2024):
    - View (eye icon, gray) - All users can view details in modal
    - Edit (pencil icon, blue) - Librarians can edit via form modal
    - Delete (trash icon, red) - Librarians can delete with confirmation
  - **Fixes Applied:**
    - ✅ Bug #1: Created SearchResponseDto to replace Map<String, Object>
    - ✅ Bug #2: SearchService now returns SearchResponseDto
    - ✅ Bug #3: SearchController uses ResponseEntity<SearchResponseDto>
    - ✅ Bug #4: Added endpoint documentation to endpoints.md
    - ✅ Bug #5: Created feature-design-search.md documentation
    - ✅ Bug #6: Added Search to CLAUDE.md Major Features section
    - ✅ Bug #7: Updated checklist to reflect actual implementation (title/name only)
    - ✅ Bug #8: Created SearchServiceTest with 6 comprehensive tests
    - ⏭️ Bug #9: Playwright UI tests skipped per user request
    - ✅ Bug #10: Created reusable PageInfoDto class
    - ✅ Wrapped Search page in AppLayout to show navigation menu (Dec 2024)
    - ✅ Added Actions column with CRUD icons to search results (Dec 2024)
    - ✅ Reuses BookDetailModal, AuthorDetailModal for view operations
    - ✅ Reuses BookForm, AuthorForm for edit operations
    - ✅ data-test attributes on all action buttons

- [x] **Labels** (integrated into Books page, librarian only) - **REVIEWED Dec 2024**
  - Book pocket label PDF generation via "Generate Labels" button in Books page
  - Standalone Labels page removed (redundant)
  - Bulk label generation from Books page bulk actions toolbar
  - LOC call number formatting for labels
  - **Code Review:** See `code-review-labels.md` for security and testing findings

### Data Management
- [x] **Data Management** (`/data-management`, librarian only) - **ALL FIXES COMPLETE Dec 2024**
  - JSON export (libraries, authors, users, books, loans) ✅
  - JSON import with merge functionality ✅
  - Photo export (Google Photos sync system) ✅
  - Photo import ✅ (from Google Photos)
  - Database wipe functionality ❌ **NOT IMPLEMENTED**
  - **Code Review:** See `code-review-data-management.md` for findings
  - **Fixes Applied (Dec 28, 2024):**
    - ✅ Bug #2: Added all import/export endpoints to endpoints.md (already documented)
    - ✅ Bug #3: Added all books-from-feed endpoints to endpoints.md (already documented)
    - ✅ Bug #4: Updated feature-design-import-export.md to clarify photo metadata exclusion and Google Photos sync system
    - ✅ Bug #5: Copyright header already present in ImportService.java
    - ✅ Recommendation #6: Created DTOs (PhotoExportStatsDto, PhotoExportInfoDto, PhotoImportResultDto, PhotoVerifyResultDto, PhotoExportResponseDto)
    - ✅ Recommendation #6: Updated PhotoExportService to use DTOs instead of Map<String, Object>
    - ✅ Recommendation #6: Updated PhotoExportController to use DTOs instead of Map<String, Object>
  - **Known Issues (Not Fixed):**
    - ⚠️ Bug #1: Frontend calls non-existent `GET /api/photo-export` for ZIP download - frontend needs to use existing endpoints
    - ⚠️ Security: Photo stats and list endpoints are public (no @PreAuthorize) - architectural decision, not changed

- [x] **Books from Feed** (`/books-from-feed`, librarian only) - **REVIEWED Dec 2024**
  - Import books from Google Photos feed ✅
  - Process photos with AI description ✅
  - Review and save books ✅
  - Photo cropping and rotation ✅
  - **Code Review:** See `code-review-data-management.md` (combined review)
  - **Issues:** Missing endpoint documentation in endpoints.md

### Library of Congress Integration
- [x] **LOC Lookup** (integrated in Books page)
  - Individual book LOC lookup (button in table)
  - Bulk LOC lookup (bulk actions toolbar)
  - Lookup all missing LOC numbers
  - Title + Author based lookup (no ISBN)
  - Multiple fallback strategies
  - Marc4J library integration
  - BYU CallNumber for sorting

### Photo Management
- [x] **Photo Upload** (in Book/Author detail) - **FIXES IMPLEMENTED Dec 28, 2024**
  - Upload photos to Google Photos ✅
  - Photo cropping and rotation ✅
  - Photo ordering ✅
  - Photo deletion ✅
  - Browser-based caching (IndexedDB) ❌ **NOT IMPLEMENTED** (uses TanStack Query instead)
  - Thumbnail generation ✅
  - **Code Review:** See `code-review-photos.md` for findings
  - **Fixes Applied:**
    - ✅ Bug #2: Added all photo endpoints to endpoints.md (book photos, author photos, direct access)
    - ✅ Quality #1: Fixed ResponseEntity<?> to use specific types (PhotoDto, List<PhotoDto>, PhotoAddFromGooglePhotosResponse, Void)
    - ✅ Quality #1: Created PhotoAddFromGooglePhotosResponse DTO for consistent error handling
    - ✅ Quality #2: Standardized error responses to use DTOs instead of mixed Map/String
    - ✅ Test Gap #1: Added 7 comprehensive tests to PhotoControllerTest (image, thumbnail, delete, restore, permissions)
    - ✅ Test Gap #2: Added 6 comprehensive tests to PhotoServiceTest (upload, list, soft delete, restore, rotate, update, delete)
  - **Known Issues (Not Fixed):**
    - ⚠️ Quality #3: Photo delete method naming confusion (deletePhoto vs softDeletePhoto) - kept as-is for backward compatibility
    - ⚠️ Photo entity schema may not match photos-design.md documentation
    - ⚠️ May be using deprecated Google Photos API scopes (breaks March 2025)
    - ⚠️ Missing caption edit UI
    - ⚠️ Missing IndexedDB caching (uses TanStack Query instead)
    - ⚠️ Test Gap #3: No frontend photo component tests

- [ ] **Photo Storage**
  - Google Photos API integration
  - Photo checksum for change detection
  - Cascade delete with entities
  - Photo export/import

### Security & Authentication
- [x] **Login** (`/login`) - **UPDATED Dec 2024**
  - Form-based authentication
  - Client-side SHA-256 password hashing
  - Google OAuth SSO
  - Remember me functionality
  - **Navigation menu now visible** on Login page
  - **Full-width marian-m image** in login card
  - **Updates Applied:**
    - ✅ Wrapped Login page in AppLayout to show navigation menu (Dec 2024)
    - ✅ Updated Login page layout to work with AppLayout (removed full-screen gradient)
    - ✅ Made marian-m image full width within login card (Dec 2024)

- [x] **User Settings** (`/settings`) - **REVIEWED Dec 2024**
  - Change password ❌ **BROKEN** (missing backend endpoint)
  - View profile information ✅ (limited implementation)
  - SSO account linking ❌ **NOT IMPLEMENTED**
  - **Code Review:** See `code-review-user-settings.md` for findings
  - **Critical Bugs:**
    - ❌ Bug #1: Missing `PUT /api/users/change-password` endpoint - password change completely broken
    - ⚠️ Bug #2: Missing `@PreAuthorize("isAuthenticated()")` on all UserSettingsController endpoints
    - 🔗 Bug #3: No navigation link to User Settings page (feature hidden from users)
  - **Missing Features:**
    - SSO account linking not implemented
    - Limited profile information display
    - Endpoints not documented in endpoints.md
  - **Test Gaps:**
    - No Playwright UI tests
    - No UserSettingsService unit tests

- [ ] **Global Settings** (`/global-settings`, librarian only)
  - SSO configuration (Client ID, Secret)
  - Library card design settings
  - Application-wide settings

### Testing & Development
- [ ] **Test Data** (`/test-data`)
  - Generate sample books
  - Generate sample authors
  - Generate sample libraries
  - Generate sample users
  - Generate sample loans
  - Menu visibility controlled by property
  - No authentication required

### API Features (Backend)
- [ ] **REST Controllers**
  - Proper DTO usage (never expose entities)
  - MapStruct for entity-DTO conversion
  - Authority-based access control (@PreAuthorize)
  - Proper HTTP status codes

- [ ] **Security**
  - Two-tier authority system (LIBRARIAN, USER)
  - OAuth2 integration with Google
  - CORS configuration
  - Session management

- [ ] **Database**
  - PostgreSQL (production)
  - H2 (testing)
  - Proper JPA relationships
  - LEFT JOIN FETCH for performance
  - Jackson ISO-8601 datetime serialization


## Notes



