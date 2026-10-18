// (c) Copyright 2025 by Muczynski
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '@/api/client'
import { AuthorCombobox } from '../AuthorCombobox'

vi.mock('@/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

function renderCombobox(value = '', onChange = vi.fn()) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return {
    onChange,
    ...render(
      <QueryClientProvider client={client}>
        <AuthorCombobox value={value} onChange={onChange} data-test="book-author" />
      </QueryClientProvider>,
    ),
  }
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('AuthorCombobox', () => {
  it('does not load authors until the name has 3 characters', async () => {
    vi.mocked(api.get).mockResolvedValue([])
    renderCombobox()

    expect(screen.getByText('Type at least 3 characters')).toBeInTheDocument()
    expect(api.get).not.toHaveBeenCalled()

    fireEvent.change(screen.getByTestId('book-author-input'), { target: { value: 'Au' } })
    expect(api.get).not.toHaveBeenCalled()

    fireEvent.change(screen.getByTestId('book-author-input'), { target: { value: 'Aug' } })

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/authors/name-matches?name=Aug')
    })
    expect(api.get).not.toHaveBeenCalledWith('/authors/summaries')
    expect(api.get).not.toHaveBeenCalledWith('/authors')
  })

  it('shows the selected author from that one record', async () => {
    vi.mocked(api.get).mockResolvedValue({ id: 5, name: 'Augustine' })

    renderCombobox('5')

    await waitFor(() => {
      expect(screen.getByTestId('book-author-input')).toHaveValue('Augustine')
    })
    expect(api.get).toHaveBeenCalledWith('/authors/5')
    expect(api.get).not.toHaveBeenCalledWith('/authors/summaries')
  })
})
