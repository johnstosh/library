// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import {
  applyUserFilters,
  defaultUserChipFilters,
  type UserChipFilters,
} from '../userChipFilters'
import type { UserDto } from '@/types/dtos'

function user(overrides: Partial<UserDto> & { id: number; username: string }): UserDto {
  return {
    authorities: ['USER'],
    lastModified: '2026-01-01T00:00:00',
    ...overrides,
  }
}

const chips = (overrides: Partial<UserChipFilters> = {}): UserChipFilters => ({
  ...defaultUserChipFilters,
  ...overrides,
})

const sample: UserDto[] = [
  user({
    id: 1,
    username: 'alice',
    email: 'alice@example.com',
    authorities: ['LIBRARIAN', 'USER'],
    ssoSubjectId: 'sub-1',
    activeLoansCount: 2,
    googlePhotosAlbumId: 'album',
  }),
  user({ id: 2, username: 'bob', authorities: ['USER'], activeLoansCount: 0 }),
]

describe('applyUserFilters', () => {
  it('returns everyone when no chips or query', () => {
    expect(applyUserFilters(sample, chips(), '').map((u) => u.id)).toEqual([1, 2])
  })

  it('substring search matches username or email', () => {
    expect(applyUserFilters(sample, chips(), 'ali').map((u) => u.id)).toEqual([1])
    expect(applyUserFilters(sample, chips(), 'EXAMPLE').map((u) => u.id)).toEqual([1])
  })

  it('ANDs librarian with has-active-loans', () => {
    expect(
      applyUserFilters(sample, chips({ librarian: true, hasActiveLoans: true }), '').map((u) => u.id)
    ).toEqual([1])
  })

  it('sso and local-account split the list', () => {
    expect(applyUserFilters(sample, chips({ sso: true }), '').map((u) => u.id)).toEqual([1])
    expect(applyUserFilters(sample, chips({ localAccount: true }), '').map((u) => u.id)).toEqual([2])
  })

  it('google-photos keeps connected accounts', () => {
    expect(applyUserFilters(sample, chips({ googlePhotos: true }), '').map((u) => u.id)).toEqual([1])
  })
})
