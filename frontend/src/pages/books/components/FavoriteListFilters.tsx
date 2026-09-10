// (c) Copyright 2025 by Muczynski
import { listNameToTestId } from '@/api/favorites'

export interface FavoriteListChip {
  listName: string
  count: number
}

interface FavoriteListFiltersProps {
  lists: FavoriteListChip[]
  selected: string[]
  onToggle: (listName: string) => void
  onClear: () => void
}

export function FavoriteListFilters({ lists, selected, onToggle, onClear }: FavoriteListFiltersProps) {
  if (lists.length === 0 && selected.length === 0) {
    return null
  }

  const selectedSet = new Set(selected)
  const visible = lists.filter((list) => list.count > 0 || selectedSet.has(list.listName))

  if (visible.length === 0) {
    return null
  }

  return (
    <div className="mt-3" data-test="favorite-list-filters">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-sm font-medium text-gray-700">Favorites</span>
        {selected.length > 0 && (
          <button
            type="button"
            onClick={onClear}
            data-test="favorite-list-filter-clear"
            className="px-2.5 py-0.5 rounded-full text-xs font-medium border border-red-300 text-red-600 bg-white hover:bg-red-50 transition-colors"
          >
            Clear favorites
          </button>
        )}
      </div>
      <div className="flex flex-wrap gap-1.5" data-test="favorite-list-filter-wrap">
        {visible.map((list) => {
          const isSelected = selectedSet.has(list.listName)
          return (
            <button
              key={list.listName}
              type="button"
              onClick={() => onToggle(list.listName)}
              data-test={`favorite-filter-${listNameToTestId(list.listName)}`}
              className={[
                'px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors whitespace-nowrap',
                isSelected
                  ? 'border-primary-500 bg-primary-50 text-primary-700'
                  : 'border-gray-300 text-gray-600 bg-white hover:border-primary-400 hover:text-primary-600',
              ].join(' ')}
            >
              {list.count} {list.listName}
            </button>
          )
        })}
      </div>
    </div>
  )
}
