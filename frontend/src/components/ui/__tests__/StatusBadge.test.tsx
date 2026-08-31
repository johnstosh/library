// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { StatusBadge } from '../StatusBadge'
import { applicationStatusLabel, applicationStatusTone, bookStatusTone } from '@/utils/status'
import { IconButton } from '../IconButton'
import { EntityLink } from '../EntityLink'
import { ViewIcon } from '../Icons'

describe('StatusBadge', () => {
  it('uses the shared pill classes and status tone', () => {
    render(
      <StatusBadge tone="success" data-test="status-badge">
        Active
      </StatusBadge>
    )
    const badge = screen.getByTestId('status-badge')
    expect(badge).toHaveClass('bg-green-100', 'text-green-800', 'rounded-full')
  })

  it('uses burgundy primary for informational status', () => {
    render(
      <StatusBadge tone="info" data-test="info-badge">
        Checked out
      </StatusBadge>
    )
    expect(screen.getByTestId('info-badge')).toHaveClass('bg-primary-100', 'text-primary-800')
  })

  it('uses forest green for accent status', () => {
    render(
      <StatusBadge tone="accent" data-test="accent-badge">
        Accent
      </StatusBadge>
    )
    expect(screen.getByTestId('accent-badge')).toHaveClass('bg-forest-100', 'text-forest-800')
  })

  it('maps book statuses onto the shared tones', () => {
    expect(bookStatusTone('ACTIVE')).toBe('success')
    expect(bookStatusTone('ON_ORDER')).toBe('info')
    expect(bookStatusTone('LOST')).toBe('danger')
    expect(bookStatusTone('DAMAGED')).toBe('warning')
  })

  it('maps application statuses onto labels and tones', () => {
    expect(applicationStatusTone('APPROVED')).toBe('success')
    expect(applicationStatusTone('QUESTION')).toBe('info')
    expect(applicationStatusTone('NOT_APPROVED')).toBe('danger')
    expect(applicationStatusTone('PENDING')).toBe('warning')
    expect(applicationStatusLabel('NOT_APPROVED')).toBe('Not approved')
    expect(applicationStatusLabel(undefined)).toBe('Pending')
  })
})

describe('IconButton', () => {
  it('renders a labelled button', () => {
    render(<IconButton icon={<ViewIcon />} label="View Details" data-test="view-btn" />)
    expect(screen.getByRole('button', { name: 'View Details' })).toBeInTheDocument()
  })

  it('renders an internal link when to is set', () => {
    render(
      <MemoryRouter>
        <IconButton icon={<ViewIcon />} label="View Details" to="/books/1" data-test="view-link" />
      </MemoryRouter>
    )
    expect(screen.getByRole('link', { name: 'View Details' })).toHaveAttribute('href', '/books/1')
  })

  it('does not navigate when a link-style button is disabled', () => {
    render(
      <MemoryRouter>
        <IconButton icon={<ViewIcon />} label="View Details" to="/books/1" disabled />
      </MemoryRouter>
    )
    const link = screen.getByRole('link', { name: 'View Details' })
    expect(link).toHaveAttribute('aria-disabled', 'true')
    expect(link).toHaveAttribute('tabindex', '-1')
  })
})

describe('EntityLink', () => {
  it('renders an internal link', () => {
    render(
      <MemoryRouter>
        <EntityLink to="/books/1" data-test="title-link">
          Dune
        </EntityLink>
      </MemoryRouter>
    )
    const link = screen.getByRole('link', { name: 'Dune' })
    expect(link).toHaveAttribute('href', '/books/1')
    expect(link).toHaveAttribute('data-test', 'title-link')
  })

  it('stops click bubbling so a parent row does not navigate instead', () => {
    const onRowClick = vi.fn()
    render(
      <MemoryRouter>
        <div onClick={onRowClick}>
          <EntityLink to="/authors/2">Frank Herbert</EntityLink>
        </div>
      </MemoryRouter>
    )
    fireEvent.click(screen.getByRole('link', { name: 'Frank Herbert' }))
    expect(onRowClick).not.toHaveBeenCalled()
  })
})
