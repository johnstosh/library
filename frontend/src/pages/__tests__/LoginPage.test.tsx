// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LoginPage } from '../LoginPage'

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: Record<string, unknown>) => unknown) =>
    selector({
      login: vi.fn(),
      getAndClearReturnUrl: () => null,
      user: null,
    }),
  homePathForUser: () => '/search',
}))

vi.mock('@/api/branches', () => ({
  useFirstBranch: () => ({
    branchName: 'Northside',
    librarySystemName: 'River Library System',
    hasBranch: true,
  }),
}))

describe('LoginPage', () => {
  it('does not promise loan renewal', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    const welcome = screen.getByTestId('login-welcome')
    expect(welcome).toHaveTextContent('Access your borrowed books and discover new reads')
    expect(welcome.textContent?.toLowerCase()).not.toContain('renew')
  })

  it('shows the first branch name from the table, not a hardcoded library name', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    const heading = screen.getByTestId('login-branch-name')
    expect(heading).toHaveTextContent('Northside Branch')
    expect(heading).toHaveTextContent('River Library System')
    expect(heading.textContent).not.toContain('St. Martin')
    expect(heading.textContent).not.toContain('Sacred Heart')
  })
})
