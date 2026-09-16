// (c) Copyright 2025 by Muczynski
import { createPortal } from 'react-dom'
import { Spinner } from './Spinner'

/**
 * Dims the relatively positioned parent while an indefinite fetch is in progress.
 * The spinner is portaled and position:fixed so it stays in the window center
 * even when the parent (a long table, for example) is taller than the viewport.
 */
export function LoadingOverlay({ show }: { show: boolean }) {
  if (!show) return null

  return (
    <>
      <div
        className="absolute inset-0 z-10 bg-white/60 rounded-lg"
        data-test="loading-overlay"
        aria-hidden="true"
      />
      {createPortal(
        <div
          className="fixed inset-0 z-50 flex items-center justify-center pointer-events-none"
          data-test="loading-overlay-spinner"
          role="status"
          aria-live="polite"
          aria-label="Loading"
        >
          <Spinner size="lg" />
        </div>,
        document.body
      )}
    </>
  )
}
