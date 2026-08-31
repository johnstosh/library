// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UserSettingsPage } from '../UserSettingsPage'

const mutateAsync = vi.fn().mockResolvedValue({})

vi.mock('@/api/settings', () => {
  const data = {
    email: 'pat@example.com',
    phone: '555-0100',
  }
  return {
    useUserSettings: () => ({
      data,
      refetch: vi.fn(),
    }),
    useUpdateUserSettings: () => ({ mutateAsync, isPending: false }),
  }
})

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { username: 'pat', authority: 'USER' },
  }),
}))

vi.mock('@/utils/auth', () => ({
  hashPassword: vi.fn().mockResolvedValue('ab'.repeat(32)),
}))

describe('UserSettingsPage', () => {
  beforeEach(() => {
    mutateAsync.mockClear()
  })

  it('lets a patron change email and phone', async () => {
    render(<UserSettingsPage />)

    const email = screen.getByTestId('settings-email')
    const phone = screen.getByTestId('settings-phone')
    expect(email).toHaveValue('pat@example.com')
    expect(phone).toHaveValue('555-0100')

    fireEvent.change(email, { target: { value: 'new@example.com' } })
    fireEvent.change(phone, { target: { value: '555-0199' } })
    fireEvent.click(screen.getByTestId('save-contact-info'))

    await waitFor(() => {
      expect(mutateAsync).toHaveBeenCalledWith({
        email: 'new@example.com',
        phone: '555-0199',
      })
    })
    expect(screen.getByText('Contact information updated successfully')).toBeInTheDocument()
  })

  it('rejects an invalid email without saving', async () => {
    render(<UserSettingsPage />)

    fireEvent.change(screen.getByTestId('settings-email'), { target: { value: 'not-an-email' } })
    fireEvent.click(screen.getByTestId('save-contact-info'))

    expect(await screen.findByText('Enter a valid email address or leave it blank')).toBeInTheDocument()
    expect(mutateAsync).not.toHaveBeenCalled()
  })
})
