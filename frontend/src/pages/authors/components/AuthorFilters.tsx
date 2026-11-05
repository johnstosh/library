// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import type { AuthorChipFilters } from '@/utils/authorChipFilters'
import { FilterChip } from '@/components/ui/FilterChip'
import { AclaIcon, EmuIcon, WccIcon, YdlIcon } from '@/components/ui/Icons'

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

function libraryChips(
  library: 'Ydl' | 'Emu' | 'Acla' | 'Wcc',
  libraryName: string,
  slug: 'ydl' | 'emu' | 'acla' | 'wcc',
): AvailabilityGroupProps['items'] {
  const formats = [
    { label: 'Audio', flag: 'Audio', kind: 'audio', noun: 'an audiobook' },
    { label: 'Book', flag: 'Book', kind: 'book', noun: 'a physical book' },
    { label: 'Ebook', flag: 'Ebook', kind: 'ebook', noun: 'an ebook' },
  ] as const
  const positive = formats.map((format) => ({
    chip: `has${library}${format.flag}` as keyof AuthorChipFilters,
    label: format.label,
    tooltip: `${libraryName} ${format.label.toLowerCase()} — authors with at least one title that has ${format.noun}. Audio, Book, and Ebook in this box match if any selected format is held.`,
    dataTest: `filter-has-${slug}-${format.kind}`,
  }))
  const without = formats.map((format) => ({
    chip: `without${library}${format.flag}` as keyof AuthorChipFilters,
    label: `Without ${format.label}`,
    tooltip: `Without ${libraryName} ${format.label.toLowerCase()} — no title has that holding. An unset flag counts as not held. Turns off ${format.label}.`,
    dataTest: `filter-without-${slug}-${format.kind}`,
  }))
  return [...positive, ...without]
}

interface AuthorFiltersProps {
  chips: AuthorChipFilters
  onToggle: (chip: keyof AuthorChipFilters) => void
}

export function AuthorFilters({ chips, onToggle }: AuthorFiltersProps) {
  return (
    <div className="space-y-3" data-test="author-filter-chips">
      <div className="grid grid-cols-1 md:grid-cols-2 2xl:grid-cols-4 gap-2">
        <AvailabilityGroup
          icon={<YdlIcon />}
          libraryAbbr="YDL"
          libraryFull="Ypsilanti District Library"
          dataTest="author-filter-ydl"
          chips={chips}
          onToggle={onToggle}
          items={libraryChips('Ydl', 'YDL', 'ydl')}
        />
        <AvailabilityGroup
          icon={<EmuIcon />}
          libraryAbbr="EMU"
          libraryFull="EMU Halle Library"
          dataTest="author-filter-emu"
          chips={chips}
          onToggle={onToggle}
          items={libraryChips('Emu', 'EMU', 'emu')}
        />
        <AvailabilityGroup
          icon={<AclaIcon />}
          libraryAbbr="ACLA"
          libraryFull="Allegheny County Library Association"
          dataTest="author-filter-acla"
          chips={chips}
          onToggle={onToggle}
          items={libraryChips('Acla', 'ACLA', 'acla')}
        />
        <AvailabilityGroup
          icon={<WccIcon />}
          libraryAbbr="WCC"
          libraryFull="Washtenaw Community College"
          dataTest="author-filter-wcc"
          chips={chips}
          onToggle={onToggle}
          items={libraryChips('Wcc', 'WCC', 'wcc')}
        />
      </div>

      <div className="flex flex-wrap gap-2">
        <FilterChip
          label="Recent Arrivals"
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
          label="Not canonical"
          active={chips.notCanonical}
          onClick={() => onToggle('notCanonical')}
          tooltip="Only authors whose name is comma-inverted, has years, expandable initials, a catalog note, or is all capitals or all lowercase"
          dataTest="filter-not-canonical"
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
