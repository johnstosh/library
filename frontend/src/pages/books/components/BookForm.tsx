// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useAuthors } from '@/api/authors'
import { useLibraries } from '@/api/libraries'
import { useCreateBook, useUpdateBook } from '@/api/books'
import type { BookDto } from '@/types/dtos'
import { BookStatus } from '@/types/enums'

interface BookFormProps {
  isOpen: boolean
  onClose: () => void
  book?: BookDto | null
}

export function BookForm({ isOpen, onClose, book }: BookFormProps) {
  const isEditing = !!book

  const [formData, setFormData] = useState({
    title: '',
    publicationYear: '',
    publisher: '',
    status: BookStatus.AVAILABLE as string,
    locCallNumber: '',
    authorId: '',
    libraryId: '',
  })
  const [error, setError] = useState('')

  const { data: authors, isLoading: authorsLoading } = useAuthors()
  const { data: libraries, isLoading: librariesLoading } = useLibraries()
  const createBook = useCreateBook()
  const updateBook = useUpdateBook()

  useEffect(() => {
    if (book) {
      setFormData({
        title: book.title,
        publicationYear: book.publicationYear?.toString() || '',
        publisher: book.publisher || '',
        status: book.status,
        locCallNumber: book.locCallNumber || '',
        authorId: book.authorId.toString(),
        libraryId: book.libraryId.toString(),
      })
    } else {
      setFormData({
        title: '',
        publicationYear: '',
        publisher: '',
        status: BookStatus.AVAILABLE,
        locCallNumber: '',
        authorId: '',
        libraryId: '',
      })
    }
    setError('')
  }, [book, isOpen])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!formData.title || !formData.authorId || !formData.libraryId) {
      setError('Title, Author, and Library are required')
      return
    }

    try {
      const bookData = {
        title: formData.title,
        publicationYear: formData.publicationYear ? parseInt(formData.publicationYear) : undefined,
        publisher: formData.publisher || undefined,
        status: formData.status as BookDto['status'],
        locCallNumber: formData.locCallNumber || undefined,
        authorId: parseInt(formData.authorId),
        libraryId: parseInt(formData.libraryId),
      }

      if (isEditing) {
        await updateBook.mutateAsync({ id: book.id, book: bookData })
      } else {
        await createBook.mutateAsync(bookData)
      }

      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    }
  }

  const authorOptions = authors
    ? authors.map((a) => ({
        value: a.id,
        label: `${a.lastName}, ${a.firstName}`,
      }))
    : []

  const libraryOptions = libraries
    ? libraries.map((l) => ({
        value: l.id,
        label: l.name,
      }))
    : []

  const statusOptions = [
    { value: BookStatus.AVAILABLE, label: 'Available' },
    { value: BookStatus.CHECKED_OUT, label: 'Checked Out' },
    { value: BookStatus.LOST, label: 'Lost' },
    { value: BookStatus.DAMAGED, label: 'Damaged' },
  ]

  const isLoading = createBook.isPending || updateBook.isPending

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? 'Edit Book' : 'Add New Book'}
      size="lg"
      footer={
        <div className="flex justify-end gap-3">
          <Button variant="ghost" onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            isLoading={isLoading}
            data-test="book-form-submit"
          >
            {isEditing ? 'Update' : 'Create'}
          </Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <ErrorMessage message={error} />}

        <Input
          label="Title"
          value={formData.title}
          onChange={(e) => setFormData({ ...formData, title: e.target.value })}
          required
          data-test="book-title"
        />

        <div className="grid grid-cols-2 gap-4">
          <Select
            label="Author"
            value={formData.authorId}
            onChange={(e) => setFormData({ ...formData, authorId: e.target.value })}
            options={[{ value: '', label: 'Select Author' }, ...authorOptions]}
            required
            disabled={authorsLoading}
            data-test="book-author"
          />

          <Select
            label="Library"
            value={formData.libraryId}
            onChange={(e) => setFormData({ ...formData, libraryId: e.target.value })}
            options={[{ value: '', label: 'Select Library' }, ...libraryOptions]}
            required
            disabled={librariesLoading}
            data-test="book-library"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Publication Year"
            type="number"
            value={formData.publicationYear}
            onChange={(e) => setFormData({ ...formData, publicationYear: e.target.value })}
            data-test="book-year"
          />

          <Input
            label="Publisher"
            value={formData.publisher}
            onChange={(e) => setFormData({ ...formData, publisher: e.target.value })}
            data-test="book-publisher"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="LOC Call Number"
            value={formData.locCallNumber}
            onChange={(e) => setFormData({ ...formData, locCallNumber: e.target.value })}
            data-test="book-loc"
          />

          <Select
            label="Status"
            value={formData.status}
            onChange={(e) => setFormData({ ...formData, status: e.target.value })}
            options={statusOptions}
            required
            data-test="book-status"
          />
        </div>
      </form>
    </Modal>
  )
}
