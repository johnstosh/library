// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import type { BookChipFilters } from '@/utils/bookChipFilters'
import { PiFunnel } from 'react-icons/pi'
import { EmuIcon, YdlIcon } from '@/components/ui/Icons'

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

interface AvailabilityGroupProps {
  icon: ReactNode
  libraryFull: string
  dataTest: string
  items: {
    chip: keyof BookChipFilters
    label: string
    tooltip: string
    dataTest: string
  }[]
  chips: BookChipFilters
  onToggle: (chip: keyof BookChipFilters) => void
}

function AvailabilityGroup({ icon, libraryFull, dataTest, items, chips, onToggle }: AvailabilityGroupProps) {
  return (
    <div
      className="flex flex-wrap items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2"
      data-test={dataTest}
    >
      <span
        className="inline-flex items-center justify-center w-6 shrink-0 text-gray-500"
        title={libraryFull}
        aria-label={libraryFull}
      >
        {icon}
      </span>
      {items.map((item) => (
        <FilterChip
          key={item.chip}
          label={item.label}
          active={chips[item.chip]}
          onClick={() => onToggle(item.chip)}
          tooltip={item.tooltip}
          dataTest={item.dataTest}
        />
      ))}
    </div>
  )
}

interface BookFiltersProps {
  chips: BookChipFilters
  onToggle: (chip: keyof BookChipFilters) => void
  /** Books page: Most Recent Day cannot be combined with other filters. */
  mostRecentDisabled?: boolean
  showAvailabilityFilters?: boolean
}

export function BookFilters({
  chips,
  onToggle,
  mostRecentDisabled = false,
  showAvailabilityFilters = false,
}: BookFiltersProps) {
  const toggle = (chip: keyof BookChipFilters) => onToggle(chip)

  return (
    <div className="space-y-2">
      {showAvailabilityFilters && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-2">
          <AvailabilityGroup
            icon={<YdlIcon />}
            libraryFull="Ypsilanti District Library"
            dataTest="book-filter-ydl"
            chips={chips}
            onToggle={onToggle}
            items={[
              {
                chip: 'hasYdlAudio',
                label: 'Audio',
                tooltip: 'Has YDL audio — only books with an audiobook held at Ypsilanti District Library',
                dataTest: 'filter-has-ydl-audio',
              },
              {
                chip: 'hasYdlBook',
                label: 'Book',
                tooltip: 'Has YDL book — only books with a physical copy held at Ypsilanti District Library',
                dataTest: 'filter-has-ydl-book',
              },
              {
                chip: 'hasYdlEbook',
                label: 'Ebook',
                tooltip: 'Has YDL ebook — only books with an ebook held at Ypsilanti District Library',
                dataTest: 'filter-has-ydl-ebook',
              },
            ]}
          />
          <AvailabilityGroup
            icon={<EmuIcon />}
            libraryFull="EMU Halle Library"
            dataTest="book-filter-emu"
            chips={chips}
            onToggle={onToggle}
            items={[
              {
                chip: 'hasEmuAudio',
                label: 'Audio',
                tooltip: 'Has EMU audio — only books with an audiobook held at EMU Halle Library',
                dataTest: 'filter-has-emu-audio',
              },
              {
                chip: 'hasEmuBook',
                label: 'Book',
                tooltip: 'Has EMU book — only books with a physical copy held at EMU Halle Library',
                dataTest: 'filter-has-emu-book',
              },
              {
                chip: 'hasEmuEbook',
                label: 'Ebook',
                tooltip: 'Has EMU ebook — only books with an ebook held at EMU Halle Library',
                dataTest: 'filter-has-emu-ebook',
              },
            ]}
          />
        </div>
      )}

      {/* Type filters */}
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
        <FilterChip
          label="Without Grokipedia"
          active={chips.withoutGrokipedia}
          onClick={() => toggle('withoutGrokipedia')}
          tooltip="Only books without a Grokipedia URL"
          dataTest="filter-without-grokipedia"
        />
        <FilterChip
          label="With Grokipedia"
          active={chips.withGrokipedia}
          onClick={() => toggle('withGrokipedia')}
          tooltip="Only books that have a Grokipedia URL"
          dataTest="filter-with-grokipedia"
        />
        <FilterChip
          label="Without Genres"
          active={chips.withoutGenres}
          onClick={() => toggle('withoutGenres')}
          tooltip="Only books with no genres assigned"
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
