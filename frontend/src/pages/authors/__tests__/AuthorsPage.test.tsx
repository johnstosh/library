// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, useLocation, useSearchParams } from 'react-router-dom'
import { AuthorsPage } from '../AuthorsPage'
import type { AuthorDto } from '@/types/dtos'
import type { AuthorListFilters } from '@/api/authors'

const { catalog } = vi.hoisted(() => {
  const catalog: AuthorDto[] = [
    {
      id: 1,
      name: 'Initial Author',
      alternateNames: ['Saint Initial'],
      lastModified: '2026-01-01T00:00:00',
    },
    {
      id: 2,
      name: 'Other Person',
      lastModified: '2026-01-01T00:00:00',
    },
  ]
  return { catalog }
})

function matchesQuery(author: AuthorDto, q: string) {
  const needle = q.trim().toLowerCase()
  if (!needle) return true
  const names = [author.name, ...(author.alternateNames ?? [])]
  return names.some((name) => name.toLowerCase().includes(needle))
}

vi.mock('@/api/authors', async () => {
  const actual = await vi.importActual<typeof import('@/api/authors')>('@/api/authors')
  return {
    ...actual,
    useFilteredAuthors: (filters: AuthorListFilters) => {
      const data = catalog.filter((author) => matchesQuery(author, filters.q ?? ''))
      return {
        data,
        total: data.length,
        hasMore: false,
        loadMore: () => {},
        isLoadingMore: false,
        isLoading: false,
        isFetching: false,
        error: null,
      }
    },
    useAuthorCount: () => ({ data: { count: catalog.length } }),
  }
})

vi.mock('@/api/favorites', () => ({
  useFavoriteSummary: () => ({ data: { lists: [] } }),
  favoriteListChips: () => [],
}))

vi.mock('../components/AuthorTable', () => ({
  AuthorTable: ({ authors }: { authors: AuthorDto[] }) => (
    <div data-test="mocked-author-table">
      {authors.map((author) => (
        <div key={author.id} data-test={`author-row-${author.id}`}>
          {author.name}
        </div>
      ))}
    </div>
  ),
}))

vi.mock('../components/AuthorBulkActionsToolbar', () => ({
  AuthorBulkActionsToolbar: () => null,
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
      <div data-test="location">{`${location.pathname}${location.search}`}</div>
    </>
  )
}

function renderAuthorsPage(path = '/authors') {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <AuthorsPage />
        <UrlQuery />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AuthorsPage name search', () => {
  it('keeps the field wide beside a Search button that does not shrink', () => {
    renderAuthorsPage()

    const input = screen.getByTestId('authors-name-filter')
    const button = screen.getByTestId('authors-search-button')
    const controls = screen.getByTestId('authors-search-controls')

    expect(input).toHaveAttribute('type', 'search')
    expect(input).toHaveAttribute('enterkeyhint', 'search')
    expect(input).toHaveClass('text-base')
    expect(input).toHaveAttribute('placeholder', 'Filter by name...')
    expect(controls).toContainElement(input)
    expect(controls).toContainElement(button)
    expect(controls).toHaveClass('flex')
    expect(controls).not.toHaveClass('flex-col')
    expect(input.parentElement?.parentElement).toHaveClass('min-w-0', 'flex-1', 'w-full')
    expect(button).toHaveClass('shrink-0', 'whitespace-nowrap')
    expect(screen.queryByRole('button', { name: 'Clear' })).not.toBeInTheDocument()
  })

  it('does not search until Enter or the Search button', () => {
    renderAuthorsPage()

    const input = screen.getByTestId('authors-name-filter')
    fireEvent.change(input, { target: { value: 'Initial' } })

    expect(screen.getByTestId('url-q').textContent).toBe('')
    expect(screen.getByTestId('author-row-1')).toBeInTheDocument()
    expect(screen.getByTestId('author-row-2')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('authors-search-button'))

    expect(screen.getByTestId('url-q')).toHaveTextContent('Initial')
    expect(screen.getByTestId('author-row-1')).toBeInTheDocument()
    expect(screen.queryByTestId('author-row-2')).not.toBeInTheDocument()

    fireEvent.change(input, { target: { value: 'NoSuchAuthorZZZ' } })
    expect(screen.getByTestId('url-q')).toHaveTextContent('Initial')
    expect(screen.getByTestId('author-row-1')).toBeInTheDocument()

    fireEvent.keyDown(input, { key: 'Enter' })

    expect(screen.getByTestId('url-q')).toHaveTextContent('NoSuchAuthorZZZ')
    expect(screen.queryByTestId('author-row-1')).not.toBeInTheDocument()
    expect(screen.queryByTestId('author-row-2')).not.toBeInTheDocument()
  })

  it('keeps favorite lists in the URL when a name search is submitted', () => {
    renderAuthorsPage('/authors?favoriteLists=Saints')

    fireEvent.change(screen.getByTestId('authors-name-filter'), { target: { value: '  Initial  ' } })
    fireEvent.click(screen.getByTestId('authors-search-button'))

    const params = new URLSearchParams(screen.getByTestId('location').textContent?.split('?')[1])
    expect(params.get('q')).toBe('Initial')
    expect(params.get('favoriteLists')).toBe('Saints')
  })

  it('replaces a typed name when the URL query changes', () => {
    renderAuthorsPage('/authors?q=Initial')
    const input = screen.getByTestId('authors-name-filter')
    expect(input).toHaveValue('Initial')
    expect(screen.getByTestId('author-row-1')).toBeInTheDocument()
    expect(screen.queryByTestId('author-row-2')).not.toBeInTheDocument()

    fireEvent.change(input, { target: { value: 'Typed' } })
    expect(input).toHaveValue('Typed')

    fireEvent.click(screen.getByTestId('set-url-q'))
    expect(input).toHaveValue('FromUrl')
  })
})
