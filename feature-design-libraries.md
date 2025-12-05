# Libraries Feature Design

## Overview

The Libraries page provides management of library branches in the system. Each library represents a physical location that can house books. The page serves as the central hub for library administration and includes database-wide import/export functionality.

## Purpose

- **Library Management**: Create, read, update, and delete library branches
- **Multi-Branch Support**: While the system currently operates as a single-branch library, the architecture supports multiple branches
- **Database Backup/Restore**: Provides JSON import/export for entire database backup and migration
- **Photo Management**: Track Google Photos integration status for all photos across the library

## Domain Model

### Library Entity

**File**: `src/main/java/com/muczynski/library/domain/Library.java`

```java
@Entity
public class Library {
    private Long id;              // Auto-generated primary key
    private String name;          // Library branch name (e.g., "Sacred Heart")
    private String hostname;      // Hostname for this branch
    private List<Book> books;     // One-to-many relationship with books
}
```

**Fields**:
- `id`: Unique identifier (auto-generated)
- `name`: Display name of the library branch
- `hostname`: Server hostname where this library instance runs
- `books`: Collection of all books belonging to this library

## API Endpoints

**Base Path**: `/api/libraries`

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/libraries` | Get all libraries |
| GET | `/api/libraries/{id}` | Get library by ID |

### Librarian-Only Endpoints

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| POST | `/api/libraries` | Create new library | `LIBRARIAN` |
| PUT | `/api/libraries/{id}` | Update library | `LIBRARIAN` |
| DELETE | `/api/libraries/{id}` | Delete library | `LIBRARIAN` |

**Controller**: `src/main/java/com/muczynski/library/controller/LibraryController.java`

## User Interface

**Location**: `index.html` lines 130-270
**JavaScript**: `src/main/resources/static/js/libraries.js`

### Page Structure

The Libraries page consists of four main sections:

#### 1. Library List (Lines 131-132)
- Table displaying all library branches
- Shows: Library name, hostname, and action buttons
- **Columns**:
  - Library: `name (hostname)`
  - Actions: Edit (✏️) and Delete (🗑️) buttons (librarian-only)

#### 2. Add/Edit Library Form (Lines 133-140)
- **Visibility**: Librarian-only
- **Fields**:
  - Library Name (text input)
  - Hostname (text input)
- **Actions**:
  - "Add Library" button (changes to "Update Library" in edit mode)
- **Behavior**:
  - Single form handles both create and update operations
  - Edit mode: Clicking edit button populates form and changes button to "Update Library"

#### 3. JSON Import/Export Section (Lines 142-169)
**CRITICAL**: Essential for database backup and restore operations

- **Visibility**: Librarian-only
- **Export Card**:
  - Exports entire database to JSON file
  - Includes: libraries, authors, books, loans, users, photo metadata
  - **Note**: Photo image bytes NOT exported (only metadata)
  - **Filename Format**: `{library-name}-{book-count}-books-{author-count}-authors.json`
  - **Button**: "Export Database to JSON"

- **Import Card**:
  - Imports database from JSON file
  - File input accepts `.json` files
  - **Note**: Photo image bytes NOT imported (must be re-downloaded from Google Photos)
  - **Button**: "Import JSON to Database"

**Why This Section Exists Here**:
- Libraries page is the "root" administrative page
- Natural place for database-wide operations
- Librarians expect backup/restore near system configuration

**Implementation Note**: This section was previously dynamically injected via `setupImportUI()` in JavaScript but is now statically defined in HTML for better visibility and maintainability. See comment in `libraries.js` lines 233-236.

#### 4. Photo Import/Export Status (Lines 172-239)
- **Visibility**: Librarian-only
- **Purpose**: Monitor Google Photos integration for all photos in the library
- **Features**:
  - Album name display (auto-created on first export)
  - Statistics dashboard (6 cards):
    - Total Photos
    - Exported (to Google Photos)
    - Imported (from Google Photos)
    - Pending Export
    - Pending Import
    - Failed
  - Bulk action buttons:
    - "Export All Pending Photos"
    - "Import All Pending Photos"
    - "Refresh Status"
  - Photo details table with columns:
    - Photo thumbnail
    - Title/Author
    - LOC Call Number (formatted for spine)
    - Status (badge: Completed, Failed, In Progress, Pending)
    - Exported At (timestamp)
    - Permanent ID (Google Photos ID, truncated)
    - Actions (Export, Import, View, Verify, Unlink, Delete)

## JavaScript Functions

**File**: `src/main/resources/static/js/libraries.js`

### Library CRUD Operations

| Function | Purpose |
|----------|---------|
| `loadLibraries()` | Fetches and displays all libraries; updates page title |
| `addLibrary()` | Creates new library from form data |
| `editLibrary(id)` | Populates form with library data for editing |
| `updateLibrary(id)` | Updates existing library |
| `deleteLibrary(id)` | Deletes library with confirmation |

### JSON Import/Export

| Function | Purpose |
|----------|---------|
| `exportJson()` | Exports entire database to JSON file with smart filename |
| `importJson()` | Imports database from uploaded JSON file |

**Export Process**:
1. Fetches libraries, books, and authors counts
2. Sanitizes library name for filename (lowercase, dashes only)
3. Creates filename: `{library}-{books}-books-{authors}-authors.json`
4. Downloads JSON file to browser

**Import Process**:
1. Reads selected file
2. Parses JSON
3. Validates structure
4. POSTs to `/api/import/json`
5. Refreshes libraries list
6. Shows success alert

### Photo Management Functions

**File**: `src/main/resources/static/js/photos.js`

| Function | Purpose |
|----------|---------|
| `loadPhotoExportStatus()` | Loads statistics and photo table |
| `exportAllPhotos()` | Exports all pending photos to Google Photos |
| `importAllPhotos()` | Imports all pending photos from Google Photos |
| `exportSinglePhoto(id)` | Exports individual photo |
| `importSinglePhoto(id)` | Imports individual photo |
| `verifyPhoto(id)` | Verifies Google Photos permanent ID still works |
| `unlinkPhoto(id)` | Removes permanent ID from photo |
| `deletePhotoWithUndo(id)` | Soft deletes photo with undo option |
| `updatePhotoRow(id)` | Updates single table row without full reload |

## Security Model

### Role-Based Access Control

| Role | Permissions |
|------|-------------|
| **Public/Unauthenticated** | View libraries list |
| **USER** | View libraries list |
| **LIBRARIAN** | Full CRUD access + Import/Export + Photo management |

### Implementation

- **Frontend**: Elements with class `librarian-only` are hidden for non-librarians
- **Backend**: `@PreAuthorize("hasAuthority('LIBRARIAN')")` on write endpoints
- **API**: Public read access via `@PreAuthorize("permitAll()")`

## Page Title Behavior

The navbar page title dynamically updates based on library configuration:

- **With Libraries**: `"The {library.name} Branch"` + `"of the Sacred Heart Library System"`
- **Without Libraries**: `"Library Management"`

**Implementation**: `loadLibraries()` lines 55-65

## Data Flow

### Library CRUD Flow
```
User Action → Form Submit → JavaScript Function → API Endpoint
→ Service Layer → Repository → Database → Response
→ Refresh UI (loadLibraries())
```

### JSON Export Flow
```
Export Button → exportJson() → Parallel Fetch [libraries, books, authors]
→ Generate Filename → Fetch /api/import/json
→ Create Blob → Download File
```

### JSON Import Flow
```
Select File → importJson() → Read File → Parse JSON
→ Validate → POST /api/import/json
→ Refresh UI → Alert Success
```

### Photo Export Flow
```
Export Button → exportAllPhotos() → Filter Pending Photos
→ Confirm → Loop through photos → POST /api/photo-export/export/{id}
→ Update Row (no full reload) → Show Stats
```

## Integration Points

### Book Management
- Books belong to libraries (foreign key: `book.library_id`)
- Deleting a library requires handling associated books
- Book form dropdowns populated via `populateBookDropdowns()`

### Google Photos Integration
- Photos exported to album named after library
- Album ID stored in user settings: `googlePhotosAlbumId`
- Permanent IDs allow re-importing photos without storing image bytes
- Export/Import status tracked per photo

### Test Data Generation
- Test data page can generate libraries
- Used for development and demos

## Important Patterns

### UI Update Pattern
After any CRUD operation:
```javascript
await loadLibraries();           // Refresh libraries list
await populateBookDropdowns();   // Update book form dropdowns
```

### Edit Mode Toggle Pattern
```javascript
// Add mode
btn.textContent = 'Add Library';
btn.onclick = addLibrary;

// Edit mode
btn.textContent = 'Update Library';
btn.onclick = () => updateLibrary(id);
```

### Button Spinner Pattern
```javascript
showButtonSpinner(btn, 'Loading...');
try {
    // Operation
} finally {
    hideButtonSpinner(btn, 'Original Text');
}
```

### Photo Row Update Pattern
Instead of reloading entire table:
```javascript
await updatePhotoRow(photoId);  // Updates single row in place
```

## Known Limitations

1. **Single Library Mode**: While the architecture supports multiple branches, the current deployment uses only one library
2. **Hostname Field**: Currently set to `window.location.hostname` but could support branch-specific URLs
3. **Photo Bytes Not Exported**: JSON export excludes photo image data (only metadata)
4. **No Library Deletion Cascade UI**: Backend may prevent deletion if library has books, but UI doesn't warn beforehand

## Best Practices

### When Adding Features to Libraries Page

1. **CRUD Operations**: Follow the standard Edit (✏️) / Delete (🗑️) button pattern
2. **Librarian-Only Elements**: Add `librarian-only` class to restrict access
3. **Data Tests**: Add `data-test` attributes for Playwright tests
4. **Error Handling**: Use `showError('libraries', message)` for user feedback
5. **Button Spinners**: Always show loading state during async operations
6. **Import/Export Section**: **DO NOT REMOVE** - Essential for backups (see HTML comment lines 142-147)

### Photo Management

1. **Row Updates**: Use `updatePhotoRow(id)` instead of full table reload for better UX
2. **Batch Operations**: Show progress in button text: "Exporting 3/10..."
3. **Undo Pattern**: Soft delete with strikethrough + undo button
4. **Status Badges**: Use Bootstrap badge colors (success, danger, warning, info)

## File References

### Backend
- `src/main/java/com/muczynski/library/domain/Library.java` - Entity
- `src/main/java/com/muczynski/library/controller/LibraryController.java` - REST endpoints
- `src/main/java/com/muczynski/library/service/LibraryService.java` - Business logic
- `src/main/java/com/muczynski/library/dto/LibraryDto.java` - Data transfer object
- `src/main/java/com/muczynski/library/mapper/LibraryMapper.java` - MapStruct mapper

### Frontend
- `src/main/resources/static/index.html` (lines 130-270) - Page structure
- `src/main/resources/static/js/libraries.js` - Library CRUD + JSON import/export
- `src/main/resources/static/js/photos.js` - Photo export/import management
- `src/main/resources/static/js/utils.js` - Shared utilities (error handling, spinners, LOC formatting)

## Testing

### UI Tests
- **Location**: `src/test/java/com/muczynski/library/ui/` (Playwright)
- **Pattern**: Use `data-test` attributes for element selection
- **Test Coverage**:
  - Library CRUD operations
  - JSON export/import
  - Photo export/import workflows
  - Role-based access control

### Integration Tests
- **Location**: `src/test/java/com/muczynski/library/controller/LibraryControllerTest.java`
- **Coverage**: All API endpoints with different roles

## Future Enhancements

1. **Multi-Branch Support**: UI for switching between library branches
2. **Hostname Management**: Better configuration for branch-specific URLs
3. **Photo Byte Export**: Option to include full photo data in JSON export
4. **Batch Library Operations**: Import/export library configurations separately
5. **Library-Specific Settings**: Per-branch configuration (hours, contact info, etc.)
6. **Cascade Delete Warning**: Show book count before allowing library deletion
7. **Photo Album Auto-Creation**: One-click album setup per library
