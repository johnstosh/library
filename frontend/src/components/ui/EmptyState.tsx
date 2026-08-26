// (c) Copyright 2025 by Muczynski
import { clsx } from 'clsx'

export interface EmptyStateProps {
  message: string
  description?: string
  className?: string
  'data-test'?: string
}

export function EmptyState({ message, description, className, 'data-test': dataTest }: EmptyStateProps) {
  return (
    <div className={clsx('text-center py-12 text-gray-500', className)} data-test={dataTest}>
      <p className="text-lg font-medium">{message}</p>
      {description && <p className="text-sm mt-2">{description}</p>}
    </div>
  )
}
