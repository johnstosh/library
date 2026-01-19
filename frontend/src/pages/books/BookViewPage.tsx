// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { PhotoSection } from '@/components/photos/PhotoSection'
import { useBook, useCloneBook, useDeleteBook } from '@/api/books'
import { formatBookStatus, parseISODateSafe } from '@/utils/formatters'
import { Spinner } from '@/components/progress/Spinner'
import { PiCopy, PiPencil, PiTrash, PiArrowLeft } from 'react-icons/pi'
import { useIsLibrarian } from '@/stores/authStore'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'

export function BookViewPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const bookId = id ? parseInt(id, 10) : 0
  const { data: book, isLoading } = useBook(bookId)
  const cloneBook = useCloneBook()
  const deleteBook = useDeleteBook()
  const isLibrarian = useIsLibrarian()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [error, setError] = useState('')

  const handleClone = async () => {
    try {
      await cloneBook.mutateAsync(bookId)
      navigate('/books')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to clone book')
    }
  }

  const handleDelete = async () => {
    try {
      await deleteBook.mutateAsync(bookId)
      navigate('/books')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete book')
      setShowDeleteConfirm(false)
    }
  }

  const handleEdit = () => {
    navigate(`/books/${bookId}/edit`)
  }

  const handleBack = () => {
    navigate('/books')
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!book) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-lg shadow p-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Book Not Found</h1>
          <p className="text-gray-600 mb-4">The book you're looking for doesn't exist.</p>
          <button
            onClick={handleBack}
            className="text-blue-600 hover:text-blue-800"
          >
            Return to Books
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Button
          variant="ghost"
          onClick={handleBack}
          leftIcon={<PiArrowLeft />}
          data-test="back-to-books"
        >
          Back to Books
        </Button>
      </div>

      <div className="bg-white rounded-lg shadow">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-start justify-between">
            <h1 className="text-2xl font-bold text-gray-900" data-test="book-title">
              {book.title}
            </h1>
            {isLibrarian && (
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={handleEdit}
                  leftIcon={<PiPencil />}
                  data-test="book-view-edit"
                >
                  Edit
                </Button>
                <Button
                  variant="outline"
                  onClick={handleClone}
                  isLoading={cloneBook.isPending}
                  leftIcon={<PiCopy />}
                  data-test="book-view-clone"
                >
                  Clone
                </Button>
                <Button
                  variant="danger"
                  onClick={() => setShowDeleteConfirm(true)}
                  leftIcon={<PiTrash />}
                  data-test="book-view-delete"
                >
                  Delete
                </Button>
              </div>
            )}
          </div>
        </div>

        {/* Body */}
        <div className="px-6 py-6 space-y-6">
          {error && <ErrorMessage message={error} />}

          {/* Delete Confirmation */}
          {showDeleteConfirm && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <p className="text-red-900 font-semibold mb-3">
                Are you sure you want to delete this book?
              </p>
              <p className="text-red-700 mb-4">This action cannot be undone.</p>
              <div className="flex gap-3">
                <Button
                  variant="danger"
                  onClick={handleDelete}
                  isLoading={deleteBook.isPending}
                  data-test="confirm-delete-book"
                >
                  Yes, Delete
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => setShowDeleteConfirm(false)}
                  data-test="cancel-delete-book"
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}

          {/* Book Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Author</p>
                <p className="text-gray-900">{book.author}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Branch</p>
                <p className="text-gray-900">{book.library}</p>
              </div>
              {book.publicationYear && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Publication Year</p>
                  <p className="text-gray-900">{book.publicationYear}</p>
                </div>
              )}
              {book.publisher && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Publisher</p>
                  <p className="text-gray-900">{book.publisher}</p>
                </div>
              )}
              {book.locNumber && (
                <div>
                  <p className="text-sm font-medium text-gray-500">LOC Call Number</p>
                  <p className="text-gray-900 font-mono">{book.locNumber}</p>
                </div>
              )}
              {book.grokipediaUrl && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Grokipedia</p>
                  <a
                    href={book.grokipediaUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-800 underline"
                    data-test="book-grokipedia-link"
                  >
                    View on Grokipedia
                  </a>
                </div>
              )}
              {book.freeTextUrl && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Free Online Text</p>
                  <a
                    href={book.freeTextUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-800 underline"
                    data-test="book-free-text-link"
                  >
                    Read Online
                  </a>
                </div>
              )}
              <div>
                <p className="text-sm font-medium text-gray-500">Status</p>
                <p className="text-gray-900">{formatBookStatus(book.status)}</p>
              </div>
              {book.dateAddedToLibrary && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Date Added</p>
                  <p className="text-gray-900">
                    {parseISODateSafe(book.dateAddedToLibrary).toLocaleDateString()}
                  </p>
                </div>
              )}
            </div>

            {book.statusReason && (
              <div className="mt-4 pt-4 border-t border-gray-200">
                <p className="text-sm font-medium text-gray-500">Status Reason</p>
                <p className="text-gray-900">{book.statusReason}</p>
              </div>
            )}

            {book.plotSummary && (
              <div className="mt-4 pt-4 border-t border-gray-200">
                <p className="text-sm font-medium text-gray-500">Plot Summary</p>
                <p className="text-gray-900 whitespace-pre-wrap">{book.plotSummary}</p>
              </div>
            )}

            {book.relatedWorks && (
              <div className="mt-4 pt-4 border-t border-gray-200">
                <p className="text-sm font-medium text-gray-500">Related Works</p>
                <p className="text-gray-900 whitespace-pre-wrap">{book.relatedWorks}</p>
              </div>
            )}

            {book.detailedDescription && (
              <div className="mt-4 pt-4 border-t border-gray-200">
                <p className="text-sm font-medium text-gray-500">Detailed Description</p>
                <p className="text-gray-900 whitespace-pre-wrap">{book.detailedDescription}</p>
              </div>
            )}
          </div>

          {/* Photos */}
          <PhotoSection
            entityType="book"
            entityId={book.id}
            entityName={book.title}
          />
        </div>
      </div>
    </div>
  )
}
