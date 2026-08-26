// (c) Copyright 2025 by Muczynski
import { PageCard } from './PageCard'
import { Button } from './Button'
import { BackIcon } from './Icons'

export interface EntityNotFoundProps {
  title: string
  entityLabel: string
  onBack: () => void
  backLabel: string
}

export function EntityNotFound({ title, entityLabel, onBack, backLabel }: EntityNotFoundProps) {
  return (
    <div className="max-w-4xl mx-auto">
      <PageCard className="p-8" padding={false}>
        <h1 className="text-2xl font-bold text-gray-900 mb-4">{title}</h1>
        <p className="text-gray-600 mb-4">The {entityLabel} you're looking for doesn't exist.</p>
        <Button variant="ghost" onClick={onBack} leftIcon={<BackIcon />}>
          {backLabel}
        </Button>
      </PageCard>
    </div>
  )
}
