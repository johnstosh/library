// (c) Copyright 2025 by Muczynski
import { useUiStore, useBooksFilter } from '@/stores/uiStore'

type FilterValue = 'all' | 'most-recent' | 'without-loc' | '3-letter-loc' | 'without-grokipedia'

interface FilterOption {
  value: FilterValue
  label: string
}

const filterOptions: FilterOption[] = [
  { value: 'all', label: 'All Books' },
  { value: 'most-recent', label: 'Most Recent Day' },
  { value: 'without-loc', label: 'Without LOC' },
  { value: '3-letter-loc', label: '3-Letter Call Numbers' },
  { value: 'without-grokipedia', label: 'Without Grokipedia' },
]

export function BookFilters() {
  const currentFilter = useBooksFilter()
  const setFilter = useUiStore((state) => state.setFilter)

  const handleFilterChange = (value: FilterValue) => {
    setFilter('books', value)
  }

  return (
    <div className="flex items-center gap-4">
      <span className="text-sm font-medium text-gray-700">Filter:</span>
      <div className="flex gap-4">
        {filterOptions.map((option) => (
          <label key={option.value} className="flex items-center cursor-pointer">
            <input
              type="radio"
              name="book-filter"
              value={option.value}
              checked={currentFilter === option.value}
              onChange={() => handleFilterChange(option.value)}
              className="h-4 w-4 text-blue-600 border-gray-300 focus:ring-blue-500"
              data-test={`filter-${option.value}`}
            />
            <span className="ml-2 text-sm text-gray-700">{option.label}</span>
          </label>
        ))}
      </div>
    </div>
  )
}
