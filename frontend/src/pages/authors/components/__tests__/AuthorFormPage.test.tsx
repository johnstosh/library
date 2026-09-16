// (c) Copyright 2025 by Muczynski
import { StrictMode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthorFormPage } from '../AuthorFormPage'
import type { AuthorDto } from '@/types/dtos'

const { mocks } = vi.hoisted(() => ({
  mocks: {
    lookupGrokipedia: vi.fn(),
    librarian: { current: true },
  },
}))

const idleMutation = () => ({ isPending: false, mutateAsync: vi.fn() })

vi.mock('@/api/authors', () => ({
  useAuthorBooks: () => ({ data: [], isLoading: false }),
  useCreateAuthor: () => idleMutation(),
  useUpdateAuthor: () => idleMutation(),
}))

vi.mock('@/api/grokipedia-lookup', () => ({
  useLookupSingleAuthorGrokipedia: () => ({
    isPending: false,
    mutateAsync: mocks.lookupGrokipedia,
  }),
}))

vi.mock('@/stores/authStore', () => ({
  useIsLibrarian: () => mocks.librarian.current,
}))

const author: AuthorDto = {
  id: 1,
  name: 'Thomas Aquinas',
  lastModified: '2026-01-01T00:00:00',
}

function renderForm(props: { author?: AuthorDto } = {}) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <StrictMode>
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <AuthorFormPage
            title={props.author ? 'Edit Author' : 'Add New Author'}
            author={props.author}
            onSuccess={() => undefined}
            onCancel={() => undefined}
          />
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  )
}

afterEach(() => {
  vi.clearAllMocks()
  mocks.librarian.current = true
})

describe('AuthorFormPage Grokipedia lookup', () => {
  it('shows quick and slow lookup buttons when editing as a librarian', () => {
    renderForm({ author })

    expect(screen.getByTestId('author-field-lookup-grokipedia-quick')).toBeInTheDocument()
    expect(screen.getByTestId('author-field-lookup-grokipedia-slow')).toBeInTheDocument()
  })

  it('hides lookup buttons when creating a new author', () => {
    renderForm()

    expect(screen.queryByTestId('author-field-lookup-grokipedia-quick')).not.toBeInTheDocument()
    expect(screen.queryByTestId('author-field-lookup-grokipedia-slow')).not.toBeInTheDocument()
  })

  it('fills the Grokipedia URL after a successful lookup', async () => {
    mocks.lookupGrokipedia.mockResolvedValue({
      authorId: 1,
      name: 'Thomas Aquinas',
      success: true,
      grokipediaUrl: 'https://grokipedia.com/page/Thomas_Aquinas',
    })

    renderForm({ author })

    fireEvent.click(screen.getByTestId('author-field-lookup-grokipedia-quick'))

    await waitFor(() => {
      expect(screen.getByTestId('author-grokipedia-url')).toHaveValue(
        'https://grokipedia.com/page/Thomas_Aquinas',
      )
    })
    expect(mocks.lookupGrokipedia).toHaveBeenCalledWith({ authorId: 1, slow: false })
  })
})
