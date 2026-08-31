// (c) Copyright 2025 by Muczynski
import { Outlet } from 'react-router-dom'
import { Navigation } from './Navigation'
import { ToastProvider } from '@/hooks/useToast'

export function AppLayout() {
  return (
    <ToastProvider>
      <div className="min-h-screen">
        <Navigation />
        <main className="mx-[2%] py-4 sm:py-8">
          <Outlet />
        </main>
      </div>
    </ToastProvider>
  )
}
