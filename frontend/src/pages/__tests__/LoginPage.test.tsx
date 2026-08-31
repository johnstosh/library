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
})
