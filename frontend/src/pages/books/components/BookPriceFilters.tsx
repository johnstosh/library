// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import type { BookChipFilters } from '@/utils/bookChipFilters'

interface BookPriceFiltersProps {
  chips: BookChipFilters
  onToggle: (chip: keyof BookChipFilters) => void
  priceOlderDays: number
  onPriceOlderDaysChange: (days: number) => void
  /** Listing-level chip (Looked up recently) shown on the Prices page. */
  listingFilters?: ReactNode
  maxTotal?: string
  onMaxTotalChange?: (value: string) => void
}

const chipClass = (active: boolean) =>
  [
    'px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors whitespace-nowrap',
    active
      ? 'bg-primary-600 text-white border-primary-600 hover:bg-primary-700'
      : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50 hover:border-gray-400',
  ].join(' ')

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
          <button
            type="button"
            onClick={() => onToggle('withPrices')}
            data-test="filter-with-prices"
            className={chipClass(chips.withPrices)}
            aria-pressed={chips.withPrices}
          >
            Books with Pricing
          </button>
          <button
            type="button"
            onClick={() => onToggle('noPrices')}
            data-test="filter-no-prices"
            className={chipClass(chips.noPrices)}
            aria-pressed={chips.noPrices}
          >
            Books without Pricing
          </button>
          <button
            type="button"
            onClick={() => onToggle('lookupErrors')}
            data-test="filter-lookup-errors"
            className={chipClass(chips.lookupErrors)}
            aria-pressed={chips.lookupErrors}
          >
            Lookup Errors
          </button>
          <button
            type="button"
            onClick={() => onToggle('priceOlder')}
            data-test="filter-price-older"
            className={chipClass(chips.priceOlder)}
            aria-pressed={chips.priceOlder}
          >
            Price older than
          </button>
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
