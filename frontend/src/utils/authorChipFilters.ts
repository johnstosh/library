// (c) Copyright 2025 by Muczynski
import type { AuthorDto } from '@/types/dtos'

/**
 * Independent boolean chip filters for the Authors page.
 * All active chips AND together — more buttons on = fewer results.
 *
 * Row 1: hasYdlAudio, hasYdlBook, hasYdlEbook, hasEmuAudio, hasEmuBook, hasEmuEbook
 * Row 2: mostRecent, withoutDescription, withoutGrokipedia, withGrokipedia,
 *   zeroBooks, withoutPhotos, withPhotos, withoutBirthDate, withoutDeathDate
 */
export interface AuthorChipFilters {
  hasYdlBook: boolean
  hasYdlEbook: boolean
  hasYdlAudio: boolean
  hasEmuBook: boolean
  hasEmuEbook: boolean
  hasEmuAudio: boolean
  mostRecent: boolean
  withoutDescription: boolean
  withoutGrokipedia: boolean
  withGrokipedia: boolean
  zeroBooks: boolean
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
  mostRecent: false,
  withoutDescription: false,
  withoutGrokipedia: false,
  withGrokipedia: false,
  zeroBooks: false,
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
]

export function isAvailabilityChipActive(chips: AuthorChipFilters): boolean {
  return AVAILABILITY_CHIPS.some((key) => chips[key])
}

export interface AuthorAvailabilityFlags {
  hasYdlBook?: boolean
  hasYdlEbook?: boolean
  hasYdlAudio?: boolean
  hasEmuBook?: boolean
  hasEmuEbook?: boolean
  hasEmuAudio?: boolean
}

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim() === ''
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
    if (chips.hasYdlBook && !availability?.hasYdlBook) return false
    if (chips.hasYdlEbook && !availability?.hasYdlEbook) return false
    if (chips.hasYdlAudio && !availability?.hasYdlAudio) return false
    if (chips.hasEmuBook && !availability?.hasEmuBook) return false
    if (chips.hasEmuEbook && !availability?.hasEmuEbook) return false
    if (chips.hasEmuAudio && !availability?.hasEmuAudio) return false
    if (chips.withoutDescription && !isBlank(author.briefBiography)) return false
    if (chips.withoutGrokipedia && !isBlank(author.grokipediaUrl)) return false
    if (chips.withGrokipedia && isBlank(author.grokipediaUrl)) return false
    if (chips.zeroBooks && (author.bookCount ?? 0) > 0) return false
    if (chips.withoutPhotos && author.firstPhotoId) return false
    if (chips.withPhotos && !author.firstPhotoId) return false
    if (chips.withoutBirthDate && author.dateOfBirth) return false
    if (chips.withoutDeathDate && author.dateOfDeath) return false
    return true
  })
}
