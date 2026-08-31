// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { memberSinceLabel, MyLibraryCardPage } from '../MyLibraryCardPage'
import type { CurrentUser } from '@/stores/authStore'

const authState = {
  user: {
    id: 7,
    username: 'pat',
    authority: 'USER',
    createdAt: '2024-03-15T10:00:00',
  } as CurrentUser,
}

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector?: (state: { user: CurrentUser }) => unknown) =>
    selector ? selector(authState) : authState,
}))

vi.mock('@/hooks/useToast', () => ({
  useToast: () => ({ error: vi.fn(), success: vi.fn() }),
}))

vi.mock('@/api/settings', () => ({
  useUserSettings: () => ({ data: { libraryCardDesign: 'CLASSICAL_DEVOTION' } }),
  useUpdateUserSettings: () => ({ mutateAsync: vi.fn() }),
}))

vi.mock('@/api/library-cards', () => ({
  printLibraryCard: vi.fn(),
  printAllLibraryCards: vi.fn(),
}))

vi.mock('@/components/LibraryCardDesignPicker', () => ({
  LibraryCardDesignPicker: () => <div data-test="design-picker" />,
}))

describe('memberSinceLabel', () => {
  it('uses the account created year instead of the current year', () => {
    expect(memberSinceLabel('2024-03-15T10:00:00')).toBe('Member Since 2024')
    expect(memberSinceLabel(undefined)).toBe('Member')
    expect(memberSinceLabel(null)).toBe('Member')
  })
})

describe('MyLibraryCardPage', () => {
  it('labels PDF actions as downloads and shows the real member year', () => {
    render(<MyLibraryCardPage />)

    expect(screen.getByTestId('member-since')).toHaveTextContent('Member Since 2024')
    expect(screen.getByTestId('print-library-card')).toHaveTextContent('Download PDF')
    expect(screen.getByTestId('print-all-library-cards')).toHaveTextContent('Download All Card Designs')
    expect(screen.queryByText('Print Card')).not.toBeInTheDocument()
  })
})
