// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, useSearchParams } from 'react-router-dom'
import { BooksPage } from '../BooksPage'
import type { BookDto } from '@/types/dtos'

const { catalog } = vi.hoisted(() => {
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
  return { catalog }
})

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

function UrlQuery() {
  const [params] = useSearchParams()
  return (
    <>
      <div data-test="url-q">{params.get('q') ?? ''}</div>
      <div data-test="url-reading-difficulty">{params.get('readingDifficulty') ?? ''}</div>
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
