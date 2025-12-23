// (c) Copyright 2025 by Muczynski
import { useUiStore, useAuthorsFilter } from '@/stores/uiStore'

type FilterValue = 'all' | 'without-description' | 'zero-books'

interface FilterOption {
  value: FilterValue
  label: string
}

const filterOptions: FilterOption[] = [
  { value: 'all', label: 'All Authors' },
  { value: 'without-description', label: 'Without Description' },
  { value: 'zero-books', label: 'Zero Books' },
]

export function AuthorFilters() {
  const currentFilter = useAuthorsFilter()
  const setFilter = useUiStore((state) => state.setFilter)

  const handleFilterChange = (value: FilterValue) => {
    setFilter('authors', value)
  }

  return (
    <div className="flex items-center gap-4">
      <span className="text-sm font-medium text-gray-700">Filter:</span>
      <div className="flex gap-4">
        {filterOptions.map((option) => (
          <label key={option.value} className="flex items-center cursor-pointer">
            <input
              type="radio"
              name="author-filter"
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
