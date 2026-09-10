// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { FavoriteStar } from '../FavoriteStar'

const { authState, summary } = vi.hoisted(() => ({
  authState: { authenticated: false },
  summary: {
    favoriteBookIds: [7] as number[],
    favoriteAuthorIds: [] as number[],
  },
}))

vi.mock('@/stores/authStore', () => ({
  useIsAuthenticated: () => authState.authenticated,
}))

vi.mock('@/api/favorites', () => ({
  useFavoriteSummary: () => ({ data: summary }),
}))

vi.mock('../FavoriteListsModal', () => ({
  FavoriteListsModal: ({ isOpen }: { isOpen: boolean }) =>
    isOpen ? <div data-test="favorite-modal">Favorite lists</div> : null,
}))

describe('FavoriteStar', () => {
  it('hides the star when the user is not logged in', () => {
    authState.authenticated = false
    render(<FavoriteStar itemType="BOOK" itemId={7} />)
    expect(screen.queryByTestId('favorite-star-book-7')).not.toBeInTheDocument()
  })

  it('shows a filled red star for favorited items and an outline otherwise', () => {
    authState.authenticated = true
    summary.favoriteBookIds = [7]
    const { rerender } = render(<FavoriteStar itemType="BOOK" itemId={7} />)
    expect(screen.getByTestId('favorite-star-book-7-filled')).toBeInTheDocument()

    summary.favoriteBookIds = []
    rerender(<FavoriteStar itemType="BOOK" itemId={7} />)
    expect(screen.getByTestId('favorite-star-book-7-outline')).toBeInTheDocument()
  })

  it('opens the lists modal when the star is clicked', () => {
    authState.authenticated = true
    summary.favoriteBookIds = [7]
    render(<FavoriteStar itemType="BOOK" itemId={7} />)
    fireEvent.click(screen.getByTestId('favorite-star-book-7'))
    expect(screen.getByTestId('favorite-modal')).toBeInTheDocument()
  })
})
