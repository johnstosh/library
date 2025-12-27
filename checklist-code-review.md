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


## Features/Pages Checklist

### Core Entity Management
- [x] **Books** (`/books`) - **FIXES IMPLEMENTED**
  - Books table with filters
  - Add/Edit/Delete books
  - Book detail view with photos
  - LOC lookup integration
  - Book status management (ACTIVE, ON_ORDER, INACTIVE)
  - Bulk operations (select, delete, LOC lookup)
  - **Fixes Applied:**
    - ✅ Added missing copyright headers to LocBulkLookupController, ImportController, UserSettingsController
    - ✅ Removed duplicate filter endpoints from LocBulkLookupController (missing-loc, most-recent)
    - ✅ Added missing fields to BookDto interface (plotSummary, relatedWorks, detailedDescription, statusReason, dateAddedToLibrary)
    - ✅ Added missing fields to BookForm component with textarea inputs
    - ✅ Added missing fields to BookDetailModal display
    - ✅ Added test for clone book endpoint
    - ✅ Added test for suggest LOC endpoint
    - ✅ Updated feature-design-loc.md to use correct /api/books endpoints

- [x] **Authors** (`/authors`) - **FIXES IMPLEMENTED**
  - Authors table
  - Add/Edit/Delete authors
  - Author photos
  - Author biographical information
  - **Fixes Applied:**
    - ✅ Added bulk delete endpoint to AuthorController (POST /api/authors/delete-bulk)
    - ✅ Added deleteBulkAuthors method to AuthorService
    - ✅ Added missing fields to frontend AuthorDto (firstPhotoId, firstPhotoChecksum)
    - ✅ Added missing fields to AuthorForm (religiousAffiliation, birthCountry, nationality)
    - ✅ Added missing fields to AuthorDetailModal display
    - ✅ Added 11 missing controller tests (update, delete, bulk delete, photo operations, etc.)

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

- [ ] **Users** (`/users`)
  - User management (librarian only)
  - Add/Edit/Delete users
  - Authority assignment (LIBRARIAN vs USER)
  - Password hashing (SHA-256 client-side)
  - SSO user handling

- [ ] **Loans** (`/loans`)
  - Book checkout tracking
  - Check out books
  - Return books
  - View active loans
  - Loan history

### Library Card Features
- [ ] **My Library Card** (`/my-card`)
  - View personal library card
  - Download library card PDF

- [ ] **Apply for Card** (`/apply`)
  - Library card application form
  - Application submission

- [ ] **Applications** (`/applications`, librarian only)
  - Review card applications
  - Approve/Reject applications
  - View application details

- [ ] **Library Card Design** (in Global Settings)
  - Customize library card PDF design
  - Logo upload
  - Color customization
  - PDF generation with iText 8

### Search & Discovery
- [ ] **Search** (`/search`)
  - Global search across books and authors
  - Search by title, author, publisher
  - Search filters

- [ ] **Labels** (`/labels`, librarian only)
  - Book spine label PDF generation
  - Filter by most recent day or all books
  - Bulk label generation
  - LOC call number formatting for labels

### Data Management
- [ ] **Data Management** (`/data-management`, librarian only)
  - JSON export (libraries, authors, users, books, loans)
  - JSON import with merge functionality
  - Photo export (separate from JSON)
  - Photo import
  - Database wipe functionality

- [ ] **Books from Feed** (`/books-from-feed`, librarian only)
  - Import books from Google Photos feed
  - Process photos with AI description
  - Review and save books
  - Photo cropping and rotation

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
- [ ] **Photo Upload** (in Book/Author detail)
  - Upload photos to Google Photos
  - Photo cropping and rotation
  - Photo ordering
  - Photo deletion
  - Browser-based caching (IndexedDB)
  - Thumbnail generation

- [ ] **Photo Storage**
  - Google Photos API integration
  - Photo checksum for change detection
  - Cascade delete with entities
  - Photo export/import

### Security & Authentication
- [ ] **Login** (`/login`)
  - Form-based authentication
  - Client-side SHA-256 password hashing
  - Google OAuth SSO
  - Remember me functionality

- [ ] **User Settings** (`/settings`)
  - Change password
  - View profile information
  - SSO account linking

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



