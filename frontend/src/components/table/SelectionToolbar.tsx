// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import { Button } from '@/components/ui/Button'
import { BranchNameDisplay } from '@/components/layout/BranchNameDisplay'

/**
 * Shared slot for the bulk-action carousel and the empty-selection stats
 * placeholder. Same padding and min-height in both states so the table does
 * not jump when checkboxes are toggled.
 */
export function SelectionToolbar({
  children,
  dataTest,
  selected,
}: {
  children: ReactNode
  dataTest: string
  selected: boolean
}) {
  return (
    <div
      className={`border rounded-lg px-4 py-3 mb-4 min-h-[3.75rem] flex items-stretch ${
        selected ? 'bg-primary-50 border-primary-200' : 'bg-gray-50 border-gray-200 items-center'
      }`}
      data-test={dataTest}
    >
      {children}
    </div>
  )
}

/**
 * Horizontally scrolling bulk-action buttons. Labels wrap so each button grows
 * taller instead of staying one wide line of text.
 */
export function ActionCarousel({ children }: { children: ReactNode }) {
  return (
    <div
      className="flex gap-2 overflow-x-auto flex-nowrap min-w-0 items-stretch [&_button]:shrink-0 [&_button]:h-auto [&_button]:max-w-[8.5rem] [&_button]:whitespace-normal [&_button]:break-words [&_button]:leading-snug [&_button]:text-left [&_button]:py-2"
      data-test="action-carousel"
    >
      {children}
    </div>
  )
}

/** Count + Clear Selection, stacked so the action carousel has room on a phone. */
export function SelectionSummary({
  count,
  singular,
  plural,
  onClear,
}: {
  count: number
  singular: string
  plural: string
  onClear: () => void
}) {
  return (
    <div className="flex flex-col items-start gap-0.5 min-w-0 max-w-full sm:max-w-[9.5rem]">
      <span className="text-sm font-medium text-primary-900 whitespace-normal break-words leading-snug">
        {count} {count === 1 ? singular : plural} selected
      </span>
      <Button
        variant="ghost"
        size="sm"
        className="whitespace-normal h-auto py-1 px-2 text-left"
        onClick={onClear}
        data-test="clear-selection"
      >
        Clear Selection
      </Button>
    </div>
  )
}

export function TableCountPlaceholder({
  tableCount,
  totalCount,
  singular,
  plural,
  isLoading = false,
  branchName,
  librarySystemName,
}: {
  tableCount: number
  totalCount?: number
  singular: string
  plural: string
  isLoading?: boolean
  branchName?: string
  librarySystemName?: string
}) {
  const tableNoun = tableCount === 1 ? singular : plural
  const totalNoun = totalCount === 1 ? singular : plural

  return (
    <div
      className="flex flex-col justify-center gap-1 w-full min-w-0"
      data-test="table-stats-placeholder"
    >
      {branchName && librarySystemName && (
        <BranchNameDisplay
          branchName={branchName}
          librarySystemName={librarySystemName}
          dataTest="table-branch-name"
        />
      )}
      <div className="flex items-center gap-x-6 gap-y-1 text-sm text-gray-700 whitespace-nowrap overflow-x-auto w-full">
        {isLoading ? (
          <span>Loading {plural}…</span>
        ) : (
          <>
            <span data-test="table-count">
              <span className="font-semibold">{tableCount.toLocaleString('en-US')}</span>
              {' '}{tableNoun} in this table
            </span>
            {totalCount != null && (
              <span data-test="database-count">
                <span className="font-semibold">{totalCount.toLocaleString('en-US')}</span>
                {' '}{totalNoun} in the database
              </span>
            )}
          </>
        )}
      </div>
    </div>
  )
}
