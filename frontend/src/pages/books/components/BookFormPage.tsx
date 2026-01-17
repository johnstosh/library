// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { useAuthors } from '@/api/authors'
import { useLibraries } from '@/api/libraries'
import { useCreateBook, useUpdateBook, useSuggestLocNumber } from '@/api/books'
import { useLookupSingleBook } from '@/api/loc-lookup'
import type { BookDto } from '@/types/dtos'
import { BookStatus } from '@/types/enums'
import { PiSparkle } from 'react-icons/pi'

interface BookFormPageProps {
  title: string
  book?: BookDto
  onSuccess: () => void
  onCancel: () => void
}

export function BookFormPage({ title, book, onSuccess, onCancel }: BookFormPageProps) {
  const isEditing = !!book

  const [formData, setFormData] = useState({
    title: '',
    publicationYear: '',
    publisher: '',
    plotSummary: '',
    relatedWorks: '',
    detailedDescription: '',
    grokipediaUrl: '',
    freeTextUrl: '',
    status: BookStatus.ACTIVE as string,
    statusReason: '',
    locNumber: '',
    authorId: '',
    libraryId: '',
  })
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)

  const { data: authors, isLoading: authorsLoading } = useAuthors()
  const { data: libraries, isLoading: librariesLoading } = useLibraries()
  const createBook = useCreateBook()
  const updateBook = useUpdateBook()
  const lookupLoc = useLookupSingleBook()
  const suggestLoc = useSuggestLocNumber()

  useEffect(() => {
    if (book) {
      setFormData({
        title: book.title,
        publicationYear: book.publicationYear?.toString() || '',
        publisher: book.publisher || '',
        plotSummary: book.plotSummary || '',
        relatedWorks: book.relatedWorks || '',
        detailedDescription: book.detailedDescription || '',
        grokipediaUrl: book.grokipediaUrl || '',
        freeTextUrl: book.freeTextUrl || '',
        status: book.status,
        statusReason: book.statusReason || '',
        locNumber: book.locNumber || '',
        authorId: book.authorId?.toString() || '',
        libraryId: book.libraryId?.toString() || '',
      })
    } else {
      setFormData({
        title: '',
        publicationYear: '',
        publisher: '',
        plotSummary: '',
        relatedWorks: '',
        detailedDescription: '',
        grokipediaUrl: '',
        freeTextUrl: '',
        status: BookStatus.ACTIVE,
        statusReason: '',
        locNumber: '',
        authorId: '',
        libraryId: '',
      })
    }
  }, [book])

  // Warn user about unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges) {
        e.preventDefault()
        e.returnValue = ''
      }
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges])

  const handleFieldChange = (field: string, value: string) => {
    setFormData({ ...formData, [field]: value })
    setHasUnsavedChanges(true)
  }

  const handleLookupLoc = async () => {
    if (!book?.id) return

    setError('')
    setSuccessMessage('')

    try {
      const result = await lookupLoc.mutateAsync(book.id)
      if (result.success && result.locNumber) {
        setFormData({ ...formData, locNumber: result.locNumber })
        setSuccessMessage(`LOC call number found: ${result.locNumber}`)
        setHasUnsavedChanges(true)
      } else {
        setError(result.errorMessage || 'LOC call number not found')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to lookup LOC')
    }
  }

  const handleSuggestLoc = async () => {
    setError('')
    setSuccessMessage('')

    if (!formData.title) {
      setError('Title is required to get LOC suggestion')
      return
    }

    try {
      // Get author name for better suggestions
      const authorName = formData.authorId
        ? authors?.find((a) => a.id === parseInt(formData.authorId))?.name
        : undefined

      const result = await suggestLoc.mutateAsync({
        title: formData.title,
        author: authorName,
      })

      if (result.suggestion) {
        setFormData({ ...formData, locNumber: result.suggestion })
        setSuccessMessage(`AI suggested LOC: ${result.suggestion}`)
        setHasUnsavedChanges(true)
      } else {
        setError('No LOC suggestion available')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to get LOC suggestion')
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccessMessage('')

    if (!formData.title || !formData.authorId || !formData.libraryId) {
      setError('Title, Author, and Branch are required')
      return
    }

    try {
      const bookData = {
        title: formData.title,
        publicationYear: formData.publicationYear ? parseInt(formData.publicationYear) : undefined,
        publisher: formData.publisher || undefined,
        plotSummary: formData.plotSummary || undefined,
        relatedWorks: formData.relatedWorks || undefined,
        detailedDescription: formData.detailedDescription || undefined,
        grokipediaUrl: formData.grokipediaUrl || undefined,
        freeTextUrl: formData.freeTextUrl || undefined,
        status: formData.status as BookDto['status'],
        statusReason: formData.statusReason || undefined,
        locNumber: formData.locNumber || undefined,
        authorId: parseInt(formData.authorId),
        libraryId: parseInt(formData.libraryId),
      }

      if (isEditing) {
        await updateBook.mutateAsync({ id: book.id, book: bookData })
      } else {
        await createBook.mutateAsync(bookData)
      }

      setHasUnsavedChanges(false)
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    }
  }

  const handleCancel = () => {
    if (hasUnsavedChanges) {
      if (window.confirm('You have unsaved changes. Are you sure you want to leave?')) {
        onCancel()
      }
    } else {
      onCancel()
    }
  }

  const authorOptions = authors
    ? authors.map((a) => ({
        value: a.id,
        label: a.name,
      }))
    : []

  const libraryOptions = libraries
    ? libraries.map((l) => ({
        value: l.id,
        label: l.branchName,
      }))
    : []

  const statusOptions = [
    { value: BookStatus.ACTIVE, label: 'Active' },
    { value: BookStatus.LOST, label: 'Lost' },
    { value: BookStatus.WITHDRAWN, label: 'Withdrawn' },
    { value: BookStatus.ON_ORDER, label: 'On Order' },
  ]

  const isLoading = createBook.isPending || updateBook.isPending
  const isLookingUp = lookupLoc.isPending
  const isSuggesting = suggestLoc.isPending

  return (
    <div className="bg-white rounded-lg shadow">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="px-6 py-6 space-y-4">
        {error && <ErrorMessage message={error} />}
        {successMessage && <SuccessMessage message={successMessage} />}

        <Input
          label="Title"
          value={formData.title}
          onChange={(e) => handleFieldChange('title', e.target.value)}
          required
          data-test="book-title"
        />

        <div className="grid grid-cols-2 gap-4">
          <Select
            label="Author"
            value={formData.authorId}
            onChange={(e) => handleFieldChange('authorId', e.target.value)}
            options={[{ value: '', label: 'Select Author' }, ...authorOptions]}
            required
            disabled={authorsLoading}
            data-test="book-author"
          />

          <Select
            label="Branch"
            value={formData.libraryId}
            onChange={(e) => handleFieldChange('libraryId', e.target.value)}
            options={[{ value: '', label: 'Select Branch' }, ...libraryOptions]}
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
            onChange={(e) => handleFieldChange('publicationYear', e.target.value)}
            data-test="book-year"
          />

          <Input
            label="Publisher"
            value={formData.publisher}
            onChange={(e) => handleFieldChange('publisher', e.target.value)}
            data-test="book-publisher"
          />
        </div>

        <Textarea
          label="Plot Summary"
          value={formData.plotSummary}
          onChange={(e) => handleFieldChange('plotSummary', e.target.value)}
          rows={3}
          data-test="book-plot-summary"
        />

        <Textarea
          label="Related Works"
          value={formData.relatedWorks}
          onChange={(e) => handleFieldChange('relatedWorks', e.target.value)}
          rows={2}
          data-test="book-related-works"
        />

        <Textarea
          label="Detailed Description"
          value={formData.detailedDescription}
          onChange={(e) => handleFieldChange('detailedDescription', e.target.value)}
          rows={4}
          data-test="book-detailed-description"
        />

        <Input
          label="Grokipedia URL"
          value={formData.grokipediaUrl}
          onChange={(e) => handleFieldChange('grokipediaUrl', e.target.value)}
          placeholder="https://grokipedia.example.com/book/123"
          data-test="book-grokipedia-url"
        />

        <Input
          label="Free Text URL"
          value={formData.freeTextUrl}
          onChange={(e) => handleFieldChange('freeTextUrl', e.target.value)}
          placeholder="https://www.gutenberg.org/ebooks/123"
          data-test="book-free-text-url"
        />

        <div className="space-y-4">
          <div className="flex items-end gap-2">
            <div className="flex-1">
              <Input
                label="LOC Call Number"
                value={formData.locNumber}
                onChange={(e) => handleFieldChange('locNumber', e.target.value)}
                data-test="book-loc"
              />
            </div>
            <Button
              type="button"
              variant="secondary"
              size="md"
              onClick={handleSuggestLoc}
              isLoading={isSuggesting}
              disabled={isLoading || isSuggesting || isLookingUp}
              leftIcon={<PiSparkle />}
              data-test="suggest-loc-button"
              className="mb-0"
            >
              AI Suggest
            </Button>
            {isEditing && (
              <Button
                type="button"
                variant="outline"
                size="md"
                onClick={handleLookupLoc}
                isLoading={isLookingUp}
                disabled={isLoading || isLookingUp || isSuggesting}
                leftIcon={<span>🗃️</span>}
                data-test="lookup-loc-button"
                className="mb-0"
              >
                Lookup
              </Button>
            )}
          </div>

          <Select
            label="Status"
            value={formData.status}
            onChange={(e) => handleFieldChange('status', e.target.value)}
            options={statusOptions}
            required
            data-test="book-status"
          />

          <Textarea
            label="Status Reason"
            value={formData.statusReason}
            onChange={(e) => handleFieldChange('statusReason', e.target.value)}
            rows={2}
            placeholder="Optional reason for status (e.g., why book is withdrawn)"
            data-test="book-status-reason"
          />
        </div>
      </form>

      {/* Footer */}
      <div className="px-6 py-4 bg-gray-50 border-t rounded-b-lg">
        <div className="flex justify-end gap-3">
          <Button
            variant="ghost"
            onClick={handleCancel}
            disabled={isLoading}
            data-test="book-form-cancel"
          >
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
      </div>
    </div>
  )
}
