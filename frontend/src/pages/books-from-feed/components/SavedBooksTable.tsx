// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Spinner } from '@/components/progress/Spinner'
import { useProcessSingleBook, type SavedBookDto } from '@/api/books-from-feed'
import { useDeleteBook } from '@/api/books'
import { PiMagicWand, PiTrash } from 'react-icons/pi'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { EmptyState } from '@/components/ui/EmptyState'
import { EntityLink } from '@/components/ui/EntityLink'
import { IconButton } from '@/components/ui/IconButton'
import { AuthorIcon, BookIcon, EditIcon } from '@/components/ui/Icons'

interface SavedBooksTableProps {
  books: SavedBookDto[]
  isLoading: boolean
  onRefresh: () => void
}

export function SavedBooksTable({ books, isLoading, onRefresh }: SavedBooksTableProps) {
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
      <EmptyState
        message="No saved books yet"
        description={'Click "Select Photos" above to import book photos from Google Photos'}
      />
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
                    <EntityLink to={`/books/${book.id}`} className="text-sm font-medium" data-test={`saved-book-title-${book.id}`}>
                      {book.title}
                    </EntityLink>
                    {book.author && (
                      book.authorId ? (
                        <div className="text-sm">
                          <EntityLink to={`/authors/${book.authorId}`} data-test={`saved-book-author-${book.id}`}>
                            {book.author}
                          </EntityLink>
                        </div>
                      ) : (
                        <div className="text-sm text-gray-500">{book.author}</div>
                      )
                    )}
                  </div>
                </td>
                <td className="px-6 py-4 overflow-hidden truncate">
                  <div className="text-sm text-gray-900">{book.photoCount}</div>
                </td>
                <td className="px-6 py-4 overflow-hidden truncate">
                  {book.needsProcessing ? (
                    <StatusBadge tone="warning">Needs Processing</StatusBadge>
                  ) : (
                    <StatusBadge tone="success">Processed</StatusBadge>
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <div className="flex justify-end gap-2">
                    <IconButton
                      to={`/books/${book.id}`}
                      icon={<BookIcon />}
                      label="View Details"
                      data-test={`view-book-${book.id}`}
                    />
                    {book.authorId && (
                      <IconButton
                        to={`/authors/${book.authorId}`}
                        icon={<AuthorIcon />}
                        label="See Author"
                        tone="info"
                        data-test={`see-author-${book.id}`}
                      />
                    )}
                    <IconButton
                      to={`/books/${book.id}/edit`}
                      icon={<EditIcon />}
                      label="Edit"
                      tone="primary"
                      data-test={`edit-book-${book.id}`}
                    />
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
