// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
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
  // Duplicate titles mocks for Issue #351
  useFindDuplicateTitles: () => ({
    mutateAsync: vi.fn().mockResolvedValue({
      pairs: [
        {
          score: 0.950123,
          bookAId: 1,
          bookATitle: 'Confessions',
          bookAAlternateTitle: null,
          bookAAuthorName: 'Augustine',
          bookAStatus: 'ACTIVE',
          bookBId: 2,
          bookBTitle: 'Confession',
          bookBAlternateTitle: null,
          bookBAuthorName: 'Augustine',
          bookBStatus: 'WITHDRAWN',
        },
      ],
      booksScanned: 10,
      representativesCompared: 9,
      message: 'Found 1 near-duplicate pair(s).',
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

function renderPage() {
  return render(
    <MemoryRouter>
      <DataManagementPage />
    </MemoryRouter>
  )
}

describe('DataManagementPage Books Availability', () => {
  it('uses a single column on phone and does not truncate in-library labels', () => {
    renderPage()

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
    renderPage()

    expect(screen.getByTestId('favorite-stats-section')).toHaveTextContent('Favorites Statistics')
    expect(screen.getByTestId('favorite-stat-Have Read')).toHaveTextContent('Have Read')
    expect(screen.getByTestId('favorite-stat-books-Have Read')).toHaveTextContent('4 books')
    expect(screen.getByTestId('favorite-stat-authors-Have Read')).toHaveTextContent('1 authors')
  })
})

describe('DataManagementPage database statistics', () => {
  it('shows Favorites after Loans and before Prices', () => {
    renderPage()

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
    renderPage()

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

describe('DataManagementPage Duplicate catalog entries (Issue #351)', () => {
  it('renders the duplicate titles section with a Find button', () => {
    renderPage()

    expect(screen.getByTestId('duplicate-titles-section')).toHaveTextContent('Duplicate catalog entries')
    expect(screen.getByTestId('find-duplicate-titles')).toHaveTextContent('Find')
    expect(screen.getByText(/Click Find to scan the catalog for near-duplicate titles/)).toBeInTheDocument()
  })

  it('shows book status in results and does not show Matched lines', async () => {
    renderPage()

    fireEvent.click(screen.getByTestId('find-duplicate-titles'))

    await waitFor(() => {
      expect(screen.getByTestId('duplicate-titles-results')).toBeInTheDocument()
    })
    expect(screen.getByTestId('dup-book-a-status')).toHaveTextContent('Status: ACTIVE')
    expect(screen.getByTestId('dup-book-b-status')).toHaveTextContent('Status: WITHDRAWN')
    expect(screen.queryByText(/Matched:/)).not.toBeInTheDocument()
    // Score display uses 6 decimal places (not toFixed(3))
    expect(screen.getByText('0.950123')).toBeInTheDocument()
  })
})
