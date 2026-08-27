// (c) Copyright 2025 by Muczynski
import type { UserChipFilters } from '@/utils/userChipFilters'
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

interface UserFiltersProps {
  chips: UserChipFilters
  onToggle: (chip: keyof UserChipFilters) => void
}

export function UserFilters({ chips, onToggle }: UserFiltersProps) {
  return (
    <div className="flex flex-wrap gap-2" data-test="user-filter-chips">
      <FilterChip
        label="Librarian"
        active={chips.librarian}
        onClick={() => onToggle('librarian')}
        tooltip="Only users with the Librarian authority"
        dataTest="filter-librarian"
      />
      <FilterChip
        label="User"
        active={chips.user}
        onClick={() => onToggle('user')}
        tooltip="Only users with the User authority"
        dataTest="filter-user"
      />
      <FilterChip
        label="SSO"
        active={chips.sso}
        onClick={() => onToggle('sso')}
        tooltip="Only users signed in with SSO"
        dataTest="filter-sso"
      />
      <FilterChip
        label="Local account"
        active={chips.localAccount}
        onClick={() => onToggle('localAccount')}
        tooltip="Only users without SSO"
        dataTest="filter-local-account"
      />
      <FilterChip
        label="Has active loans"
        active={chips.hasActiveLoans}
        onClick={() => onToggle('hasActiveLoans')}
        tooltip="Only users with at least one active loan"
        dataTest="filter-has-active-loans"
      />
      <FilterChip
        label="No active loans"
        active={chips.noActiveLoans}
        onClick={() => onToggle('noActiveLoans')}
        tooltip="Only users with no active loans"
        dataTest="filter-no-active-loans"
      />
      <FilterChip
        label="Has email"
        active={chips.hasEmail}
        onClick={() => onToggle('hasEmail')}
        tooltip="Only users with an email address"
        dataTest="filter-has-email"
      />
      <FilterChip
        label="Without email"
        active={chips.withoutEmail}
        onClick={() => onToggle('withoutEmail')}
        tooltip="Only users missing an email address"
        dataTest="filter-without-email"
      />
      <FilterChip
        label="Google Photos"
        active={chips.googlePhotos}
        onClick={() => onToggle('googlePhotos')}
        tooltip="Only users with Google Photos connected"
        dataTest="filter-google-photos"
      />
    </div>
  )
}
