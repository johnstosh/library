# Frontend Architecture

## Overview
The frontend is a single-page application (SPA) using vanilla JavaScript, Bootstrap 5.3.8, and minimal dependencies.

## Technology Stack
- Vanilla JavaScript (ES6 where appropriate)
- Bootstrap 5.3.8 for UI components and styling
- Cropper.js for image manipulation
- No framework (React, Vue, Angular, etc.)

## Module System

### CRITICAL: Hybrid Module Approach
- **NOT all files use ES6 modules**
- Core infrastructure files use ES6 `import`/`export`:
  - `auth.js`
  - `sections.js`
  - `utils.js`
  - `init.js`
- Feature modules may use traditional global scope
- **Check existing file patterns before adding new JavaScript**
- **Script load order is critical** - the order in `index.html` must be preserved

### When to Use Which Approach
- **ES6 modules**: Core infrastructure, shared utilities
- **Global scope**: Feature-specific modules that need to be called from HTML onclick handlers
- **Rule**: Maintain consistency with the existing file's pattern

## Core Structure

### Main Files
- `index.html` - SPA shell with navigation and section containers
- `js/init.js` - Application initialization and authentication check
- `js/sections.js` - Section navigation and visibility management
- `js/app.js` - Main application orchestration
- `js/auth.js` - Login/logout handling, role-based UI visibility

### Application Flow
1. User loads `index.html`
2. `init.js` runs `initApp()` on DOM ready
3. `checkAuthentication()` determines user's authority level
4. UI elements shown/hidden based on authority (librarian-only, public-item, etc.)
5. `showSection()` manages section visibility
6. Feature modules load data when their section is shown

## Feature Modules

### Domain-Specific Modules
Each module manages a specific domain:
- `js/books-table.js` - Book listing with LOC lookup
- `js/books-edit.js` - Book CRUD operations
- `js/books-photo.js` - Book photo management
- `js/books-from-feed.js` - Import books from Google Photos
- `js/authors-table.js` - Author listing
- `js/authors-edit.js` - Author CRUD operations
- `js/authors-photo.js` - Author photo management
- `js/libraries.js` - Library management
- `js/loans.js` - Loan checkout and tracking
- `js/users.js` - User management
- `js/settings.js` - User settings (password, API keys)
- `js/global-settings.js` - Global application settings
- `js/search.js` - Book/author search
- `js/photos.js` - Google Photos integration
- `js/labels.js` - PDF label generation
- `js/librarycard.js` - Library card PDF generation
- `js/loc-bulk-lookup.js` - Bulk LOC lookup
- `js/test-data.js` - Test data generation UI

## Utility Modules

### Common Utilities
- `js/utils.js` - Shared utilities:
  - Date formatting
  - API helpers (`fetchData`, `postData`, `putData`, `deleteData`)
  - LOC formatting (`formatLocForSpine`)
  - Password hashing (`hashPassword`)
  - Error handling (`showError`, `clearError`)

### Performance Utilities
- `js/thumbnail-cache.js` - Photo thumbnail caching (IndexedDB)
- `js/book-cache.js` - Book data caching (IndexedDB)
- `js/photo-crop.js` - Photo cropping utilities

## UI Patterns

### Section Management
- All content divided into sections (Books, Authors, Loans, etc.)
- Only one section visible at a time
- `showSection(sectionId)` function controls visibility
- Section configuration in `sections.js`:
  ```javascript
  const sectionConfig = {
    'books': { load: loadBooks, reset: resetBookForm },
    'authors': { load: loadAuthors, reset: resetAuthorForm },
    // ...
  }
  ```

### Access Control Classes
- **`librarian-only`**: Hidden for non-librarian users
- **`public-item`**: Visible to unauthenticated users
- **`librarian-or-unauthenticated`**: Visible to LIBRARIAN and unauthenticated (hidden from USER)

### CRUD Pattern
Consistent across all features:
- **Create**: "Add" button/section
- **Read**: List/table view with "View" option
- **Update**: "Edit" button with pencil icon (✏️)
- **Delete**: "Delete" button with trash can icon (🗑️)

### Test Attributes
- All interactive elements have `data-test` attributes
- Used by Playwright tests for element selection
- Format: `data-test="section-name-action"`
- Examples: `data-test="add-book-btn"`, `data-test="book-title"`

## API Communication

### Fetch Utilities
All API calls use `fetch()` with helper functions in `utils.js`:
- `fetchData(url)` - GET requests
- `postData(url, data)` - POST requests
- `putData(url, data)` - PUT requests
- `deleteData(url)` - DELETE requests

### Error Handling
- Global error handling in utility functions
- `showError(sectionId, message)` displays errors to user
- `clearError(sectionId)` removes error messages
- Bootstrap styling for error/success feedback

## Form Handling

### Validation
- Bootstrap form validation classes
- Client-side validation before submission
- Server-side validation with error messages

### Data Binding
- Manual DOM manipulation (no two-way binding)
- Form data extracted with `FormData` or manual field access
- DTOs sent to backend as JSON

## Performance Optimization

### Caching Strategy
- **Book Cache**: Caches book list data in IndexedDB
  - Compares `lastModified` timestamps
  - Only fetches changed books
  - Reduces bandwidth and improves load times
- **Thumbnail Cache**: Caches photo thumbnails
  - Uses photo ID + checksum as key
  - Avoids re-downloading unchanged photos

### Lazy Loading
- Sections load data only when shown
- `load` function in section configuration
- Prevents unnecessary API calls on app startup

## Styling

### Bootstrap Integration
- Bootstrap 5.3.8 for layout and components
- Custom CSS in `css/style.css` for application-specific styling
- Responsive design with Bootstrap grid system
- Bootstrap icons for common actions

### Consistent Look and Feel
- All forms use Bootstrap form components
- Tables use Bootstrap table classes
- Buttons follow Bootstrap button sizing (btn-sm for actions)
- Icons from Bootstrap Icons (bi bi-eye, bi-pencil, bi-trash, etc.)

## Related Files
- `index.html` - SPA shell and structure
- `css/style.css` - Custom styling
- All `js/*.js` files - Feature implementations
- `uitest-requirements.md` - Playwright test patterns
- `feature-design-security.md` - UI access control details
