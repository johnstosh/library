// (c) Copyright 2025 by Muczynski
import type { ApplicationChipFilters } from '@/utils/applicationChipFilters'
import { FilterChip } from '@/components/ui/FilterChip'

interface ApplicationFiltersProps {
  chips: ApplicationChipFilters
  onToggle: (chip: keyof ApplicationChipFilters) => void
}

export function ApplicationFilters({ chips, onToggle }: ApplicationFiltersProps) {
  return (
    <div className="flex flex-wrap gap-2" data-test="application-filter-chips">
      <FilterChip
        label="Needs approval"
        active={chips.needsApproval}
        onClick={() => onToggle('needsApproval')}
        tooltip="Only applications still waiting for a librarian decision (pending or question)"
        dataTest="filter-needs-approval"
      />
      <FilterChip
        label="Pending"
        active={chips.pending}
        onClick={() => onToggle('pending')}
        tooltip="Only applications with pending status"
        dataTest="filter-pending"
      />
      <FilterChip
        label="Question"
        active={chips.question}
        onClick={() => onToggle('question')}
        tooltip="Only applications marked as needing more information"
        dataTest="filter-question"
      />
      <FilterChip
        label="Approved"
        active={chips.approved}
        onClick={() => onToggle('approved')}
        tooltip="Only applications that have already been approved"
        dataTest="filter-approved"
      />
      <FilterChip
        label="Not approved"
        active={chips.notApproved}
        onClick={() => onToggle('notApproved')}
        tooltip="Only applications that were declined"
        dataTest="filter-not-approved"
      />
      <FilterChip
        label="Has email"
        active={chips.hasEmail}
        onClick={() => onToggle('hasEmail')}
        tooltip="Only applications that include an email address"
        dataTest="filter-has-email"
      />
      <FilterChip
        label="Without email"
        active={chips.withoutEmail}
        onClick={() => onToggle('withoutEmail')}
        tooltip="Only applications missing an email address"
        dataTest="filter-without-email"
      />
    </div>
  )
}
