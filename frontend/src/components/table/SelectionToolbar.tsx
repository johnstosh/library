// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import { Button } from '@/components/ui/Button'

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
        selected ? 'bg-blue-50 border-blue-200' : 'bg-gray-50 border-gray-200 items-center'
      }`}
      data-test={dataTest}
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
      <span className="text-sm font-medium text-blue-900 whitespace-normal break-words leading-snug">
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
}: {
  tableCount: number
  totalCount?: number
  singular: string
  plural: string
  isLoading?: boolean
}) {
  const tableNoun = tableCount === 1 ? singular : plural
  const totalNoun = totalCount === 1 ? singular : plural

  return (
    <div
      className="flex items-center gap-x-6 gap-y-1 text-sm text-gray-700 whitespace-nowrap overflow-x-auto w-full"
      data-test="table-stats-placeholder"
    >
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
  )
}
