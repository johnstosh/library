// (c) Copyright 2025 by Muczynski
import { useUiStore, useBooksChips } from '@/stores/uiStore'
import type { BooksChips } from '@/stores/uiStore'
import { PiFunnel } from 'react-icons/pi'

// ─── Filter chip component ────────────────────────────────────────────────────

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
      data-test={dataTest}
      className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-full border transition-colors cursor-pointer select-none ${
        active
          ? 'border-blue-500 bg-blue-50 text-blue-700 font-medium hover:bg-blue-100'
          : 'border-gray-300 text-gray-600 hover:border-blue-400 hover:text-blue-600 bg-white'
      }`}
    >
      {active ? (
        <svg className="w-3.5 h-3.5 text-blue-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      ) : (
        <PiFunnel className="w-3.5 h-3.5 text-gray-400 shrink-0" />
      )}
      {label}
      <span className="text-gray-400 text-xs shrink-0" aria-hidden="true">ⓘ</span>
    </button>
  )
}

// ─── BookFilters ──────────────────────────────────────────────────────────────

export function BookFilters() {
  const chips = useBooksChips()
  const { toggleBooksChip } = useUiStore()

  const toggle = (chip: keyof BooksChips) => toggleBooksChip(chip)

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
        <FilterChip
          label="Most Recent Day"
          active={chips.mostRecent}
          onClick={() => toggle('mostRecent')}
          tooltip="Only books added on the most recent day (or with a temporary date-format title)"
          dataTest="filter-most-recent"
        />
        <FilterChip
          label="Without LOC"
          active={chips.withoutLoc}
          onClick={() => toggle('withoutLoc')}
          tooltip="Only books without a Library of Congress call number"
          dataTest="filter-without-loc"
        />
        <FilterChip
          label="3-Letter Call Numbers"
          active={chips.threeLetterLoc}
          onClick={() => toggle('threeLetterLoc')}
          tooltip="Only books whose LOC call number starts with three uppercase letters"
          dataTest="filter-3-letter-loc"
        />
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
          tooltip="Only books that are not in Active status (lost, withdrawn, on order, etc.)"
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
