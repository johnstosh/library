// (c) Copyright 2025 by Muczynski
import { useEffect } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { Spinner } from '@/components/progress/Spinner'

export function ProtectedRoute() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const isLoading = useAuthStore((state) => state.isLoading)
  const setReturnUrl = useAuthStore((state) => state.setReturnUrl)
  const location = useLocation()

  // Save intended destination before redirecting to login (Alternative 5: state parameter)
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      const currentPath = location.pathname + location.search
      // Don't save "/" as return URL - it would just redirect to /books anyway
      if (currentPath !== '/') {
        setReturnUrl(currentPath)
      }
    }
  }, [isAuthenticated, isLoading, location, setReturnUrl])

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
