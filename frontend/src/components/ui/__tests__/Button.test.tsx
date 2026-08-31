// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Button } from '../Button'

describe('Button', () => {
  it('uses burgundy primary instead of software blue', () => {
    render(<Button variant="primary">Save</Button>)
    const button = screen.getByRole('button', { name: 'Save' })
    expect(button).toHaveClass('bg-primary-600')
    expect(button.className).not.toMatch(/blue-/)
  })

  it('uses charcoal for the secondary variant', () => {
    render(<Button variant="secondary">Cancel</Button>)
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveClass('bg-charcoal-700')
  })
})
