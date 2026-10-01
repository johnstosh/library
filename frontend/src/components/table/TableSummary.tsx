// (c) Copyright 2025 by Muczynski

export interface TableSummaryProps {
  count: number
  /** Matching rows that have not all been loaded yet. */
  total?: number
  singular: string
  plural: string
  isLoading?: boolean
}

export function TableSummary({ count, total, singular, plural, isLoading = false }: TableSummaryProps) {
  if (isLoading || count === 0) return null
  const noun = count === 1 && (total == null || total === count) ? singular : plural
  const label = total != null && total !== count
    ? `Showing ${count} of ${total} ${total === 1 ? singular : plural}`
    : `Showing ${count} ${noun}`

  return (
    <div className="px-4 py-3 border-t border-gray-200 bg-gray-50">
      <p className="text-sm text-gray-700">
        {label}
      </p>
    </div>
  )
}
