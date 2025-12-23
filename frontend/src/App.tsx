// (c) Copyright 2025 by Muczynski
import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { LibrarianRoute } from '@/components/auth/LibrarianRoute'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { BooksPage } from '@/pages/books/BooksPage'
import { AuthorsPage } from '@/pages/authors/AuthorsPage'
import { LibrariesPage } from '@/pages/libraries/LibrariesPage'
import { DataManagementPage } from '@/pages/libraries/DataManagementPage'
import { LoansPage } from '@/pages/loans/LoansPage'
import { UsersPage } from '@/pages/users/UsersPage'
import { SearchPage } from '@/pages/search/SearchPage'
import { MyLibraryCardPage } from '@/pages/library-cards/MyLibraryCardPage'
import { ApplicationsPage } from '@/pages/library-cards/ApplicationsPage'
import { LabelsPage } from '@/pages/labels/LabelsPage'

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

      {/* Protected routes (authenticated users) */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/books" replace />} />
          <Route path="/books" element={<BooksPage />} />
          <Route path="/authors" element={<AuthorsPage />} />
          <Route path="/loans" element={<LoansPage />} />
          <Route path="/my-card" element={<MyLibraryCardPage />} />

          {/* Librarian-only routes */}
          <Route element={<LibrarianRoute />}>
            <Route path="/libraries" element={<LibrariesPage />} />
            <Route path="/data-management" element={<DataManagementPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/applications" element={<ApplicationsPage />} />
            <Route path="/labels" element={<LabelsPage />} />
          </Route>
        </Route>
      </Route>

      {/* 404 */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
