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
    data: {
      branchCount: 1,
      bookCount: 10,
      authorCount: 5,
      userCount: 3,
      loanCount: 2,
      favoriteCount: 7,
      priceCount: 4,
    },
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
      availableAtAcla: 2,
      aclaPaper: 1,
      aclaEbook: 1,
      aclaAudio: 0,
    },
    isLoading: false,
  }),
  usePhotoZipParts: () => ({
    data: [],
    isLoading: false,
    isError: false,
  }),
  // Maintenance mocks for Issue #347 - full shape to prevent render errors in useMutation
  useRecalcIllegalGenres: () => ({
    mutateAsync: vi.fn().mockResolvedValue({
      booksAffected: 3,
      message: '3 book(s) have illegal or mismatched genre tags.',
    }),
    isPending: false,
  }),
  useCleanupIllegalGenres: () => ({
    mutateAsync: vi.fn().mockResolvedValue({
      booksAffected: 0,
      booksScanned: 42,
      booksUpdated: 5,
      pluralCorrections: 7,
      illegalRemoved: 12,
      message: 'Scanned 42 books. Updated 5. Corrected 7 plural/spelling variants. Removed 12 illegal tags.',
    }),
    isPending: false,
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

describe('DataManagementPage database statistics', () => {
  it('shows Favorites after Loans and before Prices', () => {
    render(<DataManagementPage />)

    const loans = screen.getByTestId('stat-loans')
    const favorites = screen.getByTestId('stat-favorites')
    const prices = screen.getByTestId('stat-prices')

    expect(favorites).toHaveTextContent('7')
    expect(favorites).toHaveTextContent('Favorites')
    expect(prices).toHaveTextContent('4')
    expect(prices).toHaveTextContent('Prices')

    expect(loans.compareDocumentPosition(favorites) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(favorites.compareDocumentPosition(prices) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })
})

describe('DataManagementPage Maintenance Section (Issue #347)', () => {
  it('renders the maintenance table at the bottom with Illegal genres cleanup row, Count as "-", and Recalc/Clean buttons', () => {
    render(<DataManagementPage />)

    expect(screen.getByTestId('maintenance-section')).toHaveTextContent('Maintenance')
    expect(screen.getByTestId('maintenance-row-illegal-genres')).toBeInTheDocument()
    expect(screen.getByText('Clean Genres')).toBeInTheDocument()
    expect(screen.getByText('Recalc')).toBeInTheDocument()
    expect(screen.getByText(/Fixes genre names that use the wrong plural/)).toBeInTheDocument()
    // Count shows "-" before any Recalc (per spec)
    expect(screen.getByText('-')).toBeInTheDocument()
    expect(screen.getByText(/Click Recalc or Clean Genres to see results here/)).toBeInTheDocument()
  })
})
