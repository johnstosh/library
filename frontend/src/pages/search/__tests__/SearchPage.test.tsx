// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { SearchPage } from '../SearchPage'
import type { AuthorDto, BookDto } from '@/types/dtos'
import type { SearchResponse } from '@/api/search'

const { searchResult } = vi.hoisted(() => {
  const books: BookDto[] = [
    {
      id: 1,
      title: 'Summa Theologica',
      author: 'Thomas Aquinas',
      authorId: 1,
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      firstPhotoId: 10,
      firstPhotoChecksum: 'cover-summa',
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
  return { searchResult }
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
  useIsLibrarian: () => false,
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
})
