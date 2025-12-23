# Front-End Rewrite Design Specification
## Library Management System - React + TypeScript Migration

**Document Version:** 1.0
**Date:** 2025-12-23
**Status:** Design Specification
**Scope:** Complete frontend rewrite from vanilla JavaScript to React + TypeScript + Tailwind CSS + TanStack Query

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Technology Stack](#technology-stack)
3. [Architecture Overview](#architecture-overview)
4. [Project Structure](#project-structure)
5. [Core Infrastructure](#core-infrastructure)
6. [Routing & Navigation](#routing--navigation)
7. [Authentication & Authorization](#authentication--authorization)
8. [State Management & Caching](#state-management--caching)
9. [Component Library](#component-library)
10. [Page Components](#page-components)
11. [Data Fetching Patterns](#data-fetching-patterns)
12. [Image Caching Strategy](#image-caching-strategy)
13. [Consistent CRUD Patterns](#consistent-crud-patterns)
14. [Table Components & Bulk Operations](#table-components--bulk-operations)
15. [Progress Indicators](#progress-indicators)
16. [Button Styling Standards](#button-styling-standards)
17. [Menu & Navigation Structure](#menu--navigation-structure)
18. [Migration from Vanilla JS](#migration-from-vanilla-js)
19. [Testing Strategy](#testing-strategy)
20. [Implementation Plan](#implementation-plan)

---

## 1. Executive Summary

### Current State
- **8,512 lines** of vanilla JavaScript across 26 files
- Hybrid ES6/global scope module system
- Manual DOM manipulation with jQuery-style patterns
- Inconsistent CRUD patterns across features
- Ad-hoc caching with IndexedDB (photos only)
- Section-based navigation with CSS visibility toggles
- Bootstrap 5.3.8 for UI styling

### Target State
- Modern React 18+ application with TypeScript
- Tailwind CSS for utility-first styling
- TanStack Query for server state management
- Consistent CRUD patterns across all features
- Optimized client-side caching with lastModified tracking
- React Router for URL-based navigation
- Reusable component library with TypeScript interfaces
- Comprehensive bulk operations with checkbox selection
- Progress indicators for all async operations

### Key Objectives
1. **Consistency**: Standardize CRUD operations, tables, forms, and bulk actions
2. **Performance**: Implement efficient caching with lastModified tracking and IndexedDB for images
3. **Type Safety**: Full TypeScript coverage with strict mode
4. **Maintainability**: Clear component boundaries, separation of concerns
5. **User Experience**: Progress indicators, optimistic updates, better visual hierarchy
6. **Testability**: Maintain data-test attributes for Playwright tests

### Out of Scope (Backend - Phase 2)
These will be addressed after frontend migration:
- Session persistence across server reboots
- lastModified fields on Authors, Users, Loans
- @PreUpdate annotations for automatic timestamp updates
- Image checksum fixes
- Statistics endpoint for Libraries page
- Filter endpoints returning ID + lastModified
- Grok API integration for LOC suggestions
- Photo datestamp transfer during books-from-feed

---

## 2. Technology Stack

### Core Framework
- **React 18.3+** - Component-based UI library
  - Hooks for state management
  - Concurrent features for better UX
  - Suspense for loading states

- **TypeScript 5.3+** - Type-safe JavaScript
  - Strict mode enabled
  - Path aliases for clean imports
  - Interface-driven development

### Styling
- **Tailwind CSS 3.4+** - Utility-first CSS framework
  - Custom configuration for library branding
  - Responsive design utilities
  - Dark mode support (future enhancement)
  - Component extraction for repeated patterns

- **Headless UI** - Unstyled, accessible components
  - Dialogs/Modals
  - Dropdowns/Menus
  - Disclosure panels
  - Transitions

### Data Fetching & State
- **TanStack Query (React Query) v5** - Server state management
  - Automatic caching with cache invalidation
  - Optimistic updates
  - Background refetching
  - Pagination support
  - Mutation management

- **Zustand** - Client state management
  - Authentication state
  - UI state (selected rows, filters)
  - Minimal boilerplate compared to Redux

### Routing
- **React Router v6** - Client-side routing
  - Nested routes for sections
  - Protected routes for authentication
  - URL query parameters for filters
  - Navigation guards

### Forms
- **React Hook Form** - Form state management
  - Minimal re-renders
  - Built-in validation
  - TypeScript integration
  - File upload support

- **Zod** - Schema validation
  - Runtime type checking
  - Form validation rules
  - API response validation

### Image Processing
- **react-cropper** - Image cropping (Cropper.js wrapper)
- **idb** - IndexedDB wrapper for image caching
- **blurhash** - Image placeholders (future enhancement)

### Build Tools
- **Vite** - Fast build tool and dev server
  - Fast HMR (Hot Module Replacement)
  - Optimized production builds
  - TypeScript support out-of-the-box
  - Environment variable management

### Testing (Maintain Existing)
- **Playwright** - E2E testing
  - Keep existing test structure
  - Update selectors to match new DOM
  - Maintain data-test attributes

### Additional Utilities
- **date-fns** - Date formatting/manipulation
- **clsx** - Conditional className utility
- **react-icons** - Icon library (replacing Bootstrap Icons)

---

## 3. Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         React App                            │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Router     │  │  Auth Store  │  │  TanStack    │      │
│  │  (Routes)    │  │   (Zustand)  │  │   Query      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Layout Component                        │   │
│  │  ┌──────────────┐  ┌────────────────────────────┐  │   │
│  │  │  Navigation  │  │    Main Content Area       │  │   │
│  │  │    Menu      │  │  (Route-based)             │  │   │
│  │  └──────────────┘  └────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  Page Components (Libraries, Books, Authors, etc.)         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  List View   │  │  Create Form │  │  Edit Form   │     │
│  │  (Table)     │  │              │  │              │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
├─────────────────────────────────────────────────────────────┤
│  Reusable Components                                        │
│  • DataTable  • Form  • Button  • Modal  • ImageUpload     │
│  • ProgressIndicator  • ErrorBoundary  • ConfirmDialog     │
├─────────────────────────────────────────────────────────────┤
│  API Layer (fetch + TanStack Query)                        │
│  • Type-safe API functions  • Query hooks  • Mutations     │
├─────────────────────────────────────────────────────────────┤
│  Utilities                                                  │
│  • Image Cache (IndexedDB)  • Formatters  • Validators     │
└─────────────────────────────────────────────────────────────┘
```

### Key Architectural Decisions

#### 1. Component-Based Architecture
- **Smart (Container) Components**: Handle data fetching, mutations, business logic
- **Presentational Components**: Receive props, render UI, emit events
- **Layout Components**: Handle page structure and navigation
- **Utility Components**: Reusable UI elements (Button, Input, Modal)

#### 2. Colocation of Concerns
- Components, styles, tests, and types colocated in feature folders
- Shared components in `/components` directory
- API calls in `/api` directory with corresponding query hooks

#### 3. Type-First Development
- Define TypeScript interfaces before implementation
- DTOs match backend response shapes exactly
- Component props strictly typed
- API responses validated with Zod schemas

#### 4. Progressive Enhancement
- Server data fetched via TanStack Query
- Optimistic updates for better UX
- Fallback UI for loading and error states
- Graceful degradation when API is unavailable

#### 5. Performance Optimization
- Code splitting by route
- Lazy loading for heavy components (photo cropper, PDF viewer)
- Memoization of expensive computations
- Virtual scrolling for long lists (future enhancement)
- Image caching with IndexedDB

---

## 4. Project Structure

```
src/
├── main.tsx                    # Application entry point
├── App.tsx                     # Root component with providers
├── index.css                   # Global styles (Tailwind imports)
│
├── api/                        # API layer
│   ├── client.ts               # Fetch wrapper with auth
│   ├── books.ts                # Book API functions + query hooks
│   ├── authors.ts              # Author API functions + query hooks
│   ├── libraries.ts            # Library API functions + query hooks
│   ├── loans.ts                # Loan API functions + query hooks
│   ├── users.ts                # User API functions + query hooks
│   ├── photos.ts               # Photo API functions + query hooks
│   ├── library-cards.ts        # Library card API + query hooks
│   ├── auth.ts                 # Authentication API
│   └── types.ts                # Shared API types/DTOs
│
├── stores/                     # Zustand stores
│   ├── authStore.ts            # Authentication state
│   └── uiStore.ts              # UI state (filters, selected rows)
│
├── hooks/                      # Custom React hooks
│   ├── useAuth.ts              # Authentication hook
│   ├── useImageCache.ts        # IndexedDB image caching
│   ├── useTableSelection.ts    # Checkbox selection logic
│   ├── usePagination.ts        # Pagination logic
│   └── useDebounce.ts          # Debounce hook
│
├── components/                 # Reusable components
│   ├── layout/
│   │   ├── AppLayout.tsx       # Main layout wrapper
│   │   ├── Navigation.tsx      # Top navigation menu
│   │   └── Footer.tsx          # Footer (if needed)
│   │
│   ├── ui/                     # Basic UI components
│   │   ├── Button.tsx          # Styled button variants
│   │   ├── Input.tsx           # Form input
│   │   ├── Select.tsx          # Select dropdown
│   │   ├── Checkbox.tsx        # Checkbox input
│   │   ├── Radio.tsx           # Radio button
│   │   ├── Modal.tsx           # Modal dialog
│   │   ├── ConfirmDialog.tsx   # Confirmation modal
│   │   ├── ErrorMessage.tsx    # Error display
│   │   ├── SuccessMessage.tsx  # Success display
│   │   └── Card.tsx            # Card container
│   │
│   ├── table/                  # Table components
│   │   ├── DataTable.tsx       # Generic table component
│   │   ├── TableHeader.tsx     # Table header with sorting
│   │   ├── TableRow.tsx        # Table row
│   │   ├── TableCell.tsx       # Table cell
│   │   ├── TableCheckbox.tsx   # Selection checkbox
│   │   └── TableActions.tsx    # Action buttons column
│   │
│   ├── form/                   # Form components
│   │   ├── Form.tsx            # Form wrapper
│   │   ├── FormField.tsx       # Form field with label/error
│   │   ├── FormSection.tsx     # Form section grouping
│   │   └── ImageUpload.tsx     # Image upload/crop component
│   │
│   ├── progress/               # Progress indicators
│   │   ├── Spinner.tsx         # Circular spinner
│   │   ├── ProgressBar.tsx     # Linear progress bar
│   │   └── LoadingOverlay.tsx  # Full-screen loading
│   │
│   └── photo/                  # Photo components
│       ├── PhotoGallery.tsx    # Photo grid display
│       ├── PhotoUpload.tsx     # Upload interface
│       ├── PhotoCropper.tsx    # Cropping interface
│       └── ThumbnailImage.tsx  # Cached thumbnail display
│
├── pages/                      # Page components (routes)
│   ├── LoginPage.tsx           # Login page
│   ├── NotFoundPage.tsx        # 404 page
│   │
│   ├── books/                  # Books feature
│   │   ├── BooksPage.tsx       # Books list page
│   │   ├── BookCreatePage.tsx  # Create book page
│   │   ├── BookEditPage.tsx    # Edit book page
│   │   ├── BookViewPage.tsx    # View book (read-only)
│   │   ├── components/         # Book-specific components
│   │   │   ├── BookTable.tsx
│   │   │   ├── BookForm.tsx
│   │   │   ├── BookFilters.tsx # Radio buttons for filters
│   │   │   ├── BookActions.tsx
│   │   │   └── BookPhotos.tsx
│   │   └── types.ts            # Book-specific types
│   │
│   ├── authors/                # Authors feature
│   │   ├── AuthorsPage.tsx
│   │   ├── AuthorCreatePage.tsx
│   │   ├── AuthorEditPage.tsx
│   │   ├── AuthorViewPage.tsx
│   │   ├── components/
│   │   │   ├── AuthorTable.tsx
│   │   │   ├── AuthorForm.tsx
│   │   │   ├── AuthorFilters.tsx
│   │   │   ├── AuthorActions.tsx
│   │   │   └── AuthorPhotos.tsx
│   │   └── types.ts
│   │
│   ├── libraries/              # Libraries feature
│   │   ├── LibrariesPage.tsx   # Libraries list/management
│   │   ├── DataManagementPage.tsx  # Import/Export/Photo Export
│   │   ├── components/
│   │   │   ├── LibraryTable.tsx
│   │   │   ├── LibraryForm.tsx
│   │   │   ├── ImportExport.tsx
│   │   │   └── PhotoExport.tsx
│   │   └── types.ts
│   │
│   ├── loans/                  # Loans feature
│   │   ├── LoansPage.tsx
│   │   ├── LoanCheckoutPage.tsx
│   │   ├── components/
│   │   │   ├── LoanTable.tsx
│   │   │   ├── LoanForm.tsx
│   │   │   └── LoanActions.tsx
│   │   └── types.ts
│   │
│   ├── users/                  # Users feature
│   │   ├── UsersPage.tsx
│   │   ├── UserCreatePage.tsx
│   │   ├── UserEditPage.tsx
│   │   ├── components/
│   │   │   ├── UserTable.tsx
│   │   │   ├── UserForm.tsx
│   │   │   └── UserActions.tsx
│   │   └── types.ts
│   │
│   ├── library-cards/          # Library card feature
│   │   ├── MyLibraryCardPage.tsx      # User's card view
│   │   ├── ApplyForCardPage.tsx       # Application form
│   │   ├── ApplicationsPage.tsx       # Librarian applications view
│   │   ├── components/
│   │   │   ├── CardDesignPicker.tsx
│   │   │   ├── CardPreview.tsx
│   │   │   ├── ApplicationForm.tsx
│   │   │   └── ApplicationTable.tsx
│   │   └── types.ts
│   │
│   ├── labels/                 # Label generation
│   │   ├── LabelsPage.tsx
│   │   ├── components/
│   │   │   ├── LabelFilters.tsx
│   │   │   └── LabelPreview.tsx
│   │   └── types.ts
│   │
│   ├── search/                 # Search feature
│   │   ├── SearchPage.tsx
│   │   ├── components/
│   │   │   ├── SearchInput.tsx
│   │   │   └── SearchResults.tsx
│   │   └── types.ts
│   │
│   ├── settings/               # Settings pages
│   │   ├── UserSettingsPage.tsx     # User profile/settings
│   │   ├── GlobalSettingsPage.tsx   # Global OAuth settings
│   │   └── components/
│   │       ├── SettingsForm.tsx
│   │       └── OAuthConfig.tsx
│   │
│   ├── books-from-feed/        # Google Photos import
│   │   ├── BooksFromFeedPage.tsx
│   │   ├── components/
│   │   │   ├── PhotoPicker.tsx
│   │   │   ├── SavedBooksTable.tsx
│   │   │   └── ProcessingStatus.tsx
│   │   └── types.ts
│   │
│   └── test-data/              # Test data generation
│       ├── TestDataPage.tsx
│       └── components/
│           └── TestDataControls.tsx
│
├── utils/                      # Utility functions
│   ├── formatters.ts           # Date, number, text formatting
│   ├── validators.ts           # Validation functions
│   ├── constants.ts            # App constants
│   ├── imageCache.ts           # IndexedDB image caching
│   ├── auth.ts                 # Auth helpers (hash password, etc.)
│   └── api-helpers.ts          # API utility functions
│
├── types/                      # Global TypeScript types
│   ├── entities.ts             # Entity types (Book, Author, etc.)
│   ├── dtos.ts                 # DTO types matching backend
│   ├── enums.ts                # Enums (BookStatus, UserRole, etc.)
│   └── common.ts               # Common types (ApiResponse, etc.)
│
├── config/                     # Configuration
│   ├── routes.ts               # Route definitions
│   ├── queryClient.ts          # TanStack Query configuration
│   └── constants.ts            # App-wide constants
│
└── styles/                     # Additional styles
    ├── tailwind.css            # Tailwind directives
    └── custom.css              # Custom CSS (if needed)
```

### Build Configuration Files (Root)

```
library/
├── package.json                # Dependencies and scripts
├── tsconfig.json               # TypeScript configuration
├── tsconfig.node.json          # Node TypeScript config
├── vite.config.ts              # Vite configuration
├── tailwind.config.js          # Tailwind configuration
├── postcss.config.js           # PostCSS configuration
├── .eslintrc.cjs               # ESLint rules
├── .prettierrc                 # Prettier formatting
└── index.html                  # HTML entry point
```

---

## 5. Core Infrastructure

### 5.1 Application Entry Point

**File: `src/main.tsx`**

```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { BrowserRouter } from 'react-router-dom'
import { queryClient } from './config/queryClient'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>
)
```

### 5.2 Root App Component

**File: `src/App.tsx`**

```typescript
import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './stores/authStore'
import { AppLayout } from './components/layout/AppLayout'
import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { LibrarianRoute } from './components/auth/LibrarianRoute'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
// Import all page components...

function App() {
  const checkAuth = useAuthStore((state) => state.checkAuth)

  useEffect(() => {
    checkAuth() // Check authentication on app load
  }, [checkAuth])

  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/search" element={<SearchPage />} />
      <Route path="/apply" element={<ApplyForCardPage />} />

      {/* Protected routes (authenticated users) */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/books" replace />} />
          <Route path="/books/*" element={<BooksPage />} />
          <Route path="/authors/*" element={<AuthorsPage />} />
          <Route path="/loans/*" element={<LoansPage />} />
          <Route path="/my-card" element={<MyLibraryCardPage />} />
          <Route path="/settings" element={<UserSettingsPage />} />

          {/* Librarian-only routes */}
          <Route element={<LibrarianRoute />}>
            <Route path="/libraries" element={<LibrariesPage />} />
            <Route path="/data-management" element={<DataManagementPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/applications" element={<ApplicationsPage />} />
            <Route path="/labels" element={<LabelsPage />} />
            <Route path="/books-from-feed" element={<BooksFromFeedPage />} />
            <Route path="/global-settings" element={<GlobalSettingsPage />} />
            <Route path="/test-data" element={<TestDataPage />} />
          </Route>
        </Route>
      </Route>

      {/* 404 */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
```

### 5.3 TanStack Query Configuration

**File: `src/config/queryClient.ts`**

```typescript
import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes default
      gcTime: 1000 * 60 * 30, // 30 minutes garbage collection
      retry: 1,
      refetchOnWindowFocus: false,
      refetchOnMount: 'always',
    },
    mutations: {
      retry: 0,
    },
  },
})

// Query key factory pattern
export const queryKeys = {
  books: {
    all: ['books'] as const,
    summaries: () => [...queryKeys.books.all, 'summaries'] as const,
    list: (filter?: string) => [...queryKeys.books.all, 'list', filter] as const,
    detail: (id: number) => [...queryKeys.books.all, 'detail', id] as const,
    photos: (id: number) => [...queryKeys.books.all, id, 'photos'] as const,
  },
  authors: {
    all: ['authors'] as const,
    list: (filter?: string) => [...queryKeys.authors.all, 'list', filter] as const,
    detail: (id: number) => [...queryKeys.authors.all, 'detail', id] as const,
    photos: (id: number) => [...queryKeys.authors.all, id, 'photos'] as const,
    books: (id: number) => [...queryKeys.authors.all, id, 'books'] as const,
  },
  libraries: {
    all: ['libraries'] as const,
    list: () => [...queryKeys.libraries.all, 'list'] as const,
    detail: (id: number) => [...queryKeys.libraries.all, 'detail', id] as const,
  },
  loans: {
    all: ['loans'] as const,
    list: (showAll?: boolean) => [...queryKeys.loans.all, 'list', showAll] as const,
    detail: (id: number) => [...queryKeys.loans.all, 'detail', id] as const,
  },
  users: {
    all: ['users'] as const,
    me: () => [...queryKeys.users.all, 'me'] as const,
    list: () => [...queryKeys.users.all, 'list'] as const,
    detail: (id: number) => [...queryKeys.users.all, 'detail', id] as const,
  },
  photos: {
    all: ['photos'] as const,
    image: (id: number, checksum: string) =>
      [...queryKeys.photos.all, 'image', id, checksum] as const,
    thumbnail: (id: number, checksum: string, width: number) =>
      [...queryKeys.photos.all, 'thumbnail', id, checksum, width] as const,
  },
}
```

### 5.4 API Client

**File: `src/api/client.ts`**

```typescript
import { useAuthStore } from '../stores/authStore'

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public statusText: string
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface RequestOptions extends RequestInit {
  requireAuth?: boolean
}

export async function apiClient<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { requireAuth = true, ...fetchOptions } = options

  const url = endpoint.startsWith('http')
    ? endpoint
    : `${import.meta.env.VITE_API_BASE_URL || ''}/api${endpoint}`

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...fetchOptions.headers,
  }

  // Note: Authentication is cookie-based, no manual token handling needed
  const config: RequestInit = {
    ...fetchOptions,
    headers,
    credentials: 'include', // Important for cookie-based auth
  }

  try {
    const response = await fetch(url, config)

    // Handle 401 Unauthorized
    if (response.status === 401 && requireAuth) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
      throw new ApiError('Unauthorized', 401, response.statusText)
    }

    // Handle other error responses
    if (!response.ok) {
      const errorText = await response.text()
      throw new ApiError(
        errorText || 'An error occurred',
        response.status,
        response.statusText
      )
    }

    // Handle empty responses (204 No Content)
    if (response.status === 204) {
      return null as T
    }

    // Parse JSON response
    const data = await response.json()
    return data as T
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    throw new ApiError(
      error instanceof Error ? error.message : 'Network error',
      0,
      'Network Error'
    )
  }
}

// Helper functions for different HTTP methods
export const api = {
  get: <T>(endpoint: string, options?: RequestOptions) =>
    apiClient<T>(endpoint, { ...options, method: 'GET' }),

  post: <T>(endpoint: string, data?: unknown, options?: RequestOptions) =>
    apiClient<T>(endpoint, {
      ...options,
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    }),

  put: <T>(endpoint: string, data?: unknown, options?: RequestOptions) =>
    apiClient<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    }),

  delete: <T>(endpoint: string, options?: RequestOptions) =>
    apiClient<T>(endpoint, { ...options, method: 'DELETE' }),

  // Special handler for multipart/form-data (file uploads)
  postFormData: <T>(endpoint: string, formData: FormData, options?: RequestOptions) =>
    apiClient<T>(endpoint, {
      ...options,
      method: 'POST',
      body: formData,
      headers: {
        // Don't set Content-Type, let browser set it with boundary
        ...options?.headers,
        'Content-Type': undefined,
      } as HeadersInit,
    }),
}
```

---

## 6. Routing & Navigation

*(Content added above)*

---

## 7. Authentication & Authorization

*(See Section 5 for full implementation details)*

---

## 8. State Management & Caching

### 8.1 State Management Strategy

**Three-Tier State Management:**

1. **Server State** (TanStack Query)
   - Data from API endpoints
   - Automatic caching, refetching, and invalidation
   - Optimistic updates for mutations

2. **Client State** (Zustand)
   - Authentication status
   - UI state (filters, selected rows, modal visibility)
   - User preferences

3. **Component State** (useState/useReducer)
   - Form inputs
   - Local UI state (expanded/collapsed, etc.)
   - Temporary values

### 8.2 UI State Store

**File: `src/stores/uiStore.ts`**

```typescript
import { create } from 'zustand'

interface TableState {
  selectedIds: Set<number>
  selectAll: boolean
}

interface UiState {
  // Table selection state per feature
  booksTable: TableState
  authorsTable: TableState
  usersTable: TableState
  loansTable: TableState

  // Filter state per feature
  booksFilter: 'all' | 'most-recent' | 'without-loc'
  authorsFilter: 'all' | 'without-description' | 'zero-books'
  loansShowAll: boolean

  // Actions
  setSelectedIds: (table: keyof Omit<UiState, 'setSelectedIds' | 'toggleSelectAll' | 'clearSelection' | 'setFilter' | 'loansShowAll' | 'setLoansShowAll'>, ids: Set<number>) => void
  toggleSelectAll: (table: string) => void
  clearSelection: (table: string) => void
  setFilter: (feature: 'books' | 'authors', filter: string) => void
  setLoansShowAll: (showAll: boolean) => void
}

export const useUiStore = create<UiState>((set) => ({
  // Initial state
  booksTable: { selectedIds: new Set(), selectAll: false },
  authorsTable: { selectedIds: new Set(), selectAll: false },
  usersTable: { selectedIds: new Set(), selectAll: false },
  loansTable: { selectedIds: new Set(), selectAll: false },

  booksFilter: 'most-recent',
  authorsFilter: 'all',
  loansShowAll: false,

  // Actions
  setSelectedIds: (table, ids) =>
    set((state) => ({
      [table]: { ...state[table], selectedIds: ids },
    })),

  toggleSelectAll: (table) =>
    set((state) => ({
      [table]: { ...state[table], selectAll: !state[table].selectAll },
    })),

  clearSelection: (table) =>
    set((state) => ({
      [table]: { selectedIds: new Set(), selectAll: false },
    })),

  setFilter: (feature, filter) =>
    set({ [`${feature}Filter`]: filter }),

  setLoansShowAll: (showAll) => set({ loansShowAll: showAll }),
}))
```

### 8.3 Caching Strategy

**Books (Full Optimistic Caching with lastModified):**

```typescript
// File: src/api/books.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '../config/queryClient'
import type { BookDto, BookSummaryDto } from '../types/dtos'

// Hook to get all books with smart caching
export function useBooks(filter?: 'all' | 'most-recent' | 'without-loc') {
  const queryClient = useQueryClient()

  // Step 1: Fetch summaries (ID + lastModified)
  const { data: summaries } = useQuery({
    queryKey: queryKeys.books.summaries(),
    queryFn: () => api.get<BookSummaryDto[]>('/books/summaries'),
    staleTime: Infinity, // Summaries never stale - we use them to detect changes
  })

  // Step 2: Determine which books need fetching
  const booksToFetch = useMemo(() => {
    if (!summaries) return []

    return summaries.filter(summary => {
      const cached = queryClient.getQueryData<BookDto>(queryKeys.books.detail(summary.id))
      return !cached || cached.lastModified !== summary.lastModified
    }).map(s => s.id)
  }, [summaries, queryClient])

  // Step 3: Batch fetch changed books
  const { data: fetchedBooks, isLoading } = useQuery({
    queryKey: queryKeys.books.list(filter),
    queryFn: () => {
      // Apply filter on backend
      if (filter === 'most-recent') {
        return api.get<BookDto[]>('/books/most-recent-day')
      } else if (filter === 'without-loc') {
        return api.get<BookDto[]>('/books/without-loc')
      } else if (booksToFetch.length > 0) {
        return api.post<BookDto[]>('/books/by-ids', booksToFetch)
      }
      return []
    },
    enabled: summaries !== undefined,
    onSuccess: (books) => {
      // Populate individual book caches
      books?.forEach(book => {
        queryClient.setQueryData(queryKeys.books.detail(book.id), book)
      })
    },
  })

  // Step 4: Get all cached books for display
  const allBooks = useMemo(() => {
    if (!summaries) return []

    return summaries
      .map(summary => queryClient.getQueryData<BookDto>(queryKeys.books.detail(summary.id)))
      .filter((book): book is BookDto => book !== undefined)
  }, [summaries, queryClient, fetchedBooks])

  return {
    books: allBooks,
    isLoading,
  }
}

// Mutation for creating a book
export function useCreateBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (book: Partial<BookDto>) => api.post<BookDto>('/books', book),
    onSuccess: () => {
      // Invalidate summaries to trigger re-fetch
      queryClient.invalidateQueries(queryKeys.books.summaries())
    },
  })
}

// Mutation for updating a book
export function useUpdateBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, book }: { id: number; book: Partial<BookDto> }) =>
      api.put<BookDto>(`/books/${id}`, book),
    onSuccess: (data, variables) => {
      // Update both the detail cache and invalidate summaries
      queryClient.setQueryData(queryKeys.books.detail(variables.id), data)
      queryClient.invalidateQueries(queryKeys.books.summaries())
    },
  })
}

// Mutation for deleting a book
export function useDeleteBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => api.delete(`/books/${id}`),
    onSuccess: (_, id) => {
      // Remove from cache and invalidate summaries
      queryClient.removeQueries(queryKeys.books.detail(id))
      queryClient.invalidateQueries(queryKeys.books.summaries())
    },
  })
}
```

**Authors, Libraries, Loans, Users (Simple Caching - no lastModified yet):**

```typescript
// File: src/api/authors.ts
export function useAuthors(filter?: 'all' | 'without-description' | 'zero-books') {
  return useQuery({
    queryKey: queryKeys.authors.list(filter),
    queryFn: async () => {
      const authors = await api.get<AuthorDto[]>('/authors')

      // Client-side filtering until backend supports it
      if (filter === 'without-description') {
        return authors.filter(a => !a.briefBiography)
      } else if (filter === 'zero-books') {
        return authors.filter(a => a.bookCount === 0)
      }

      return authors
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}

export function useCreateAuthor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (author: Partial<AuthorDto>) => api.post<AuthorDto>('/authors', author),
    onSuccess: () => {
      queryClient.invalidateQueries(queryKeys.authors.all)
    },
  })
}
```

---

## 9. Component Library

### 9.1 Button Component

**File: `src/components/ui/Button.tsx`**

```typescript
import { forwardRef, ButtonHTMLAttributes } from 'react'
import { clsx } from 'clsx'
import { PiSpinner } from 'react-icons/pi'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'outline'
  size?: 'sm' | 'md' | 'lg'
  isLoading?: boolean
  fullWidth?: boolean
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      children,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      fullWidth = false,
      leftIcon,
      rightIcon,
      className,
      disabled,
      ...props
    },
    ref
  ) => {
    const baseStyles = 'inline-flex items-center justify-center font-medium rounded-md transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed'

    const variantStyles = {
      primary: 'bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500',
      secondary: 'bg-gray-600 text-white hover:bg-gray-700 focus:ring-gray-500',
      danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
      ghost: 'bg-transparent text-gray-700 hover:bg-gray-100 focus:ring-gray-500',
      outline: 'border-2 border-gray-300 text-gray-700 hover:bg-gray-50 focus:ring-gray-500',
    }

    const sizeStyles = {
      sm: 'px-3 py-1.5 text-sm',
      md: 'px-4 py-2 text-base',
      lg: 'px-6 py-3 text-lg',
    }

    const widthStyles = fullWidth ? 'w-full' : ''

    return (
      <button
        ref={ref}
        className={clsx(
          baseStyles,
          variantStyles[variant],
          sizeStyles[size],
          widthStyles,
          className
        )}
        disabled={disabled || isLoading}
        {...props}
      >
        {isLoading && <PiSpinner className="mr-2 h-4 w-4 animate-spin" />}
        {!isLoading && leftIcon && <span className="mr-2">{leftIcon}</span>}
        {children}
        {!isLoading && rightIcon && <span className="ml-2">{rightIcon}</span>}
      </button>
    )
  }
)

Button.displayName = 'Button'
```

### 9.2 DataTable Component

**File: `src/components/table/DataTable.tsx`**

```typescript
import { ReactNode } from 'react'
import { clsx } from 'clsx'

export interface Column<T> {
  key: string
  header: string
  accessor: (item: T) => ReactNode
  sortable?: boolean
  width?: string
}

export interface DataTableProps<T> {
  data: T[]
  columns: Column<T>[]
  keyExtractor: (item: T) => string | number
  onRowClick?: (item: T) => void
  selectable?: boolean
  selectedIds?: Set<number>
  onSelectToggle?: (id: number) => void
  onSelectAll?: () => void
  selectAll?: boolean
  actions?: (item: T) => ReactNode
  emptyMessage?: string
  isLoading?: boolean
  className?: string
}

export function DataTable<T>({
  data,
  columns,
  keyExtractor,
  onRowClick,
  selectable = false,
  selectedIds = new Set(),
  onSelectToggle,
  onSelectAll,
  selectAll = false,
  actions,
  emptyMessage = 'No data available',
  isLoading = false,
  className,
}: DataTableProps<T>) {
  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
      </div>
    )
  }

  if (data.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        {emptyMessage}
      </div>
    )
  }

  return (
    <div className={clsx('overflow-x-auto', className)}>
      <table className="min-w-full divide-y divide-gray-200" data-test="data-table">
        <thead className="bg-gray-50">
          <tr>
            {selectable && (
              <th className="w-12 px-6 py-3 text-left">
                <input
                  type="checkbox"
                  checked={selectAll}
                  onChange={onSelectAll}
                  className="rounded border-gray-300"
                  data-test="select-all-checkbox"
                />
              </th>
            )}
            {columns.map((column) => (
              <th
                key={column.key}
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                style={{ width: column.width }}
              >
                {column.header}
              </th>
            ))}
            {actions && (
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {data.map((item) => {
            const key = keyExtractor(item)
            const isSelected = selectedIds.has(Number(key))

            return (
              <tr
                key={key}
                className={clsx(
                  'hover:bg-gray-50 transition-colors',
                  onRowClick && 'cursor-pointer',
                  isSelected && 'bg-blue-50'
                )}
                onClick={() => onRowClick?.(item)}
                data-entity-id={key}
              >
                {selectable && (
                  <td className="px-6 py-4">
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => onSelectToggle?.(Number(key))}
                      onClick={(e) => e.stopPropagation()}
                      className="rounded border-gray-300"
                      data-test={`select-checkbox-${key}`}
                    />
                  </td>
                )}
                {columns.map((column) => (
                  <td key={column.key} className="px-6 py-4 whitespace-nowrap">
                    {column.accessor(item)}
                  </td>
                ))}
                {actions && (
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="flex justify-end gap-2" onClick={(e) => e.stopPropagation()}>
                      {actions(item)}
                    </div>
                  </td>
                )}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
```

### 9.3 Form Components

**File: `src/components/ui/Input.tsx`**

```typescript
import { forwardRef, InputHTMLAttributes } from 'react'
import { clsx } from 'clsx'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helpText?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helpText, className, ...props }, ref) => {
    return (
      <div className="w-full">
        {label && (
          <label className="block text-sm font-medium text-gray-700 mb-1">
            {label}
            {props.required && <span className="text-red-500 ml-1">*</span>}
          </label>
        )}
        <input
          ref={ref}
          className={clsx(
            'block w-full px-3 py-2 border rounded-md shadow-sm',
            'focus:ring-blue-500 focus:border-blue-500',
            'disabled:bg-gray-100 disabled:cursor-not-allowed',
            error ? 'border-red-300' : 'border-gray-300',
            className
          )}
          {...props}
        />
        {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
        {helpText && !error && <p className="mt-1 text-sm text-gray-500">{helpText}</p>}
      </div>
    )
  }
)

Input.displayName = 'Input'
```

### 9.4 Modal Component

**File: `src/components/ui/Modal.tsx`**

```typescript
import { Fragment, ReactNode } from 'react'
import { Dialog, Transition } from '@headlessui/react'
import { PiX } from 'react-icons/pi'

export interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  children: ReactNode
  size?: 'sm' | 'md' | 'lg' | 'xl'
  footer?: ReactNode
}

export function Modal({
  isOpen,
  onClose,
  title,
  children,
  size = 'md',
  footer,
}: ModalProps) {
  const sizeClasses = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
  }

  return (
    <Transition show={isOpen} as={Fragment}>
      <Dialog onClose={onClose} className="relative z-50">
        {/* Backdrop */}
        <Transition.Child
          as={Fragment}
          enter="ease-out duration-300"
          enterFrom="opacity-0"
          enterTo="opacity-100"
          leave="ease-in duration-200"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <div className="fixed inset-0 bg-black/30" aria-hidden="true" />
        </Transition.Child>

        {/* Modal panel */}
        <div className="fixed inset-0 flex items-center justify-center p-4">
          <Transition.Child
            as={Fragment}
            enter="ease-out duration-300"
            enterFrom="opacity-0 scale-95"
            enterTo="opacity-100 scale-100"
            leave="ease-in duration-200"
            leaveFrom="opacity-100 scale-100"
            leaveTo="opacity-0 scale-95"
          >
            <Dialog.Panel
              className={`w-full ${sizeClasses[size]} bg-white rounded-lg shadow-xl`}
            >
              {/* Header */}
              <div className="flex items-center justify-between px-6 py-4 border-b">
                <Dialog.Title className="text-lg font-semibold">
                  {title}
                </Dialog.Title>
                <button
                  onClick={onClose}
                  className="text-gray-400 hover:text-gray-600 transition-colors"
                  data-test="modal-close"
                >
                  <PiX className="h-6 w-6" />
                </button>
              </div>

              {/* Body */}
              <div className="px-6 py-4">{children}</div>

              {/* Footer */}
              {footer && (
                <div className="px-6 py-4 bg-gray-50 border-t rounded-b-lg">
                  {footer}
                </div>
              )}
            </Dialog.Panel>
          </Transition.Child>
        </div>
      </Dialog>
    </Transition>
  )
}
```

### 9.5 Progress Indicators

**File: `src/components/progress/Spinner.tsx`**

```typescript
import { clsx } from 'clsx'

export interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

export function Spinner({ size = 'md', className }: SpinnerProps) {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-8 w-8',
    lg: 'h-12 w-12',
  }

  return (
    <div
      className={clsx(
        'animate-spin rounded-full border-2 border-gray-200 border-t-blue-600',
        sizeClasses[size],
        className
      )}
      data-test="spinner"
    />
  )
}
```

**File: `src/components/progress/ProgressBar.tsx`**

```typescript
export interface ProgressBarProps {
  value: number // 0-100
  max?: number
  showLabel?: boolean
  className?: string
}

export function ProgressBar({
  value,
  max = 100,
  showLabel = true,
  className,
}: ProgressBarProps) {
  const percentage = Math.min(100, Math.max(0, (value / max) * 100))

  return (
    <div className={className}>
      <div className="flex justify-between mb-1">
        {showLabel && (
          <span className="text-sm font-medium text-gray-700">
            {Math.round(percentage)}%
          </span>
        )}
      </div>
      <div className="w-full bg-gray-200 rounded-full h-2.5">
        <div
          className="bg-blue-600 h-2.5 rounded-full transition-all duration-300"
          style={{ width: `${percentage}%` }}
          data-test="progress-bar-fill"
        />
      </div>
    </div>
  )
}
```

---

## 10. Image Caching with IndexedDB

**See Section 8.3 for the full image caching implementation including:**
- IndexedDB wrapper utility
- useImageCache custom hook
- ThumbnailImage component
- Checksum-based cache invalidation

This maintains the existing photo caching strategy from the vanilla JS implementation.

---

## 11. Consistent Button Styling

**See Section 12 for button styling standards**

---

## 12. Menu & Navigation Hierarchy

**See Section 13 for menu structure with left/right split**

---

## 13. TypeScript Types & DTOs

**See Section 14 for complete DTO type definitions**

---

## 14. CRUD Pattern Examples

**See Section 11 for:**
- Standard CRUD page structure
- Books page complete example
- Filter radio buttons pattern
- Bulk actions toolbar

---

## 15. Implementation Plan

### Phase 1: Project Setup & Core Infrastructure (Week 1)

**1.1 Initialize React + TypeScript + Vite Project**
- [ ] Create new Vite project with React + TypeScript template
- [ ] Configure `tsconfig.json` with strict mode and path aliases
- [ ] Set up Tailwind CSS with custom configuration
- [ ] Configure ESLint and Prettier

**1.2 Install Dependencies**
```bash
npm install react react-dom react-router-dom
npm install @tanstack/react-query @tanstack/react-query-devtools
npm install zustand
npm install @headlessui/react
npm install react-hook-form zod @hookform/resolvers
npm install react-icons clsx date-fns idb
npm install react-cropper
npm install -D @types/react @types/react-dom
npm install -D tailwindcss postcss autoprefixer
npm install -D eslint prettier
```

**1.3 Core Infrastructure**
- [ ] Create project structure (see Section 4)
- [ ] Set up API client with fetch wrapper (`src/api/client.ts`)
- [ ] Configure TanStack Query client (`src/config/queryClient.ts`)
- [ ] Set up Zustand auth store (`src/stores/authStore.ts`)
- [ ] Set up Zustand UI store (`src/stores/uiStore.ts`)
- [ ] Create route configuration (`src/config/routes.ts`)

**1.4 Base Components**
- [ ] Button component with variants
- [ ] Input component
- [ ] Modal/Dialog component
- [ ] Spinner and ProgressBar components
- [ ] ErrorMessage and SuccessMessage components

---

### Phase 2: Authentication & Layout (Week 1-2)

**2.1 Authentication**
- [ ] Login page with form-based auth
- [ ] Google SSO integration
- [ ] Password hashing utility (SHA-256)
- [ ] Protected route components
- [ ] Librarian route components
- [ ] Auth store with persistence

**2.2 Layout**
- [ ] App layout component
- [ ] Navigation component with left/right split
- [ ] Route-based navigation
- [ ] Not found (404) page

---

### Phase 3: Reusable Components (Week 2)

**3.1 Table Components**
- [ ] Generic DataTable component
- [ ] Checkbox selection support
- [ ] Sorting support (if needed)
- [ ] Loading and empty states

**3.2 Form Components**
- [ ] Form wrapper with React Hook Form
- [ ] FormField component
- [ ] Select dropdown
- [ ] Checkbox component
- [ ] Radio button component
- [ ] Textarea component

**3.3 Photo Components**
- [ ] IndexedDB image cache utility
- [ ] useImageCache hook
- [ ] ThumbnailImage component
- [ ] Photo upload component
- [ ] Photo cropper component (react-cropper)
- [ ] Photo gallery component

---

### Phase 4: Books Feature (Week 3)

**4.1 Books API Integration**
- [ ] Book API functions (`src/api/books.ts`)
- [ ] useBooks hook with caching
- [ ] CRUD mutation hooks
- [ ] Photo API integration

**4.2 Books Pages**
- [ ] Books list page with DataTable
- [ ] Book filters (radio buttons)
- [ ] Bulk selection and actions
- [ ] Create book page/modal
- [ ] Edit book page/modal
- [ ] View book page (read-only)

**4.3 Book Photos**
- [ ] Photo upload interface
- [ ] Photo cropping
- [ ] Photo ordering (move left/right)
- [ ] Photo rotation
- [ ] Photo deletion

**4.4 Advanced Features (Deferred to Backend Phase 2)**
- [ ] LOC lookup integration
- [ ] Grok LOC suggestion button (backend required)
- [ ] Book-from-image button (backend required)
- [ ] Books-from-feed integration

---

### Phase 5: Authors Feature (Week 4)

**5.1 Authors API Integration**
- [ ] Author API functions
- [ ] useAuthors hook
- [ ] CRUD mutation hooks
- [ ] Photo API integration

**5.2 Authors Pages**
- [ ] Authors list page
- [ ] Author filters (radio buttons)
- [ ] Bulk selection and actions
- [ ] Create author page/modal
- [ ] Edit author page/modal
- [ ] View author page with books table

**5.3 Author Photos**
- [ ] Photo upload interface
- [ ] Photo management (same as books)

---

### Phase 6: Libraries & Data Management (Week 5)

**6.1 Libraries Page**
- [ ] Library list/table
- [ ] Library CRUD operations
- [ ] Move export/import to Data Management page

**6.2 Data Management Page** (split from Libraries)
- [ ] JSON export with statistics
- [ ] JSON import
- [ ] Photo export/import
- [ ] Export status dashboard

---

### Phase 7: Supporting Features (Week 6)

**7.1 Loans**
- [ ] Loans list page
- [ ] Checkout interface
- [ ] Return functionality
- [ ] Show/hide returned loans toggle

**7.2 Users**
- [ ] Users list page
- [ ] User CRUD operations
- [ ] SSO badge display
- [ ] User settings page

**7.3 Library Cards**
- [ ] Split into "My Library Card" and "Apply for Card"
- [ ] Card design picker for users
- [ ] PDF generation
- [ ] Applications list (librarian)

**7.4 Other Pages**
- [ ] Search page (public)
- [ ] Labels page
- [ ] Global settings page
- [ ] Test data page

---

### Phase 8: Polish & Testing (Week 7)

**8.1 Progress Indicators**
- [ ] Add loading spinners to all async operations
- [ ] Progress bars for image loading
- [ ] Loading overlays for long operations

**8.2 Error Handling**
- [ ] Error boundaries
- [ ] Toast notifications (optional)
- [ ] Consistent error messages

**8.3 Playwright Tests**
- [ ] Update selectors to match new React components
- [ ] Verify all data-test attributes are present
- [ ] Run full test suite and fix failures

**8.4 Performance Optimization**
- [ ] Code splitting by route
- [ ] Lazy loading for heavy components
- [ ] Optimize re-renders with React.memo

---

### Phase 9: Deployment & Documentation (Week 8)

**9.1 Build Configuration**
- [ ] Configure Vite for production builds
- [ ] Set up environment variables
- [ ] Configure Spring Boot to serve React build

**9.2 Documentation**
- [ ] Create this design doc as `front-end-rewrite.md` in project root
- [ ] Document component usage patterns
- [ ] Update README with new dev workflow

**9.3 Migration Cleanup**
- [ ] Remove old vanilla JS files
- [ ] Remove old index.html
- [ ] Clean up unused CSS

---

## 16. Backend Changes (Phase 2 - Post-Frontend)

These will be implemented AFTER the frontend migration is complete:

### 16.1 Database Schema Changes
- [ ] Add `lastModified` field to Authors, Users, Loans tables
- [ ] Add `@PreUpdate` annotations for automatic timestamp updates
- [ ] Add session persistence table for cross-reboot auth

### 16.2 API Changes
- [ ] Fix image checksum calculation for photos
- [ ] Create statistics endpoint for Libraries page
- [ ] Update filter endpoints (without-loc, most-recent) to return summaries
- [ ] Create Author filter endpoints (without-description, zero-books)
- [ ] Add Grok API integration for LOC suggestions
- [ ] Transfer photo datestamp to book during books-from-feed

### 16.3 Label Generation
- [ ] Fix PDF column positioning (0.1" adjustment for column 3)

### 16.4 Login Page Updates
- [ ] Make Marian M image bigger
- [ ] Correct library name to match first branch

---

## 17. Success Criteria

The frontend migration is complete when:

1. ✅ **All features working** - Every feature from vanilla JS is functional in React
2. ✅ **Type-safe** - Full TypeScript coverage with no `any` types
3. ✅ **Consistent CRUD** - All features follow the same CRUD patterns
4. ✅ **Consistent tables** - All tables use DataTable component with checkboxes
5. ✅ **Caching works** - Books use lastModified caching, others use standard caching
6. ✅ **Images cached** - IndexedDB caching for photos with checksum validation
7. ✅ **Progress indicators** - All async operations show loading state
8. ✅ **Button consistency** - All buttons use semantic variant names
9. ✅ **Menu hierarchy** - User/librarian menu items visually separated
10. ✅ **Playwright passes** - All existing UI tests pass with new React components
11. ✅ **Production build** - Optimized Vite build integrates with Spring Boot

---

## 18. Out of Scope for Phase 1

These features require backend changes and will be added in Phase 2:

1. **Grok LOC suggestions** - Requires Grok API backend integration
2. **Book-from-image processing** - Requires AI backend integration
3. **Photo datestamp transfer** - Requires backend logic changes
4. **Statistics endpoint** - Requires new backend endpoint
5. **Filter endpoints returning summaries** - Requires backend API changes
6. **Session persistence** - Requires backend database changes
7. **Image checksum fixes** - Requires backend photo processing changes
8. **Label PDF column adjustment** - Requires backend PDF generation changes

---

## 19. Critical Files to Create

**See Section 4 (Project Structure) for complete file list**

Approximate count: **100+ new React/TypeScript files**

Key files include:
- Core infrastructure (10 files)
- API layer (15 files)
- Components (30+ files)
- Pages (40+ files)
- Hooks (5 files)
- Utils (5 files)
- Types (5 files)

---

## 20. Next Steps

1. ✅ **User approval** of this design document
2. **Initialize Vite project** in `frontend/` directory
3. **Create Git branch** for frontend migration
4. **Begin Phase 1** - Project setup and core infrastructure
5. **Iterative development** following the 9-phase plan
6. **Copy this document** to `front-end-rewrite.md` in project root
7. **Regular commits** and testing throughout migration

---

**END OF DESIGN SPECIFICATION**

Total estimated effort: **7-8 weeks** for complete frontend migration

---
