// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { CoverThumbnail } from '@/components/ui/CoverThumbnail'
import { useDeleteBook } from '@/api/books'
import { formatBookStatus, truncate, isValidUrl, formatDateTime, parseSpaceSeparatedUrls, extractDomain, isFreeAudioUrl } from '@/utils/formatters'
import type { BookDto } from '@/types/dtos'
import { useToast } from '@/hooks/useToast'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { bookStatusTone } from '@/utils/status'
import { IconButton } from '@/components/ui/IconButton'
import { EntityLink } from '@/components/ui/EntityLink'
import {
  AuthorIcon,
  BookIcon,
  DeleteIcon,
  EditIcon,
  FreeAudioIcon,
  FreeTextIcon,
  GrokipediaIcon,
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
  const deleteBook = useDeleteBook()
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

  const columns: Column<BookDto>[] = [
    {
      key: 'photo',
      header: 'Cover',
      accessor: (book) => (
        <CoverThumbnail
          photoId={book.firstPhotoId}
          checksum={book.firstPhotoChecksum}
          alt={`Cover of ${book.title}`}
          stopPropagation
        />
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
          <EntityLink to={`/books/${book.id}`} className="font-medium" data-test={`book-title-link-${book.id}`}>
            {truncate(book.title, 50)}
          </EntityLink>
          {book.author && (
            <div className="text-sm">
              {book.authorId ? (
                <EntityLink to={`/authors/${book.authorId}`} data-test={`book-author-link-${book.id}`}>
                  {truncate(book.author, 40)}
                </EntityLink>
              ) : (
                <span className="text-gray-500">{truncate(book.author, 40)}</span>
              )}
            </div>
          )}
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
                {parseSpaceSeparatedUrls(book.freeTextUrl).map((url, index) => {
                  const audio = isFreeAudioUrl(url)
                  return (
                  <IconButton
                    key={index}
                    href={url}
                    icon={audio ? <FreeAudioIcon /> : <FreeTextIcon />}
                    label={`${audio ? 'Free audio' : 'Free text'}: ${extractDomain(url)}`}
                    tone="success"
                    onClick={(e) => e.stopPropagation()}
                    data-test={`${audio ? 'free-audio' : 'free-text'}-book-${book.id}-${index}`}
                  />
                  )
                })}
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
            {/* Line 2: view, author */}
            <div className="flex gap-1 justify-end items-center">
              <IconButton
                to={`/books/${book.id}`}
                icon={<BookIcon />}
                label="View Details"
                onClick={(e) => e.stopPropagation()}
                data-test={`view-book-${book.id}`}
              />
              {book.authorId && (
                <IconButton
                  to={`/authors/${book.authorId}`}
                  icon={<AuthorIcon />}
                  label="See Author"
                  tone="info"
                  onClick={(e) => e.stopPropagation()}
                  data-test={`see-author-${book.id}`}
                />
              )}
            </div>
            {/* Line 3: edit, delete */}
            <div className="flex gap-1 justify-end">
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
    </>
  )
}
