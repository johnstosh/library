# Libraries Feature Design

## Overview

The Libraries page provides management of library branches in the system. Each library represents a physical location that can house books. The page displays library information with statistics on books and active loans.

## Purpose

- **Library Management**: Create, read, update, and delete library branches
- **Multi-Branch Support**: While the system currently operates as a single-branch library, the architecture supports multiple branches
- **Statistics Display**: View book counts and active loan counts per library

## Domain Model

### Library Entity

**File**: `src/main/java/com/muczynski/library/domain/Library.java`

```java
@Entity
public class Library {
    private Long id;              // Auto-generated primary key
    private String name;          // Library branch name (e.g., "Sacred Heart")
    private String hostname;      // Hostname for this branch
}
```

**Fields**:
- `id`: Unique identifier (auto-generated)
- `name`: Display name of the library branch
- `hostname`: Server hostname where this library instance runs

**Note**: Books have a `@ManyToOne` relationship to Library (via `book.library_id` foreign key), but Library does not maintain a collection of books. Book counts are queried directly via `bookRepository.countByLibraryId(libraryId)`.

## API Endpoints

**Base Path**: `/api/libraries`

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/libraries` | Get all libraries |
| GET | `/api/libraries/{id}` | Get library by ID |
| GET | `/api/libraries/statistics` | Get library statistics (books count, active loans count) |

### Librarian-Only Endpoints

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| POST | `/api/libraries` | Create new library | `LIBRARIAN` |
| PUT | `/api/libraries/{id}` | Update library | `LIBRARIAN` |
| DELETE | `/api/libraries/{id}` | Delete library | `LIBRARIAN` |

**Controller**: `src/main/java/com/muczynski/library/controller/LibraryController.java`

## User Interface

**Location**: `frontend/src/pages/libraries/LibrariesPage.tsx`
**Technology**: React 18+ with TypeScript, TanStack Query, Tailwind CSS

### Page Structure

The Libraries page is a React component that displays:

#### 1. Library Table
- DataTable component displaying all library branches
- **Columns**:
  - Name: Library branch name
  - Hostname: Server hostname
  - Books: Count of books in this library (from statistics)
  - Active Loans: Count of active loans for books in this library (from statistics)
  - Actions: Edit and Delete buttons (librarian-only)

#### 2. Add/Edit Library Modal
- **Visibility**: Modal-based form (opens when "Add Library" clicked or edit button clicked)
- **Fields**:
  - Library Name (required text input)
  - Hostname (required text input with help text)
- **Actions**:
  - Cancel button
  - Create/Update button (changes based on mode)
- **Behavior**:
  - Single form handles both create and update operations
  - Edit mode: Clicking edit button opens modal with populated form

#### 3. Delete Confirmation Dialog
- **Component**: ConfirmDialog
- **Behavior**:
  - Opens when delete button clicked
  - Shows warning message
  - Requires explicit confirmation

## React Implementation

### State Management
- **Server State**: TanStack Query hooks for API calls
  - `useLibraries()`: Fetches all libraries
  - `useLibraryStatistics()`: Fetches statistics (book counts, active loan counts)
  - `useCreateLibrary()`: Creates new library
  - `useUpdateLibrary()`: Updates existing library
  - `useDeleteLibrary()`: Deletes library
- **Local State**: useState for form data, modals, error messages

### Key React Hooks

| Hook | Purpose | File |
|------|---------|------|
| `useLibraries()` | Fetches and caches all libraries | `frontend/src/api/libraries.ts` |
| `useLibraryStatistics()` | Fetches library statistics (books, active loans) | `frontend/src/api/libraries.ts` |
| `useCreateLibrary()` | Mutation for creating library | `frontend/src/api/libraries.ts` |
| `useUpdateLibrary()` | Mutation for updating library | `frontend/src/api/libraries.ts` |
| `useDeleteLibrary()` | Mutation for deleting library | `frontend/src/api/libraries.ts` |

### Components Used
- `DataTable`: Reusable table component with sorting and actions
- `Modal`: Modal dialog for add/edit forms
- `ConfirmDialog`: Confirmation dialog for delete operations
- `Button`: Styled button with loading states
- `Input`: Form input with label and validation
- `ErrorMessage`: Error display component

## Security Model

### Role-Based Access Control

| Role | Permissions |
|------|-------------|
| **Public/Unauthenticated** | View libraries list and statistics |
| **USER** | View libraries list and statistics |
| **LIBRARIAN** | Full CRUD access |

### Implementation

- **Frontend**:
  - Add/Edit/Delete buttons only shown to librarians (handled by protected routes)
  - Public users can view library information
- **Backend**:
  - `@PreAuthorize("hasAuthority('LIBRARIAN')")` on write endpoints
  - `@PreAuthorize("permitAll()")` on read endpoints

## Data Flow

### Library CRUD Flow
```
User Action → React Event Handler → TanStack Query Mutation
→ API Endpoint → Service Layer → Repository → Database
→ TanStack Query Cache Update → React Re-render
```

### Statistics Loading Flow
```
Component Mount → useLibraryStatistics() Hook → Parallel Fetch
→ Cache Statistics → Display in Table Columns
```

### Form Submission Flow
```
Form Submit → handleSubmit() → Validation → Create/Update Mutation
→ API Call → Success → Close Modal → TanStack Query Refetch
→ Table Updates Automatically
```

## Integration Points

### Book Management
- Books belong to libraries (foreign key: `book.library_id`)
- Deleting a library requires handling associated books
- Statistics show book count per library

### Loan Management
- Active loans count is calculated per library
- Uses query: `countByBookLibraryIdAndReturnDateIsNull(libraryId)`
- Shows librarians how many books are currently checked out

### Data Management
- JSON Import/Export functionality is on separate **Data Management** page
- Photo Export functionality is on separate **Data Management** page
- See `frontend/src/pages/libraries/DataManagementPage.tsx`

### Test Data Generation
- Test data page can generate libraries
- Used for development and demos

## Important Patterns

### TanStack Query Pattern
All data fetching uses TanStack Query for automatic caching and refetching:
```typescript
const { data: libraries = [], isLoading } = useLibraries()
const createLibrary = useCreateLibrary()
await createLibrary.mutateAsync(formData)
// TanStack Query automatically refetches and updates UI
```

### Modal Form Pattern
```typescript
// Open modal in add mode
setEditingLibrary(null)
setFormData({ name: '', hostname: '' })
setShowForm(true)

// Open modal in edit mode
setEditingLibrary(library)
setFormData({ name: library.name, hostname: library.hostname })
setShowForm(true)
```

### Statistics Integration Pattern
```typescript
// Statistics fetched separately
const { data: statistics = [] } = useLibraryStatistics()

// Looked up per library in table cell
const stats = statistics.find((s) => s.libraryId === library.id)
const bookCount = stats?.bookCount
const activeLoansCount = stats?.activeLoansCount
```

## Known Limitations

1. **Single Library Mode**: While the architecture supports multiple branches, the current deployment uses only one library
2. **Hostname Field**: Currently set manually but could support branch-specific URLs
3. **No Library Deletion Cascade UI**: Backend may prevent deletion if library has books, but UI doesn't warn beforehand
4. **Statistics Update Timing**: Statistics refetch on every page load; no real-time updates

## Best Practices

### When Adding Features to Libraries Page

1. **CRUD Operations**: Follow the standard TanStack Query mutation pattern
2. **Librarian-Only Elements**: Handle via protected routes, not UI conditionals
3. **Data Tests**: Add `data-test` attributes for Playwright tests (already present)
4. **Error Handling**: Use ErrorMessage component and try/catch in handlers
5. **Loading States**: Use `isLoading` from queries and `isPending` from mutations
6. **TypeScript**: All props and state must be properly typed

### TypeScript Patterns

1. **DTOs**: Import types from `frontend/src/types/dtos.ts`
2. **Props**: Define inline or as separate type/interface
3. **Event Handlers**: Use proper React event types (`React.FormEvent`, etc.)
4. **Null Checks**: Handle undefined data from queries with default values

## File References

### Backend
- `src/main/java/com/muczynski/library/domain/Library.java` - Entity
- `src/main/java/com/muczynski/library/controller/LibraryController.java` - REST endpoints
- `src/main/java/com/muczynski/library/service/LibraryService.java` - Business logic
- `src/main/java/com/muczynski/library/dto/LibraryDto.java` - Data transfer object
- `src/main/java/com/muczynski/library/dto/LibraryStatisticsDto.java` - Statistics DTO
- `src/main/java/com/muczynski/library/mapper/LibraryMapper.java` - MapStruct mapper
- `src/main/java/com/muczynski/library/repository/LibraryRepository.java` - JPA repository
- `src/main/java/com/muczynski/library/repository/LoanRepository.java` - Loan queries (for statistics)

### Frontend
- `frontend/src/pages/libraries/LibrariesPage.tsx` - Main page component
- `frontend/src/pages/libraries/DataManagementPage.tsx` - JSON/Photo import/export
- `frontend/src/api/libraries.ts` - TanStack Query hooks
- `frontend/src/types/dtos.ts` - TypeScript DTO interfaces

### Testing
- `src/test/java/com/muczynski/library/controller/LibraryControllerTest.java` - Integration tests

## Testing

### Backend Integration Tests
- **Location**: `src/test/java/com/muczynski/library/controller/LibraryControllerTest.java`
- **Coverage**:
  - All CRUD endpoints
  - Statistics endpoint
  - Role-based access control

### Frontend UI Tests
- **Location**: `src/test/java/com/muczynski/library/ui/` (Playwright)
- **Pattern**: Use `data-test` attributes for element selection
- **Coverage Needed**:
  - Library CRUD operations
  - Statistics display
  - Role-based access control

## Future Enhancements

1. **Multi-Branch Support**: UI for switching between library branches
2. **Hostname Management**: Better configuration for branch-specific URLs
3. **Cascade Delete Warning**: Show book count before allowing library deletion
4. **Library-Specific Settings**: Per-branch configuration (hours, contact info, etc.)
5. **Real-time Statistics**: WebSocket updates for active loan counts
6. **Statistics History**: Track book and loan counts over time
