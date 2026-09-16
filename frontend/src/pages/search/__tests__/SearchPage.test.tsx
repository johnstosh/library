// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SearchPage } from '../SearchPage'
import type { AuthorDto, BookDto } from '@/types/dtos'
import type { SearchResponse } from '@/api/search'

const { searchResult, librarianState } = vi.hoisted(() => {
  const books: BookDto[] = [
    {
      id: 1,
      title: 'Summa Theologica',
      author: 'Thomas Aquinas',
      authorId: 1,
      authorGrokipediaUrl: 'https://grokipedia.com/page/Thomas_Aquinas',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      firstPhotoId: 10,
      firstPhotoChecksum: 'cover-summa',
      readingDifficulty: 'demanding',
      publicationYear: 1485,
      publisher: 'Catholic Press',
      library: 'St. Martin de Porres',
    },
    {
      id: 8,
      title: 'Canticle of the Sun',
      author: 'Francis of Assisi',
      authorId: 5,
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
    },
  ]
  const authors: AuthorDto[] = [
    {
      id: 4,
      name: 'Teresa of Avila',
      lastModified: '2026-01-01T00:00:00',
      firstPhotoId: 20,
      firstPhotoChecksum: 'author-teresa',
      bookCount: 2,
    },
  ]
  const searchResult: SearchResponse = {
    books,
    authors,
    bookPage: { totalPages: 1, totalElements: 2, currentPage: 0, pageSize: 20 },
    authorPage: { totalPages: 1, totalElements: 1, currentPage: 0, pageSize: 20 },
  }
  return { searchResult, librarianState: { current: false } }
})

vi.mock('@/api/search', () => ({
  useSearch: () => ({
    data: searchResult,
    isLoading: false,
    error: null,
  }),
}))

vi.mock('@/api/books', () => ({
  useDeleteBook: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/api/authors', () => ({
  useDeleteAuthor: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/stores/authStore', () => ({
  useIsLibrarian: () => librarianState.current,
  useIsAuthenticated: () => librarianState.current,
}))

vi.mock('@/components/favorites/FavoriteStar', () => ({
  FavoriteStar: () => null,
}))

vi.mock('@/api/favorites', () => ({
  useFavoriteSummary: () => ({ data: { lists: [] } }),
  favoriteListChips: () => [],
  listNameToTestId: (name: string) => name,
}))

vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn() }),
}))

function renderSearch(path = '/search?q=Summa') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <SearchPage />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  librarianState.current = false
})

describe('SearchPage header', () => {
  it('tells patrons they can search by name or browse with filters', () => {
    renderSearch('/search')

    const description = screen.getByText(/Search for books and authors by title or name/)
    expect(description).toHaveTextContent('Use the filters to browse.')
    expect(description.querySelector('br')).toBeInTheDocument()
  })

  it('puts the search controls on a white card like the other pages', () => {
    renderSearch('/search')

    const form = screen.getByTestId('search-input').closest('form')
    expect(form).not.toBeNull()
    const card = form!.closest('.bg-white.rounded-lg.shadow')
    expect(card).not.toBeNull()
    expect(card).toContainElement(screen.getByTestId('search-button'))
    expect(card).toContainElement(screen.getByTestId('search-filter-chips'))
    expect(card).toContainElement(screen.getByTestId('book-label-filters'))
    expect(card).toContainElement(screen.getByTestId('reading-difficulty-filters'))
    expect(screen.getByTestId('reading-difficulty-filter-unset')).toHaveTextContent('Unset')
  })
})

describe('SearchPage covers', () => {
  it('shows book and author thumbnails the same way the Books table does', () => {
    renderSearch()

    const bookCover = screen.getByTestId('book-result-cover-1')
    expect(bookCover).toHaveAttribute('href', '/photos/10')
    const bookImg = screen.getByAltText('Cover of Summa Theologica')
    expect(bookImg).toHaveAttribute('src', '/api/photos/10/thumbnail?width=70&v=cover-summa')
    fireEvent.load(bookImg)
    expect(bookImg).toHaveAttribute('data-test', 'thumbnail-img')

    const placeholder = screen.getByTestId('book-result-cover-8')
    expect(placeholder).toHaveTextContent('-')
    expect(screen.queryByAltText('Cover of Canticle of the Sun')).not.toBeInTheDocument()

    const authorCover = screen.getByTestId('author-result-cover-4')
    expect(authorCover).toHaveAttribute('href', '/photos/20')
    const authorImg = screen.getByAltText('Photo of Teresa of Avila')
    expect(authorImg).toHaveAttribute('src', '/api/photos/20/thumbnail?width=70&v=author-teresa')
    fireEvent.load(authorImg)
    expect(authorImg).toHaveAttribute('data-test', 'thumbnail-img')
  })

  it('shows reading difficulty as a third line under title and author', () => {
    renderSearch()

    expect(screen.getByTestId('book-result-reading-difficulty-1')).toHaveTextContent('Demanding')
    expect(screen.getByTestId('book-result-reading-difficulty-8')).toHaveTextContent('Unset')
  })

  it('shows a Grokipedia link next to the author when the author has a URL', () => {
    renderSearch()

    const authorLink = screen.getByTestId('book-result-author-grokipedia-1')
    expect(authorLink).toHaveAttribute('href', 'https://grokipedia.com/page/Thomas_Aquinas')
    expect(screen.queryByTestId('book-result-author-grokipedia-8')).not.toBeInTheDocument()
  })

  it('hides year, publisher, and branch on phone widths', () => {
    renderSearch()

    const meta = screen.getByTestId('book-result-meta-1')
    expect(meta).toHaveClass('hidden')
    expect(meta).toHaveClass('sm:flex')
    expect(meta).toHaveTextContent('1485')
    expect(meta).toHaveTextContent('Catholic Press')
    expect(meta).toHaveTextContent('St. Martin de Porres')
    expect(screen.queryByTestId('book-result-meta-8')).not.toBeInTheDocument()
  })
})

describe('SearchPage librarian controls', () => {
  it('renders Open in Books below the search row, not in the same flex container as the input', () => {
    librarianState.current = true
    renderSearch('/search')

    const searchInput = screen.getByTestId('search-input')
    const searchButton = screen.getByTestId('search-button')
    const openInBooks = screen.getByTestId('open-in-books')
    const searchControls = screen.getByTestId('search-controls')
    const form = searchInput.closest('form')

    expect(openInBooks).toHaveTextContent('Open in Books')
    expect(searchControls).toContainElement(searchInput)
    expect(searchControls).toContainElement(searchButton)
    expect(searchControls).not.toContainElement(openInBooks)
    expect(form).toContainElement(openInBooks)
    expect(openInBooks.parentElement).toBe(form)
    expect(searchControls.parentElement).toBe(form)
  })

  it('uses a Books URL so it can be opened in a new tab', () => {
    librarianState.current = true
    renderSearch('/search?q=Summa&status=in-library')

    const link = screen.getByTestId('open-in-books')
    expect(link.tagName).toBe('A')
    expect(link).toHaveAttribute('href', '/books?q=Summa&status=in-library')
  })

  it('includes the typed query in the Books URL before Search is submitted', () => {
    librarianState.current = true
    renderSearch('/search')

    fireEvent.change(screen.getByTestId('search-input'), { target: { value: 'City of God' } })
    expect(screen.getByTestId('open-in-books')).toHaveAttribute('href', '/books?q=City+of+God')
  })

  it('uses default md sizes like Books instead of lg', () => {
    librarianState.current = true
    renderSearch('/search?q=Summa')

    expect(screen.getByTestId('search-input')).not.toHaveClass('text-lg')

    for (const testId of ['search-button', 'open-in-books']) {
      const button = screen.getByTestId(testId)
      expect(button).toHaveClass('text-base')
      expect(button).toHaveClass('px-4')
      expect(button).toHaveClass('py-2.5')
      expect(button).not.toHaveClass('text-lg')
      expect(button).not.toHaveClass('px-6')
    }
  })
})

describe('SearchPage clear control', () => {
  it('uses a search input (x) like Books instead of a Clear button', () => {
    renderSearch('/search?q=Summa')

    expect(screen.getByTestId('search-input')).toHaveAttribute('type', 'search')
    expect(screen.queryByTestId('clear-search')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Clear' })).not.toBeInTheDocument()
  })
})
