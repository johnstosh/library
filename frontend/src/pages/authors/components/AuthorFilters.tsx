// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import type { AuthorChipFilters } from '@/utils/authorChipFilters'
import { PiFunnel } from 'react-icons/pi'
import { EmuIcon, YdlIcon } from '@/components/ui/Icons'

interface FilterChipProps {
  label: string
  active: boolean
  onClick: () => void
  tooltip: string
  dataTest: string
  disabled?: boolean
  hideOnMobile?: boolean
}

function FilterChip({ label, active, onClick, tooltip, dataTest, disabled = false, hideOnMobile = false }: FilterChipProps) {
  return (
    <button
      type="button"
      onClick={disabled ? undefined : onClick}
      title={tooltip}
      aria-pressed={active}
      data-test={dataTest}
      disabled={disabled}
      aria-disabled={disabled}
      className={`${hideOnMobile ? 'hidden sm:inline-flex' : 'inline-flex'} items-center gap-1.5 px-3 py-1.5 text-sm rounded-full border transition-colors select-none ${
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

interface AvailabilityGroupProps {
  icon: ReactNode
  libraryAbbr: string
  libraryFull: string
  dataTest: string
  items: {
    chip: keyof AuthorChipFilters
    label: string
    tooltip: string
    dataTest: string
  }[]
  chips: AuthorChipFilters
  onToggle: (chip: keyof AuthorChipFilters) => void
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

interface AuthorFiltersProps {
  chips: AuthorChipFilters
  onToggle: (chip: keyof AuthorChipFilters) => void
  /** Authors page: Most Recent Day cannot be combined with other filters. */
  mostRecentDisabled?: boolean
}

export function AuthorFilters({ chips, onToggle, mostRecentDisabled = false }: AuthorFiltersProps) {
  return (
    <div className="space-y-3" data-test="author-filter-chips">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-2">
        <AvailabilityGroup
          icon={<YdlIcon />}
          libraryAbbr="YDL"
          libraryFull="Ypsilanti District Library"
          dataTest="author-filter-ydl"
          chips={chips}
          onToggle={onToggle}
          items={[
            {
              chip: 'hasYdlAudio',
              label: 'Audio',
              tooltip: 'Has YDL audio — only authors with an audiobook held at Ypsilanti District Library',
              dataTest: 'filter-has-ydl-audio',
            },
            {
              chip: 'hasYdlBook',
              label: 'Book',
              tooltip: 'Has YDL book — only authors with a physical book held at Ypsilanti District Library',
              dataTest: 'filter-has-ydl-book',
            },
            {
              chip: 'hasYdlEbook',
              label: 'Ebook',
              tooltip: 'Has YDL ebook — only authors with an ebook held at Ypsilanti District Library',
              dataTest: 'filter-has-ydl-ebook',
            },
          ]}
        />
        <AvailabilityGroup
          icon={<EmuIcon />}
          libraryAbbr="EMU"
          libraryFull="EMU Halle Library"
          dataTest="author-filter-emu"
          chips={chips}
          onToggle={onToggle}
          items={[
            {
              chip: 'hasEmuAudio',
              label: 'Audio',
              tooltip: 'Has EMU audio — only authors with an audiobook held at EMU Halle Library',
              dataTest: 'filter-has-emu-audio',
            },
            {
              chip: 'hasEmuBook',
              label: 'Book',
              tooltip: 'Has EMU book — only authors with a physical book held at EMU Halle Library',
              dataTest: 'filter-has-emu-book',
            },
            {
              chip: 'hasEmuEbook',
              label: 'Ebook',
              tooltip: 'Has EMU ebook — only authors with an ebook held at EMU Halle Library',
              dataTest: 'filter-has-emu-ebook',
            },
          ]}
        />
      </div>

      <div className="flex flex-wrap gap-2">
        <FilterChip
          label="Most Recent Day"
          active={chips.mostRecent && !mostRecentDisabled}
          onClick={() => onToggle('mostRecent')}
          tooltip={
            mostRecentDisabled
              ? 'Most Recent Day cannot be combined with other filters. Turn the others off to use it.'
              : 'Only authors of books added on the most recent day'
          }
          dataTest="filter-most-recent"
          disabled={mostRecentDisabled}
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
          hideOnMobile
        />
        <FilterChip
          label="With Photos"
          active={chips.withPhotos}
          onClick={() => onToggle('withPhotos')}
          tooltip="Only authors that have a photo"
          dataTest="filter-with-photos"
          hideOnMobile
        />
        <FilterChip
          label="Without Birth Date"
          active={chips.withoutBirthDate}
          onClick={() => onToggle('withoutBirthDate')}
          tooltip="Only authors missing a date of birth"
          dataTest="filter-without-birth-date"
          hideOnMobile
        />
        <FilterChip
          label="Without Death Date"
          active={chips.withoutDeathDate}
          onClick={() => onToggle('withoutDeathDate')}
          tooltip="Only authors missing a date of death"
          dataTest="filter-without-death-date"
          hideOnMobile
        />
      </div>
    </div>
  )
}
