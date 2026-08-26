// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import { clsx } from 'clsx'

export interface PageHeaderProps {
  title: ReactNode
  description?: ReactNode
  actions?: ReactNode
  size?: 'lg' | 'md'
  className?: string
}

export function PageHeader({
  title,
  description,
  actions,
  size = 'lg',
  className,
}: PageHeaderProps) {
  return (
    <div className={clsx('flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6 gap-3', className)}>
      <div>
        <h1
          className={clsx(
            'font-bold text-gray-900',
            size === 'lg' ? 'text-3xl' : 'text-2xl'
          )}
        >
          {title}
        </h1>
        {description && <p className="text-gray-600 mt-1">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}
