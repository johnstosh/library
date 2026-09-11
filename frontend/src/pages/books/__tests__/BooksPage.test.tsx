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
    expect(screen.getByTestId('filter-no-prices')).toBeInTheDocument()
    expect(screen.getByTestId('filter-price-older')).toBeInTheDocument()
  })
})

describe('BooksPage Open in Prices', () => {
  it('hands current filters to /prices', () => {
    librarianState.current = true
    renderBooksPage('/books?q=Initial&inLib=true')

    const button = screen.getByTestId('open-in-prices')
    expect(button).toHaveTextContent('Open in Prices')
    expect(button.closest('form')).toBeNull()
    expect(button.getAttribute('href')).toBeNull()
    fireEvent.click(button)
    expect(screen.getByTestId('location')).toHaveTextContent('/prices?q=Initial&inLib=true')
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
