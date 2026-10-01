// (c) Copyright 2025 by Muczynski
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LoanFormPage } from '../LoanFormPage'
import type { CurrentUser } from '@/stores/authStore'

const { authState, usersState } = vi.hoisted(() => ({
  authState: {
    user: {
      id: 2,
      username: 'librarian',
      authority: 'LIBRARIAN',
    } as CurrentUser,
  },
  usersState: {
    data: [
      { id: 1, username: 'testuser', authorities: ['USER'], lastModified: '2026-01-01T00:00:00' },
      { id: 2, username: 'librarian', authorities: ['LIBRARIAN'], lastModified: '2026-01-01T00:00:00' },
      { id: 3, username: 'otheruser', authorities: ['USER'], lastModified: '2026-01-01T00:00:00' },
    ] as Array<{ id: number; username: string; authorities: string[]; lastModified: string }>,
  },
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector?: (state: { user: CurrentUser }) => unknown) =>
    selector ? selector(authState) : authState,
  useIsLibrarian: () => authState.user.authority === 'LIBRARIAN',
}))

vi.mock('@/api/loans', () => ({
  useCheckoutBook: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useCheckoutBookWithPhoto: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useTranscribeCheckoutCard: () => ({ isPending: false, mutateAsync: vi.fn() }),
}))

const { checkoutState } = vi.hoisted(() => ({
  checkoutState: {
    data: [] as Array<{ id: number; title: string; author: string }>,
    isFetching: false,
    error: '',
  },
}))

vi.mock('@/api/checkoutBooks', async () => {
  const actual = await vi.importActual<typeof import('@/api/checkoutBooks')>('@/api/checkoutBooks')
  return {
    ...actual,
    useCheckoutBookSearch: () => checkoutState,
  }
})

vi.mock('@/api/users', () => ({
  useUsers: () => ({ data: usersState.data }),
}))

function renderCheckoutForm() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <LoanFormPage title="Checkout Book" onSuccess={() => {}} onCancel={() => {}} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const directory = [
  { id: 1, username: 'testuser', authorities: ['USER'], lastModified: '2026-01-01T00:00:00' },
  { id: 2, username: 'librarian', authorities: ['LIBRARIAN'], lastModified: '2026-01-01T00:00:00' },
  { id: 3, username: 'otheruser', authorities: ['USER'], lastModified: '2026-01-01T00:00:00' },
]

describe('LoanFormPage checkout', () => {
  it('shows the search progress on the book selector', () => {
    checkoutState.isFetching = true
    checkoutState.data = []
    checkoutState.error = ''
    renderCheckoutForm()

    expect(screen.getByTestId('loan-book-select-progress')).toBeTruthy()
    expect(screen.queryByTestId('loan-title-filter-progress')).toBeNull()
    expect(screen.queryByTestId('loan-author-filter-progress')).toBeNull()
    expect(screen.queryByTestId('loan-loc-filter-progress')).toBeNull()
    checkoutState.isFetching = false
  })

  it('defaults the borrower to the logged in librarian', () => {
    authState.user = { id: 2, username: 'librarian', authority: 'LIBRARIAN' }
    usersState.data = directory
    renderCheckoutForm()

    expect(screen.getByTestId('loan-user-select')).toHaveValue('2')
  })

  it('defaults the borrower to the logged in patron', () => {
    authState.user = { id: 1, username: 'testuser', authority: 'USER' }
    usersState.data = directory
    renderCheckoutForm()

    expect(screen.getByTestId('loan-user-select')).toHaveValue('1')
  })

  it('shows the logged in patron when the user list is unavailable', () => {
    authState.user = { id: 1, username: 'testuser', authority: 'USER' }
    usersState.data = []
    renderCheckoutForm()

    expect(screen.getByTestId('loan-user-select')).toHaveValue('1')
    expect(screen.getByRole('option', { name: 'testuser' })).toBeTruthy()
  })
})
