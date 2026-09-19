// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { BookTable } from '../BookTable'
import type { BookDto } from '@/types/dtos'

vi.mock('@/api/books', () => ({
  useDeleteBook: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn() }),
}))

vi.mock('@/components/favorites/FavoriteStar', () => ({
  FavoriteStar: () => null,
}))

const books: BookDto[] = [
  {
    id: 1,
    title: 'Summa Theologica',
    author: 'Thomas Aquinas',
    authorId: 1,
    authorGrokipediaUrl: 'https://grokipedia.com/page/Thomas_Aquinas',
    status: 'ACTIVE',
    lastModified: '2026-01-01T00:00:00',
    readingDifficulty: 'demanding',
  },
  {
    id: 8,
    title: 'Canticle of the Sun',
    author: 'Francis of Assisi',
    authorId: 5,
    status: 'ACTIVE',
    lastModified: '2026-01-01T00:00:00',
  },
  {
    id: 9,
    title: 'Electronic Book Example',
    author: 'Test Author',
    status: 'ACTIVE',
    lastModified: '2026-01-01T00:00:00',
    electronicResource: true,
    binding: 'UNKNOWN',
  },
]

function renderTable() {
  return render(
    <MemoryRouter>
      <BookTable
        books={books}
        isLoading={false}
        selectedIds={new Set()}
        selectAll={false}
        onSelectToggle={() => undefined}
        onSelectAll={() => undefined}
        onView={() => undefined}
      />
    </MemoryRouter>,
  )
}

describe('BookTable title column', () => {
  it('shows reading difficulty as a third line under title and author', () => {
    renderTable()

    expect(screen.getByTestId('book-title-link-1')).toHaveTextContent('Summa Theologica')
    expect(screen.getByTestId('book-author-link-1')).toHaveTextContent('Thomas Aquinas')
    expect(screen.getByTestId('book-reading-difficulty-1')).toHaveTextContent('Demanding')
    expect(screen.getByTestId('book-reading-difficulty-8')).toHaveTextContent('Unset')
    expect(screen.getByTestId('book-binding-1')).toHaveTextContent('Unknown')
    expect(screen.getByTestId('book-binding-8')).toHaveTextContent('Unknown')
    // electronic resource book shows special label (updated in BookTable via bookBindingDisplay)
    expect(screen.getByTestId('book-binding-9')).toHaveTextContent('Electronic resource')
  })

  it('shows a Grokipedia link next to the author when the author has a URL', () => {
    renderTable()

    const authorLink = screen.getByTestId('book-author-grokipedia-1')
    expect(authorLink).toHaveAttribute('href', 'https://grokipedia.com/page/Thomas_Aquinas')
    expect(screen.queryByTestId('book-author-grokipedia-8')).not.toBeInTheDocument()
  })
})
