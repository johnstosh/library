// (c) Copyright 2025 by Muczynski
import { Button } from './Button'
import { BackIcon } from './Icons'

export interface BackLinkProps {
  onClick: () => void
  children: string
  'data-test'?: string
}

export function BackLink({ onClick, children, 'data-test': dataTest }: BackLinkProps) {
  return (
    <div className="mb-6">
      <Button variant="ghost" onClick={onClick} leftIcon={<BackIcon />} data-test={dataTest}>
        {children}
      </Button>
    </div>
  )
}
