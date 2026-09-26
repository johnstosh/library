// (c) Copyright 2025 by Muczynski
import { describe, expect, it } from 'vitest'
import { buildExportFilename } from '../exportFilename'

const base = {
  branchName: 'St. Martin de Porres',
  bookCount: 10,
  authorCount: 5,
  userCount: 3,
  loanCount: 2,
  favoriteCount: 7,
  priceCount: 4,
  photoCount: 1,
  date: '2026-09-26',
}

describe('buildExportFilename', () => {
  it('inserts DEV after the date on library-dev hosts', () => {
    expect(
      buildExportFilename({ ...base, hostname: 'library-dev.muczynskifamily.com' }),
    ).toBe(
      '2026-09-26-DEV-st-martin-de-porres-10-books-5-authors-3-users-2-loans-7-favorites-4-prices-1-photos.json',
    )
  })

  it('omits DEV on production hostnames', () => {
    expect(buildExportFilename({ ...base, hostname: 'library.muczynskifamily.com' })).toBe(
      '2026-09-26-st-martin-de-porres-10-books-5-authors-3-users-2-loans-7-favorites-4-prices-1-photos.json',
    )
  })
})
