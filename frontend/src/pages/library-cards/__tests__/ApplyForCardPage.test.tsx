// (c) Copyright 2025 by Muczynski
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ApplyForCardPage } from '../ApplyForCardPage'

const mutateAsync = vi.fn().mockResolvedValue({})

vi.mock('@/api/library-cards', () => ({
  useApplyForCard: () => ({ mutateAsync, isPending: false }),
}))

vi.mock('@/utils/auth', () => ({
  hashPassword: vi.fn().mockResolvedValue('ab'.repeat(32)),
}))

async function submitApplication(email?: string) {
  fireEvent.change(screen.getByTestId('apply-name'), { target: { value: 'Pat Patron' } })
  fireEvent.change(screen.getByTestId('apply-password'), { target: { value: 'password123' } })
  fireEvent.change(screen.getByTestId('apply-confirm-password'), { target: { value: 'password123' } })
  if (email) {
    fireEvent.change(screen.getByTestId('apply-email'), { target: { value: email } })
  }
  fireEvent.click(screen.getByTestId('apply-submit'))
}

describe('ApplyForCardPage', () => {
  it('tells applicants notification depends on providing an email', () => {
    render(<ApplyForCardPage />)

    expect(screen.getByText(/If you included an email, we will notify you when it is reviewed/)).toBeInTheDocument()
    expect(screen.queryByText(/You'll receive an email when approved/)).not.toBeInTheDocument()
  })

  it('does not promise email notification when no address was provided', async () => {
    render(<ApplyForCardPage />)
    await submitApplication()

    await waitFor(() => {
      expect(screen.getByTestId('application-next-step')).toHaveTextContent(
        'We cannot notify you by email because no address was provided',
      )
    })
    expect(screen.getByTestId('application-next-step').textContent).not.toMatch(/you will be notified/i)
  })

  it('promises email notification only when an address was provided', async () => {
    render(<ApplyForCardPage />)
    await submitApplication('pat@example.com')

    await waitFor(() => {
      expect(screen.getByTestId('application-next-step')).toHaveTextContent(
        'We will email you when your card is approved',
      )
    })
  })
})
