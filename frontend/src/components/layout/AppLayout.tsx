// (c) Copyright 2025 by Muczynski
import { Outlet } from 'react-router-dom'
import { Navigation } from './Navigation'

export function AppLayout() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navigation />
      <main className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}
