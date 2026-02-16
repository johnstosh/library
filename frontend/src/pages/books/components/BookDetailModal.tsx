// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { PhotoSection } from '@/components/photos/PhotoSection'
import { useBook, useCloneBook } from '@/api/books'
import { formatBookStatus, formatDateTime } from '@/utils/formatters'
import { Spinner } from '@/components/progress/Spinner'
import { PiCopy, PiPencil } from 'react-icons/pi'
import { useIsLibrarian } from '@/stores/authStore'

interface BookDetailModalProps {
  isOpen: boolean
  onClose: () => void
  bookId: number | null
  onEdit?: (bookId: number) => void
}

export function BookDetailModal({ isOpen, onClose, bookId, onEdit }: BookDetailModalProps) {
  const { data: book, isLoading } = useBook(bookId || 0)
  const cloneBook = useCloneBook()
  const isLibrarian = useIsLibrarian()

  const handleClone = async () => {
    if (!bookId) return
    try {
      await cloneBook.mutateAsync(bookId)
      onClose()
    } catch (error) {
      console.error('Failed to clone book:', error)
    }
  }

  const handleEdit = () => {
    if (bookId && onEdit) {
      onEdit(bookId)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={book?.title || 'Book Details'}
      size="xl"
      footer={
        <div className="flex justify-between">
          <div className="flex gap-3">
            {isLibrarian && bookId && (
              <>
                <Button
                  variant="outline"
                  onClick={handleEdit}
                  leftIcon={<PiPencil />}
                  data-test="book-detail-edit"
                >
                  Edit
                </Button>
                <Button
                  variant="outline"
                  onClick={handleClone}
                  isLoading={cloneBook.isPending}
                  leftIcon={<PiCopy />}
                  data-test="book-detail-clone"
                >
                  Clone
                </Button>
              </>
            )}
          </div>
          <Button variant="ghost" onClick={onClose} data-test="book-detail-close">
            Close
          </Button>
        </div>
      }
    >
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : book ? (
        <div className="space-y-6">
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
              <div>
                <p className="text-sm font-medium text-gray-500">Status</p>
                <p className="text-gray-900">{formatBookStatus(book.status)}</p>
              </div>
              {book.dateAddedToLibrary && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Date Added</p>
                  <p className="text-gray-900">
                    {formatDateTime(book.dateAddedToLibrary)}
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
      ) : (
        <p className="text-gray-500">Book not found</p>
      )}
    </Modal>
  )
}
