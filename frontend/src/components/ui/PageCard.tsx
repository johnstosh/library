// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import { clsx } from 'clsx'

export interface PageCardProps {
  children: ReactNode
  className?: string
  padding?: boolean
}

export function PageCard({ children, className, padding = true }: PageCardProps) {
  return (
    <div className={clsx('bg-white rounded-lg shadow', padding && 'p-4 sm:p-6', className)}>
      {children}
    </div>
  )
}
