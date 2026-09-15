// (c) Copyright 2025 by Muczynski
import type { ReactNode } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { FavoriteListsModal } from '../FavoriteListsModal'
import type { FavoriteSummaryDto } from '@/api/favorites'

const { authState, summaryState, mutate } = vi.hoisted(() => ({
  authState: { librarian: false },
  summaryState: { data: undefined as FavoriteSummaryDto | undefined },
  mutate: vi.fn(),
}))

vi.mock('@/stores/authStore', () => ({
  useIsLibrarian: () => authState.librarian,
}))

vi.mock('@/api/favorites', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/favorites')>()
  return {
    ...actual,
    useFavoriteSummary: () => ({ data: summaryState.data }),
    useReplaceFavoriteItem: () => ({ mutate }),
  }
})

vi.mock('@/components/ui/Modal', () => ({
  Modal: ({
    isOpen,
    children,
    title,
  }: {
    isOpen: boolean
    children: ReactNode
    title: string
  }) => (isOpen ? (
    <div data-test="favorite-modal">
      <h2>{title}</h2>
      {children}
    </div>
  ) : null),
}))

describe('FavoriteListsModal', () => {
  it('renders lists from the cached summary without waiting on GET /favorites/item', () => {
    authState.librarian = true
    mutate.mockReset()
    summaryState.data = {
      favoriteBookIds: [7],
      favoriteAuthorIds: [],
      lists: [{ listName: 'Have Read', bookIds: [7], authorIds: [] }],
      availableLists: [
        'Have Read',
        'Want to Read',
        'Want to Recommend',
        'Needs Review',
        'Need to Locate',
        'Nightstand',
      ],
    }

    render(<FavoriteListsModal isOpen itemType="BOOK" itemId={7} onClose={() => {}} />)

    expect(screen.queryByText('Loading lists…')).not.toBeInTheDocument()
    expect(screen.getByTestId('favorite-list-have-read')).toBeChecked()
    expect(screen.getByTestId('favorite-list-want-to-read')).not.toBeChecked()
    expect(screen.getByTestId('favorite-list-needs-review')).toBeInTheDocument()
    expect(screen.getByTestId('favorite-list-nightstand')).toBeInTheDocument()
  })

  it('persists a checkbox change immediately', () => {
    authState.librarian = false
    mutate.mockReset()
    summaryState.data = {
      favoriteBookIds: [],
      favoriteAuthorIds: [],
      lists: [],
      availableLists: ['Have Read', 'Want to Read', 'Want to Recommend'],
    }

    render(<FavoriteListsModal isOpen itemType="BOOK" itemId={7} onClose={() => {}} />)
    fireEvent.click(screen.getByTestId('favorite-list-have-read'))

    expect(mutate).toHaveBeenCalledWith({
      itemType: 'BOOK',
      itemId: 7,
      listNames: ['Have Read'],
    })
  })
})
