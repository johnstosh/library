// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, useLocation, useSearchParams } from 'react-router-dom'
import { BooksPage } from '../BooksPage'
import type { BookDto } from '@/types/dtos'

const { catalog, librarianState, filterCatalog } = vi.hoisted(() => {
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
  function filterCatalog(filters?: {
    q?: string
    readingDifficulties?: readonly string[]
    bindings?: readonly string[]
  }) {
    if (!filters) return catalog
    const q = (filters.q ?? '').trim().toLowerCase()
    const difficulties = filters.readingDifficulties ?? []
    const bindings = filters.bindings ?? []
    return catalog.filter((book) => {
      if (q) {
        const title = (book.title ?? '').toLowerCase()
        const author = (book.author ?? '').toLowerCase()
        if (!title.includes(q) && !author.includes(q)) return false
      }
      if (difficulties.length > 0) {
        const value = book.readingDifficulty?.trim()
          ? book.readingDifficulty.trim().toLowerCase()
          : 'unset'
        if (!difficulties.includes(value)) return false
      }
      if (bindings.length > 0) {
        const value = book.binding?.trim() ? book.binding.trim().toUpperCase() : 'UNKNOWN'
        if (!bindings.includes(value)) return false
      }
      return true
    })
  }
  return { catalog, librarianState: { current: true }, filterCatalog }
})

vi.mock('@/stores/authStore', () => ({
  useIsLibrarian: () => librarianState.current,
}))

vi.mock('@/api/books', () => ({
  useBooks: (filters?: Parameters<typeof filterCatalog>[0]) => {
    const data = filterCatalog(filters)
    return {
      data,
      total: data.length,
      isLoading: false,
      isFetching: false,
      error: null,
    }
  },
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
  const [params, setSearchParams] = useSearchParams()
  const location = useLocation()
  return (
    <>
      <div data-test="url-q">{params.get('q') ?? ''}</div>
      <button type="button" data-test="set-url-q" onClick={() => setSearchParams({ q: 'FromUrl' })}>
        set url
      </button>
      <div data-test="url-reading-difficulty">{params.get('readingDifficulty') ?? ''}</div>
      <div data-test="url-binding">{params.get('binding') ?? ''}</div>
      <div data-test="url-status">{params.get('status') ?? ''}</div>
      <div data-test="location">{`${location.pathname}${location.search}`}</div>
    </>
  )
}

function renderBooksPage(path = '/books') {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <BooksPage />
        <UrlQuery />
      </MemoryRouter>
    </QueryClientProvider>,
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

    fireEvent.keyDown(input, { key: 'Enter' })

    expect(screen.getByTestId('url-q')).toHaveTextContent('NoSuchTitleZZZ')
    expect(screen.queryByTestId('book-row-1')).not.toBeInTheDocument()
    expect(screen.queryByTestId('book-row-2')).not.toBeInTheDocument()
  })

  it('replaces a typed title when the URL query changes', () => {
    renderBooksPage('/books?q=Initial')
    const input = screen.getByTestId('books-title-filter')
    expect(input).toHaveValue('Initial')

    fireEvent.change(input, { target: { value: 'Typed' } })
    expect(input).toHaveValue('Typed')

    fireEvent.click(screen.getByTestId('set-url-q'))
    expect(input).toHaveValue('FromUrl')
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
