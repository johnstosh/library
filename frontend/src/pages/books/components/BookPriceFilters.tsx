// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import type { BookChipFilters } from '@/utils/bookChipFilters'
import { FilterChip } from '@/components/ui/FilterChip'

interface BookPriceFiltersProps {
  chips: BookChipFilters
  onToggle: (chip: keyof BookChipFilters) => void
  priceOlderDays: number
  onPriceOlderDaysChange: (days: number) => void
  /** Listing-level chips (hardcover, has listing, …) shown on the Prices page. */
  listingFilters?: ReactNode
  maxTotal?: string
  onMaxTotalChange?: (value: string) => void
}

export function BookPriceFilters({
  chips,
  onToggle,
  priceOlderDays,
  onPriceOlderDaysChange,
  listingFilters,
  maxTotal,
  onMaxTotalChange,
}: BookPriceFiltersProps) {
  return (
    <div className="mt-3" data-test="book-price-filters">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-sm font-medium text-gray-700">Pricing</span>
      </div>
      <div className="space-y-2">
        {listingFilters}
        <div className="flex flex-wrap items-center gap-2" data-test="book-price-filter-chips">
          <FilterChip
            label="No prices saved"
            active={chips.noPrices}
            onClick={() => onToggle('noPrices')}
            tooltip="Only books with no AbeBooks price rows"
            dataTest="filter-no-prices"
          />
          <FilterChip
            label="Price older than"
            active={chips.priceOlder}
            onClick={() => onToggle('priceOlder')}
            tooltip="Only books whose latest AbeBooks lookup is older than N days. Combine with No prices saved to find books that need a lookup."
            dataTest="filter-price-older"
          />
          <label className="inline-flex items-center gap-1 text-sm text-gray-600">
            <input
              type="number"
              min={1}
              step={1}
              value={priceOlderDays}
              disabled={!chips.priceOlder}
              onChange={(e) => {
                const n = parseInt(e.target.value, 10)
                if (Number.isFinite(n) && n >= 1) {
                  onPriceOlderDaysChange(n)
                }
              }}
              className="w-16 px-2 py-1 border border-gray-300 rounded-md text-sm disabled:bg-gray-100 disabled:cursor-not-allowed"
              data-test="filter-price-older-days"
              aria-label="Price older than days"
            />
            days
          </label>
          {onMaxTotalChange && (
            <label className="inline-flex items-center gap-1 text-sm text-gray-600">
              Total less than $
              <input
                type="number"
                min={0}
                step={0.01}
                value={maxTotal ?? ''}
                onChange={(e) => onMaxTotalChange(e.target.value)}
                placeholder="—"
                className="w-24 px-2 py-1 border border-gray-300 rounded-md text-sm"
                data-test="prices-max-total"
                aria-label="Total less than dollars"
              />
            </label>
          )}
        </div>
      </div>
    </div>
  )
}
