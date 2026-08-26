// (c) Copyright 2025 by Muczynski
import { Spinner } from './Spinner'

export function LoadingOverlay({ show }: { show: boolean }) {
  if (!show) return null

  return (
    <div className="absolute inset-0 z-10 flex items-center justify-center bg-white/60 rounded-lg">
      <Spinner size="lg" />
    </div>
  )
}
