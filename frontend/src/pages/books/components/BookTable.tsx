// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
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
import { useAuthStore } from '@/stores/authStore'
import { useToast } from '@/hooks/useToast'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { bookStatusTone } from '@/utils/status'
import { IconButton } from '@/components/ui/IconButton'
import {
  AuthorIcon,
  CopyIcon,
  DeleteIcon,
  EditIcon,
  FreeTextIcon,
  GrokipediaIcon,
  LocIcon,
  ViewIcon,
} from '@/components/ui/Icons'

interface BookTableProps {
  books: BookDto[]
  isLoading: boolean
  selectedIds: Set<number>
  selectAll: boolean
  onSelectToggle: (id: number) => void
  onSelectAll: () => void
  onView: (book: BookDto) => void
}

export function BookTable({
  books,
  isLoading,
  selectedIds,
  selectAll,
  onSelectToggle,
  onSelectAll,
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
  const toast = useToast()

  const handleDelete = async () => {
    if (deleteBookId === null) return

    try {
      await deleteBook.mutateAsync(deleteBookId)
      setDeleteBookId(null)
    } catch (error) {
      console.error('Failed to delete book:', error)
      toast.error('Failed to delete book')
    }
  }

  const handleLookupLoc = async (bookId: number) => {
    try {
      const result = await lookupSingleBook.mutateAsync(bookId)
      setLookupResults([result])
      setShowLookupResults(true)
    } catch (error) {
      console.error('Failed to lookup LOC:', error)
      toast.error('Failed to look up LOC')
    }
  }

  const handleClone = async (bookId: number) => {
    try {
      await cloneBook.mutateAsync(bookId)
    } catch (error) {
      console.error('Failed to clone book:', error)
      toast.error('Failed to clone book')
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
              className="w-14 max-h-20 h-auto object-contain rounded hover:opacity-80 transition-opacity cursor-pointer"
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
      hideOnMobile: true,
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
      hideOnMobile: true,
    },
    {
      key: 'status',
      header: 'Status',
      accessor: (book) => (
        <StatusBadge tone={bookStatusTone(book.status)}>
          {formatBookStatus(book.status)}
        </StatusBadge>
      ),
      width: '10%',
    },
    {
      key: 'tags',
      header: 'Genres',
      accessor: (book) => (
        <div className="flex flex-wrap gap-1">
          {book.tagsList?.map((tag) => (
            <StatusBadge key={tag} tone="accent" shape="rounded">
              {tag}
            </StatusBadge>
          ))}
        </div>
      ),
      hideOnMobile: true,
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
              <div className="flex flex-wrap gap-1 justify-end" style={{ maxWidth: '108px' }}>
                {parseSpaceSeparatedUrls(book.freeTextUrl).map((url, index) => (
                  <IconButton
                    key={index}
                    href={url}
                    icon={<FreeTextIcon />}
                    label={`Free text: ${extractDomain(url)}`}
                    tone="success"
                    onClick={(e) => e.stopPropagation()}
                    data-test={`free-text-book-${book.id}-${index}`}
                  />
                ))}
                {isValidUrl(book.grokipediaUrl) && (
                  <IconButton
                    href={book.grokipediaUrl}
                    icon={<GrokipediaIcon />}
                    label="View on Grokipedia"
                    tone="warning"
                    onClick={(e) => e.stopPropagation()}
                    data-test={`grokipedia-book-${book.id}`}
                  />
                )}
              </div>
            )}
            {/* Line 2: view, author, loc */}
            <div className="flex gap-1 justify-end items-center">
              <IconButton
                to={`/books/${book.id}`}
                icon={<ViewIcon />}
                label="View Details"
                onClick={(e) => e.stopPropagation()}
                data-test={`view-book-${book.id}`}
              />
              {book.authorId && (
                <IconButton
                  to={isLibrarian ? `/authors/${book.authorId}/edit` : `/authors/${book.authorId}`}
                  icon={<AuthorIcon />}
                  label="See Author"
                  tone="info"
                  onClick={(e) => e.stopPropagation()}
                  data-test={`see-author-${book.id}`}
                />
              )}
              {isLibrarian && (
                <IconButton
                  icon={<LocIcon />}
                  label="Lookup LOC"
                  tone="accent"
                  disabled={lookupSingleBook.isPending}
                  onClick={(e) => {
                    e.stopPropagation()
                    handleLookupLoc(book.id)
                  }}
                  data-test={`lookup-loc-${book.id}`}
                />
              )}
            </div>
            {/* Line 3: clone, edit, delete */}
            <div className="flex gap-1 justify-end">
              {isLibrarian && (
                <IconButton
                  icon={<CopyIcon />}
                  label="Clone"
                  tone="success"
                  disabled={cloneBook.isPending}
                  onClick={(e) => {
                    e.stopPropagation()
                    handleClone(book.id)
                  }}
                  data-test={`clone-book-${book.id}`}
                />
              )}
              <IconButton
                to={`/books/${book.id}/edit`}
                icon={<EditIcon />}
                label="Edit"
                tone="primary"
                onClick={(e) => e.stopPropagation()}
                data-test={`edit-book-${book.id}`}
              />
              <IconButton
                icon={<DeleteIcon />}
                label="Delete"
                tone="danger"
                onClick={(e) => {
                  e.stopPropagation()
                  setDeleteBookId(book.id)
                }}
                data-test={`delete-book-${book.id}`}
              />
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
