// (c) Copyright 2025 by Muczynski
import type { UserChipFilters } from '@/utils/userChipFilters'
import { FilterChip } from '@/components/ui/FilterChip'

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
