// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthorTable } from '../AuthorTable'
import type { AuthorDto } from '@/types/dtos'

vi.mock('@/api/authors', () => ({
  useDeleteAuthor: () => ({ mutateAsync: vi.fn(), isPending: false }),
}))

vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn() }),
}))

vi.mock('@/components/favorites/FavoriteStar', () => ({
  FavoriteStar: () => null,
}))

const longName = 'A'.repeat(70)
const longBiography = 'B'.repeat(70)

const author: AuthorDto = {
  id: 4,
  name: longName,
  briefBiography: longBiography,
  religiousAffiliation: 'Order of Preachers',
  bookCount: 3,
  lastModified: '2026-01-01T00:00:00',
}

function renderTable() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <AuthorTable
          authors={[author]}
          isLoading={false}
          selectedIds={new Set()}
          selectAll={false}
          onSelectToggle={() => undefined}
          onSelectAll={() => undefined}
          onView={() => undefined}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AuthorTable column widths', () => {
  it('gives Name the wide column and Biography the narrow one, with matching ellipsis', () => {
    renderTable()

    const headers = screen.getAllByRole('columnheader')
    const name = headers.find((header) => header.textContent === 'Name')
    const biography = headers.find((header) => header.textContent === 'Biography')
    const affiliation = headers.find((header) => header.textContent === 'Religious Affiliation')
    const books = headers.find((header) => header.textContent === 'Books')

    expect(name).toHaveStyle({ width: '48%' })
    expect(biography).toHaveStyle({ width: '7%' })
    expect(affiliation).toHaveStyle({ width: '15%' })
    expect(books).toHaveStyle({ width: '10%' })

    const headerText = headers.map((header) => header.textContent)
    expect(headerText.indexOf('Name')).toBeLessThan(headerText.indexOf('Biography'))

    expect(screen.getByTestId('author-name-link-4')).toHaveTextContent(`${'A'.repeat(60)}...`)
    expect(screen.getByText(`${'B'.repeat(18)}...`)).toBeInTheDocument()
    expect(screen.getByText('Order of Preachers')).toBeInTheDocument()
  })
})
