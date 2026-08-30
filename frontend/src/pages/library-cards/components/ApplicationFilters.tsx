// (c) Copyright 2025 by Muczynski
import type { ApplicationChipFilters } from '@/utils/applicationChipFilters'
import { PiFunnel } from 'react-icons/pi'

interface FilterChipProps {
  label: string
  active: boolean
  onClick: () => void
  tooltip: string
  dataTest: string
}

function FilterChip({ label, active, onClick, tooltip, dataTest }: FilterChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={tooltip}
      aria-pressed={active}
      data-test={dataTest}
      className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-full border transition-colors cursor-pointer select-none ${
        active
          ? 'border-blue-500 bg-blue-50 text-blue-700 font-medium hover:bg-blue-100'
          : 'border-gray-300 text-gray-600 hover:border-blue-400 hover:text-blue-600 bg-white'
      }`}
    >
      {active ? (
        <svg className="hidden sm:block w-3.5 h-3.5 text-blue-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      ) : (
        <PiFunnel className="hidden sm:block w-3.5 h-3.5 text-gray-400 shrink-0" />
      )}
      {label}
      <span className="hidden sm:inline text-gray-400 text-xs shrink-0" aria-hidden="true">ⓘ</span>
    </button>
  )
}

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
