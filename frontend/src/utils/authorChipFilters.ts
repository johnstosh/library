// (c) Copyright 2025 by Muczynski
import type { AuthorDto } from '@/types/dtos'
import { authorNeedsCanonicalName } from '@/utils/canonicalAuthorName'

/**
 * Chip filters for the Authors page. Chips AND together, except Audio, Book,
 * and Ebook within one public library, which OR. A positive chip means at least
 * one title has that holding. A Without chip means no title has it.
 *
 * Row 1: has/without YDL, EMU, ACLA, WCC, and MSU audio, book, and ebook
 * Row 2: mostRecent, withoutDescription, withoutGrokipedia, withGrokipedia,
 *   zeroBooks, notCanonical, withoutPhotos, withPhotos, withoutBirthDate, withoutDeathDate
 */
export interface AuthorChipFilters {
  hasYdlBook: boolean
  hasYdlEbook: boolean
  hasYdlAudio: boolean
  hasEmuBook: boolean
  hasEmuEbook: boolean
  hasEmuAudio: boolean
  hasAclaBook: boolean
  hasAclaEbook: boolean
  hasAclaAudio: boolean
  hasWccBook: boolean
  hasWccEbook: boolean
  hasWccAudio: boolean
  hasMsuBook: boolean
  hasMsuEbook: boolean
  hasMsuAudio: boolean
  withoutYdlBook: boolean
  withoutYdlEbook: boolean
  withoutYdlAudio: boolean
  withoutEmuBook: boolean
  withoutEmuEbook: boolean
  withoutEmuAudio: boolean
  withoutAclaBook: boolean
  withoutAclaEbook: boolean
  withoutAclaAudio: boolean
  withoutWccBook: boolean
  withoutWccEbook: boolean
  withoutWccAudio: boolean
  withoutMsuBook: boolean
  withoutMsuEbook: boolean
  withoutMsuAudio: boolean
  mostRecent: boolean
  withoutDescription: boolean
  withoutGrokipedia: boolean
  withGrokipedia: boolean
  zeroBooks: boolean
  notCanonical: boolean
  withoutPhotos: boolean
  withPhotos: boolean
  withoutBirthDate: boolean
  withoutDeathDate: boolean
}

export const defaultAuthorChipFilters: AuthorChipFilters = {
  hasYdlBook: false,
  hasYdlEbook: false,
  hasYdlAudio: false,
  hasEmuBook: false,
  hasEmuEbook: false,
  hasEmuAudio: false,
  hasAclaBook: false,
  hasAclaEbook: false,
  hasAclaAudio: false,
  hasWccBook: false,
  hasWccEbook: false,
  hasWccAudio: false,
  hasMsuBook: false,
  hasMsuEbook: false,
  hasMsuAudio: false,
  withoutYdlBook: false,
  withoutYdlEbook: false,
  withoutYdlAudio: false,
  withoutEmuBook: false,
  withoutEmuEbook: false,
  withoutEmuAudio: false,
  withoutAclaBook: false,
  withoutAclaEbook: false,
  withoutAclaAudio: false,
  withoutWccBook: false,
  withoutWccEbook: false,
  withoutWccAudio: false,
  withoutMsuBook: false,
  withoutMsuEbook: false,
  withoutMsuAudio: false,
  // Shared default is off. The Authors page overrides this to true in uiStore
  // so it can use GET /authors/most-recent-day instead of GET /authors/summaries.
  mostRecent: false,
  withoutDescription: false,
  withoutGrokipedia: false,
  withGrokipedia: false,
  zeroBooks: false,
  notCanonical: false,
  withoutPhotos: false,
  withPhotos: false,
  withoutBirthDate: false,
  withoutDeathDate: false,
}

const AVAILABILITY_CHIPS: (keyof AuthorChipFilters)[] = [
  'hasYdlBook',
  'hasYdlEbook',
  'hasYdlAudio',
  'hasEmuBook',
  'hasEmuEbook',
  'hasEmuAudio',
  'hasAclaBook',
  'hasAclaEbook',
  'hasAclaAudio',
  'hasWccBook',
  'hasWccEbook',
  'hasWccAudio',
  'hasMsuBook',
  'hasMsuEbook',
  'hasMsuAudio',
  'withoutYdlBook',
  'withoutYdlEbook',
  'withoutYdlAudio',
  'withoutEmuBook',
  'withoutEmuEbook',
  'withoutEmuAudio',
  'withoutAclaBook',
  'withoutAclaEbook',
  'withoutAclaAudio',
  'withoutWccBook',
  'withoutWccEbook',
  'withoutWccAudio',
  'withoutMsuBook',
  'withoutMsuEbook',
  'withoutMsuAudio',
]

const AUTHOR_AVAILABILITY_OPPOSITE: Partial<Record<keyof AuthorChipFilters, keyof AuthorChipFilters>> = {
  hasYdlAudio: 'withoutYdlAudio',
  withoutYdlAudio: 'hasYdlAudio',
  hasYdlBook: 'withoutYdlBook',
  withoutYdlBook: 'hasYdlBook',
  hasYdlEbook: 'withoutYdlEbook',
  withoutYdlEbook: 'hasYdlEbook',
  hasEmuAudio: 'withoutEmuAudio',
  withoutEmuAudio: 'hasEmuAudio',
  hasEmuBook: 'withoutEmuBook',
  withoutEmuBook: 'hasEmuBook',
  hasEmuEbook: 'withoutEmuEbook',
  withoutEmuEbook: 'hasEmuEbook',
  hasAclaAudio: 'withoutAclaAudio',
  withoutAclaAudio: 'hasAclaAudio',
  hasAclaBook: 'withoutAclaBook',
  withoutAclaBook: 'hasAclaBook',
  hasAclaEbook: 'withoutAclaEbook',
  withoutAclaEbook: 'hasAclaEbook',
  hasWccAudio: 'withoutWccAudio',
  withoutWccAudio: 'hasWccAudio',
  hasWccBook: 'withoutWccBook',
  withoutWccBook: 'hasWccBook',
  hasWccEbook: 'withoutWccEbook',
  withoutWccEbook: 'hasWccEbook',
  hasMsuAudio: 'withoutMsuAudio',
  withoutMsuAudio: 'hasMsuAudio',
  hasMsuBook: 'withoutMsuBook',
  withoutMsuBook: 'hasMsuBook',
  hasMsuEbook: 'withoutMsuEbook',
  withoutMsuEbook: 'hasMsuEbook',
}

/** Turning a format chip on clears its Without pair, and the reverse. */
export function toggleAuthorChip(chips: AuthorChipFilters, chip: keyof AuthorChipFilters): AuthorChipFilters {
  const turningOn = !chips[chip]
  const next: AuthorChipFilters = { ...chips, [chip]: turningOn }
  const opposite = AUTHOR_AVAILABILITY_OPPOSITE[chip]
  if (turningOn && opposite) {
    next[opposite] = false
  }
  return next
}

function matchesAuthorFormatOr(selected: boolean[], held: Array<boolean | undefined>): boolean {
  if (!selected.some(Boolean)) return true
  return selected.some((on, index) => on && held[index] === true)
}

export function isAvailabilityChipActive(chips: AuthorChipFilters): boolean {
  return AVAILABILITY_CHIPS.some((key) => chips[key])
}

/** True if any chip other than mostRecent is on. */
export function isOtherAuthorChipActive(chips: AuthorChipFilters): boolean {
  return (Object.keys(chips) as (keyof AuthorChipFilters)[])
    .filter((key) => key !== 'mostRecent')
    .some((key) => chips[key])
}

export interface AuthorAvailabilityFlags {
  hasYdlBook?: boolean
  hasYdlEbook?: boolean
  hasYdlAudio?: boolean
  hasEmuBook?: boolean
  hasEmuEbook?: boolean
  hasEmuAudio?: boolean
  hasAclaBook?: boolean
  hasAclaEbook?: boolean
  hasAclaAudio?: boolean
  hasWccBook?: boolean
  hasWccEbook?: boolean
  hasWccAudio?: boolean
  hasMsuBook?: boolean
  hasMsuEbook?: boolean
  hasMsuAudio?: boolean
}

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim() === ''
}

function isMissingGrokipediaUrl(value: string | null | undefined): boolean {
  const trimmed = value?.trim()
  return !trimmed || trimmed === '-'
}

export function applyAuthorChipFilters(
  authors: AuthorDto[],
  chips: AuthorChipFilters,
  mostRecentIds?: Set<number>,
  availabilityByAuthorId?: Map<number, AuthorAvailabilityFlags>
): AuthorDto[] {
  return authors.filter((author) => {
    if (chips.mostRecent) {
      if (!mostRecentIds || !mostRecentIds.has(author.id)) return false
    }
    const availability = availabilityByAuthorId?.get(author.id)
    if (!matchesAuthorFormatOr(
      [chips.hasYdlAudio, chips.hasYdlBook, chips.hasYdlEbook],
      [availability?.hasYdlAudio, availability?.hasYdlBook, availability?.hasYdlEbook],
    )) return false
    if (!matchesAuthorFormatOr(
      [chips.hasEmuAudio, chips.hasEmuBook, chips.hasEmuEbook],
      [availability?.hasEmuAudio, availability?.hasEmuBook, availability?.hasEmuEbook],
    )) return false
    if (!matchesAuthorFormatOr(
      [chips.hasAclaAudio, chips.hasAclaBook, chips.hasAclaEbook],
      [availability?.hasAclaAudio, availability?.hasAclaBook, availability?.hasAclaEbook],
    )) return false
    if (chips.withoutYdlAudio && availability?.hasYdlAudio) return false
    if (chips.withoutYdlBook && availability?.hasYdlBook) return false
    if (chips.withoutYdlEbook && availability?.hasYdlEbook) return false
    if (chips.withoutEmuAudio && availability?.hasEmuAudio) return false
    if (chips.withoutEmuBook && availability?.hasEmuBook) return false
    if (chips.withoutEmuEbook && availability?.hasEmuEbook) return false
    if (chips.withoutAclaAudio && availability?.hasAclaAudio) return false
    if (chips.withoutAclaBook && availability?.hasAclaBook) return false
    if (chips.withoutAclaEbook && availability?.hasAclaEbook) return false
    if (!matchesAuthorFormatOr(
      [chips.hasWccAudio, chips.hasWccBook, chips.hasWccEbook],
      [availability?.hasWccAudio, availability?.hasWccBook, availability?.hasWccEbook],
    )) return false
    if (!matchesAuthorFormatOr(
      [chips.hasMsuAudio, chips.hasMsuBook, chips.hasMsuEbook],
      [availability?.hasMsuAudio, availability?.hasMsuBook, availability?.hasMsuEbook],
    )) return false
    if (chips.withoutWccAudio && availability?.hasWccAudio) return false
    if (chips.withoutWccBook && availability?.hasWccBook) return false
    if (chips.withoutWccEbook && availability?.hasWccEbook) return false
    if (chips.withoutMsuAudio && availability?.hasMsuAudio) return false
    if (chips.withoutMsuBook && availability?.hasMsuBook) return false
    if (chips.withoutMsuEbook && availability?.hasMsuEbook) return false
    if (chips.withoutDescription && !isBlank(author.briefBiography)) return false
    if (chips.withoutGrokipedia && !isMissingGrokipediaUrl(author.grokipediaUrl)) return false
    if (chips.withGrokipedia && isMissingGrokipediaUrl(author.grokipediaUrl)) return false
    if (chips.zeroBooks && (author.bookCount ?? 0) > 0) return false
    if (chips.notCanonical && !authorNeedsCanonicalName(author.name)) return false
    if (chips.withoutPhotos && author.firstPhotoId) return false
    if (chips.withPhotos && !author.firstPhotoId) return false
    if (chips.withoutBirthDate && author.dateOfBirth) return false
    if (chips.withoutDeathDate && author.dateOfDeath) return false
    return true
  })
}
