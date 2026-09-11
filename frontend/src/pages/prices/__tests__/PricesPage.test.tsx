// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
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
    },
    {
      id: 2,
      title: 'Expensive Tome',
      author: 'Someone',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
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
  ]
  return { prices, books }
})

vi.mock('@/api/prices', () => ({
  usePrices: () => ({
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
}))

vi.mock('@/api/favorites', () => ({
  useFavoriteSummary: () => ({ data: { lists: [] } }),
  favoriteListChips: () => [],
  favoriteItemIdsForLists: () => new Set(),
  listNameToTestId: (name: string) => name,
}))

function renderPrices(path = '/prices') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <PricesPage />
    </MemoryRouter>,
  )
}

describe('PricesPage', () => {
  it('renders listings and filters by max total', () => {
    renderPrices('/prices')

    expect(screen.getByTestId('price-total-11')).toHaveTextContent('$4.86')
    expect(screen.getByTestId('price-total-12')).toHaveTextContent('$45.00')

    fireEvent.change(screen.getByTestId('prices-max-total'), { target: { value: '10' } })
    fireEvent.click(screen.getByTestId('prices-search-button'))

    expect(screen.getByTestId('price-total-11')).toBeInTheDocument()
    expect(screen.queryByTestId('price-total-12')).not.toBeInTheDocument()
  })

  it('filters by hardcover chip', () => {
    renderPrices('/prices?hardcover=true')
    expect(screen.getByTestId('price-total-11')).toBeInTheDocument()
    expect(screen.queryByTestId('price-total-12')).not.toBeInTheDocument()
  })
})
