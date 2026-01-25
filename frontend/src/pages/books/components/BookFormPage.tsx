// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { GrokipediaLookupResultsModal } from '@/components/GrokipediaLookupResultsModal'
import { FreeTextLookupResultsModal } from '@/components/FreeTextLookupResultsModal'
import { PhotoSection } from '@/components/photos/PhotoSection'
import { useAuthors } from '@/api/authors'
import { useBranches } from '@/api/branches'
import { useCreateBook, useUpdateBook, useSuggestLocNumber, useDeleteBook, useCloneBook, useBookFromImage } from '@/api/books'
import { useLookupSingleBook } from '@/api/loc-lookup'
import { useLookupSingleBookGrokipedia, type GrokipediaLookupResultDto } from '@/api/grokipedia-lookup'
import { useLookupSingleFreeText, type FreeTextLookupResultDto } from '@/api/free-text-lookup'
import { generateLabelsPdf } from '@/api/labels'
import { useAuthStore } from '@/stores/authStore'
import type { BookDto } from '@/types/dtos'
import { BookStatus } from '@/types/enums'
import { PiSparkle, PiCopy, PiFilePdf, PiBookOpen, PiCamera, PiTrash } from 'react-icons/pi'

interface BookFormPageProps {
  title: string
  book?: BookDto
  onSuccess: () => void
  onCancel: () => void
}

export function BookFormPage({ title, book, onSuccess, onCancel }: BookFormPageProps) {
  const navigate = useNavigate()
  const isEditing = !!book
  const { user } = useAuthStore()
  const isLibrarian = user?.authority === 'LIBRARIAN'

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

  // Operations state
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showGrokipediaResults, setShowGrokipediaResults] = useState(false)
  const [grokipediaResults, setGrokipediaResults] = useState<GrokipediaLookupResultDto[]>([])
  const [showFreeTextResults, setShowFreeTextResults] = useState(false)
  const [freeTextResults, setFreeTextResults] = useState<FreeTextLookupResultDto[]>([])
  const [isGeneratingLabel, setIsGeneratingLabel] = useState(false)

  const { data: authors, isLoading: authorsLoading } = useAuthors()
  const { data: libraries, isLoading: librariesLoading } = useBranches()
  const createBook = useCreateBook()
  const updateBook = useUpdateBook()
  const lookupLoc = useLookupSingleBook()
  const suggestLoc = useSuggestLocNumber()

  // Operations hooks
  const deleteBook = useDeleteBook()
  const cloneBook = useCloneBook()
  const bookFromImage = useBookFromImage()
  const lookupGrokipedia = useLookupSingleBookGrokipedia()
  const lookupFreeText = useLookupSingleFreeText()

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

  // Operations handlers
  const handleDelete = async () => {
    if (!book?.id) return

    try {
      await deleteBook.mutateAsync(book.id)
      setShowDeleteConfirm(false)
      navigate('/books')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete book')
      setShowDeleteConfirm(false)
    }
  }

  const handleClone = async () => {
    if (!book?.id) return

    setError('')
    setSuccessMessage('')

    try {
      const cloned = await cloneBook.mutateAsync(book.id)
      setSuccessMessage('Book cloned successfully')
      // Navigate to the cloned book's edit page
      navigate(`/books/${cloned.id}/edit`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to clone book')
    }
  }

  const handleGenerateLabel = async () => {
    if (!book?.id) return

    setIsGeneratingLabel(true)
    try {
      const blob = await generateLabelsPdf([book.id])
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `book-label-${book.id}.pdf`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      setSuccessMessage('Label PDF generated')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate label PDF')
    } finally {
      setIsGeneratingLabel(false)
    }
  }

  const handleGrokipediaLookup = async () => {
    if (!book?.id) return

    setError('')
    setSuccessMessage('')

    try {
      const result = await lookupGrokipedia.mutateAsync(book.id)
      setGrokipediaResults([result])
      setShowGrokipediaResults(true)
      if (result.success && result.grokipediaUrl) {
        setFormData({ ...formData, grokipediaUrl: result.grokipediaUrl })
        setHasUnsavedChanges(true)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to lookup Grokipedia URL')
    }
  }

  const handleFreeTextLookup = async () => {
    if (!book?.id) return

    setError('')
    setSuccessMessage('')

    try {
      const result = await lookupFreeText.mutateAsync(book.id)
      setFreeTextResults([result])
      setShowFreeTextResults(true)
      if (result.success && result.freeTextUrl) {
        setFormData({ ...formData, freeTextUrl: result.freeTextUrl })
        setHasUnsavedChanges(true)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to lookup free text URL')
    }
  }

  const handleBookFromImage = async () => {
    if (!book?.id) return

    setError('')
    setSuccessMessage('')

    try {
      const updated = await bookFromImage.mutateAsync(book.id)
      // Update form with the extracted data
      setFormData({
        title: updated.title || formData.title,
        publicationYear: updated.publicationYear?.toString() || formData.publicationYear,
        publisher: updated.publisher || formData.publisher,
        plotSummary: updated.plotSummary || formData.plotSummary,
        relatedWorks: updated.relatedWorks || formData.relatedWorks,
        detailedDescription: updated.detailedDescription || formData.detailedDescription,
        grokipediaUrl: updated.grokipediaUrl || formData.grokipediaUrl,
        freeTextUrl: updated.freeTextUrl || formData.freeTextUrl,
        status: updated.status || formData.status,
        statusReason: updated.statusReason || formData.statusReason,
        locNumber: updated.locNumber || formData.locNumber,
        authorId: updated.authorId?.toString() || formData.authorId,
        libraryId: updated.libraryId?.toString() || formData.libraryId,
      })
      setHasUnsavedChanges(true)
      setSuccessMessage('Book metadata extracted from image')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to extract book from image')
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

  const isOperationPending = cloneBook.isPending || deleteBook.isPending || bookFromImage.isPending ||
    lookupGrokipedia.isPending || lookupFreeText.isPending || isGeneratingLabel

  return (
    <div className="bg-white rounded-lg shadow">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <h1 className="text-2xl font-bold text-gray-900">{title}</h1>

        {/* Operations Toolbar - visible for librarians when editing */}
        {isEditing && isLibrarian && (
          <div className="mt-3 flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleClone}
              isLoading={cloneBook.isPending}
              disabled={isOperationPending || isLoading}
              leftIcon={<PiCopy />}
              data-test="book-operation-clone"
            >
              Clone
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleGenerateLabel}
              isLoading={isGeneratingLabel}
              disabled={isOperationPending || isLoading}
              leftIcon={<PiFilePdf />}
              data-test="book-operation-generate-label"
            >
              Generate Label
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleGrokipediaLookup}
              isLoading={lookupGrokipedia.isPending}
              disabled={isOperationPending || isLoading}
              leftIcon={<span>🌐</span>}
              data-test="book-operation-grokipedia"
            >
              Find Grokipedia URL
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleFreeTextLookup}
              isLoading={lookupFreeText.isPending}
              disabled={isOperationPending || isLoading}
              leftIcon={<PiBookOpen />}
              data-test="book-operation-free-text"
            >
              Find Free Text URL
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleBookFromImage}
              isLoading={bookFromImage.isPending}
              disabled={isOperationPending || isLoading}
              leftIcon={<PiCamera />}
              data-test="book-operation-book-from-image"
            >
              Book from Image
            </Button>
            <Button
              type="button"
              variant="danger"
              size="sm"
              onClick={() => setShowDeleteConfirm(true)}
              disabled={isOperationPending || isLoading}
              leftIcon={<PiTrash />}
              data-test="book-operation-delete"
            >
              Delete
            </Button>
          </div>
        )}
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

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="flex items-end gap-2">
            <div className="flex-1">
              <Select
                label="Author"
                value={formData.authorId}
                onChange={(e) => handleFieldChange('authorId', e.target.value)}
                options={[{ value: '', label: 'Select Author' }, ...authorOptions]}
                required
                isLoading={authorsLoading}
                data-test="book-author"
              />
            </div>
            {formData.authorId && (
              <Link
                to={`/authors/${formData.authorId}`}
                className="mb-0.5 px-3 py-2 text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded inline-flex items-center gap-1"
                data-test="book-see-author"
                title="View Author"
              >
                <span>👤</span>
              </Link>
            )}
          </div>

          <Select
            label="Branch"
            value={formData.libraryId}
            onChange={(e) => handleFieldChange('libraryId', e.target.value)}
            options={[{ value: '', label: 'Select Branch' }, ...libraryOptions]}
            required
            isLoading={librariesLoading}
            data-test="book-library"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
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

      {/* Operation Modals */}
      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete Book"
        message="Are you sure you want to delete this book? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteBook.isPending}
      />

      <GrokipediaLookupResultsModal
        isOpen={showGrokipediaResults}
        onClose={() => setShowGrokipediaResults(false)}
        results={grokipediaResults}
        entityType="book"
      />

      <FreeTextLookupResultsModal
        isOpen={showFreeTextResults}
        onClose={() => setShowFreeTextResults(false)}
        results={freeTextResults}
      />

      {/* Photos Section - only show when editing */}
      {isEditing && book && (
        <div className="mt-8">
          <PhotoSection
            entityType="book"
            entityId={book.id}
            entityName={book.title}
          />
        </div>
      )}
    </div>
  )
}
