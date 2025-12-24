// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { PhotoSection } from '@/components/photos/PhotoSection'
import { useBook } from '@/api/books'
import { formatBookStatus } from '@/utils/formatters'
import { Spinner } from '@/components/progress/Spinner'

interface BookDetailModalProps {
  isOpen: boolean
  onClose: () => void
  bookId: number | null
}

export function BookDetailModal({ isOpen, onClose, bookId }: BookDetailModalProps) {
  const { data: book, isLoading } = useBook(bookId || 0)

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={book?.title || 'Book Details'}
      size="xl"
      footer={
        <div className="flex justify-end">
          <Button variant="ghost" onClick={onClose}>
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
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Author</p>
                <p className="text-gray-900">{book.authorName}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Library</p>
                <p className="text-gray-900">{book.libraryName}</p>
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
              {book.locCallNumber && (
                <div>
                  <p className="text-sm font-medium text-gray-500">LOC Call Number</p>
                  <p className="text-gray-900 font-mono">{book.locCallNumber}</p>
                </div>
              )}
              <div>
                <p className="text-sm font-medium text-gray-500">Status</p>
                <p className="text-gray-900">{formatBookStatus(book.status)}</p>
              </div>
            </div>
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
