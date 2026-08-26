// (c) Copyright 2025 by Muczynski
/* Toast context and hook live with the provider so consumers share one module. */
/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'

export interface ToastApi {
  success: (message: string) => void
  error: (message: string) => void
}

interface ToastItem {
  id: number
  type: 'success' | 'error'
  message: string
}

const ToastContext = createContext<ToastApi | null>(null)

const TOAST_DURATION_MS = 5000

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const nextId = useRef(0)
  const timers = useRef<number[]>([])

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const push = useCallback(
    (type: ToastItem['type'], message: string) => {
      const id = ++nextId.current
      setToasts((current) => [...current, { id, type, message }])
      const timer = window.setTimeout(() => dismiss(id), TOAST_DURATION_MS)
      timers.current.push(timer)
    },
    [dismiss]
  )

  useEffect(() => {
    const activeTimers = timers.current
    return () => {
      activeTimers.forEach((timer) => window.clearTimeout(timer))
    }
  }, [])

  const api = useMemo<ToastApi>(
    () => ({
      success: (message) => push('success', message),
      error: (message) => push('error', message),
    }),
    [push]
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div
        className="fixed top-20 right-4 z-[60] space-y-2 max-w-sm w-[calc(100%-2rem)] pointer-events-none"
        data-test="toast-region"
        aria-live="polite"
      >
        {toasts.map((toast) => (
          <div key={toast.id} className="pointer-events-auto">
            {toast.type === 'success' ? (
              <SuccessMessage message={toast.message} className="shadow-lg" />
            ) : (
              <ErrorMessage message={toast.message} className="shadow-lg" />
            )}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastApi {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider')
  }
  return context
}
