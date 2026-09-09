// (c) Copyright 2025 by Muczynski
import {
  READING_DIFFICULTY_FILTER_LABELS,
  READING_DIFFICULTY_FILTER_VALUES,
} from '@/utils/readingDifficulty'
import type { ReadingDifficulty } from '@/types/enums'

interface ReadingDifficultyFiltersProps {
  selected: string[]
  onToggle: (value: ReadingDifficulty) => void
  onClear: () => void
}

export function ReadingDifficultyFilters({
  selected,
  onToggle,
  onClear,
}: ReadingDifficultyFiltersProps) {
  const selectedSet = new Set(selected)

  return (
    <div className="mt-3" data-test="reading-difficulty-filters">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-sm font-medium text-gray-700">Reading Difficulty</span>
        {selected.length > 0 && (
          <button
            type="button"
            onClick={onClear}
            data-test="reading-difficulty-filter-clear"
            className="px-2.5 py-0.5 rounded-full text-xs font-medium border border-red-300 text-red-600 bg-white hover:bg-red-50 transition-colors"
          >
            Clear reading difficulty
          </button>
        )}
      </div>
      <div
        className="flex flex-wrap gap-1.5"
        data-test="reading-difficulty-filter-wrap"
      >
        {READING_DIFFICULTY_FILTER_VALUES.map((value) => {
          const isSelected = selectedSet.has(value)
          return (
            <button
              key={value}
              type="button"
              onClick={() => onToggle(value)}
              data-test={`reading-difficulty-filter-${value}`}
              className={[
                'px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors whitespace-nowrap',
                isSelected
                  ? 'bg-primary-600 text-white border-primary-600 hover:bg-primary-700'
                  : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50 hover:border-gray-400',
              ].join(' ')}
              aria-pressed={isSelected}
            >
              {READING_DIFFICULTY_FILTER_LABELS[value]}
            </button>
          )
        })}
      </div>
    </div>
  )
}
