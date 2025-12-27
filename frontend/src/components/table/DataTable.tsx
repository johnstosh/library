// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import { clsx } from 'clsx'

export interface Column<T> {
  key: string
  header: string
  accessor: (item: T) => ReactNode
  sortable?: boolean
  width?: string
}

export interface DataTableProps<T> {
  data: T[]
  columns: Column<T>[]
  keyExtractor: (item: T) => string | number
  onRowClick?: (item: T) => void
  selectable?: boolean
  selectedIds?: Set<number>
  onSelectToggle?: (id: number) => void
  onSelectAll?: () => void
  selectAll?: boolean
  actions?: (item: T) => ReactNode
  emptyMessage?: string
  isLoading?: boolean
  className?: string
}

export function DataTable<T>({
  data,
  columns,
  keyExtractor,
  onRowClick,
  selectable = false,
  selectedIds = new Set(),
  onSelectToggle,
  onSelectAll,
  selectAll = false,
  actions,
  emptyMessage = 'No data available',
  isLoading = false,
  className,
}: DataTableProps<T>) {
  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
      </div>
    )
  }

  if (data.length === 0) {
    return <div className="text-center py-12 text-gray-500">{emptyMessage}</div>
  }

  return (
    <div className={clsx('overflow-x-auto', className)}>
      <table className="min-w-full divide-y divide-gray-200" data-test="data-table">
        <thead className="bg-gray-50">
          <tr>
            {selectable && (
              <th className="w-12 px-6 py-3 text-left">
                <input
                  type="checkbox"
                  checked={selectAll}
                  onChange={onSelectAll}
                  className="rounded border-gray-300"
                  data-test="select-all-checkbox"
                />
              </th>
            )}
            {columns.map((column) => (
              <th
                key={column.key}
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                style={{ width: column.width }}
              >
                {column.header}
              </th>
            ))}
            {actions && (
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {data.map((item) => {
            const key = keyExtractor(item)
            const isSelected = selectedIds.has(Number(key))

            return (
              <tr
                key={key}
                className={clsx(
                  'hover:bg-gray-50 transition-colors',
                  onRowClick && 'cursor-pointer',
                  isSelected && 'bg-blue-50'
                )}
                onClick={() => onRowClick?.(item)}
                data-entity-id={key}
              >
                {selectable && (
                  <td className="px-6 py-4">
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => onSelectToggle?.(Number(key))}
                      onClick={(e) => e.stopPropagation()}
                      className="rounded border-gray-300"
                      data-test={`select-checkbox-${key}`}
                    />
                  </td>
                )}
                {columns.map((column) => (
                  <td key={column.key} className="px-6 py-4 whitespace-nowrap">
                    {column.accessor(item)}
                  </td>
                ))}
                {actions && (
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div
                      className="flex justify-end gap-2"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {actions(item)}
                    </div>
                  </td>
                )}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
