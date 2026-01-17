# Frontend Architecture

## Overview
The frontend is a modern single-page application (SPA) built with React 18, TypeScript, and Tailwind CSS. Migrated from vanilla JavaScript (Dec 2024) to improve maintainability, type safety, and developer experience.

## Technology Stack

### Core Framework
- **React 18.3** - Component-based UI library with hooks
- **TypeScript 5.3** - Type-safe JavaScript (strict mode)
- **Vite 7.3** - Fast build tool and dev server

### Styling
- **Tailwind CSS v4** - Utility-first CSS framework
- **Headless UI** - Unstyled, accessible components (modals, dialogs)

### State & Data Management
- **TanStack Query v5** - Server state management with automatic caching
- **Zustand** - Lightweight client state management
- **date-fns** - Date formatting and manipulation

### Routing
- **React Router v6** - Client-side routing with protected routes

### Build & Development
- **Vite** - Development server with Hot Module Replacement (HMR)
- **ESLint** - Code linting
- **Prettier** - Code formatting
- **TypeScript** - Type checking

## Architecture Overview

### Application Structure

```
frontend/src/
├── main.tsx                 # Application entry point
├── App.tsx                  # Root component with routing
├── index.css                # Global styles (Tailwind imports)
│
├── api/                     # API layer
│   ├── client.ts            # Fetch wrapper with auth
│   ├── books.ts             # Book API + React Query hooks
│   ├── authors.ts           # Author API + hooks
│   ├── branches.ts         # Branch API + hooks
│   ├── loans.ts             # Loan API + hooks
│   ├── users.ts             # User API + hooks
│   ├── photos.ts            # Photo API + hooks
│   ├── library-cards.ts     # Library card API + hooks
│   ├── labels.ts            # Label generation API + hooks
│   ├── loc-lookup.ts        # LOC lookup API + hooks
│   ├── books-from-feed.ts   # Google Photos import API + hooks
│   ├── search.ts            # Search API + hooks
│   ├── settings.ts          # Settings API + hooks
│   ├── test-data.ts         # Test data API + hooks
│   └── data-management.ts   # Import/Export API + hooks
│
├── stores/                  # Zustand stores
│   ├── authStore.ts         # Authentication state
│   └── uiStore.ts           # UI state (filters, selections)
│
├── hooks/                   # Custom React hooks
│   └── (future: useDebounce, useLocalStorage, etc.)
│
├── components/              # Reusable components
│   ├── layout/
│   │   ├── AppLayout.tsx    # Main layout wrapper
│   │   └── Navigation.tsx   # Top navigation menu
│   │
│   ├── ui/                  # Basic UI components
│   │   ├── Button.tsx       # Styled button variants
│   │   ├── Input.tsx        # Form input
│   │   ├── Select.tsx       # Select dropdown
│   │   ├── Textarea.tsx     # Textarea input
│   │   ├── Checkbox.tsx     # Checkbox input
│   │   ├── Modal.tsx        # Modal dialog
│   │   ├── ConfirmDialog.tsx # Confirmation modal
│   │   ├── ErrorMessage.tsx  # Error display
│   │   └── SuccessMessage.tsx # Success display
│   │
│   ├── table/
│   │   └── DataTable.tsx    # Generic table component
│   │
│   ├── progress/
│   │   ├── Spinner.tsx      # Loading spinner
│   │   └── ProgressBar.tsx  # Progress bar
│   │
│   ├── photos/
│   │   ├── PhotoGallery.tsx    # Photo grid display
│   │   ├── PhotoSection.tsx    # Photo management section
│   │   └── PhotoUploadModal.tsx # Photo upload interface
│   │
│   ├── auth/
│   │   ├── ProtectedRoute.tsx   # Authenticated route wrapper
│   │   └── LibrarianRoute.tsx   # Librarian-only route wrapper
│   │
│   └── errors/
│       └── ErrorBoundary.tsx    # Error boundary component
│
├── pages/                   # Page components (routes)
│   ├── LoginPage.tsx        # Login page
│   ├── NotFoundPage.tsx     # 404 page
│   │
│   ├── books/               # Books feature
│   │   ├── BooksPage.tsx    # Books list page
│   │   ├── BookNewPage.tsx  # Create book page
│   │   ├── BookEditPage.tsx # Edit book page
│   │   ├── BookViewPage.tsx # View book details page
│   │   └── components/
│   │       ├── BookFilters.tsx          # Filter radio buttons
│   │       ├── BookTable.tsx            # Books table
│   │       ├── BookFormPage.tsx         # Create/Edit form (page-based)
│   │       ├── BulkActionsToolbar.tsx   # Bulk operations (LOC, Grokipedia, Labels, Book from Images, Delete)
│   │       ├── LocLookupResultsModal.tsx # LOC lookup results
│   │       └── BookFromImageResultsModal.tsx # Book from Images results
│   │
│   ├── authors/             # Authors feature
│   │   ├── AuthorsPage.tsx   # Authors list page
│   │   ├── AuthorNewPage.tsx # Create author page
│   │   ├── AuthorEditPage.tsx # Edit author page
│   │   ├── AuthorViewPage.tsx # View author details page
│   │   └── components/
│   │       ├── AuthorFilters.tsx
│   │       ├── AuthorTable.tsx
│   │       └── AuthorFormPage.tsx       # Create/Edit form (page-based)
│   │
│   ├── libraries/           # Libraries feature
│   │   ├── LibrariesPage.tsx  # Libraries list page
│   │   ├── LibraryNewPage.tsx # Create library page
│   │   ├── LibraryEditPage.tsx # Edit library page
│   │   ├── LibraryViewPage.tsx # View library details page
│   │   ├── DataManagementPage.tsx
│   │   └── components/
│   │       └── LibraryFormPage.tsx      # Create/Edit form (page-based)
│   │
│   ├── loans/               # Loans feature
│   │   ├── LoansPage.tsx     # Loans list page
│   │   ├── LoanNewPage.tsx   # Checkout book page
│   │   ├── LoanEditPage.tsx  # View loan (read-only)
│   │   ├── LoanViewPage.tsx  # View loan details page
│   │   └── components/
│   │       └── LoanFormPage.tsx         # Checkout form (page-based)
│   │
│   ├── users/               # Users feature
│   │   ├── UsersPage.tsx     # Users list page
│   │   ├── UserNewPage.tsx   # Create user page
│   │   ├── UserEditPage.tsx  # Edit user page
│   │   ├── UserViewPage.tsx  # View user details page
│   │   └── components/
│   │       ├── UserTable.tsx
│   │       └── UserFormPage.tsx         # Create/Edit form (page-based)
│   │
│   ├── library-cards/       # Library card feature
│   │   ├── MyLibraryCardPage.tsx
│   │   ├── ApplyForCardPage.tsx
│   │   └── ApplicationsPage.tsx
│   │
│   ├── labels/              # Label generation
│   │   └── LabelsPage.tsx
│   │
│   ├── search/              # Search feature
│   │   └── SearchPage.tsx
│   │
│   ├── settings/            # Settings pages
│   │   ├── UserSettingsPage.tsx
│   │   └── GlobalSettingsPage.tsx
│   │
│   ├── books-from-feed/     # Google Photos import
│   │   ├── BooksFromFeedPage.tsx
│   │   └── components/
│   │       ├── PhotoPickerModal.tsx
│   │       ├── SavedBooksTable.tsx
│   │       └── ProcessingResultsModal.tsx
│   │
│   └── test-data/           # Test data generation
│       └── TestDataPage.tsx
│
├── utils/                   # Utility functions
│   ├── formatters.ts        # Date, number, text formatting
│   ├── auth.ts              # Auth helpers (hash password)
│   └── constants.ts         # App constants
│
├── types/                   # TypeScript types
│   ├── dtos.ts              # DTO types matching backend
│   └── enums.ts             # Enums (BookStatus, etc.)
│
└── config/                  # Configuration
    ├── routes.ts            # Route definitions
    └── queryClient.ts       # TanStack Query configuration
```

## Core Patterns

### 1. API Layer Pattern

All API interactions use TanStack Query for automatic caching, loading states, and error handling:

```typescript
// api/books.ts
export function useBooks(filter?: string) {
  return useQuery({
    queryKey: queryKeys.books.list(filter),
    queryFn: () => api.get<BookDto[]>(`/books?filter=${filter}`),
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}

export function useCreateBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (book: Partial<BookDto>) => api.post<BookDto>('/books', book),
    onSuccess: () => {
      queryClient.invalidateQueries(queryKeys.books.all)
    },
  })
}
```

### 2. Component Pattern

**Page Components:**
- Handle routing and navigation
- Fetch data with React Query hooks
- Render child components
- Manage page-level state

**Presentational Components:**
- Receive data via props
- Render UI elements
- Emit events via callbacks

Example:
```typescript
// BooksPage.tsx (Page Component - List View)
export function BooksPage() {
  const navigate = useNavigate()
  const { data: books, isLoading } = useBooks(filter)

  const handleAddBook = () => navigate('/books/new')
  const handleViewBook = (book: BookDto) => navigate(`/books/${book.id}`)

  return (
    <div>
      <h1>Books</h1>
      <Button onClick={handleAddBook}>Add Book</Button>
      <BookFilters />
      <BookTable books={books} isLoading={isLoading} onView={handleViewBook} />
    </div>
  )
}

// BookViewPage.tsx (Page Component - Detail View)
export function BookViewPage() {
  const navigate = useNavigate()
  const { id } = useParams()
  const { data: book, isLoading } = useBook(parseInt(id))

  const handleEdit = () => navigate(`/books/${id}/edit`)
  const handleBack = () => navigate('/books')

  if (isLoading) return <Spinner />
  if (!book) return <NotFound />

  return (
    <div>
      <Button onClick={handleBack}>Back</Button>
      <h1>{book.title}</h1>
      <Button onClick={handleEdit}>Edit</Button>
      {/* Book details */}
    </div>
  )
}

// BookTable.tsx (Presentational Component)
interface BookTableProps {
  books: BookDto[]
  isLoading: boolean
  onView: (book: BookDto) => void
}

export function BookTable({ books, isLoading, onView }: BookTableProps) {
  return (
    <DataTable
      data={books}
      columns={columns}
      onRowClick={onView}
    />
  )
}
```

### 3. Routing Pattern

Protected routes with authentication and role-based access. All CRUD operations use URL-based routing (not modals):

```typescript
<Routes>
  {/* Public routes */}
  <Route path="/login" element={<LoginPage />} />
  <Route path="/search" element={<SearchPage />} />

  {/* Protected routes (authenticated users) */}
  <Route element={<ProtectedRoute />}>
    <Route element={<AppLayout />}>
      <Route path="/" element={<Navigate to="/books" replace />} />

      {/* Books - URL-based CRUD */}
      <Route path="/books" element={<BooksPage />} />
      <Route path="/books/new" element={<BookNewPage />} />
      <Route path="/books/:id" element={<BookViewPage />} />
      <Route path="/books/:id/edit" element={<BookEditPage />} />

      {/* Authors - URL-based CRUD */}
      <Route path="/authors" element={<AuthorsPage />} />
      <Route path="/authors/new" element={<AuthorNewPage />} />
      <Route path="/authors/:id" element={<AuthorViewPage />} />
      <Route path="/authors/:id/edit" element={<AuthorEditPage />} />

      {/* Loans - URL-based CRUD */}
      <Route path="/loans" element={<LoansPage />} />
      <Route path="/loans/new" element={<LoanNewPage />} />
      <Route path="/loans/:id" element={<LoanViewPage />} />
      <Route path="/loans/:id/edit" element={<LoanEditPage />} />

      <Route path="/my-card" element={<MyLibraryCardPage />} />
      <Route path="/settings" element={<UserSettingsPage />} />

      {/* Librarian-only routes */}
      <Route element={<LibrarianRoute />}>
        {/* Users - URL-based CRUD */}
        <Route path="/users" element={<UsersPage />} />
        <Route path="/users/new" element={<UserNewPage />} />
        <Route path="/users/:id" element={<UserViewPage />} />
        <Route path="/users/:id/edit" element={<UserEditPage />} />

        {/* Libraries - URL-based CRUD */}
        <Route path="/libraries" element={<LibrariesPage />} />
        <Route path="/libraries/new" element={<LibraryNewPage />} />
        <Route path="/libraries/:id" element={<LibraryViewPage />} />
        <Route path="/libraries/:id/edit" element={<LibraryEditPage />} />

        <Route path="/data-management" element={<DataManagementPage />} />
        <Route path="/applications" element={<ApplicationsPage />} />
        <Route path="/books-from-feed" element={<BooksFromFeedPage />} />
        <Route path="/global-settings" element={<GlobalSettingsPage />} />
        <Route path="/test-data" element={<TestDataPage />} />
      </Route>
    </Route>
  </Route>
</Routes>
```

**URL-Based CRUD Pattern:**
- List: `/entity` - Table view with filters
- Create: `/entity/new` - Form to create new entity
- View: `/entity/:id` - Read-only view with action buttons
- Edit: `/entity/:id/edit` - Form to edit existing entity

Benefits of URL-based routing:
- Bookmarkable URLs for specific items
- Browser back/forward navigation works naturally
- Deep linking support
- Better accessibility
- Clearer navigation history

### Navigation Menu Access

The navigation menu shows different items based on authentication status:

**Unauthenticated Users (Public):**
- Search (search library)
- Login (sign in button)

**Authenticated Users:**
- Search (same as public)
- Books (browse catalog)
- Authors (browse authors)
- Loans (view checkout history)
- Settings (manage account)
- My Card (view library card)
- User info and Logout (right side of header)

**Librarians Only:**
- All authenticated user items, plus:
- Libraries, Users, Applications
- Books from Feed, Data Management
- Global Settings, Test Data

**User Menu Layout (Desktop):**
The user menu is positioned on the right side of the navigation header. User information (username, SSO badge, Librarian badge) is displayed above the Logout button in a vertical stack, right-aligned:
```
        [username] [SSO] [Librarian]
                          [Logout]
```

**User Menu Layout (Mobile):**
On mobile, the user info and Logout button appear in the expandable mobile menu, with username and badges displayed above the Logout button, separated by a border from the navigation links.

### 4. State Management Pattern

**Three-Tier State:**

1. **Server State** (TanStack Query)
   - Data from API endpoints
   - Automatic caching and invalidation
   - Loading and error states

2. **Client State** (Zustand)
   - Authentication status
   - UI state (filters, selected rows)
   - Persisted preferences

3. **Component State** (useState)
   - Form inputs
   - Modal visibility
   - Local UI state

Example:
```typescript
// Server state
const { data: books } = useBooks(filter)

// Client state (Zustand)
const { filter, setFilter } = useUiStore()

// Component state
const [showModal, setShowModal] = useState(false)
```

### 5. Form Pattern

Forms are now page-based (not modals) with unsaved changes warnings:

```typescript
interface BookFormPageProps {
  title: string
  book?: BookDto
  onSuccess: () => void
  onCancel: () => void
}

export function BookFormPage({ title, book, onSuccess, onCancel }: BookFormPageProps) {
  const isEditing = !!book
  const [formData, setFormData] = useState({
    title: '',
    authorId: '',
    libraryId: '',
  })
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)

  const createBook = useCreateBook()
  const updateBook = useUpdateBook()

  // Warn user about unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges) {
        e.preventDefault()
        e.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges])

  const handleFieldChange = (field: string, value: string) => {
    setFormData({ ...formData, [field]: value })
    setHasUnsavedChanges(true)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const mutation = isEditing ? updateBook : createBook
    await mutation.mutateAsync(formData)
    setHasUnsavedChanges(false)
    onSuccess()
  }

  const handleCancel = () => {
    if (hasUnsavedChanges) {
      if (window.confirm('You have unsaved changes. Are you sure you want to leave?')) {
        onCancel()
      }
    } else {
      onCancel()
    }
  }

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-6 py-4 border-b">
        <h1>{title}</h1>
      </div>
      <form onSubmit={handleSubmit} className="px-6 py-6 space-y-4">
        <Input
        label="Title"
        value={formData.title}
        onChange={(e) => setFormData({ ...formData, title: e.target.value })}
        required
      />
      {/* ... */}
    </form>
  )
}
```

### 6. Table Pattern

All tables use a consistent styling approach with fixed layout and percentage-based column widths:

**Standard Table Configuration:**

```typescript
// Using DataTable component
const columns: Column<BookDto>[] = [
  {
    key: 'title',
    header: 'Title',
    accessor: (book) => book.title,
    width: '45%',  // Percentage-based width
  },
  {
    key: 'status',
    header: 'Status',
    accessor: (book) => book.status,
    width: '15%',
  },
]
```

**Key Principles:**

1. **Fixed Table Layout** - All tables use `table-fixed` class for consistent column widths
2. **Percentage Widths** - Column widths defined as percentages, not content-based
3. **Text Truncation** - Long text truncated with ellipsis (`overflow-hidden truncate`)
4. **Standard Allocations:**
   - Checkbox column (if present): 5%
   - Actions column: 15%
   - Content columns: Distribute remaining 75-80%
   - Prioritize space for longer text fields (titles, names, biographies)

**Implementation:**

- `DataTable.tsx` uses `table-fixed` class and handles overflow automatically
- Content cells use `overflow-hidden truncate` classes
- Action cells use `whitespace-nowrap` to prevent button wrapping
- Custom tables follow the same pattern

**Benefits:**

- Consistent column widths regardless of content length
- No text wrapping (cleaner appearance)
- Predictable table layout
- Better performance (fixed layout is faster to render)

## Performance Optimizations

### 1. Code Splitting

All page components are lazy-loaded to reduce initial bundle size:

```typescript
const BooksPage = lazy(() =>
  import('@/pages/books/BooksPage').then(m => ({ default: m.BooksPage }))
)
```

**Results:**
- Initial bundle: 275 KB (down from 544 KB - 49% reduction)
- 34 optimized chunks loaded on demand

### 2. Query Caching

TanStack Query automatically caches API responses:

```typescript
queryClient: new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,      // 5 minutes
      gcTime: 1000 * 60 * 30,        // 30 minutes
      refetchOnWindowFocus: false,
    },
  },
})
```

### 3. Photo Caching

Photo thumbnails cached in IndexedDB (planned enhancement).

## Authentication & Authorization

### Authentication Flow

1. **Login**: User submits credentials (password hashed client-side with SHA-256)
2. **Session**: Server creates cookie-based session
3. **Auth Check**: `useAuthStore.checkAuth()` on app load
4. **Protected Routes**: `ProtectedRoute` component checks auth status
5. **Logout**: Clear session and redirect to login

### Authorization Levels

- **PUBLIC (Unauthenticated)**: Access to Search, Apply for Card
- **USER**: Public access + Books, Authors, Loans (own loans only), Settings, My Card (download card)
- **LIBRARIAN**: Full access including Users, Libraries, Applications, Books from Feed, Data Management, Global Settings, Test Data
  - **Loans**: Librarians see all loans from all users with "Borrowed by" information, plus Return/Delete buttons
  - **Users**: Regular users see only their own loans without Return/Delete buttons

Note: Books and Authors menu items are only visible to authenticated users. Unauthenticated users see only Search and Login.

### Access Control

```typescript
// Route-level
<Route element={<LibrarianRoute />}>
  <Route path="/users" element={<UsersPage />} />
</Route>

// Component-level
{isLibrarian && (
  <Button onClick={handleBulkDelete}>Delete Selected</Button>
)}
```

## Data Flow

### Read Operations
1. Component mounts
2. React Query hook fetches data
3. Loading spinner displayed
4. Data cached and displayed
5. Background refetch (if stale)

### Write Operations
1. User submits form
2. Mutation hook sends request
3. Loading state shown
4. On success: invalidate cache, show success message
5. On error: show error message

### Example Flow
```
User clicks "Create Book"
  → BookForm modal opens (local state)
  → User fills form
  → Submit triggers useCreateBook mutation
  → API request sent
  → On success:
    - Query cache invalidated
    - Books list auto-refreshes
    - Modal closes
    - Success message shown
```

## Testing Strategy

### Data-Test Attributes

All interactive elements have `data-test` attributes for Playwright tests:

```typescript
<Button data-test="create-book-button">Create Book</Button>
<Input data-test="book-title-input" />
<table data-test="books-table" />
```

### Test Organization

- **E2E Tests**: Playwright tests in `src/test/java/.../ui/` (backend repo)
- **Component Tests**: Future - React Testing Library
- **Type Safety**: TypeScript strict mode catches errors at compile time

## Common UI Patterns

### CRUD Pattern

All features follow consistent CRUD pattern:

1. **List View**:
   - Filters (radio buttons)
   - Create button (top right)
   - Data table with checkboxes
   - Bulk actions toolbar (when selected)
   - Edit/Delete actions per row

2. **Create**:
   - Modal form
   - Required field validation
   - Loading state on submit
   - Success/error messages

3. **Edit**:
   - Same modal as create (pre-filled)
   - Optional password (keep existing if blank)
   - Update cache on success

4. **Delete**:
   - Confirmation dialog
   - Bulk delete for multiple items
   - Remove from cache on success

### Filter Pattern

Radio buttons for mutually exclusive filters:

```typescript
<div className="flex gap-4">
  <label>
    <input
      type="radio"
      checked={filter === 'all'}
      onChange={() => setFilter('all')}
    />
    All Books
  </label>
  <label>
    <input
      type="radio"
      checked={filter === 'most-recent'}
      onChange={() => setFilter('most-recent')}
    />
    Most Recent Day
  </label>
</div>
```

### Table Pattern

Reusable DataTable component with:
- Checkbox selection
- Sort headers (planned)
- Action column (Edit, Delete)
- Loading states
- Empty states

### Modal Pattern

Headless UI Dialog with consistent structure:
- Title
- Close button (X)
- Body content
- Footer (Cancel, Submit buttons)

## Key Features

### 1. Books Management
- Filter by All, Most Recent Day, Without LOC
- CRUD operations
- Bulk delete
- LOC lookup (single & bulk)
- Photo management
- Status tracking (Available, Checked Out, Lost, Damaged)

### 2. Authors Management
- Filter by All, Without Description, Zero Books
- CRUD operations
- Bulk delete
- Biography management
- Photo management
- Book count tracking

### 3. Photo Management
- Upload photos
- Crop with react-cropper
- Rotate (CW/CCW)
- Reorder (move left/right)
- Delete
- Support for books and authors

### 4. Google Photos Integration
- Photo Picker modal
- Import photos from Google Photos
- AI processing for book metadata extraction
- Batch processing
- Processing results modal

### 5. Library Cards
- Visual card preview
- Print wallet-sized PDF
- Apply for card (public)
- Approve applications (librarian)

### 6. Search (Public Access)
- Search by book title or author name
- Paginated results
- No authentication required

### 7. Data Management
- Export database to JSON
- Import database from JSON
- Export photos as ZIP
- Import merges with existing data

## Error Handling

### Error Boundary

Root-level error boundary catches React errors:

```typescript
<ErrorBoundary>
  <App />
</ErrorBoundary>
```

Displays:
- Friendly error message
- Error details (collapsible)
- Refresh button
- Go to Home button

### API Errors

TanStack Query handles API errors:

```typescript
const { data, error, isLoading } = useBooks()

if (error) {
  return <ErrorMessage message={error.message} />
}
```

### Form Validation

Client-side validation with error messages:

```typescript
<Input
  label="Title"
  error={errors.title}
  required
/>
```

## Build & Deployment

### Development
```bash
npm run dev      # Start dev server (http://localhost:5173)
```

### Production Build
```bash
npm run build    # TypeScript compile + Vite build
```

Output:
- `dist/` directory
- Optimized and minified assets
- Code-split chunks
- Source maps for debugging

### Spring Boot Integration

Production build served by Spring Boot:
- Static assets in `src/main/resources/static/`
- Single HTML file for SPA
- Backend handles `/api/*` routes
- Frontend handles all other routes (React Router)

## Migration from Vanilla JS

### Completed (Dec 2024)

- ✅ All 14 feature pages migrated
- ✅ React components with TypeScript
- ✅ TanStack Query for data fetching
- ✅ Tailwind CSS styling
- ✅ React Router navigation
- ✅ Error boundaries
- ✅ Code splitting (49% bundle reduction)
- ✅ Photo management
- ✅ Google Photos integration
- ✅ LOC lookup integration

### Old System (Archived)

The previous vanilla JavaScript implementation (8,512 lines across 26 files) has been completely replaced. Key differences:

| Aspect | Old (Vanilla JS) | New (React) |
|--------|-----------------|-------------|
| Framework | None | React 18 |
| Type Safety | None | TypeScript strict mode |
| Styling | Bootstrap 5 | Tailwind CSS v4 |
| State | Global variables | React Query + Zustand |
| Routing | CSS visibility | React Router |
| Components | jQuery-style DOM | React components |
| Caching | Manual IndexedDB | TanStack Query |
| Bundle Size | ~450 KB | 275 KB (initial) |
| Developer Experience | Manual DOM manipulation | Declarative components |

## Future Enhancements

### Planned
- IndexedDB caching for photo thumbnails
- Virtual scrolling for large lists
- Dark mode support
- Accessibility improvements (ARIA labels, keyboard navigation)
- Toast notifications
- Optimistic updates for mutations
- WebSocket support for real-time updates

### Out of Scope (Backend - Phase 2)
- Session persistence across server reboots
- lastModified fields on all entities
- Image checksum fixes
- Statistics endpoint
- Grok API integration for LOC suggestions

## Development Guidelines

### Adding a New Feature

1. **Create API functions** (`src/api/feature.ts`)
   - API call functions
   - React Query hooks (`useFeatures`, `useCreateFeature`, etc.)

2. **Create page component** (`src/pages/feature/FeaturePage.tsx`)
   - Use React Query hooks for data
   - Manage local state (modals, forms)

3. **Create child components** (`src/pages/feature/components/`)
   - Filters, table, form, modals
   - Keep presentational

4. **Add route** (`src/App.tsx`)
   - Lazy-loaded for code splitting
   - Protected or librarian route as needed

5. **Add navigation** (`src/components/layout/Navigation.tsx`)
   - Menu item with proper access control

6. **Add types** (`src/types/dtos.ts`)
   - TypeScript interfaces matching backend DTOs

### Code Style

- TypeScript strict mode
- Functional components with hooks
- Props interfaces for all components
- data-test attributes for testing
- Copyright header: `// (c) Copyright 2025 by Muczynski`
- Path aliases: `@/` for `src/`

### State Management Rules

- **Server data**: Always use TanStack Query
- **Auth state**: Use Zustand authStore
- **UI state**: Use Zustand uiStore for global, useState for local
- **Forms**: Use useState (not React Hook Form for now)

### Performance Best Practices

- Lazy load page components
- Use React.memo for expensive components (sparingly)
- Avoid inline object creation in render
- Use query keys effectively for caching
- Invalidate queries strategically

## Troubleshooting

### Common Issues

**Build Errors:**
- Run `npm install` to ensure dependencies are installed
- Check TypeScript errors with `npm run build`
- Clear `dist/` and rebuild

**Authentication:**
- Check cookie-based session in DevTools
- Verify `/api/auth/check` returns user info
- Clear cookies and re-login

**Routing:**
- React Router handles client-side routes
- Refresh on non-root path requires server config
- Use `Navigate` component for redirects

**API Errors:**
- Check Network tab in DevTools
- Verify backend is running
- Check CORS configuration
- Verify authentication cookies

## Resources

- [React Documentation](https://react.dev)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [TanStack Query](https://tanstack.com/query/latest)
- [Tailwind CSS](https://tailwindcss.com/docs)
- [React Router](https://reactrouter.com)
- [Zustand](https://github.com/pmndrs/zustand)
