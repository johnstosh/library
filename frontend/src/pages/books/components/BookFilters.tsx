// (c) Copyright 2025 by Muczynski
import type { BookChipFilters } from '@/utils/bookChipFilters'
import { PiFunnel } from 'react-icons/pi'

// ─── Filter chip component ────────────────────────────────────────────────────

interface FilterChipProps {
  label: string
  active: boolean
  onClick: () => void
  tooltip: string
  dataTest: string
  disabled?: boolean
}

function FilterChip({ label, active, onClick, tooltip, dataTest, disabled = false }: FilterChipProps) {
  return (
    <button
      type="button"
      onClick={disabled ? undefined : onClick}
      title={tooltip}
      data-test={dataTest}
      disabled={disabled}
      aria-disabled={disabled}
      className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-full border transition-colors select-none ${
        disabled
          ? 'border-gray-200 text-gray-400 bg-gray-50 cursor-not-allowed'
          : active
            ? 'border-blue-500 bg-blue-50 text-blue-700 font-medium hover:bg-blue-100 cursor-pointer'
            : 'border-gray-300 text-gray-600 hover:border-blue-400 hover:text-blue-600 bg-white cursor-pointer'
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

// ─── BookFilters ──────────────────────────────────────────────────────────────

interface BookFiltersProps {
  chips: BookChipFilters
  onToggle: (chip: keyof BookChipFilters) => void
  /** Books page hides this chip; Search still shows it. */
  showThreeLetterLoc?: boolean
  /** Books page: Most Recent Day cannot be combined with other filters. */
  mostRecentDisabled?: boolean
}

export function BookFilters({
  chips,
  onToggle,
  showThreeLetterLoc = true,
  mostRecentDisabled = false,
}: BookFiltersProps) {
  const toggle = (chip: keyof BookChipFilters) => onToggle(chip)

  return (
    <div className="space-y-2">
      {/* Row 1: search-style type filters */}
      <div className="flex flex-wrap gap-2" data-test="book-type-filter-chips">
        <FilterChip
          label="In-library materials"
          active={chips.inLibrary}
          onClick={() => toggle('inLibrary')}
          tooltip="Only books with a Library of Congress call number — physically in the collection"
          dataTest="filter-in-library"
        />
        <FilterChip
          label="Electronic resource"
          active={chips.electronic}
          onClick={() => toggle('electronic')}
          tooltip="Only books marked as electronic resources"
          dataTest="filter-electronic"
        />
        <FilterChip
          label="Has free online text"
          active={chips.freeText}
          onClick={() => toggle('freeText')}
          tooltip="Only books that have a free online text URL (e.g., Project Gutenberg, Internet Archive)"
          dataTest="filter-free-text"
        />
        <FilterChip
          label="Has free online audio"
          active={chips.audio}
          onClick={() => toggle('audio')}
          tooltip="Only books with a free LibriVox audio recording"
          dataTest="filter-audio"
        />
      </div>

      {/* Row 2: books-specific filters */}
      <div className="flex flex-wrap gap-2" data-test="book-source-filter-chips">
        {/* Defaults on so BooksPage can call GET /books/most-recent-day. Forced off
            and disabled when any other filter is on — those need the full catalog. */}
        <FilterChip
          label="Most Recent Day"
          active={chips.mostRecent && !mostRecentDisabled}
          onClick={() => toggle('mostRecent')}
          tooltip={
            mostRecentDisabled
              ? 'Most Recent Day cannot be combined with other filters. Turn the others off to use it.'
              : 'Only books added on the most recent day (or with a temporary date-format title)'
          }
          dataTest="filter-most-recent"
          disabled={mostRecentDisabled}
        />
        <FilterChip
          label="Without LOC"
          active={chips.withoutLoc}
          onClick={() => toggle('withoutLoc')}
          tooltip="Only books without a Library of Congress call number"
          dataTest="filter-without-loc"
        />
        {showThreeLetterLoc && (
          <FilterChip
            label="3-Letter Call Numbers"
            active={chips.threeLetterLoc}
            onClick={() => toggle('threeLetterLoc')}
            tooltip="Only books whose LOC call number starts with three uppercase letters"
            dataTest="filter-3-letter-loc"
          />
        )}
        <FilterChip
          label="Without Grokipedia"
          active={chips.withoutGrokipedia}
          onClick={() => toggle('withoutGrokipedia')}
          tooltip="Only books without a Grokipedia URL"
          dataTest="filter-without-grokipedia"
        />
        <FilterChip
          label="Without Genres"
          active={chips.withoutGenres}
          onClick={() => toggle('withoutGenres')}
          tooltip="Only books with no genre tags assigned"
          dataTest="filter-without-genres"
        />
        <FilterChip
          label="Not Active Status"
          active={chips.notActiveStatus}
          onClick={() => toggle('notActiveStatus')}
          tooltip="When off: hide withdrawn books. When on: only books that are not Active (lost, withdrawn, on order, etc.)"
          dataTest="filter-not-active-status"
        />
        <FilterChip
          label="Without Free-Text URLs"
          active={chips.withoutFreeTextUrls}
          onClick={() => toggle('withoutFreeTextUrls')}
          tooltip="Only books that have no free online text URL"
          dataTest="filter-without-free-text-urls"
        />
      </div>
    </div>
  )
}
