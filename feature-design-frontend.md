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
│   ├── libraries.ts         # Library API + hooks
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
│   │   ├── BooksPage.tsx    # Main books page
│   │   └── components/
│   │       ├── BookFilters.tsx          # Filter radio buttons
│   │       ├── BookTable.tsx            # Books table
│   │       ├── BookForm.tsx             # Create/Edit form
│   │       ├── BookDetailModal.tsx      # View modal
│   │       ├── BulkActionsToolbar.tsx   # Bulk operations
│   │       └── LocLookupResultsModal.tsx # LOC lookup results
│   │
│   ├── authors/             # Authors feature
│   │   ├── AuthorsPage.tsx
│   │   └── components/
│   │       ├── AuthorFilters.tsx
│   │       ├── AuthorTable.tsx
│   │       ├── AuthorForm.tsx
│   │       └── AuthorDetailModal.tsx
│   │
│   ├── libraries/           # Libraries feature
│   │   ├── LibrariesPage.tsx
│   │   └── DataManagementPage.tsx
│   │
│   ├── loans/               # Loans feature
│   │   └── LoansPage.tsx
│   │
│   ├── users/               # Users feature
│   │   ├── UsersPage.tsx
│   │   └── components/
│   │       ├── UserTable.tsx
│   │       └── UserForm.tsx
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

**Smart (Container) Components:**
- Handle data fetching with React Query hooks
- Manage local state (modals, forms)
- Pass data to presentational components

**Presentational Components:**
- Receive data via props
- Render UI
- Emit events via callbacks

Example:
```typescript
// BooksPage.tsx (Smart Component)
export function BooksPage() {
  const { data: books, isLoading } = useBooks(filter)
  const [showModal, setShowModal] = useState(false)

  return (
    <div>
      <BookFilters />
      <BookTable books={books} isLoading={isLoading} />
      <BookForm isOpen={showModal} onClose={() => setShowModal(false)} />
    </div>
  )
}

// BookTable.tsx (Presentational Component)
interface BookTableProps {
  books: BookDto[]
  isLoading: boolean
}

export function BookTable({ books, isLoading }: BookTableProps) {
  return <DataTable data={books} columns={columns} />
}
```

### 3. Routing Pattern

Protected routes with authentication and role-based access:

```typescript
<Routes>
  {/* Public routes */}
  <Route path="/login" element={<LoginPage />} />
  <Route path="/search" element={<SearchPage />} />
  <Route path="/books" element={<BooksPage />} />
  <Route path="/authors" element={<AuthorsPage />} />
  <Route path="/my-card" element={<MyLibraryCardPage />} />

  {/* Protected routes (authenticated users) */}
  <Route element={<ProtectedRoute />}>
    <Route element={<AppLayout />}>
      <Route path="/loans" element={<LoansPage />} />
      <Route path="/settings" element={<UserSettingsPage />} />

      {/* Librarian-only routes */}
      <Route element={<LibrarianRoute />}>
        <Route path="/users" element={<UsersPage />} />
        <Route path="/libraries" element={<LibrariesPage />} />
      </Route>
    </Route>
  </Route>
</Routes>
```

### Navigation Menu Access

The navigation menu shows different items based on authentication status:

**Unauthenticated Users (Public):**
- Books (browse catalog)
- Authors (browse authors)
- Search (search library)
- My Card (view/apply for library card)
- Login (sign in button)

**Authenticated Users:**
- Books, Authors, Search, My Card (same as public)
- Loans (view checkout history)
- Settings (manage account)
- Logout (sign out button)

**Librarians Only:**
- All authenticated user items, plus:
- Libraries, Users, Applications
- Books from Feed, Data Management
- Global Settings, Test Data

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

Forms use controlled components with TypeScript interfaces:

```typescript
interface BookFormData {
  title: string
  authorId: number
  libraryId: number
  publicationYear?: number
  publisher?: string
}

export function BookForm({ book, onSubmit }: BookFormProps) {
  const [formData, setFormData] = useState<BookFormData>(
    book || { title: '', authorId: 0, libraryId: 0 }
  )

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSubmit(formData)
  }

  return (
    <form onSubmit={handleSubmit}>
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

- **PUBLIC (Unauthenticated)**: Access to Books, Authors, Search, My Card (apply for card)
- **USER**: Public access + Loans, Settings, My Card (download card)
- **LIBRARIAN**: Full access including Users, Libraries, Applications, Books from Feed, Data Management, Global Settings, Test Data

Note: Books and Authors pages are publicly accessible to allow browsing the catalog without login.

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
