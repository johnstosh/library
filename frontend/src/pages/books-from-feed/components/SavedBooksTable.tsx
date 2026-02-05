// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Spinner } from '@/components/progress/Spinner'
import { useProcessSingleBook, type SavedBookDto } from '@/api/books-from-feed'
import { useDeleteBook } from '@/api/books'
import { PiEye, PiMagicWand, PiTrash } from 'react-icons/pi'
import { useNavigate } from 'react-router-dom'

interface SavedBooksTableProps {
  books: SavedBookDto[]
  isLoading: boolean
  onRefresh: () => void
}

export function SavedBooksTable({ books, isLoading, onRefresh }: SavedBooksTableProps) {
  const navigate = useNavigate()
  const [processingBookId, setProcessingBookId] = useState<number | null>(null)
  const [deletingBookId, setDeletingBookId] = useState<number | null>(null)

  const processSingle = useProcessSingleBook()
  const deleteBook = useDeleteBook()

  const handleProcessSingle = async (bookId: number) => {
    setProcessingBookId(bookId)
    try {
      await processSingle.mutateAsync(bookId)
      await onRefresh()
    } catch (error) {
      console.error('Failed to process book:', error)
    } finally {
      setProcessingBookId(null)
    }
  }

  const handleDelete = async () => {
    if (!deletingBookId) return

    try {
      await deleteBook.mutateAsync(deletingBookId)
      await onRefresh()
      setDeletingBookId(null)
    } catch (error) {
      console.error('Failed to delete book:', error)
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    )
  }

  if (books.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p className="text-lg font-medium mb-2">No saved books yet</p>
        <p className="text-sm">
          Click "Select Photos" above to import book photos from Google Photos
        </p>
      </div>
    )
  }

  return (
    <>
      <div className="overflow-x-auto">
        <table className="min-w-full table-fixed divide-y divide-gray-200" data-test="saved-books-table">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '35%' }}>
                Title
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '15%' }}>
                Photos
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '25%' }}>
                Status
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '25%' }}>
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {books.map((book) => (
              <tr key={book.id} className="hover:bg-gray-50" data-entity-id={book.id}>
                <td className="px-6 py-4 overflow-hidden truncate">
                  <div>
                    <div className="text-sm font-medium text-gray-900">{book.title}</div>
                    {book.author && <div className="text-sm text-gray-500">{book.author}</div>}
                  </div>
                </td>
                <td className="px-6 py-4 overflow-hidden truncate">
                  <div className="text-sm text-gray-900">{book.photoCount}</div>
                </td>
                <td className="px-6 py-4 overflow-hidden truncate">
                  {book.needsProcessing ? (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                      Needs Processing
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Processed
                    </span>
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => navigate(`/books/${book.id}`)}
                      data-test={`view-book-${book.id}`}
                      title="View Details"
                    >
                      <PiEye className="w-4 h-4 text-gray-600" />
                    </Button>
                    {book.needsProcessing && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleProcessSingle(book.id)}
                        isLoading={processingBookId === book.id}
                        disabled={processingBookId !== null}
                        leftIcon={<PiMagicWand />}
                        data-test={`process-book-${book.id}`}
                      >
                        Process
                      </Button>
                    )}
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setDeletingBookId(book.id)}
                      disabled={processingBookId !== null || deleteBook.isPending}
                      data-test={`delete-book-${book.id}`}
                    >
                      <PiTrash className="w-4 h-4 text-red-600" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ConfirmDialog
        isOpen={deletingBookId !== null}
        onClose={() => setDeletingBookId(null)}
        onConfirm={handleDelete}
        title="Delete Book"
        message="Are you sure you want to delete this book? This will also delete all associated photos."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteBook.isPending}
      />
    </>
  )
}
