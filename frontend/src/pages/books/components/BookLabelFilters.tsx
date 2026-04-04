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

interface BookLabelFiltersProps {
  selectedLabels: string[]
  onToggleLabel: (label: string) => void
  onClearLabels: () => void
}

export function BookLabelFilters({ selectedLabels, onToggleLabel, onClearLabels }: BookLabelFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-1.5 mt-3" data-test="book-label-filters">
      <span className="text-sm font-medium text-gray-700 mr-1 shrink-0">Labels:</span>
      {ALL_BOOK_LABELS.map((label) => {
        const isSelected = selectedLabels.includes(label)
        return (
          <button
            key={label}
            type="button"
            onClick={() => onToggleLabel(label)}
            data-test={`label-filter-${label}`}
            className={[
              'px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors',
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
      {selectedLabels.length > 0 && (
        <button
          type="button"
          onClick={onClearLabels}
          data-test="label-filter-clear"
          className="px-2.5 py-0.5 rounded-full text-xs font-medium border border-red-300 text-red-600 bg-white hover:bg-red-50 transition-colors ml-1"
        >
          Clear labels
        </button>
      )}
    </div>
  )
}
