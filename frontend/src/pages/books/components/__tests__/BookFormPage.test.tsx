// (c) Copyright 2025 by Muczynski
import { StrictMode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { BookFormPage } from '../BookFormPage'
import type { BookDto } from '@/types/dtos'

const { mocks } = vi.hoisted(() => ({
  mocks: {
    titleAuthorFromPhoto: vi.fn(),
    updateBook: vi.fn(),
  },
}))

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
  useUpdateBook: () => ({ isPending: false, mutateAsync: mocks.updateBook }),
  useSuggestLocNumber: () => idleMutation(),
  useDeleteBook: () => idleMutation(),
  useCloneBook: () => idleMutation(),
  useBookFromImage: () => idleMutation(),
  useBookFromFirstPhoto: () => idleMutation(),
  useTitleAuthorFromPhoto: () => ({ isPending: false, mutateAsync: mocks.titleAuthorFromPhoto }),
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
      <option value="1">Original Author</option>
      <option value="2">Extracted Author</option>
    </select>
  ),
}))

const editedBook: BookDto = {
  id: 1,
  title: 'Edited Book',
  status: 'ACTIVE',
  lastModified: '2026-01-01T00:00:00',
  authorId: 1,
  libraryId: 9,
}

function renderForm(props: { book?: BookDto; onSuccess?: () => void; onCancel?: () => void } = {}) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <StrictMode>
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <BookFormPage
            title={props.book ? 'Edit Book' : 'Add New Book'}
            book={props.book}
            onSuccess={props.onSuccess ?? (() => {})}
            onCancel={props.onCancel ?? (() => {})}
          />
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  )
}

afterEach(() => {
  vi.clearAllMocks()
})

describe('BookFormPage add new book', () => {
  it('defaults the branch to the first branch', () => {
    renderForm()

    expect(screen.getByTestId('book-branch')).toHaveValue('5')
  })

  it('keeps the book branch when editing', () => {
    renderForm({ book: editedBook })

    expect(screen.getByTestId('book-branch')).toHaveValue('9')
  })
})

describe('BookFormPage title and author from photo', () => {
  it('fills the form from the photo but does not persist when Cancel is clicked', async () => {
    mocks.titleAuthorFromPhoto.mockResolvedValue({
      ...editedBook,
      title: 'Extracted Title',
      authorId: 2,
    })
    const onCancel = vi.fn()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)

    renderForm({ book: editedBook, onCancel })

    fireEvent.click(screen.getByTestId('book-operation-title-author-from-photo'))

    await waitFor(() => {
      expect(screen.getByTestId('book-title')).toHaveValue('Extracted Title')
    })
    expect(screen.getByTestId('book-author')).toHaveValue('2')

    fireEvent.click(screen.getByTestId('book-form-cancel'))

    expect(confirmSpy).toHaveBeenCalled()
    expect(onCancel).toHaveBeenCalled()
    expect(mocks.updateBook).not.toHaveBeenCalled()

    confirmSpy.mockRestore()
  })

  it('persists extracted title and author only when the form is submitted', async () => {
    mocks.titleAuthorFromPhoto.mockResolvedValue({
      ...editedBook,
      title: 'Extracted Title',
      authorId: 2,
    })
    mocks.updateBook.mockResolvedValue({
      ...editedBook,
      title: 'Extracted Title',
      authorId: 2,
    })
    const onSuccess = vi.fn()

    renderForm({ book: editedBook, onSuccess })

    fireEvent.click(screen.getByTestId('book-operation-title-author-from-photo'))

    await waitFor(() => {
      expect(screen.getByTestId('book-title')).toHaveValue('Extracted Title')
    })

    fireEvent.click(screen.getByTestId('book-form-submit'))

    await waitFor(() => {
      expect(mocks.updateBook).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 1,
          book: expect.objectContaining({
            title: 'Extracted Title',
            authorId: 2,
          }),
        }),
      )
    })
    expect(onSuccess).toHaveBeenCalled()
  })
})
