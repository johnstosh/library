// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { DataManagementPage } from '../DataManagementPage'

vi.mock('@/api/data-management', () => ({
  exportJsonData: vi.fn(),
  useImportJsonData: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
  useDatabaseStats: () => ({
    data: { branchCount: 1, bookCount: 10, authorCount: 5, userCount: 3, loanCount: 2 },
  }),
  usePhotoExportStats: () => ({
    data: { total: 0 },
  }),
  useLabelCounts: () => ({
    data: [],
  }),
  useAvailabilityStats: () => ({
    data: {
      electronicResource: 1,
      hasCallNumber: 8,
      hasFreeOnlineText: 2,
      hasFreeOnlineAudio: 1,
      withdrawn: 1,
      requested: 1,
      availableAtYdl: 3,
      ydlPaper: 1,
      ydlEbook: 1,
      ydlAudio: 1,
      availableAtEmu: 2,
      emuPaper: 1,
      emuEbook: 1,
      emuAudio: 0,
    },
    isLoading: false,
  }),
  usePhotoZipParts: () => ({
    data: [],
    isLoading: false,
    isError: false,
  }),
}))

vi.mock('@/api/branches', () => ({
  useBranches: () => ({
    data: [],
  }),
}))

vi.mock('@/api/favorites', () => ({
  useFavoriteStats: () => ({
    data: [
      { listName: 'Have Read', bookCount: 4, authorCount: 1 },
      { listName: 'Want to Read', bookCount: 2, authorCount: 0 },
    ],
    isLoading: false,
  }),
}))

vi.mock('@/api/photos', () => ({
  useImportPhotosFromZipChunked: () => ({
    progress: {
      mbSent: 0,
      totalMb: 0,
      percentage: 0,
      imagesProcessed: 0,
      imagesSuccess: 0,
      imagesFailure: 0,
      imagesSkipped: 0,
      isUploading: false,
    },
    mutateAsync: vi.fn(),
    isPending: false,
  }),
}))

describe('DataManagementPage Books Availability', () => {
  it('uses a single column on phone and does not truncate in-library labels', () => {
    render(<DataManagementPage />)

    const grid = screen.getByTestId('availability-stats-grid')
    expect(grid).toHaveClass('grid-cols-1')
    expect(grid).not.toHaveClass('grid-cols-2')

    const inLibrary = screen.getByTestId('availability-count-in-library')
    expect(inLibrary).toHaveTextContent('In-library materials')

    const label = inLibrary.querySelector('span')
    expect(label).not.toBeNull()
    expect(label!.className.split(/\s+/)).not.toContain('truncate')
  })
})

describe('DataManagementPage Favorites Statistics', () => {
  it('lists favorite counts split by books and authors', () => {
    render(<DataManagementPage />)

    expect(screen.getByTestId('favorite-stats-section')).toHaveTextContent('Favorites Statistics')
    expect(screen.getByTestId('favorite-stat-Have Read')).toHaveTextContent('Have Read')
    expect(screen.getByTestId('favorite-stat-books-Have Read')).toHaveTextContent('4 books')
    expect(screen.getByTestId('favorite-stat-authors-Have Read')).toHaveTextContent('1 authors')
  })
})
