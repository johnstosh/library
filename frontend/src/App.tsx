// (c) Copyright 2025 by Muczynski
import { useEffect, lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { LibrarianRoute } from '@/components/auth/LibrarianRoute'
import { ErrorBoundary } from '@/components/errors/ErrorBoundary'
import { Spinner } from '@/components/progress/Spinner'

// Eager-loaded pages (small and frequently accessed)
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { ApplyForCardPage } from '@/pages/library-cards/ApplyForCardPage'

// Lazy-loaded pages (code splitting)
const BooksPage = lazy(() => import('@/pages/books/BooksPage').then(m => ({ default: m.BooksPage })))
const AuthorsPage = lazy(() => import('@/pages/authors/AuthorsPage').then(m => ({ default: m.AuthorsPage })))
const LibrariesPage = lazy(() => import('@/pages/libraries/LibrariesPage').then(m => ({ default: m.LibrariesPage })))
const DataManagementPage = lazy(() => import('@/pages/libraries/DataManagementPage').then(m => ({ default: m.DataManagementPage })))
const LoansPage = lazy(() => import('@/pages/loans/LoansPage').then(m => ({ default: m.LoansPage })))
const UsersPage = lazy(() => import('@/pages/users/UsersPage').then(m => ({ default: m.UsersPage })))
const SearchPage = lazy(() => import('@/pages/search/SearchPage').then(m => ({ default: m.SearchPage })))
const MyLibraryCardPage = lazy(() => import('@/pages/library-cards/MyLibraryCardPage').then(m => ({ default: m.MyLibraryCardPage })))
const ApplicationsPage = lazy(() => import('@/pages/library-cards/ApplicationsPage').then(m => ({ default: m.ApplicationsPage })))
const LabelsPage = lazy(() => import('@/pages/labels/LabelsPage').then(m => ({ default: m.LabelsPage })))
const UserSettingsPage = lazy(() => import('@/pages/settings/UserSettingsPage').then(m => ({ default: m.UserSettingsPage })))
const GlobalSettingsPage = lazy(() => import('@/pages/settings/GlobalSettingsPage').then(m => ({ default: m.GlobalSettingsPage })))
const TestDataPage = lazy(() => import('@/pages/test-data/TestDataPage').then(m => ({ default: m.TestDataPage })))
const BooksFromFeedPage = lazy(() => import('@/pages/books-from-feed/BooksFromFeedPage').then(m => ({ default: m.BooksFromFeedPage })))

// Loading fallback component
function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <Spinner size="lg" />
    </div>
  )
}

function App() {
  const checkAuth = useAuthStore((state) => state.checkAuth)

  useEffect(() => {
    checkAuth() // Check authentication on app load
  }, [checkAuth])

  return (
    <ErrorBoundary>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/apply" element={<ApplyForCardPage />} />

          {/* Protected routes (authenticated users) */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/books" replace />} />
              <Route path="/books" element={<BooksPage />} />
              <Route path="/authors" element={<AuthorsPage />} />
              <Route path="/loans" element={<LoansPage />} />
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
      </Suspense>
    </ErrorBoundary>
  )
}

export default App
