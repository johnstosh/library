// (c) Copyright 2025 by Muczynski
import type { MouseEvent, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { clsx } from 'clsx'
import { TEXT_LINK_CLASS } from './IconButton'

export interface EntityLinkProps {
  to: string
  children: ReactNode
  className?: string
  'data-test'?: string
}

export function EntityLink({ to, children, className, 'data-test': dataTest }: EntityLinkProps) {
  return (
    <Link
      to={to}
      className={clsx(TEXT_LINK_CLASS, className)}
      onClick={(event: MouseEvent) => event.stopPropagation()}
      data-test={dataTest}
    >
      {children}
    </Link>
  )
}
