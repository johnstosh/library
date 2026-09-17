// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, useLocation, useSearchParams } from 'react-router-dom'
import { BooksPage } from '../BooksPage'
import type { BookDto } from '@/types/dtos'

const { catalog, librarianState } = vi.hoisted(() => {
  const catalog: BookDto[] = [
    {
      id: 1,
      title: 'Initial Book',
      author: 'Lewis',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      dateAddedToLibrary: '2026-08-30T00:00:00',
      readingDifficulty: 'children',
      binding: 'HARDCOVER',
    },
    {
      id: 2,
      title: 'Other Book',
      author: 'Tolkien',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      dateAddedToLibrary: '2026-08-30T00:00:00',
    },
  ]
  return { catalog, librarianState: { current: true } }
})

vi.mock('@/stores/authStore', () => ({
  useIsLibrarian: () => librarianState.current,
}))

vi.mock('@/api/books', () => ({
  useBooks: () => ({
    data: catalog,
    isLoading: false,
    isFetching: false,
    error: null,
  }),
  useBookCount: () => ({ data: { count: 2 } }),
}))

vi.mock('../components/BookTable', () => ({
  BookTable: ({ books }: { books: BookDto[] }) => (
    <div data-test="mocked-book-table">
      {books.map((row) => (
        <div key={row.id} data-test={`book-row-${row.id}`}>
          {row.title}
        </div>
      ))}
    </div>
  ),
}))

vi.mock('../components/BulkActionsToolbar', () => ({
  BulkActionsToolbar: () => null,
}))

vi.mock('@/api/prices', () => ({
  usePrices: () => ({
    data: [],
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

function UrlQuery() {
  const [params] = useSearchParams()
  const location = useLocation()
  return (
    <>
      <div data-test="url-q">{params.get('q') ?? ''}</div>
      <div data-test="url-reading-difficulty">{params.get('readingDifficulty') ?? ''}</div>
      <div data-test="url-binding">{params.get('binding') ?? ''}</div>
      <div data-test="url-status">{params.get('status') ?? ''}</div>
      <div data-test="location">{`${location.pathname}${location.search}`}</div>
    </>
  )
}

function renderBooksPage(path = '/books') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <BooksPage />
      <UrlQuery />
    </MemoryRouter>,
  )
}

describe('BooksPage Pricing filters', () => {
  it('shows Pricing at the bottom of the filters for librarians', () => {
    librarianState.current = true
    renderBooksPage('/books?mostRecent=false')
    expect(screen.getByTestId('book-price-filters')).toHaveTextContent('Pricing')
    expect(screen.getByTestId('filter-with-prices')).toBeInTheDocument()
    expect(screen.getByTestId('filter-no-prices')).toHaveTextContent('Books without Pricing')
    expect(screen.getByTestId('filter-lookup-errors')).toHaveTextContent('Lookup Errors')
    expect(screen.getByTestId('filter-price-older')).toBeInTheDocument()
    expect(screen.queryByTestId('filter-price-hardcover')).not.toBeInTheDocument()
    expect(screen.queryByTestId('filter-price-other-unknown')).not.toBeInTheDocument()
  })

  it('does not show price statistics on the books page', () => {
    librarianState.current = true
    renderBooksPage('/books?mostRecent=false')
    expect(screen.queryByTestId('price-statistics')).not.toBeInTheDocument()
  })
})

describe('BooksPage Open in Prices', () => {
  it('hands current filters to /prices as a URL', () => {
    librarianState.current = true
    renderBooksPage('/books?q=Initial&status=in-library')

    const link = screen.getByTestId('open-in-prices')
    expect(link).toHaveTextContent('Open in Prices')
    expect(link.closest('form')).toBeNull()
    expect(link).toHaveAttribute('href', '/prices?q=Initial&status=in-library')
    fireEvent.click(link)
    expect(screen.getByTestId('location')).toHaveTextContent('/prices?q=Initial&status=in-library')
  })
})

describe('BooksPage title filter', () => {
  it('does not search until Enter or the Search button', () => {
    renderBooksPage()

    const input = screen.getByTestId('books-title-filter')
    fireEvent.change(input, { target: { value: 'Initial' } })

    expect(screen.getByTestId('url-q').textContent).toBe('')
    expect(screen.getByTestId('book-row-1')).toBeInTheDocument()
    expect(screen.getByTestId('book-row-2')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('books-search-button'))

    expect(screen.getByTestId('url-q')).toHaveTextContent('Initial')
    expect(screen.getByTestId('book-row-1')).toBeInTheDocument()
    expect(screen.queryByTestId('book-row-2')).not.toBeInTheDocument()

    fireEvent.change(input, { target: { value: 'NoSuchTitleZZZ' } })
    expect(screen.getByTestId('url-q')).toHaveTextContent('Initial')
    expect(screen.getByTestId('book-row-1')).toBeInTheDocument()

    fireEvent.submit(input.closest('form')!)

    expect(screen.getByTestId('url-q')).toHaveTextContent('NoSuchTitleZZZ')
    expect(screen.queryByTestId('book-row-1')).not.toBeInTheDocument()
    expect(screen.queryByTestId('book-row-2')).not.toBeInTheDocument()
  })
})

describe('BooksPage reading difficulty filter', () => {
  it('ORs selected difficulties and treats a missing value as Unset', () => {
    renderBooksPage('/books?readingDifficulty=children')
    expect(screen.getByTestId('book-row-1')).toBeInTheDocument()
    expect(screen.queryByTestId('book-row-2')).not.toBeInTheDocument()
  })

  it('keeps Unset books when Unset is selected', () => {
    renderBooksPage('/books?readingDifficulty=unset')
    expect(screen.queryByTestId('book-row-1')).not.toBeInTheDocument()
    expect(screen.getByTestId('book-row-2')).toBeInTheDocument()
  })

  it('writes the selected chip into the URL', () => {
    renderBooksPage('/books?mostRecent=false')
    fireEvent.click(screen.getByTestId('reading-difficulty-filter-children'))
    expect(screen.getByTestId('url-reading-difficulty')).toHaveTextContent('children')
  })
})

describe('BooksPage binding filter', () => {
  it('ORs selected bindings and treats a missing value as Unknown', () => {
    renderBooksPage('/books?binding=HARDCOVER')
    expect(screen.getByTestId('book-row-1')).toBeInTheDocument()
    expect(screen.queryByTestId('book-row-2')).not.toBeInTheDocument()
  })

  it('keeps Unknown books when Unknown is selected', () => {
    renderBooksPage('/books?binding=UNKNOWN')
    expect(screen.queryByTestId('book-row-1')).not.toBeInTheDocument()
    expect(screen.getByTestId('book-row-2')).toBeInTheDocument()
  })

  it('writes the selected chip into the URL', () => {
    renderBooksPage('/books?mostRecent=false')
    fireEvent.click(screen.getByTestId('binding-filter-library-binding'))
    expect(screen.getByTestId('url-binding')).toHaveTextContent('LIBRARY_BINDING')
  })
})

describe('BooksPage status filter', () => {
  it('writes the selected status chip into the URL', () => {
    renderBooksPage('/books?mostRecent=false')
    fireEvent.click(screen.getByTestId('status-filter-requested'))
    expect(screen.getByTestId('url-status')).toHaveTextContent('requested')
  })
})
