// (c) Copyright 2025 by Muczynski
import type { AuthorChipFilters } from '@/utils/authorChipFilters'
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

interface AuthorFiltersProps {
  chips: AuthorChipFilters
  onToggle: (chip: keyof AuthorChipFilters) => void
}

export function AuthorFilters({ chips, onToggle }: AuthorFiltersProps) {
  return (
    <div className="flex flex-wrap gap-2" data-test="author-filter-chips">
      <FilterChip
        label="Most Recent Day"
        active={chips.mostRecent}
        onClick={() => onToggle('mostRecent')}
        tooltip="Only authors of books added on the most recent day"
        dataTest="filter-most-recent"
      />
      <FilterChip
        label="Without Description"
        active={chips.withoutDescription}
        onClick={() => onToggle('withoutDescription')}
        tooltip="Only authors with no brief biography"
        dataTest="filter-without-description"
      />
      <FilterChip
        label="Without Grokipedia"
        active={chips.withoutGrokipedia}
        onClick={() => onToggle('withoutGrokipedia')}
        tooltip="Only authors without a Grokipedia URL"
        dataTest="filter-without-grokipedia"
      />
      <FilterChip
        label="With Grokipedia"
        active={chips.withGrokipedia}
        onClick={() => onToggle('withGrokipedia')}
        tooltip="Only authors that have a Grokipedia URL"
        dataTest="filter-with-grokipedia"
      />
      <FilterChip
        label="Zero Books"
        active={chips.zeroBooks}
        onClick={() => onToggle('zeroBooks')}
        tooltip="Only authors with no books in the catalog"
        dataTest="filter-zero-books"
      />
      <FilterChip
        label="Without Photos"
        active={chips.withoutPhotos}
        onClick={() => onToggle('withoutPhotos')}
        tooltip="Only authors with no photo"
        dataTest="filter-without-photos"
      />
      <FilterChip
        label="With Photos"
        active={chips.withPhotos}
        onClick={() => onToggle('withPhotos')}
        tooltip="Only authors that have a photo"
        dataTest="filter-with-photos"
      />
      <FilterChip
        label="Without Birth Date"
        active={chips.withoutBirthDate}
        onClick={() => onToggle('withoutBirthDate')}
        tooltip="Only authors missing a date of birth"
        dataTest="filter-without-birth-date"
      />
      <FilterChip
        label="Without Death Date"
        active={chips.withoutDeathDate}
        onClick={() => onToggle('withoutDeathDate')}
        tooltip="Only authors missing a date of death"
        dataTest="filter-without-death-date"
      />
    </div>
  )
}
