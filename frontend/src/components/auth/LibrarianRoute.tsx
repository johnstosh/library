// (c) Copyright 2025 by Muczynski
import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

export function LibrarianRoute() {
  const user = useAuthStore((state) => state.user)

  if (!user || user.authority !== 'LIBRARIAN') {
    return <Navigate to="/books" replace />
  }

  return <Outlet />
}
