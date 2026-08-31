// (c) Copyright 2025 by Muczynski
import type { LoanChipFilters } from '@/utils/loanChipFilters'
import { FilterChip } from '@/components/ui/FilterChip'

interface LoanFiltersProps {
  chips: LoanChipFilters
  onToggle: (chip: keyof LoanChipFilters) => void
}

export function LoanFilters({ chips, onToggle }: LoanFiltersProps) {
  return (
    <div className="flex flex-wrap gap-2" data-test="loan-filter-chips">
      <FilterChip
        label="Active"
        active={chips.active}
        onClick={() => onToggle('active')}
        tooltip="Only loans that have not been returned"
        dataTest="filter-active"
      />
      <FilterChip
        label="Returned"
        active={chips.returned}
        onClick={() => onToggle('returned')}
        tooltip="Only loans that have been returned"
        dataTest="filter-returned"
      />
      <FilterChip
        label="Overdue"
        active={chips.overdue}
        onClick={() => onToggle('overdue')}
        tooltip="Only outstanding loans past their due date"
        dataTest="filter-overdue"
      />
      <FilterChip
        label="Has Photo"
        active={chips.hasPhoto}
        onClick={() => onToggle('hasPhoto')}
        tooltip="Only loans with a checkout-card photo"
        dataTest="filter-has-photo"
      />
      <FilterChip
        label="Recent Arrivals"
        active={chips.mostRecent}
        onClick={() => onToggle('mostRecent')}
        tooltip="Only loans checked out on the most recent checkout day"
        dataTest="filter-most-recent"
      />
    </div>
  )
}
