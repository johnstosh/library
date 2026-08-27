// (c) Copyright 2025 by Muczynski
import type { AuthorDto } from '@/types/dtos'

/**
 * Independent boolean chip filters for the Authors page.
 * All active chips AND together — more buttons on = fewer results.
 */
export interface AuthorChipFilters {
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

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim() === ''
}

export function applyAuthorChipFilters(
  authors: AuthorDto[],
  chips: AuthorChipFilters,
  mostRecentIds?: Set<number>
): AuthorDto[] {
  return authors.filter((author) => {
    if (chips.mostRecent) {
      if (!mostRecentIds || !mostRecentIds.has(author.id)) return false
    }
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
