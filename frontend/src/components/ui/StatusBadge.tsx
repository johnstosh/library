// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import { clsx } from 'clsx'
import { STATUS_TONE_CLASSES, type StatusTone } from '@/utils/status'

const SHAPE_CLASSES = {
  pill: 'px-2.5 py-0.5 rounded-full',
  rounded: 'px-2 py-0.5 rounded-md',
} as const

export interface StatusBadgeProps {
  tone?: StatusTone
  shape?: keyof typeof SHAPE_CLASSES
  children: ReactNode
  className?: string
  title?: string
  'data-test'?: string
}

export function StatusBadge({
  tone = 'neutral',
  shape = 'pill',
  children,
  className,
  title,
  'data-test': dataTest,
}: StatusBadgeProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center text-xs font-medium',
        SHAPE_CLASSES[shape],
        STATUS_TONE_CLASSES[tone],
        className
      )}
      title={title}
      data-test={dataTest}
    >
      {children}
    </span>
  )
}
