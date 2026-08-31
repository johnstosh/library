// (c) Copyright 2025 by Muczynski
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LoanFormPage } from '../LoanFormPage'
import type { CurrentUser } from '@/stores/authStore'

const { authState } = vi.hoisted(() => ({
  authState: {
    user: {
      id: 2,
      username: 'librarian',
      authority: 'LIBRARIAN',
    } as CurrentUser,
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

vi.mock('@/api/books', () => ({
  useBooks: () => ({
    data: [
      {
        id: 1,
        title: 'Available Book 1',
        author: 'Author',
        status: 'ACTIVE',
        lastModified: '2026-01-01T00:00:00',
      },
    ],
    isFetching: false,
    isLoading: false,
  }),
}))

vi.mock('@/api/users', () => ({
  useUsers: () => ({
    data: [
      { id: 1, username: 'testuser', authorities: ['USER'], lastModified: '2026-01-01T00:00:00' },
      { id: 2, username: 'librarian', authorities: ['LIBRARIAN'], lastModified: '2026-01-01T00:00:00' },
      { id: 3, username: 'otheruser', authorities: ['USER'], lastModified: '2026-01-01T00:00:00' },
    ],
  }),
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

describe('LoanFormPage checkout', () => {
  it('defaults the borrower to the logged in librarian', () => {
    authState.user = { id: 2, username: 'librarian', authority: 'LIBRARIAN' }
    renderCheckoutForm()

    expect(screen.getByTestId('loan-user-select')).toHaveValue('2')
  })

  it('defaults the borrower to the logged in patron', () => {
    authState.user = { id: 1, username: 'testuser', authority: 'USER' }
    renderCheckoutForm()

    expect(screen.getByTestId('loan-user-select')).toHaveValue('1')
  })
})
