// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CATALOG_PAGE_SIZE } from '@/api/byIds'
import type { BookDto } from '@/types/dtos'
import { SavingsPage } from '../SavingsPage'

const catalog = vi.hoisted(() => {
  const first: BookDto[] = [
    {
      id: 1,
      title: 'Pride and Prejudice',
      author: 'Jane Austen',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
    },
    {
      id: 2,
      title: 'Emma',
      author: 'Jane Austen',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
    },
  ]
  return {
    initial: first,
    books: first,
    loadMore: vi.fn(),
    options: undefined as { pageSize?: number } | undefined,
    filters: undefined as { chips?: { mostRecent?: boolean } } | undefined,
  }
})

const listings = vi.hoisted(() => ({
  fetch: vi.fn(async (_ids: number[]) => []),
}))

vi.mock('@/api/books', () => ({
  useBooks: (filters: { chips?: { mostRecent?: boolean } }, options?: { pageSize?: number }) => {
    catalog.filters = filters
    catalog.options = options
    return {
      data: catalog.books,
      total: 5,
      hasMore: true,
      loadMore: catalog.loadMore,
      isLoadingMore: false,
      isLoading: false,
      isFetching: false,
      error: null,
    }
  },
  useBookCount: () => ({ data: { count: 12 } }),
}))

vi.mock('@/api/branches', () => ({
  useBranches: () => ({
    data: [{ id: 1, branchName: 'St. Martin de Porres', librarySystemName: 'Sacred Heart Library System' }],
  }),
}))

vi.mock('@/api/favorites', () => ({
  useFavoriteSummary: () => ({ data: { lists: [] } }),
  favoriteListChips: () => [],
}))

vi.mock('@/api/sellers', () => ({
  fetchListingsForBooks: (ids: number[]) => listings.fetch(ids),
}))

function renderSavings() {
  return render(
    <MemoryRouter initialEntries={['/savings']}>
      <SavingsPage />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  catalog.books = catalog.initial.map((book) => ({ ...book }))
  catalog.loadMore.mockClear()
  listings.fetch.mockReset()
  listings.fetch.mockResolvedValue([])
})

describe('SavingsPage', () => {
  it('pages 100 books, reports Books counts, and keeps a selection that leaves the list', async () => {
    renderSavings()

    expect(catalog.options).toEqual({ pageSize: CATALOG_PAGE_SIZE })
    expect(catalog.filters?.chips?.mostRecent).toBe(true)
    expect(screen.getByTestId('savings-search-button')).toHaveTextContent('Search')
    expect(screen.getByTestId('filter-most-recent')).toBeInTheDocument()
    expect(screen.getByTestId('filter-with-prices')).toBeInTheDocument()
    expect(screen.getByTestId('book-label-filters')).toBeInTheDocument()
    expect(screen.getByTestId('table-count')).toHaveTextContent('2 books in this table')
    expect(screen.getByTestId('search-results-count')).toHaveTextContent('5 books in the search results')
    expect(screen.getByTestId('database-count')).toHaveTextContent('12 books in the Sacred Heart Library System')
    expect(screen.getByText('Showing 2 of 5 books')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('savings-load-more'))
    expect(catalog.loadMore).toHaveBeenCalledTimes(1)

    expect(screen.queryByTestId('savings-select-visible')).not.toBeInTheDocument()
    expect(screen.queryByTestId('savings-clear')).not.toBeInTheDocument()
    fireEvent.click(screen.getByTestId('select-all-checkbox'))
    expect(screen.getByTestId('savings-selected-count')).toHaveTextContent('2 titles selected')
    fireEvent.click(screen.getByTestId('select-all-checkbox'))
    expect(screen.getByTestId('savings-selected-count')).toHaveTextContent('0 titles selected')

    fireEvent.click(screen.getByTestId('select-checkbox-1'))
    expect(screen.getByTestId('savings-selected-count')).toHaveTextContent('1 title selected')

    catalog.books = catalog.books.filter((book) => book.id !== 1)
    fireEvent.change(screen.getByTestId('savings-search'), { target: { value: 'Emma' } })
    fireEvent.click(screen.getByTestId('savings-search-button'))

    expect(screen.queryByTestId('savings-book-1')).not.toBeInTheDocument()
    expect(screen.getByTestId('savings-selected-count')).toHaveTextContent('1 title selected')

    fireEvent.click(screen.getByTestId('savings-compute'))
    expect(listings.fetch).toHaveBeenCalledWith([1])
    expect(await screen.findByTestId('savings-missing')).toHaveTextContent('Pride and Prejudice')
    expect(screen.getByTestId('savings-total')).toHaveTextContent('$0.00')
  })

  it('hides single books in the results until the checkbox is on', async () => {
    listings.fetch.mockResolvedValue([
      {
        id: 10,
        bookId: 1,
        listingTitle: 'Pride',
        sellerId: 7,
        sellerName: 'Solo',
        priceDollars: 10,
        shippingDollars: 5,
        additionalShippingDollars: 1,
        additionalShippingSource: 'PARSED',
        condition: 'Used - Good',
        binding: 'HARDCOVER',
      },
    ])
    renderSavings()

    const showSingles = screen.getByTestId('savings-show-singles')
    expect(showSingles).not.toBeChecked()
    expect(screen.getByText('Show single books in the results')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('select-checkbox-1'))
    fireEvent.click(screen.getByTestId('savings-compute'))

    expect(await screen.findByTestId('savings-total')).toHaveTextContent('$0.00')
    expect(screen.queryByTestId('savings-individual')).not.toBeInTheDocument()

    fireEvent.click(showSingles)
    expect(screen.getByTestId('savings-individual')).toHaveTextContent('Pride and Prejudice')
    expect(screen.getByTestId('savings-individual')).toHaveTextContent('$15.00')
  })
})
