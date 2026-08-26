// (c) Copyright 2025 by Muczynski

export interface TableSummaryProps {
  count: number
  singular: string
  plural: string
  isLoading?: boolean
}

export function TableSummary({ count, singular, plural, isLoading = false }: TableSummaryProps) {
  if (isLoading || count === 0) return null

  return (
    <div className="px-4 py-3 border-t border-gray-200 bg-gray-50">
      <p className="text-sm text-gray-700">
        Showing {count} {count === 1 ? singular : plural}
      </p>
    </div>
  )
}
