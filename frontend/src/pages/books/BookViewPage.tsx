// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { PhotoSection } from '@/components/photos/PhotoSection'
import { useBook, useCloneBook, useDeleteBook } from '@/api/books'
import { useLookupSingleYdl } from '@/api/ydl-lookup'
import { useLookupSingleEmu } from '@/api/emu-lookup'
import { formatBookStatus, formatDateTime, parseISODateSafe, parseSpaceSeparatedUrls, extractDomain } from '@/utils/formatters'
import { PageLoading } from '@/components/progress/PageLoading'
import { PiCopy, PiPencil, PiTrash, PiMagnifyingGlass, PiCheckCircle, PiXCircle } from 'react-icons/pi'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { bookStatusTone } from '@/utils/status'
import { EntityLink } from '@/components/ui/EntityLink'
import { TEXT_LINK_UNDERLINE_CLASS } from '@/components/ui/IconButton'
import { BackLink } from '@/components/ui/BackLink'
import { EntityNotFound } from '@/components/ui/EntityNotFound'
import { PageCard } from '@/components/ui/PageCard'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
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
  const lookupYdl = useLookupSingleYdl()
  const lookupEmu = useLookupSingleEmu()
  const isLibrarian = useIsLibrarian()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [error, setError] = useState('')

  const handleYdlLookup = async () => {
    try {
      await lookupYdl.mutateAsync(bookId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to look up YDL availability')
    }
  }

  const handleEmuLookup = async () => {
    try {
      await lookupEmu.mutateAsync(bookId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to look up EMU availability')
    }
  }

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
    return <PageLoading />
  }

  if (!book) {
    return (
      <EntityNotFound
        title="Book Not Found"
        entityLabel="book"
        onBack={handleBack}
        backLabel="Return to Books"
      />
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <BackLink onClick={handleBack} data-test="back-to-books">
        Back to Books
      </BackLink>

      <PageCard padding={false}>
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

          {/* Book Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Author</p>
                {book.authorId ? (
                  <EntityLink to={`/authors/${book.authorId}`} data-test="book-author-link">
                    {book.author}
                  </EntityLink>
                ) : (
                  <p className="text-gray-900">{book.author}</p>
                )}
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
                    className={TEXT_LINK_UNDERLINE_CLASS}
                    data-test="book-grokipedia-link"
                  >
                    View on Grokipedia
                  </a>
                </div>
              )}
              {book.freeTextUrl && parseSpaceSeparatedUrls(book.freeTextUrl).length > 0 && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Free Online Text</p>
                  <div className="flex flex-wrap gap-2">
                    {parseSpaceSeparatedUrls(book.freeTextUrl).map((url, index) => (
                      <a
                        key={index}
                        href={url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={TEXT_LINK_UNDERLINE_CLASS}
                        data-test={`book-free-text-link-${index}`}
                      >
                        {extractDomain(url)}
                      </a>
                    ))}
                  </div>
                </div>
              )}
              <div>
                <p className="text-sm font-medium text-gray-500">Electronic Resource</p>
                <p className="text-gray-900" data-test="book-electronic-resource">
                  {book.electronicResource ? 'Yes' : 'No'}
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Status</p>
                <StatusBadge tone={bookStatusTone(book.status)}>
                  {formatBookStatus(book.status)}
                </StatusBadge>
              </div>
              {book.dateAddedToLibrary && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Date Added</p>
                  <p className="text-gray-900">
                    {formatDateTime(book.dateAddedToLibrary)}
                  </p>
                </div>
              )}
              {book.lastModified && (
                <div data-test="book-last-modified">
                  <p className="text-sm font-medium text-gray-500">Last Modified</p>
                  <p className="text-gray-900">
                    {parseISODateSafe(book.lastModified).toLocaleDateString()}
                  </p>
                </div>
              )}
              <div data-test="book-loan-count">
                <p className="text-sm font-medium text-gray-500">Active Loans</p>
                <p className="text-gray-900">{book.loanCount ?? 0}</p>
              </div>
              {book.tagsList && book.tagsList.length > 0 && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Genres</p>
                  <div className="flex flex-wrap gap-1 mt-1">
                    {book.tagsList.map((tag) => (
                      <StatusBadge
                        key={tag}
                        tone="accent"
                        shape="rounded"
                        data-test={`book-tag-${tag}`}
                      >
                        {tag}
                      </StatusBadge>
                    ))}
                  </div>
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

          {/* YDL Availability */}
          <div className="bg-gray-50 rounded-lg p-6" data-test="ydl-availability-section">
            <div className="flex items-start justify-between mb-4">
              <h2 className="text-sm font-semibold text-gray-700">YDL Availability</h2>
              <div className="flex items-center gap-3">
                <a
                  href={`https://ypsilantidl.na4.iiivega.com/search?query=${encodeURIComponent(`"${book.title}"`)}&searchType=everything&pageSize=40`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`${TEXT_LINK_UNDERLINE_CLASS} text-sm`}
                  data-test="book-view-ydl-check-link"
                >
                  Check YDL
                </a>
                {isLibrarian && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleYdlLookup}
                    isLoading={lookupYdl.isPending}
                    disabled={lookupYdl.isPending}
                    leftIcon={<PiMagnifyingGlass />}
                    data-test="book-view-ydl-lookup"
                  >
                    {book.ydlLastChecked ? 'Retry YDL Lookup' : 'Lookup YDL Availability'}
                  </Button>
                )}
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div data-test="ydl-audio-status">
                <p className="text-sm font-medium text-gray-500">Audio Book</p>
                <p className="text-gray-900 flex items-center gap-1">
                  {book.ydlAudioAvailable === undefined ? (
                    'Not checked yet'
                  ) : book.ydlAudioAvailable ? (
                    <>
                      <PiCheckCircle className="text-green-600" /> Available
                    </>
                  ) : (
                    <>
                      <PiXCircle className="text-gray-400" /> Not available
                    </>
                  )}
                </p>
              </div>
              <div data-test="ydl-paper-status">
                <p className="text-sm font-medium text-gray-500">Paper Book</p>
                <p className="text-gray-900 flex items-center gap-1">
                  {book.ydlPaperAvailable === undefined ? (
                    'Not checked yet'
                  ) : book.ydlPaperAvailable ? (
                    <>
                      <PiCheckCircle className="text-green-600" /> Available
                    </>
                  ) : (
                    <>
                      <PiXCircle className="text-gray-400" /> Not available
                    </>
                  )}
                </p>
              </div>
              <div data-test="ydl-ebook-status">
                <p className="text-sm font-medium text-gray-500">Ebook</p>
                <p className="text-gray-900 flex items-center gap-1">
                  {book.ydlEbookAvailable === undefined ? (
                    'Not checked yet'
                  ) : book.ydlEbookAvailable ? (
                    <>
                      <PiCheckCircle className="text-green-600" /> Available
                    </>
                  ) : (
                    <>
                      <PiXCircle className="text-gray-400" /> Not available
                    </>
                  )}
                </p>
              </div>
            </div>
            {book.ydlLastChecked && (
              <p className="text-xs text-gray-500 mt-3">
                Last checked: {formatDateTime(book.ydlLastChecked)}
              </p>
            )}
            {book.ydlLookupError && (
              <p className="text-xs text-red-600 mt-1" data-test="ydl-lookup-error">
                {book.ydlLookupError}
              </p>
            )}
          </div>

          {/* EMU Availability */}
          <div className="bg-gray-50 rounded-lg p-6" data-test="emu-availability-section">
            <div className="flex items-start justify-between mb-4">
              <h2 className="text-sm font-semibold text-gray-700">EMU Availability</h2>
              <div className="flex items-center gap-3">
                <a
                  href={`https://emich.primo.exlibrisgroup.com/discovery/search?query=${encodeURIComponent(`any,contains,"${book.title}"`)}&tab=Everything&search_scope=MyInst_and_CI&vid=01EMU_INST:EMU`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`${TEXT_LINK_UNDERLINE_CLASS} text-sm`}
                  data-test="book-view-emu-check-link"
                >
                  Check EMU
                </a>
                {isLibrarian && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleEmuLookup}
                    isLoading={lookupEmu.isPending}
                    disabled={lookupEmu.isPending}
                    leftIcon={<PiMagnifyingGlass />}
                    data-test="book-view-emu-lookup"
                  >
                    {book.emuLastChecked ? 'Retry EMU Lookup' : 'Lookup EMU Availability'}
                  </Button>
                )}
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div data-test="emu-audio-status">
                <p className="text-sm font-medium text-gray-500">Audio Book</p>
                <p className="text-gray-900 flex items-center gap-1">
                  {book.emuAudioAvailable === undefined ? (
                    'Not checked yet'
                  ) : book.emuAudioAvailable ? (
                    <>
                      <PiCheckCircle className="text-green-600" /> Available
                    </>
                  ) : (
                    <>
                      <PiXCircle className="text-gray-400" /> Not available
                    </>
                  )}
                </p>
              </div>
              <div data-test="emu-paper-status">
                <p className="text-sm font-medium text-gray-500">Paper Book</p>
                <p className="text-gray-900 flex items-center gap-1">
                  {book.emuPaperAvailable === undefined ? (
                    'Not checked yet'
                  ) : book.emuPaperAvailable ? (
                    <>
                      <PiCheckCircle className="text-green-600" /> Available
                    </>
                  ) : (
                    <>
                      <PiXCircle className="text-gray-400" /> Not available
                    </>
                  )}
                </p>
              </div>
              <div data-test="emu-ebook-status">
                <p className="text-sm font-medium text-gray-500">Ebook</p>
                <p className="text-gray-900 flex items-center gap-1">
                  {book.emuEbookAvailable === undefined ? (
                    'Not checked yet'
                  ) : book.emuEbookAvailable ? (
                    <>
                      <PiCheckCircle className="text-green-600" /> Available
                    </>
                  ) : (
                    <>
                      <PiXCircle className="text-gray-400" /> Not available
                    </>
                  )}
                </p>
              </div>
            </div>
            {book.emuLastChecked && (
              <p className="text-xs text-gray-500 mt-3">
                Last checked: {formatDateTime(book.emuLastChecked)}
              </p>
            )}
            {book.emuLookupError && (
              <p className="text-xs text-red-600 mt-1" data-test="emu-lookup-error">
                {book.emuLookupError}
              </p>
            )}
          </div>

          {/* Photos */}
          <PhotoSection
            entityType="book"
            entityId={book.id}
            entityName={book.title}
          />
        </div>
      </PageCard>

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete Book"
        message="Are you sure you want to delete this book? This action cannot be undone."
        confirmText="Yes, Delete"
        variant="danger"
        isLoading={deleteBook.isPending}
        confirmDataTest="confirm-delete-book"
        cancelDataTest="cancel-delete-book"
      />
    </div>
  )
}
