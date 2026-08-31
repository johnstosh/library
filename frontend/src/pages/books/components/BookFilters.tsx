// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import type { BookChipFilters } from '@/utils/bookChipFilters'
import { FilterChip } from '@/components/ui/FilterChip'
import { EmuIcon, YdlIcon } from '@/components/ui/Icons'

// ─── BookFilters ──────────────────────────────────────────────────────────────

interface AvailabilityGroupProps {
  icon: ReactNode
  libraryAbbr: string
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

function AvailabilityGroup({ icon, libraryAbbr, libraryFull, dataTest, items, chips, onToggle }: AvailabilityGroupProps) {
  return (
    <div
      className="flex flex-wrap items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2"
      data-test={dataTest}
    >
      <span
        className="inline-flex items-center gap-1.5 shrink-0 text-sm font-medium text-gray-600"
        title={libraryFull}
        aria-label={libraryFull}
      >
        {icon}
        {libraryAbbr}
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
  /** Search page: hide cataloger-only chips (Most Recent, Without *, Not Active). */
  showCatalogerFilters?: boolean
}

export function BookFilters({
  chips,
  onToggle,
  mostRecentDisabled = false,
  showAvailabilityFilters = false,
  showCatalogerFilters = true,
}: BookFiltersProps) {
  const toggle = (chip: keyof BookChipFilters) => onToggle(chip)

  return (
    <div className="space-y-2">
      {showAvailabilityFilters && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-2">
          <AvailabilityGroup
            icon={<YdlIcon />}
            libraryAbbr="YDL"
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
            libraryAbbr="EMU"
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

      {showCatalogerFilters && (
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
      )}
    </div>
  )
}
