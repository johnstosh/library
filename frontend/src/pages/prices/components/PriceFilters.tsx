// (c) Copyright 2025 by Muczynski
import { FilterChip } from '@/components/ui/FilterChip'
import type { PriceChipFilters } from '@/utils/priceFilters'

interface PriceFiltersProps {
  chips: PriceChipFilters
  onToggle: (chip: keyof PriceChipFilters) => void
  recentHours: number
  onRecentHoursChange: (hours: number) => void
}

export function PriceFilters({ chips, onToggle, recentHours, onRecentHoursChange }: PriceFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-2" data-test="price-filter-chips">
      <FilterChip
        label="Looked up recently"
        active={chips.recent}
        onClick={() => onToggle('recent')}
        tooltip="Only listings looked up in the last N hours"
        dataTest="filter-price-recent"
      />
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
