// (c) Copyright 2025 by Muczynski
import { StrictMode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { BookFormPage } from '../BookFormPage'
import type { BookDto } from '@/types/dtos'

const idleMutation = () => ({ isPending: false, mutateAsync: vi.fn() })

vi.mock('@/api/authors', () => ({
  useAuthors: () => ({ data: [], isLoading: false }),
  useCreateAuthor: () => idleMutation(),
}))

vi.mock('@/api/branches', () => ({
  useBranches: () => ({
    data: [
      { id: 5, branchName: 'Main Branch', librarySystemName: 'Sys' },
      { id: 9, branchName: 'East Branch', librarySystemName: 'Sys' },
    ],
    isLoading: false,
  }),
}))

vi.mock('@/api/books', () => ({
  useCreateBook: () => idleMutation(),
  useUpdateBook: () => idleMutation(),
  useSuggestLocNumber: () => idleMutation(),
  useDeleteBook: () => idleMutation(),
  useCloneBook: () => idleMutation(),
  useBookFromImage: () => idleMutation(),
  useBookFromFirstPhoto: () => idleMutation(),
  useTitleAuthorFromPhoto: () => idleMutation(),
  useBookFromTitleAuthor: () => idleMutation(),
  useLookupGenres: () => idleMutation(),
}))

vi.mock('@/api/loc-lookup', () => ({
  useLookupSingleBook: () => idleMutation(),
}))

vi.mock('@/api/grokipedia-lookup', () => ({
  useLookupSingleBookGrokipedia: () => idleMutation(),
}))

vi.mock('@/api/free-text-lookup', () => ({
  useLookupSingleFreeText: () => idleMutation(),
}))

vi.mock('@/api/ydl-lookup', () => ({
  useLookupSingleYdl: () => idleMutation(),
}))

vi.mock('@/api/emu-lookup', () => ({
  useLookupSingleEmu: () => idleMutation(),
}))

vi.mock('@/api/labels', () => ({
  generateLabelsPdf: vi.fn(),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: 1, username: 'librarian', authority: 'LIBRARIAN' },
  }),
}))

vi.mock('@/components/photos/PhotoSection', () => ({
  PhotoSection: () => null,
}))

vi.mock('../AuthorCombobox', () => ({
  AuthorCombobox: ({ value, onChange }: { value: string; onChange: (value: string) => void }) => (
    <select data-test="book-author" value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">Select Author</option>
    </select>
  ),
}))

function renderNewBookForm() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <StrictMode>
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <BookFormPage title="Add New Book" onSuccess={() => {}} onCancel={() => {}} />
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  )
}

describe('BookFormPage add new book', () => {
  it('defaults the branch to the first branch', () => {
    renderNewBookForm()

    expect(screen.getByTestId('book-branch')).toHaveValue('5')
  })

  it('keeps the book branch when editing', () => {
    const book: BookDto = {
      id: 1,
      title: 'Edited Book',
      status: 'ACTIVE',
      lastModified: '2026-01-01T00:00:00',
      authorId: 1,
      libraryId: 9,
    }
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    render(
      <StrictMode>
        <QueryClientProvider client={client}>
          <MemoryRouter>
            <BookFormPage title="Edit Book" book={book} onSuccess={() => {}} onCancel={() => {}} />
          </MemoryRouter>
        </QueryClientProvider>
      </StrictMode>,
    )

    expect(screen.getByTestId('book-branch')).toHaveValue('9')
  })
})
