// (c) Copyright 2025 by Muczynski
import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { TransientFetchErrorBanner } from '../TransientFetchErrorBanner'
import { ApiError } from '@/api/client'

describe('TransientFetchErrorBanner', () => {
  const mockOnRetry = vi.fn()
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  const renderWithQueryClient = (ui: React.ReactElement) => {
    return render(
      <QueryClientProvider client={client}>
        {ui}
      </QueryClientProvider>
    )
  }

  let mockReload: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.clearAllMocks()
    // Mock reload to avoid "Cannot redefine property: reload" error in jsdom.
    mockReload = vi.fn()
    Object.defineProperty(window, 'location', {
      value: { ...window.location, reload: mockReload },
      writable: true,
      configurable: true,
    })
  })

  it('returns null when no error', () => {
    const { container } = renderWithQueryClient(<TransientFetchErrorBanner error={null} />)
    expect(container.firstChild).toBeNull()
  })

  it('shows Refresh button and calls reload for transient errors (503)', () => {
    const error = new ApiError('Service unavailable', 503, 'Service Unavailable')
    renderWithQueryClient(<TransientFetchErrorBanner error={error} data-test="test-banner" />)

    expect(screen.getByTestId('test-banner')).toBeInTheDocument()
    expect(screen.getByText(/Server temporarily unavailable/)).toBeInTheDocument()
    expect(screen.getByTestId('transient-refresh-button')).toBeInTheDocument()
    expect(screen.queryByTestId('transient-retry-button')).not.toBeInTheDocument()

    fireEvent.click(screen.getByTestId('transient-refresh-button'))
    expect(mockReload).toHaveBeenCalled()
  })

  it('shows optional Retry button when onRetry provided and calls it', () => {
    const error = new ApiError('Service unavailable', 503, 'Service Unavailable')
    renderWithQueryClient(
      <TransientFetchErrorBanner 
        error={error} 
        onRetry={mockOnRetry}
        data-test="test-banner" 
      />
    )

    const retryBtn = screen.getByTestId('transient-retry-button')
    expect(retryBtn).toBeInTheDocument()
    expect(retryBtn).toHaveTextContent('Retry')

    fireEvent.click(retryBtn)
    expect(mockOnRetry).toHaveBeenCalledTimes(1)
  })

  it('falls back to ErrorMessage for non-transient errors (400)', () => {
    const error = new ApiError('Bad request', 400, 'Bad Request')
    renderWithQueryClient(<TransientFetchErrorBanner error={error} data-test="test-banner" />)

    // Non-transient shows ErrorMessage (which uses the message)
    expect(screen.getByText(/Bad request/)).toBeInTheDocument()
    expect(screen.queryByTestId('transient-refresh-button')).not.toBeInTheDocument()
  })

  it('uses provided className and data-test', () => {
    const error = new ApiError('timeout', 408, '')
    const { container } = renderWithQueryClient(
      <TransientFetchErrorBanner 
        error={error} 
        className="custom-class"
        data-test="specific-banner" 
      />
    )
    const banner = container.querySelector('[data-test="specific-banner"]')
    expect(banner).toHaveClass('custom-class')
  })
})
