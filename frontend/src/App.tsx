// (c) Copyright 2025 by Muczynski
import { useEffect, lazy, Suspense } from 'react'
import { Routes, Route, useNavigate } from 'react-router-dom'
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
const BookNewPage = lazy(() => import('@/pages/books/BookNewPage').then(m => ({ default: m.BookNewPage })))
const BookEditPage = lazy(() => import('@/pages/books/BookEditPage').then(m => ({ default: m.BookEditPage })))
const BookViewPage = lazy(() => import('@/pages/books/BookViewPage').then(m => ({ default: m.BookViewPage })))
const AuthorsPage = lazy(() => import('@/pages/authors/AuthorsPage').then(m => ({ default: m.AuthorsPage })))
const AuthorNewPage = lazy(() => import('@/pages/authors/AuthorNewPage').then(m => ({ default: m.AuthorNewPage })))
const AuthorEditPage = lazy(() => import('@/pages/authors/AuthorEditPage').then(m => ({ default: m.AuthorEditPage })))
const AuthorViewPage = lazy(() => import('@/pages/authors/AuthorViewPage').then(m => ({ default: m.AuthorViewPage })))
const BranchesPage = lazy(() => import('./pages/branches/BranchesPage').then(m => ({ default: m.BranchesPage })))
const BranchNewPage = lazy(() => import('./pages/branches/BranchNewPage').then(m => ({ default: m.BranchNewPage })))
const BranchEditPage = lazy(() => import('./pages/branches/BranchEditPage').then(m => ({ default: m.BranchEditPage })))
const BranchViewPage = lazy(() => import('./pages/branches/BranchViewPage').then(m => ({ default: m.BranchViewPage })))
const DataManagementPage = lazy(() => import('@/pages/branches/DataManagementPage').then(m => ({ default: m.DataManagementPage })))
const LoansPage = lazy(() => import('@/pages/loans/LoansPage').then(m => ({ default: m.LoansPage })))
const LoanNewPage = lazy(() => import('@/pages/loans/LoanNewPage').then(m => ({ default: m.LoanNewPage })))
const LoanEditPage = lazy(() => import('@/pages/loans/LoanEditPage').then(m => ({ default: m.LoanEditPage })))
const LoanViewPage = lazy(() => import('@/pages/loans/LoanViewPage').then(m => ({ default: m.LoanViewPage })))
const UsersPage = lazy(() => import('@/pages/users/UsersPage').then(m => ({ default: m.UsersPage })))
const UserNewPage = lazy(() => import('@/pages/users/UserNewPage').then(m => ({ default: m.UserNewPage })))
const UserEditPage = lazy(() => import('@/pages/users/UserEditPage').then(m => ({ default: m.UserEditPage })))
const UserViewPage = lazy(() => import('@/pages/users/UserViewPage').then(m => ({ default: m.UserViewPage })))
const SearchPage = lazy(() => import('@/pages/search/SearchPage').then(m => ({ default: m.SearchPage })))
const MyLibraryCardPage = lazy(() => import('@/pages/library-cards/MyLibraryCardPage').then(m => ({ default: m.MyLibraryCardPage })))
const ApplicationsPage = lazy(() => import('@/pages/library-cards/ApplicationsPage').then(m => ({ default: m.ApplicationsPage })))
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

// Home redirect component - handles OAuth return URL (Alternative 5: state parameter)
function HomeRedirect() {
  const navigate = useNavigate()
  const getAndClearReturnUrl = useAuthStore((state) => state.getAndClearReturnUrl)

  useEffect(() => {
    // Check for saved return URL (from OAuth flow or API 401 redirect)
    const returnUrl = getAndClearReturnUrl()
    navigate(returnUrl || '/books', { replace: true })
  }, [getAndClearReturnUrl, navigate])

  // Show nothing while redirecting
  return null
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
          {/* Public routes with navigation */}
          <Route element={<AppLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/apply" element={<ApplyForCardPage />} />
          </Route>

          {/* Protected routes (authenticated users) */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<HomeRedirect />} />
              <Route path="/books" element={<BooksPage />} />
              <Route path="/books/new" element={<BookNewPage />} />
              <Route path="/books/:id" element={<BookViewPage />} />
              <Route path="/books/:id/edit" element={<BookEditPage />} />
              <Route path="/authors" element={<AuthorsPage />} />
              <Route path="/authors/new" element={<AuthorNewPage />} />
              <Route path="/authors/:id" element={<AuthorViewPage />} />
              <Route path="/authors/:id/edit" element={<AuthorEditPage />} />
              <Route path="/loans" element={<LoansPage />} />
              <Route path="/loans/new" element={<LoanNewPage />} />
              <Route path="/loans/:id" element={<LoanViewPage />} />
              <Route path="/loans/:id/edit" element={<LoanEditPage />} />
              <Route path="/my-card" element={<MyLibraryCardPage />} />
              <Route path="/settings" element={<UserSettingsPage />} />

              {/* Librarian-only routes */}
              <Route element={<LibrarianRoute />}>
                <Route path="/branches" element={<BranchesPage />} />
                <Route path="/branches/new" element={<BranchNewPage />} />
                <Route path="/branches/:id" element={<BranchViewPage />} />
                <Route path="/branches/:id/edit" element={<BranchEditPage />} />
                <Route path="/data-management" element={<DataManagementPage />} />
                <Route path="/users" element={<UsersPage />} />
                <Route path="/users/new" element={<UserNewPage />} />
                <Route path="/users/:id" element={<UserViewPage />} />
                <Route path="/users/:id/edit" element={<UserEditPage />} />
                <Route path="/applications" element={<ApplicationsPage />} />
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
