// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { ThrottledThumbnail } from '@/components/ui/ThrottledThumbnail'
import { useDeleteBook, useCloneBook } from '@/api/books'
import { useLookupSingleBook } from '@/api/loc-lookup'
import { getThumbnailUrl } from '@/api/photos'
import { LocLookupResultsModal } from './LocLookupResultsModal'
import { formatBookStatus, truncate, isValidUrl, formatDateTime, parseSpaceSeparatedUrls, extractDomain } from '@/utils/formatters'
import type { BookDto } from '@/types/dtos'
import type { LocLookupResultDto } from '@/api/loc-lookup'
import { PiBookOpen, PiCopy, PiEye } from 'react-icons/pi'
import { useAuthStore } from '@/stores/authStore'

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
  const [lookupResults, setLookupResults] = useState<LocLookupResultDto[]>([])
  const [showLookupResults, setShowLookupResults] = useState(false)
  const deleteBook = useDeleteBook()
  const cloneBook = useCloneBook()
  const lookupSingleBook = useLookupSingleBook()
  const { user } = useAuthStore()
  const isLibrarian = user?.authority === 'LIBRARIAN'

  const handleDelete = async () => {
    if (deleteBookId === null) return

    try {
      await deleteBook.mutateAsync(deleteBookId)
      setDeleteBookId(null)
    } catch (error) {
      console.error('Failed to delete book:', error)
    }
  }

  const handleLookupLoc = async (bookId: number) => {
    try {
      const result = await lookupSingleBook.mutateAsync(bookId)
      setLookupResults([result])
      setShowLookupResults(true)
    } catch (error) {
      console.error('Failed to lookup LOC:', error)
    }
  }

  const handleClone = async (bookId: number) => {
    try {
      await cloneBook.mutateAsync(bookId)
    } catch (error) {
      console.error('Failed to clone book:', error)
    }
  }

  const columns: Column<BookDto>[] = [
    {
      key: 'photo',
      header: 'Cover',
      accessor: (book) =>
        book.firstPhotoId ? (
          <a
            href={`/photos/${book.firstPhotoId}`}
            target="_blank"
            rel="noopener noreferrer"
            onClick={(e) => e.stopPropagation()}
            className="block"
            style={{ width: '3.5rem', minWidth: '3.5rem' }}
            title="View full-size photo"
          >
            <ThrottledThumbnail
              photoId={book.firstPhotoId}
              url={getThumbnailUrl(book.firstPhotoId, 70, book.firstPhotoChecksum)}
              alt={`Cover of ${book.title}`}
              className="w-14 h-20 object-cover rounded hover:opacity-80 transition-opacity cursor-pointer"
            />
          </a>
        ) : (
          <div className="w-14 h-20 bg-gray-100 rounded flex items-center justify-center text-gray-400">
            -
          </div>
        ),
      width: '70px',
      minWidth: '70px',
      cellClassName: 'px-3 py-3 sm:py-4 overflow-hidden text-sm',
    },
    {
      key: 'title',
      header: 'Title',
      accessor: (book) => (
        <div>
          <div className="font-medium text-gray-900">{truncate(book.title, 50)}</div>
          {book.author && <div className="text-sm text-gray-500">{truncate(book.author, 40)}</div>}
        </div>
      ),
      width: '28%',
    },
    {
      key: 'locCallNumber',
      header: 'LOC',
      accessor: (book) => book.locNumber || '—',
      width: '15%',
      cellClassName: 'px-3 sm:px-6 py-3 sm:py-4 overflow-hidden text-sm break-words',
    },
    {
      key: 'dateAdded',
      header: 'Date Added',
      accessor: (book) =>
        book.dateAddedToLibrary ? (
          <div>
            <div>{formatDateTime(book.dateAddedToLibrary, 'MMM d, yyyy')}</div>
            <div className="text-gray-500">{formatDateTime(book.dateAddedToLibrary, 'h:mm a')}</div>
          </div>
        ) : (
          '—'
        ),
      width: '15%',
    },
    {
      key: 'status',
      header: 'Status',
      accessor: (book) => (
        <span
          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
            book.status === 'ACTIVE'
              ? 'bg-green-100 text-green-800'
              : book.status === 'ON_ORDER'
              ? 'bg-blue-100 text-blue-800'
              : 'bg-red-100 text-red-800'
          }`}
        >
          {formatBookStatus(book.status)}
        </span>
      ),
      width: '10%',
    },
    {
      key: 'tags',
      header: 'Genres',
      accessor: (book) => (
        <div className="flex flex-wrap gap-1">
          {book.tagsList?.map((tag) => (
            <span
              key={tag}
              className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-indigo-100 text-indigo-800"
            >
              {tag}
            </span>
          ))}
        </div>
      ),
      width: '12%',
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
          <div className="flex flex-col gap-1 items-end">
            {/* Line 1: URL-type links (free text, grokipedia) */}
            {(parseSpaceSeparatedUrls(book.freeTextUrl).length > 0 || isValidUrl(book.grokipediaUrl)) && (
              <div className="flex gap-1 justify-end">
                {parseSpaceSeparatedUrls(book.freeTextUrl).map((url, index) => (
                  <a
                    key={index}
                    href={url}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={(e) => e.stopPropagation()}
                    className="text-green-600 hover:text-green-900"
                    data-test={`free-text-book-${book.id}-${index}`}
                    title={`Free text: ${extractDomain(url)}`}
                  >
                    <PiBookOpen className="w-5 h-5" />
                  </a>
                ))}
                {isValidUrl(book.grokipediaUrl) && (
                  <a
                    href={book.grokipediaUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={(e) => e.stopPropagation()}
                    className="text-orange-600 hover:text-orange-900"
                    data-test={`grokipedia-book-${book.id}`}
                    title="View on Grokipedia"
                  >
                    <span className="text-lg font-bold">Ø</span>
                  </a>
                )}
              </div>
            )}
            {/* Line 2: view, author, loc */}
            <div className="flex gap-1 justify-end">
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  onView(book)
                }}
                className="text-gray-600 hover:text-gray-900"
                data-test={`view-book-${book.id}`}
                title="View Details"
              >
                <PiEye className="w-5 h-5" />
              </button>
              {book.authorId && (
                <Link
                  to={isLibrarian ? `/authors/${book.authorId}/edit` : `/authors/${book.authorId}`}
                  onClick={(e) => e.stopPropagation()}
                  className="text-teal-600 hover:text-teal-900"
                  data-test={`see-author-${book.id}`}
                  title="See Author"
                >
                  <span className="text-lg">👤</span>
                </Link>
              )}
              {isLibrarian && (
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    handleLookupLoc(book.id)
                  }}
                  className="text-purple-600 hover:text-purple-900"
                  data-test={`lookup-loc-${book.id}`}
                  title="Lookup LOC"
                  disabled={lookupSingleBook.isPending}
                >
                  <span className="text-lg">🗃️</span>
                </button>
              )}
            </div>
            {/* Line 3: clone, edit, delete */}
            <div className="flex gap-1 justify-end">
              {isLibrarian && (
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    handleClone(book.id)
                  }}
                  className="text-green-600 hover:text-green-900"
                  data-test={`clone-book-${book.id}`}
                  title="Clone"
                  disabled={cloneBook.isPending}
                >
                  <PiCopy className="w-5 h-5" />
                </button>
              )}
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
            </div>
          </div>
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

      <LocLookupResultsModal
        isOpen={showLookupResults}
        onClose={() => setShowLookupResults(false)}
        results={lookupResults}
      />
    </>
  )
}
