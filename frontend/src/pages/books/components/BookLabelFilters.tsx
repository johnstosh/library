// (c) Copyright 2025 by Muczynski

export const ALL_BOOK_LABELS = [
  'fiction',
  'slice-of-life',
  'hagiography',
  'saint',
  'fantasy',
  'family',
  'childrens',
  'adult',
  'philosophy',
  'theology',
  'discernment',
  'talking-animals',
  'biography',
  'history',
  'prayer',
  'classic',
  'poetry',
  'science',
  'music',
  'mystery',
  'adventure',
  'romance',
  'humor',
] as const

export type BookLabel = (typeof ALL_BOOK_LABELS)[number]

const GENRES_PER_COLUMN = 3

const SORTED_BOOK_LABELS: BookLabel[] = [...ALL_BOOK_LABELS].sort((a, b) =>
  a.localeCompare(b),
)

/** Column-major groups: fill 3 rows down, then the next column to the right. */
function genreColumns(labels: BookLabel[]): BookLabel[][] {
  const columns: BookLabel[][] = []
  for (let i = 0; i < labels.length; i += GENRES_PER_COLUMN) {
    columns.push(labels.slice(i, i + GENRES_PER_COLUMN))
  }
  return columns
}

interface BookLabelFiltersProps {
  selectedLabels: string[]
  onToggleLabel: (label: string) => void
  onClearLabels: () => void
}

export function BookLabelFilters({ selectedLabels, onToggleLabel, onClearLabels }: BookLabelFiltersProps) {
  const columns = genreColumns(SORTED_BOOK_LABELS)

  return (
    <div className="mt-3" data-test="book-label-filters">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-sm font-medium text-gray-700">Genres</span>
        {selectedLabels.length > 0 && (
          <button
            type="button"
            onClick={onClearLabels}
            data-test="label-filter-clear"
            className="px-2.5 py-0.5 rounded-full text-xs font-medium border border-red-300 text-red-600 bg-white hover:bg-red-50 transition-colors"
          >
            Clear labels
          </button>
        )}
      </div>
      <div className="flex flex-wrap gap-x-4 gap-y-3">
        {columns.map((column) => (
          <div
            key={column[0]}
            className="flex flex-col items-start gap-1.5"
            data-test="label-filter-column"
          >
            {column.map((label) => {
              const isSelected = selectedLabels.includes(label)
              return (
                <button
                  key={label}
                  type="button"
                  onClick={() => onToggleLabel(label)}
                  data-test={`label-filter-${label}`}
                  className={[
                    'px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors whitespace-nowrap',
                    isSelected
                      ? 'bg-blue-600 text-white border-blue-600 hover:bg-blue-700'
                      : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50 hover:border-gray-400',
                  ].join(' ')}
                  aria-pressed={isSelected}
                >
                  {label}
                </button>
              )
            })}
          </div>
        ))}
      </div>
    </div>
  )
}
