// (c) Copyright 2025 by Muczynski
import { useEffect } from 'react'
import { Routes, Route } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/progress/Spinner'

function App() {
  const checkAuth = useAuthStore((state) => state.checkAuth)
  const isLoading = useAuthStore((state) => state.isLoading)

  useEffect(() => {
    checkAuth() // Check authentication on app load
  }, [checkAuth])

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner size="lg" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          Library Management System
        </h1>
        <p className="text-lg text-gray-600 mb-8">
          React + TypeScript Frontend - Phase 1 Setup Complete
        </p>

        <div className="space-y-4">
          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-2xl font-semibold mb-4">Infrastructure Status</h2>
            <ul className="space-y-2 text-gray-700">
              <li className="flex items-center">
                <span className="text-green-600 mr-2">✓</span>
                Vite + React + TypeScript configured
              </li>
              <li className="flex items-center">
                <span className="text-green-600 mr-2">✓</span>
                Tailwind CSS integrated
              </li>
              <li className="flex items-center">
                <span className="text-green-600 mr-2">✓</span>
                TanStack Query configured
              </li>
              <li className="flex items-center">
                <span className="text-green-600 mr-2">✓</span>
                Zustand stores (auth, UI) created
              </li>
              <li className="flex items-center">
                <span className="text-green-600 mr-2">✓</span>
                Base UI components created
              </li>
              <li className="flex items-center">
                <span className="text-green-600 mr-2">✓</span>
                API client with fetch wrapper
              </li>
            </ul>
          </div>

          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-2xl font-semibold mb-4">Component Demo</h2>
            <div className="flex gap-4 flex-wrap">
              <Button variant="primary">Primary Button</Button>
              <Button variant="secondary">Secondary Button</Button>
              <Button variant="danger">Danger Button</Button>
              <Button variant="outline">Outline Button</Button>
              <Button variant="ghost">Ghost Button</Button>
            </div>
            <div className="mt-4">
              <Button variant="primary" isLoading>
                Loading Button
              </Button>
            </div>
          </div>

          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-2xl font-semibold mb-4">Next Steps</h2>
            <p className="text-gray-700">
              Phase 1 infrastructure is complete. Next phases will add:
            </p>
            <ul className="list-disc list-inside mt-2 text-gray-700">
              <li>Authentication pages (login, SSO)</li>
              <li>Layout components (navigation, routes)</li>
              <li>Books feature (CRUD, photos, filters)</li>
              <li>Authors, Libraries, Loans, Users features</li>
              <li>Advanced components (data tables, forms, image handling)</li>
            </ul>
          </div>
        </div>

        <Routes>
          <Route path="/" element={<div />} />
        </Routes>
      </div>
    </div>
  )
}

export default App
