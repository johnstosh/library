// (c) Copyright 2025 by Muczynski
import type { UserDto } from '@/types/dtos'

/**
 * Independent boolean chip filters for the Users page.
 * All active chips AND together with the search box — more constraints = fewer results.
 */
export interface UserChipFilters {
  librarian: boolean
  user: boolean
  sso: boolean
  localAccount: boolean
  hasActiveLoans: boolean
  noActiveLoans: boolean
  hasEmail: boolean
  withoutEmail: boolean
  googlePhotos: boolean
}

export const defaultUserChipFilters: UserChipFilters = {
  librarian: false,
  user: false,
  sso: false,
  localAccount: false,
  hasActiveLoans: false,
  noActiveLoans: false,
  hasEmail: false,
  withoutEmail: false,
  googlePhotos: false,
}

function isBlank(value: string | null | undefined): boolean {
  return !value || value.trim() === ''
}

function isLibrarian(user: UserDto): boolean {
  return user.authorities?.includes('LIBRARIAN') ?? false
}

function isUserRole(user: UserDto): boolean {
  return user.authorities?.includes('USER') ?? false
}

function hasGooglePhotos(user: UserDto): boolean {
  return !!(
    user.googlePhotosRefreshToken ||
    user.googlePhotosAlbumId ||
    user.googlePhotosApiKey
  )
}

export function applyUserFilters(
  users: UserDto[],
  chips: UserChipFilters,
  query: string
): UserDto[] {
  const needle = query.trim().toLowerCase()

  return users.filter((user) => {
    if (needle) {
      const haystack = [user.username, user.email, user.ssoProvider]
        .filter((value) => !isBlank(value))
        .join(' ')
        .toLowerCase()
      if (!haystack.includes(needle)) return false
    }
    if (chips.librarian && !isLibrarian(user)) return false
    if (chips.user && !isUserRole(user)) return false
    if (chips.sso && !user.ssoSubjectId) return false
    if (chips.localAccount && user.ssoSubjectId) return false
    if (chips.hasActiveLoans && (user.activeLoansCount ?? 0) === 0) return false
    if (chips.noActiveLoans && (user.activeLoansCount ?? 0) > 0) return false
    if (chips.hasEmail && isBlank(user.email)) return false
    if (chips.withoutEmail && !isBlank(user.email)) return false
    if (chips.googlePhotos && !hasGooglePhotos(user)) return false
    return true
  })
}
