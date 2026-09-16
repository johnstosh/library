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
        label="Hardcover"
        active={chips.hardcover}
        onClick={() => onToggle('hardcover')}
        tooltip="Only hardcover AbeBooks listings"
        dataTest="filter-price-hardcover"
      />
      <FilterChip
        label="Softcover"
        active={chips.softcover}
        onClick={() => onToggle('softcover')}
        tooltip="Only softcover AbeBooks listings"
        dataTest="filter-price-softcover"
      />
      <FilterChip
        label="Other/Unknown"
        active={chips.otherUnknown}
        onClick={() => onToggle('otherUnknown')}
        tooltip="Only listings whose binding is not hardcover or softcover"
        dataTest="filter-price-other-unknown"
      />
      <FilterChip
        label="Has listing"
        active={chips.hasListing}
        onClick={() => onToggle('hasListing')}
        tooltip="Only rows that have a price from AbeBooks"
        dataTest="filter-price-has-listing"
      />
      <FilterChip
        label="Lookup failed"
        active={chips.lookupFailed}
        onClick={() => onToggle('lookupFailed')}
        tooltip="Only rows with no matching listing or a lookup error"
        dataTest="filter-price-lookup-failed"
      />
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
