// (c) Copyright 2025 by Muczynski
import { FilterChip } from '@/components/ui/FilterChip'
import type { PriceChipFilters } from '@/utils/priceFilters'

interface PriceFiltersProps {
  chips: PriceChipFilters
  onToggle: (chip: keyof PriceChipFilters) => void
}

export function PriceFilters({ chips, onToggle }: PriceFiltersProps) {
  return (
    <div className="flex flex-wrap gap-2" data-test="price-filter-chips">
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
        tooltip="Only listings looked up in the last 30 days"
        dataTest="filter-price-recent"
      />
    </div>
  )
}
