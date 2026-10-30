// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Checkbox } from '@/components/ui/Checkbox'
import { CoverThumbnail } from '@/components/ui/CoverThumbnail'
import { usePhoneLayout } from '@/hooks/usePhoneLayout'
import { useDeleteBook } from '@/api/books'
import { formatBookStatus, truncate, isValidUrl, formatDateTime, parseSpaceSeparatedUrls, extractDomain, isFreeAudioUrl } from '@/utils/formatters'
import { formatBookLabel } from './BookLabelFilters'
import { bookBindingDisplay } from '@/utils/bookBinding'
import { readingDifficultyLabel } from '@/utils/readingDifficulty'
import type { BookDto } from '@/types/dtos'
import { useToast } from '@/hooks/useToast'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { bookStatusTone } from '@/utils/status'
import { FavoriteStar } from '@/components/favorites/FavoriteStar'
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
  const phone = usePhoneLayout()

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
            <div className="text-sm flex items-center gap-1 min-w-0">
              {book.authorId ? (
                <EntityLink to={`/authors/${book.authorId}`} data-test={`book-author-link-${book.id}`}>
                  {truncate(book.author, 40)}
                </EntityLink>
              ) : (
                <span className="text-gray-500">{truncate(book.author, 40)}</span>
              )}
              {isValidUrl(book.authorGrokipediaUrl) && (
                <IconButton
                  href={book.authorGrokipediaUrl}
                  icon={<GrokipediaIcon />}
                  label={`View ${book.author} on Grokipedia`}
                  tone="warning"
                  onClick={(e) => e.stopPropagation()}
                  data-test={`book-author-grokipedia-${book.id}`}
                  className="!p-0.5 shrink-0"
                />
              )}
            </div>
          )}
          <div className="text-sm text-gray-500" data-test={`book-reading-difficulty-${book.id}`}>
            {readingDifficultyLabel(book.readingDifficulty)}
          </div>
          <div className="text-sm text-gray-500" data-test={`book-binding-${book.id}`}>
            {bookBindingDisplay(book)}
          </div>
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
              {formatBookLabel(tag)}
            </StatusBadge>
          ))}
        </div>
      ),
      hideOnMobile: true,
    },
  ]

  const renderActions = (book: BookDto, stacked: boolean) => (
    <div className={stacked ? 'flex flex-col gap-1 items-end' : 'flex flex-row flex-wrap items-center gap-2'}>
      {(parseSpaceSeparatedUrls(book.freeTextUrl).length > 0 || isValidUrl(book.grokipediaUrl)) && (
        <div
          className={stacked ? 'flex flex-wrap gap-1 justify-end' : 'flex flex-wrap gap-1'}
          style={stacked ? { maxWidth: '108px' } : undefined}
        >
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
              <FavoriteStar itemType="BOOK" itemId={book.id} />
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
  )

  const showPhoneList = phone && !isLoading && books.length > 0

  return (
    <>
      {showPhoneList ? (
        <div data-test="books-phone-list" className="divide-y divide-gray-200">
          <div className="flex items-center gap-3 pb-3">
            <Checkbox
              checked={selectAll}
              onChange={onSelectAll}
              data-test="select-all-checkbox"
            />
            <span className="text-sm text-gray-600">Select shown</span>
          </div>
          {books.map((book) => (
            <div
              key={book.id}
              className="py-4"
              data-entity-id={book.id}
              onClick={() => onView(book)}
            >
              <div className="flex items-start gap-3 min-w-0">
                <div className="pt-1" onClick={(event) => event.stopPropagation()}>
                  <Checkbox
                    checked={selectedIds.has(book.id)}
                    onChange={() => onSelectToggle(book.id)}
                    data-test={`select-checkbox-${book.id}`}
                  />
                </div>
                <CoverThumbnail
                  photoId={book.firstPhotoId}
                  checksum={book.firstPhotoChecksum}
                  alt={`Cover of ${book.title}`}
                  stopPropagation
                />
                <div className="flex-1 min-w-0" data-test={`book-phone-body-${book.id}`}>
                  <EntityLink to={`/books/${book.id}`} className="font-medium block break-words" data-test={`book-title-link-${book.id}`}>
                    {book.title}
                  </EntityLink>
                  {book.author && (
                    <div className="text-sm flex items-center gap-1 min-w-0 flex-wrap">
                      {book.authorId ? (
                        <EntityLink to={`/authors/${book.authorId}`} data-test={`book-author-link-${book.id}`}>
                          {book.author}
                        </EntityLink>
                      ) : (
                        <span className="text-gray-500">{book.author}</span>
                      )}
                      {isValidUrl(book.authorGrokipediaUrl) && (
                        <IconButton
                          href={book.authorGrokipediaUrl}
                          icon={<GrokipediaIcon />}
                          label={`View ${book.author} on Grokipedia`}
                          tone="warning"
                          onClick={(event) => event.stopPropagation()}
                          data-test={`book-author-grokipedia-${book.id}`}
                          className="!p-0.5 shrink-0"
                        />
                      )}
                    </div>
                  )}
                  <div className="text-sm text-gray-500" data-test={`book-reading-difficulty-${book.id}`}>
                    {readingDifficultyLabel(book.readingDifficulty)}
                  </div>
                  <div className="text-sm text-gray-500" data-test={`book-binding-${book.id}`}>
                    {bookBindingDisplay(book)}
                  </div>
                  {book.locNumber && (
                    <div className="text-sm text-gray-500">
                      <span className="font-medium">LOC:</span> {book.locNumber}
                    </div>
                  )}
                  <div className="mt-2">
                    <StatusBadge tone={bookStatusTone(book.status)}>
                      {formatBookStatus(book.status)}
                    </StatusBadge>
                  </div>
                </div>
              </div>
              <div className="mt-3" onClick={(event) => event.stopPropagation()}>
                {renderActions(book, false)}
              </div>
            </div>
          ))}
        </div>
      ) : (
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
          actions={(book) => renderActions(book, true)}
          isLoading={isLoading}
          emptyMessage="No books found"
        />
      )}

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
