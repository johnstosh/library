// (c) Copyright 2025 by Muczynski
import {
  DESIRE_TO_PURCHASE_UNSET,
  DESIRE_TO_PURCHASE_VALUES,
  desireToPurchaseChipLabel,
  type DesireToPurchaseFilter,
} from '@/utils/desireToPurchase'

interface DesireToPurchaseFiltersProps {
  selected: DesireToPurchaseFilter[]
  onToggle: (value: DesireToPurchaseFilter) => void
  onClear: () => void
}

const FILTER_VALUES: DesireToPurchaseFilter[] = [...DESIRE_TO_PURCHASE_VALUES, DESIRE_TO_PURCHASE_UNSET]

export function DesireToPurchaseFilters({
  selected,
  onToggle,
  onClear,
}: DesireToPurchaseFiltersProps) {
  const selectedSet = new Set(selected.map(String))

  return (
    <div className="mt-3" data-test="desire-to-purchase-filters">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-sm font-medium text-gray-700">Desire to Purchase</span>
        {selected.length > 0 && (
          <button
            type="button"
            onClick={onClear}
            data-test="desire-to-purchase-filter-clear"
            className="px-2.5 py-0.5 rounded-full text-xs font-medium border border-red-300 text-red-600 bg-white hover:bg-red-50 transition-colors"
          >
            Clear desire to purchase
          </button>
        )}
      </div>
      <div className="flex flex-wrap gap-1.5" data-test="desire-to-purchase-filter-wrap">
        {FILTER_VALUES.map((value) => {
          const isSelected = selectedSet.has(String(value))
          return (
            <button
              key={String(value)}
              type="button"
              onClick={() => onToggle(value)}
              data-test={`desire-to-purchase-filter-${value}`}
              className={[
                'px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors whitespace-nowrap',
                isSelected
                  ? 'bg-primary-600 text-white border-primary-600 hover:bg-primary-700'
                  : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50 hover:border-gray-400',
              ].join(' ')}
              aria-pressed={isSelected}
            >
              {desireToPurchaseChipLabel(value)}
            </button>
          )
        })}
      </div>
    </div>
  )
}
