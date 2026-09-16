// (c) Copyright 2025 by Muczynski
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { LoadingOverlay } from '../LoadingOverlay'

describe('LoadingOverlay', () => {
  it('renders nothing when hidden', () => {
    const { container } = render(<LoadingOverlay show={false} />)
    expect(container).toBeEmptyDOMElement()
    expect(screen.queryByTestId('loading-overlay')).not.toBeInTheDocument()
    expect(screen.queryByTestId('loading-overlay-spinner')).not.toBeInTheDocument()
  })

  it('dims the parent and portals a viewport-fixed spinner', () => {
    render(
      <div className="relative" data-test="overlay-parent">
        <LoadingOverlay show />
      </div>
    )

    const overlay = screen.getByTestId('loading-overlay')
    expect(overlay).toHaveClass('absolute', 'inset-0')
    expect(overlay.parentElement).toHaveAttribute('data-test', 'overlay-parent')

    const spinnerHost = screen.getByTestId('loading-overlay-spinner')
    expect(spinnerHost).toHaveClass('fixed', 'inset-0', 'pointer-events-none')
    expect(spinnerHost.parentElement).toBe(document.body)
    expect(spinnerHost).toHaveAttribute('role', 'status')
    expect(screen.getByTestId('spinner')).toBeInTheDocument()
  })
})
