// (c) Copyright 2025 by Muczynski
import type { PriceChipFilters } from '@/utils/priceFilters'

interface PriceFiltersProps {
  chips: PriceChipFilters
  onToggle: (chip: keyof PriceChipFilters) => void
  recentHours: number
  onRecentHoursChange: (hours: number) => void
}

const chipClass = (active: boolean) =>
  [
    'px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors whitespace-nowrap',
    active
      ? 'bg-primary-600 text-white border-primary-600 hover:bg-primary-700'
      : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50 hover:border-gray-400',
  ].join(' ')

export function PriceFilters({ chips, onToggle, recentHours, onRecentHoursChange }: PriceFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-2" data-test="price-filter-chips">
      <button
        type="button"
        onClick={() => onToggle('recent')}
        data-test="filter-price-recent"
        className={chipClass(chips.recent)}
        aria-pressed={chips.recent}
      >
        Looked up recently
      </button>
      <label className="inline-flex items-center gap-1 text-sm text-gray-600">
        last
        <input
          type="number"
          min={1}
          step={1}
          value={recentHours}
          disabled={!chips.recent}
          onChange={(e) => {
            const n = parseInt(e.target.value, 10)
            if (Number.isFinite(n) && n >= 1) {
              onRecentHoursChange(n)
            }
          }}
          className="w-16 px-2 py-1 border border-gray-300 rounded-md text-sm disabled:bg-gray-100 disabled:cursor-not-allowed"
          data-test="filter-price-recent-hours"
          aria-label="Looked up in the last hours"
        />
        hours
      </label>
    </div>
  )
}
