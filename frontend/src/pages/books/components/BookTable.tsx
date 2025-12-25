// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useDeleteBook } from '@/api/books'
import { formatBookStatus } from '@/utils/formatters'
import type { BookDto } from '@/types/dtos'

interface BookTableProps {
  books: BookDto[]
  isLoading: boolean
  selectedIds: Set<number>
  selectAll: boolean
  onSelectToggle: (id: number) => void
  onSelectAll: () => void
  onEdit: (book: BookDto) => void
  onView: (book: BookDto) => void
}

export function BookTable({
  books,
  isLoading,
  selectedIds,
  selectAll,
  onSelectToggle,
  onSelectAll,
  onEdit,
  onView,
}: BookTableProps) {
  const [deleteBookId, setDeleteBookId] = useState<number | null>(null)
  const deleteBook = useDeleteBook()

  const handleDelete = async () => {
    if (deleteBookId === null) return

    try {
      await deleteBook.mutateAsync(deleteBookId)
      setDeleteBookId(null)
    } catch (error) {
      console.error('Failed to delete book:', error)
    }
  }

  const columns: Column<BookDto>[] = [
    {
      key: 'title',
      header: 'Title',
      accessor: (book) => (
        <div>
          <div className="font-medium text-gray-900">{book.title}</div>
          {book.author && <div className="text-sm text-gray-500">{book.author}</div>}
        </div>
      ),
      width: '30%',
    },
    {
      key: 'publicationYear',
      header: 'Year',
      accessor: (book) => book.publicationYear || '—',
      width: '8%',
    },
    {
      key: 'publisher',
      header: 'Publisher',
      accessor: (book) => book.publisher || '—',
      width: '20%',
    },
    {
      key: 'library',
      header: 'Library',
      accessor: (book) => book.library || '—',
      width: '15%',
    },
    {
      key: 'locCallNumber',
      header: 'LOC',
      accessor: (book) => book.locNumber || '—',
      width: '12%',
    },
    {
      key: 'status',
      header: 'Status',
      accessor: (book) => (
        <span
          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
            book.status === 'AVAILABLE'
              ? 'bg-green-100 text-green-800'
              : book.status === 'CHECKED_OUT'
              ? 'bg-blue-100 text-blue-800'
              : 'bg-red-100 text-red-800'
          }`}
        >
          {formatBookStatus(book.status)}
        </span>
      ),
      width: '10%',
    },
  ]

  return (
    <>
      <DataTable
        data={books}
        columns={columns}
        keyExtractor={(book) => book.id}
        selectable
        selectedIds={selectedIds}
        selectAll={selectAll}
        onSelectToggle={onSelectToggle}
        onSelectAll={onSelectAll}
        onRowClick={onView}
        actions={(book) => (
          <>
            <button
              onClick={(e) => {
                e.stopPropagation()
                onEdit(book)
              }}
              className="text-blue-600 hover:text-blue-900"
              data-test={`edit-book-${book.id}`}
              title="Edit"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                />
              </svg>
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation()
                setDeleteBookId(book.id)
              }}
              className="text-red-600 hover:text-red-900"
              data-test={`delete-book-${book.id}`}
              title="Delete"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                />
              </svg>
            </button>
          </>
        )}
        isLoading={isLoading}
        emptyMessage="No books found"
      />

      <ConfirmDialog
        isOpen={deleteBookId !== null}
        onClose={() => setDeleteBookId(null)}
        onConfirm={handleDelete}
        title="Delete Book"
        message="Are you sure you want to delete this book? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteBook.isPending}
      />
    </>
  )
}
