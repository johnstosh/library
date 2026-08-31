// (c) Copyright 2025 by Muczynski

export const ALL_BOOK_LABELS = [
  'fiction',
  'slice-of-life',
  'hagiography',
  'saint',
  'fantasy',
  'family',
  'childrens',
  'adult',
  'philosophy',
  'theology',
  'discernment',
  'talking-animals',
  'biography',
  'history',
  'prayer',
  'classic',
  'poetry',
  'science',
  'music',
  'mystery',
  'adventure',
  'romance',
  'humor',
] as const

export type BookLabel = (typeof ALL_BOOK_LABELS)[number]

/** User-facing names for genre chips and badges. Stored values stay as slugs. */
export const BOOK_LABEL_DISPLAY: Record<BookLabel, string> = {
  fiction: 'Fiction',
  'slice-of-life': 'Slice of Life',
  hagiography: 'Hagiography',
  saint: 'Saint',
  fantasy: 'Fantasy',
  family: 'Family',
  childrens: "Children's",
  adult: 'Adult',
  philosophy: 'Philosophy',
  theology: 'Theology',
  discernment: 'Discernment',
  'talking-animals': 'Talking Animals',
  biography: 'Biography',
  history: 'History',
  prayer: 'Prayer',
  classic: 'Classic',
  poetry: 'Poetry',
  science: 'Science',
  music: 'Music',
  mystery: 'Mystery',
  adventure: 'Adventure',
  romance: 'Romance',
  humor: 'Humor',
}

const STANDARD_GENRES = new Set<string>(ALL_BOOK_LABELS)

export function isStandardGenre(value: string): boolean {
  return STANDARD_GENRES.has(value)
}

export function formatBookLabel(slug: string): string {
  if (isStandardGenre(slug)) {
    return BOOK_LABEL_DISPLAY[slug as BookLabel]
  }
  return slug
    .split('-')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export const SORTED_BOOK_LABELS: BookLabel[] = [...ALL_BOOK_LABELS].sort((a, b) =>
  BOOK_LABEL_DISPLAY[a].localeCompare(BOOK_LABEL_DISPLAY[b]),
)

/** Keep only genres from the standard list, lowercased and de-duplicated. */
export function standardGenresFrom(tags: string[] | undefined): string[] {
  if (!tags?.length) return []
  return [...new Set(tags.map((tag) => tag.trim().toLowerCase()).filter(isStandardGenre))]
}

interface GenreChipsProps {
  selected: string[]
  onToggle: (genre: string) => void
  dataTest?: string
  chipDataTestPrefix?: string
}

export function GenreChips({
  selected,
  onToggle,
  dataTest = 'genre-chips',
  chipDataTestPrefix = 'genre-chip',
}: GenreChipsProps) {
  const selectedSet = new Set(selected)

  return (
    <div
      className="flex flex-wrap gap-1.5 sm:grid sm:grid-rows-3 sm:grid-flow-col sm:auto-cols-max sm:gap-x-4 sm:gap-y-1.5"
      data-test={dataTest}
    >
      {SORTED_BOOK_LABELS.map((label) => {
        const isSelected = selectedSet.has(label)
        return (
          <button
            key={label}
            type="button"
            onClick={() => onToggle(label)}
            data-test={`${chipDataTestPrefix}-${label}`}
            className={[
              'px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors whitespace-nowrap',
              isSelected
                ? 'bg-primary-600 text-white border-primary-600 hover:bg-primary-700'
                : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50 hover:border-gray-400',
            ].join(' ')}
            aria-pressed={isSelected}
          >
            {formatBookLabel(label)}
          </button>
        )
      })}
    </div>
  )
}

interface BookLabelFiltersProps {
  selectedLabels: string[]
  onToggleLabel: (label: string) => void
  onClearLabels: () => void
}

export function BookLabelFilters({ selectedLabels, onToggleLabel, onClearLabels }: BookLabelFiltersProps) {
  return (
    <div className="mt-3" data-test="book-label-filters">
      <div className="flex items-center gap-2 mb-2">
        <span className="text-sm font-medium text-gray-700">Genres</span>
        {selectedLabels.length > 0 && (
          <button
            type="button"
            onClick={onClearLabels}
            data-test="label-filter-clear"
            className="px-2.5 py-0.5 rounded-full text-xs font-medium border border-red-300 text-red-600 bg-white hover:bg-red-50 transition-colors"
          >
            Clear genres
          </button>
        )}
      </div>
      <GenreChips
        selected={selectedLabels}
        onToggle={onToggleLabel}
        dataTest="label-filter-wrap"
        chipDataTestPrefix="label-filter"
      />
    </div>
  )
}
