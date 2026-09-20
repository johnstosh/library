// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { PricesPage } from '../PricesPage'
import type { BookDto, BookPriceDto } from '@/types/dtos'

const { prices, books } = vi.hoisted(() => {
  const books: BookDto[] = [
    {
      id: 1,
      title: 'Pride and Prejudice',
      author: 'Jane Austen',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      locNumber: 'PR4034 .P7',
      desireToPurchase: 10,
    },
    {
      id: 2,
      title: 'Expensive Tome',
      author: 'Someone',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      desireToPurchase: 1,
    },
    {
      id: 3,
      title: 'Missing Listing',
      author: 'Unknown',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
    },
    {
      id: 4,
      title: 'Rate Limited Book',
      author: 'Someone',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      desireToPurchase: 10,
    },
  ]
  const prices: BookPriceDto[] = [
    {
      id: 11,
      bookId: 1,
      bookTitle: 'Pride and Prejudice',
      author: 'Jane Austen',
      cover: 'HARDCOVER',
      priceDollars: 4.86,
      shippingDollars: 0,
      totalDollars: 4.86,
      condition: 'Used - Good',
      lookedUpAt: '2026-09-10T12:00:00',
      detailsUrl: 'https://www.abebooks.com/pride/bd',
    },
    {
      id: 12,
      bookId: 2,
      bookTitle: 'Expensive Tome',
      author: 'Someone',
      cover: 'SOFTCOVER',
      priceDollars: 40,
      shippingDollars: 5,
      totalDollars: 45,
      condition: 'Used - Very good',
      lookedUpAt: '2026-09-10T12:00:00',
    },
    {
      id: 13,
      bookId: 3,
      bookTitle: 'Missing Listing',
      author: 'Unknown',
      cover: 'HARDCOVER',
      priceDollars: null,
      shippingDollars: null,
      totalDollars: null,
      lookupError: 'No matching listing',
      lookedUpAt: '2026-09-10T12:00:00',
      detailsUrl: 'https://www.abebooks.com/servlet/SearchResults?tn=Missing',
    },
    {
      id: 15,
      bookId: 4,
      bookTitle: 'Rate Limited Book',
      author: 'Someone',
      cover: 'HARDCOVER',
      priceDollars: null,
      shippingDollars: null,
      totalDollars: null,
      lookupError: 'AbeBooks rate limited',
      lookedUpAt: '2026-09-10T12:00:00',
      detailsUrl: 'https://www.abebooks.com/servlet/SearchResults?tn=Rate',
    },
    {
      id: 14,
      bookId: 1,
      bookTitle: 'Pride and Prejudice',
      author: 'Jane Austen',
      cover: 'UNKNOWN',
      priceDollars: 6,
      shippingDollars: 0,
      totalDollars: 6,
      condition: 'Used - Good',
      lookedUpAt: '2026-09-10T12:00:00',
    },
  ]
  return { prices, books }
})

vi.mock('@/api/prices', () => ({
  usePrices: (options?: any) => ({
    data: prices,
    isLoading: false,
    isFetching: false,
    error: null,
  }),
}))

vi.mock('@/api/books', () => ({
  useBooks: () => ({
    data: books,
    isLoading: false,
    isFetching: false,
    error: null,
  }),
  useBookCount: () => ({ data: { count: 12 } }),
}))

vi.mock('@/api/favorites', () => ({
  useFavoriteSummary: () => ({ data: { lists: [] } }),
  favoriteListChips: () => [],
  favoriteItemIdsForLists: () => new Set(),
  listNameToTestId: (name: string) => name,
}))

function renderPrices(path = '/prices') {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    <MemoryRouter initialEntries={[path]}>
      <PricesPage />
    </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('PricesPage', () => {
  it('uses Search on the title filter like Books and Search', () => {
    renderPrices('/prices')
    expect(screen.getByTestId('prices-search-button')).toHaveTextContent('Search')
  })

  it('reports books in the table, books in the database, and price rows', () => {
    renderPrices('/prices')
    expect(screen.getByTestId('table-count')).toHaveTextContent('4 books in this table')
    expect(screen.getByTestId('database-count')).toHaveTextContent('12 books in the database')
    expect(screen.getByTestId('price-row-count')).toHaveTextContent('5 prices in this table')
  })

  it('reports cheapest-cover totals and threshold counts at the bottom of the page', () => {
    renderPrices('/prices')
    expect(screen.getByTestId('price-statistics')).toHaveTextContent('Price statistics')
    expect(screen.getByTestId('price-stats-total-cost')).toHaveTextContent('$49.86')
    expect(screen.getByTestId('price-stats-over-20')).toHaveTextContent('1')
    expect(screen.getByTestId('price-stats-over-40')).toHaveTextContent('1')
    expect(screen.getByTestId('price-stats-over-80')).toHaveTextContent('0')
    expect(screen.getByTestId('price-stats-total-books')).toHaveTextContent('4')
    expect(screen.getByTestId('price-stats-without-prices')).toHaveTextContent('2')
  })

  it('renders listings and filters by max total', () => {
    renderPrices('/prices')

    expect(screen.getByTestId('price-total-11')).toHaveTextContent('$4.86')
    expect(screen.getByTestId('price-total-12')).toHaveTextContent('$45.00')

    expect(screen.getByTestId('book-price-filters')).toHaveTextContent('Pricing')
    expect(screen.getByTestId('filter-with-prices')).toHaveTextContent('Books with Pricing')
    expect(screen.getByTestId('filter-no-prices')).toHaveTextContent('Books without Pricing')
    expect(screen.getByTestId('filter-price-older')).toBeInTheDocument()
    expect(screen.getByTestId('filter-lookup-errors')).toHaveTextContent('Lookup Errors')
    expect(screen.getByTestId('filter-price-recent')).toBeInTheDocument()
    expect(screen.getByTestId('filter-price-recent-hours')).toBeInTheDocument()
    expect(screen.getByTestId('desire-to-purchase-filters')).toHaveTextContent('Desire to Purchase')
    expect(screen.getByTestId('open-in-books')).toHaveTextContent('Open in Books')
    expect(screen.getByTestId('open-in-books')).toHaveAttribute('href', '/books?mostRecent=false')

    fireEvent.change(screen.getByTestId('prices-max-total'), { target: { value: '10' } })

    expect(screen.getByTestId('price-total-11')).toBeInTheDocument()
    expect(screen.queryByTestId('price-total-12')).not.toBeInTheDocument()
    expect(screen.getByTestId('price-stats-total-cost')).toHaveTextContent('$49.86')
    expect(screen.getByTestId('price-stats-total-books')).toHaveTextContent('4')
  })

  it('filters by looked up recently (old query params ignored)', () => {
    renderPrices('/prices?recent=true&recentHours=6&hardcover=true')
    const chip = screen.getByTestId('filter-price-recent')
    expect(chip).toHaveTextContent('Looked up recently')
    // active state uses bg-primary-50 + border-primary-500 (per FilterChip); old params ignored
    expect(chip.closest('div')).toHaveClass('border-primary-500')
  })

  it('withPrices keeps books that have a usable listing', () => {
    renderPrices('/prices?withPrices=true')
    expect(screen.getByTestId('price-total-11')).toBeInTheDocument()
    expect(screen.getByTestId('price-total-12')).toBeInTheDocument()
    expect(screen.getByTestId('price-total-14')).toBeInTheDocument()
    expect(screen.queryByTestId('price-status-13')).not.toBeInTheDocument()
  })

  it('noPrices keeps No matching listing rows and hides real listings', () => {
    renderPrices('/prices?noPrices=true')
    expect(screen.queryByTestId('price-total-11')).not.toBeInTheDocument()
    expect(screen.queryByTestId('price-total-12')).not.toBeInTheDocument()
    expect(screen.getByTestId('price-status-13')).toHaveTextContent('No matching listing')
    expect(screen.getByTestId('price-listing-13')).toHaveTextContent('Search')
    expect(screen.getByTestId('price-status-15')).toHaveTextContent('AbeBooks rate limited')
  })

  it('lookupErrors keeps rate-limited rows and hides No matching listing', () => {
    renderPrices('/prices?lookupErrors=true')
    expect(screen.getByTestId('price-status-15')).toHaveTextContent('AbeBooks rate limited')
    expect(screen.queryByTestId('price-status-13')).not.toBeInTheDocument()
    expect(screen.queryByTestId('price-total-11')).not.toBeInTheDocument()
  })

  it('puts lookup status in the Status column and listing URL in Listing', () => {
    renderPrices('/prices')
    expect(screen.getByTestId('price-status-11')).toHaveTextContent('—')
    expect(screen.getByTestId('price-listing-11')).toHaveTextContent('AbeBooks')
    expect(screen.getByTestId('price-listing-11')).toHaveAttribute(
      'href',
      'https://www.abebooks.com/pride/bd',
    )
    expect(screen.getByTestId('price-status-13')).toHaveTextContent('No matching listing')
    expect(screen.getByTestId('price-listing-13')).toHaveTextContent('Search')
    expect(screen.getByTestId('price-listing-13')).toHaveAttribute(
      'href',
      'https://www.abebooks.com/servlet/SearchResults?tn=Missing',
    )
  })

  it('price statistics follow the current book filters', () => {
    renderPrices('/prices?q=Pride')
    expect(screen.getByTestId('price-stats-total-cost')).toHaveTextContent('$4.86')
    expect(screen.getByTestId('price-stats-over-20')).toHaveTextContent('0')
    expect(screen.getByTestId('price-stats-total-books')).toHaveTextContent('1')
    expect(screen.getByTestId('price-stats-without-prices')).toHaveTextContent('0')
  })

  it('filters by Desire to Purchase like Reading Difficulty', () => {
    renderPrices('/prices?desireToPurchase=10')
    expect(screen.getByTestId('price-total-11')).toBeInTheDocument()
    expect(screen.getByTestId('price-status-15')).toBeInTheDocument()
    expect(screen.queryByTestId('price-total-12')).not.toBeInTheDocument()
    expect(screen.queryByTestId('price-status-13')).not.toBeInTheDocument()
  })

  it('Open in Books copies current filters as a URL', () => {
    renderPrices('/prices?q=Pride&status=in-library&lookupErrors=true')
    const link = screen.getByTestId('open-in-books')
    expect(link.tagName).toBe('A')
    expect(link).toHaveAttribute(
      'href',
      '/books?q=Pride&status=in-library&lookupErrors=true',
    )
  })
})
