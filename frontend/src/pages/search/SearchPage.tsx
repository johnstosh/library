// (c) Copyright 2025 by Muczynski
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/progress/Spinner'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useSearch, type SearchFilters } from '@/api/search'
import { BookLabelFilters } from '@/pages/books/components/BookLabelFilters'
import { LocLookupResultsModal } from '@/pages/books/components/LocLookupResultsModal'
import { formatBookStatus, parseSpaceSeparatedUrls, extractDomain, isValidUrl } from '@/utils/formatters'
import { PiMagnifyingGlass, PiBook, PiUser, PiFunnel } from 'react-icons/pi'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { bookStatusTone } from '@/utils/status'
import { IconButton } from '@/components/ui/IconButton'
import {
  AuthorIcon,
  BooksIcon,
  CopyIcon,
  DeleteIcon,
  EditIcon,
  FreeTextIcon,
  GrokipediaIcon,
  LocIcon,
  ViewIcon,
} from '@/components/ui/Icons'
import { PageHeader } from '@/components/ui/PageHeader'
import { useToast } from '@/hooks/useToast'
import { PageCard } from '@/components/ui/PageCard'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { EmptyState } from '@/components/ui/EmptyState'
import { useIsLibrarian } from '@/stores/authStore'
import { useDeleteBook, useCloneBook } from '@/api/books'
import { useDeleteAuthor } from '@/api/authors'
import { useLookupSingleBook } from '@/api/loc-lookup'
import type { BookDto, AuthorDto } from '@/types/dtos'
import type { LocLookupResultDto } from '@/api/loc-lookup'

// ─── Filter chip component ────────────────────────────────────────────────────

interface FilterChipProps {
  label: string
  active: boolean
  onClick: () => void
  tooltip: string
  dataTest: string
}

function FilterChip({ label, active, onClick, tooltip, dataTest }: FilterChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={tooltip}
      data-test={dataTest}
      className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-full border transition-colors cursor-pointer select-none ${
        active
          ? 'border-blue-500 bg-blue-50 text-blue-700 font-medium hover:bg-blue-100'
          : 'border-gray-300 text-gray-600 hover:border-blue-400 hover:text-blue-600 bg-white'
      }`}
    >
      {active ? (
        <svg className="hidden sm:block w-3.5 h-3.5 text-blue-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      ) : (
        <PiFunnel className="hidden sm:block w-3.5 h-3.5 text-gray-400 shrink-0" />
      )}
      {label}
      <span className="hidden sm:inline text-gray-400 text-xs shrink-0" aria-hidden="true">ⓘ</span>
    </button>
  )
}

// ─── Main search page ─────────────────────────────────────────────────────────

export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const urlQuery = searchParams.get('q') ?? ''
  const urlPage = parseInt(searchParams.get('page') ?? '0', 10)

  // Filter chip state — lives in URL for shareability
  const urlInLib = searchParams.get('inLib') === 'true'
  const urlElec = searchParams.get('elec') === 'true'
  const urlFreeText = searchParams.get('freeText') === 'true'
  const urlAudio = searchParams.get('audio') === 'true'

  const filters: SearchFilters = {
    inLib: urlInLib,
    elec: urlElec,
    freeText: urlFreeText,
    audio: urlAudio,
  }

  // Local input state (typing before submit)
  const [inputValue, setInputValue] = useState(urlQuery)
  const [selectedLabels, setSelectedLabels] = useState<string[]>([])
  const pageSize = 20

  // Sync input value when URL changes (browser back/forward)
  useEffect(() => {
    setInputValue(urlQuery)
  }, [urlQuery])

  const hasSearched = searchParams.has('q')
  const hasFilters = urlInLib || urlElec || urlFreeText || urlAudio || selectedLabels.length > 0

  const { data, isLoading, error } = useSearch(
    urlQuery,
    urlPage,
    pageSize,
    filters,
    hasSearched || hasFilters,
    selectedLabels,
  )
  const isLibrarian = useIsLibrarian()

  // ── Helpers for building URL params ──────────────────────────────────────

  const buildFilterParams = (overrides: Partial<SearchFilters> = {}): Record<string, string> => {
    const f = { ...filters, ...overrides }
    const params: Record<string, string> = {}
    if (f.inLib) params.inLib = 'true'
    if (f.elec) params.elec = 'true'
    if (f.freeText) params.freeText = 'true'
    if (f.audio) params.audio = 'true'
    return params
  }

  // ── Event handlers ────────────────────────────────────────────────────────

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const params: Record<string, string> = { q: inputValue.trim(), ...buildFilterParams() }
    setSearchParams(params)
  }

  const handleClear = () => {
    setInputValue('')
    setSelectedLabels([])
    setSearchParams({})
  }

  const handleFilterToggle = (key: keyof SearchFilters) => {
    const newValue = !filters[key]
    const overrides = { [key]: newValue } as Partial<SearchFilters>
    const params: Record<string, string> = { ...buildFilterParams(overrides) }
    // Preserve query and page if present
    if (urlQuery || hasSearched) params.q = urlQuery
    if (urlPage > 0) params.page = String(urlPage)
    setSearchParams(params)
  }

  const handleToggleLabel = (label: string) => {
    setSelectedLabels((prev) =>
      prev.includes(label) ? prev.filter((l) => l !== label) : [...prev, label],
    )
  }

  const handleClearLabels = () => {
    setSelectedLabels([])
  }

  const handlePageChange = (newPage: number) => {
    const params: Record<string, string> = { ...buildFilterParams() }
    if (urlQuery || hasSearched) params.q = urlQuery
    if (newPage > 0) params.page = String(newPage)
    setSearchParams(params)
  }

  const hasResults = data && (data.books.length > 0 || data.authors.length > 0)
  const noResults = (hasSearched || hasFilters) && !isLoading && data && data.books.length === 0 && data.authors.length === 0

  return (
    <div className="max-w-4xl mx-auto">
      <PageHeader
        title="Search Library"
        description="Search for books and authors by title or name"
      />

      {/* Search Form */}
      <form onSubmit={handleSearch} className="mb-8">
        <div className="flex gap-2">
          <div className="flex-1">
            <Input
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder="Enter book title or author name..."
              data-test="search-input"
              className="text-lg"
            />
          </div>
          <Button
            type="submit"
            variant="primary"
            size="lg"
            disabled={isLoading}
            leftIcon={<PiMagnifyingGlass />}
            data-test="search-button"
          >
            Search
          </Button>
          {(hasSearched || hasFilters) && (
            <Button
              type="button"
              variant="ghost"
              size="lg"
              onClick={handleClear}
              data-test="clear-search"
            >
              Clear
            </Button>
          )}
        </div>

        {/* Filter Chips */}
        <div className="flex flex-wrap gap-2 mt-4" data-test="search-filter-chips">
          <FilterChip
            label="In-library materials"
            active={urlInLib}
            onClick={() => handleFilterToggle('inLib')}
            tooltip="Limit results to books with a Library of Congress call number — books physically in the collection"
            dataTest="filter-in-library"
          />
          <FilterChip
            label="Electronic resource"
            active={urlElec}
            onClick={() => handleFilterToggle('elec')}
            tooltip="Limit results to books marked as electronic resources"
            dataTest="filter-electronic"
          />
          <FilterChip
            label="Has free online text"
            active={urlFreeText}
            onClick={() => handleFilterToggle('freeText')}
            tooltip="Limit results to books that have a free online text URL (e.g., Project Gutenberg, Internet Archive)"
            dataTest="filter-free-text"
          />
          <FilterChip
            label="Has free online audio"
            active={urlAudio}
            onClick={() => handleFilterToggle('audio')}
            tooltip="Limit results to books with a free LibriVox audio recording"
            dataTest="filter-audio"
          />
        </div>

        {/* Label Filter Buttons */}
        <BookLabelFilters
          selectedLabels={selectedLabels}
          onToggleLabel={handleToggleLabel}
          onClearLabels={handleClearLabels}
        />
      </form>

      {/* Loading State */}
      {isLoading && (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      )}

      {/* Error State */}
      {error && (
        <ErrorMessage
          className="mb-4"
          message={`Error performing search: ${error instanceof Error ? error.message : 'An error occurred'}`}
        />
      )}

      {/* No Results */}
      {noResults && (
        <EmptyState
          message={urlQuery ? `No books or authors found for "${urlQuery}"` : 'No books or authors found'}
        />
      )}

      {/* Search Results */}
      {hasResults && (
        <div className="space-y-8">
          {/* Books Results */}
          {data.books.length > 0 && (
            <div data-test="search-results-books">
              <div className="flex items-center gap-2 mb-4">
                <PiBook className="w-6 h-6 text-blue-600" />
                <h2 className="text-2xl font-bold text-gray-900">
                  Books
                  <span className="ml-2 text-base font-normal text-gray-500">
                    ({data.bookPage.totalElements} {data.bookPage.totalElements === 1 ? 'result' : 'results'})
                  </span>
                </h2>
              </div>

              <PageCard padding={false} className="overflow-hidden">
                <div className="divide-y divide-gray-200">
                  {data.books.map((book) => (
                    <BookResult
                      key={book.id}
                      book={book}
                      isLibrarian={isLibrarian}
                    />
                  ))}
                </div>

                {/* Books Pagination */}
                {data.bookPage.totalPages > 1 && (
                  <div className="px-4 py-3 bg-gray-50 border-t border-gray-200 flex items-center justify-between" data-test="search-pagination-books">
                    <div className="text-sm text-gray-700">
                      Page {data.bookPage.currentPage + 1} of {data.bookPage.totalPages}
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handlePageChange(urlPage - 1)}
                        disabled={urlPage === 0}
                        data-test="books-prev-page"
                      >
                        Previous
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handlePageChange(urlPage + 1)}
                        disabled={urlPage >= data.bookPage.totalPages - 1}
                        data-test="books-next-page"
                      >
                        Next
                      </Button>
                    </div>
                  </div>
                )}
              </PageCard>
            </div>
          )}

          {/* Authors Results */}
          {data.authors.length > 0 && (
            <div data-test="search-results-authors">
              <div className="flex items-center gap-2 mb-4">
                <PiUser className="w-6 h-6 text-purple-600" />
                <h2 className="text-2xl font-bold text-gray-900">
                  Authors
                  <span className="ml-2 text-base font-normal text-gray-500">
                    ({data.authorPage.totalElements} {data.authorPage.totalElements === 1 ? 'result' : 'results'})
                  </span>
                </h2>
              </div>

              <PageCard padding={false} className="overflow-hidden">
                <div className="divide-y divide-gray-200">
                  {data.authors.map((author) => (
                    <AuthorResult
                      key={author.id}
                      author={author}
                      isLibrarian={isLibrarian}
                    />
                  ))}
                </div>

                {/* Authors Pagination */}
                {data.authorPage.totalPages > 1 && (
                  <div className="px-4 py-3 bg-gray-50 border-t border-gray-200 flex items-center justify-between" data-test="search-pagination-authors">
                    <div className="text-sm text-gray-700">
                      Page {data.authorPage.currentPage + 1} of {data.authorPage.totalPages}
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handlePageChange(urlPage - 1)}
                        disabled={urlPage === 0}
                        data-test="authors-prev-page"
                      >
                        Previous
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handlePageChange(urlPage + 1)}
                        disabled={urlPage >= data.authorPage.totalPages - 1}
                        data-test="authors-next-page"
                      >
                        Next
                      </Button>
                    </div>
                  </div>
                )}
              </PageCard>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

// ─── Book Result Component ────────────────────────────────────────────────────

interface BookResultProps {
  book: BookDto
  isLibrarian: boolean
}

function BookResult({ book, isLibrarian }: BookResultProps) {
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [lookupResults, setLookupResults] = useState<LocLookupResultDto[]>([])
  const [showLookupResults, setShowLookupResults] = useState(false)
  const deleteBook = useDeleteBook()
  const cloneBook = useCloneBook()
  const lookupSingleBook = useLookupSingleBook()
  const toast = useToast()

  const handleDelete = async () => {
    try {
      await deleteBook.mutateAsync(book.id)
      setShowDeleteConfirm(false)
    } catch (error) {
      console.error('Failed to delete book:', error)
      toast.error('Failed to delete book')
    }
  }

  const handleClone = async () => {
    try {
      await cloneBook.mutateAsync(book.id)
    } catch (error) {
      console.error('Failed to clone book:', error)
      toast.error('Failed to clone book')
    }
  }

  const handleLookupLoc = async () => {
    try {
      const result = await lookupSingleBook.mutateAsync(book.id)
      setLookupResults([result])
      setShowLookupResults(true)
    } catch (error) {
      console.error('Failed to lookup LOC:', error)
      toast.error('Failed to look up LOC')
    }
  }

  const freeTextUrls = parseSpaceSeparatedUrls(book.freeTextUrl)

  return (
    <>
      <div className="p-4 hover:bg-gray-50 transition-colors" data-test={`book-result-${book.id}`}>
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
          <div className="flex-1">
            <h3 className="text-lg font-semibold text-gray-900">{book.title}</h3>
            <p className="text-gray-600 mt-1">by {book.author}</p>
            <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
              {book.publicationYear && <span>{book.publicationYear}</span>}
              {book.publisher && <span>{book.publisher}</span>}
              {book.library && <span className="font-medium">{book.library}</span>}
            </div>
            {book.locNumber && (
              <div className="mt-2 text-sm text-gray-500">
                <span className="font-medium">LOC:</span> {book.locNumber}
              </div>
            )}
          </div>
          <div className="flex flex-row flex-wrap sm:flex-col sm:items-end items-center gap-2 sm:gap-1">
            <StatusBadge tone={bookStatusTone(book.status)}>
              {formatBookStatus(book.status)}
            </StatusBadge>
            {(freeTextUrls.length > 0 || isValidUrl(book.grokipediaUrl)) && (
              <div className="flex gap-1">
                {freeTextUrls.map((url, index) => (
                  <IconButton
                    key={index}
                    href={url}
                    icon={<FreeTextIcon />}
                    label={`Free text: ${extractDomain(url)}`}
                    tone="success"
                    data-test={`book-result-free-text-${book.id}-${index}`}
                  />
                ))}
                {isValidUrl(book.grokipediaUrl) && (
                  <IconButton
                    href={book.grokipediaUrl}
                    icon={<GrokipediaIcon />}
                    label="View on Grokipedia"
                    tone="warning"
                    data-test={`book-result-grokipedia-${book.id}`}
                  />
                )}
              </div>
            )}
            <div className="flex gap-1 items-center">
              <IconButton
                to={`/books/${book.id}`}
                icon={<ViewIcon />}
                label="View Details"
                data-test={`book-result-view-${book.id}`}
              />
              {book.authorId && (
                <IconButton
                  to={isLibrarian ? `/authors/${book.authorId}/edit` : `/authors/${book.authorId}`}
                  icon={<AuthorIcon />}
                  label="See Author"
                  tone="info"
                  data-test={`book-result-author-${book.id}`}
                />
              )}
              {isLibrarian && (
                <IconButton
                  icon={<LocIcon />}
                  label="Lookup LOC"
                  tone="accent"
                  disabled={lookupSingleBook.isPending}
                  onClick={handleLookupLoc}
                  data-test={`book-result-lookup-loc-${book.id}`}
                />
              )}
            </div>
            {isLibrarian && (
              <div className="flex gap-1">
                <IconButton
                  icon={<CopyIcon />}
                  label="Clone"
                  tone="success"
                  disabled={cloneBook.isPending}
                  onClick={handleClone}
                  data-test={`book-result-clone-${book.id}`}
                />
                <IconButton
                  to={`/books/${book.id}/edit`}
                  icon={<EditIcon />}
                  label="Edit"
                  tone="primary"
                  data-test={`book-result-edit-${book.id}`}
                />
                <IconButton
                  icon={<DeleteIcon />}
                  label="Delete"
                  tone="danger"
                  onClick={() => setShowDeleteConfirm(true)}
                  data-test={`book-result-delete-${book.id}`}
                />
              </div>
            )}
          </div>
        </div>
      </div>

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
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

// ─── Author Result Component ──────────────────────────────────────────────────

interface AuthorResultProps {
  author: AuthorDto
  isLibrarian: boolean
}

function AuthorResult({ author, isLibrarian }: AuthorResultProps) {
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const deleteAuthor = useDeleteAuthor()
  const toast = useToast()

  const handleDelete = async () => {
    try {
      await deleteAuthor.mutateAsync(author.id)
      setShowDeleteConfirm(false)
    } catch (error) {
      console.error('Failed to delete author:', error)
      toast.error('Failed to delete author')
    }
  }

  return (
    <>
      <div className="p-4 hover:bg-gray-50 transition-colors" data-test={`author-result-${author.id}`}>
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
          <div className="flex-1">
            <h3 className="text-lg font-semibold text-gray-900">
              {author.name}
            </h3>
            {(author.dateOfBirth || author.dateOfDeath) && (
              <p className="text-gray-600 mt-1">
                {author.dateOfBirth && <span>{author.dateOfBirth.split('-')[0]}</span>}
                {author.dateOfBirth && author.dateOfDeath && <span> - </span>}
                {author.dateOfDeath && <span>{author.dateOfDeath.split('-')[0]}</span>}
              </p>
            )}
            {author.briefBiography && (
              <p className="text-gray-700 mt-2 line-clamp-2">{author.briefBiography}</p>
            )}
          </div>
          <div className="flex flex-row flex-wrap sm:flex-col sm:items-end items-center gap-2 sm:gap-1">
            {author.bookCount !== undefined && (
              <StatusBadge tone="emphasis">
                {author.bookCount} {author.bookCount === 1 ? 'book' : 'books'}
              </StatusBadge>
            )}
            <div className="flex items-center gap-1">
              <IconButton
                to={`/authors/${author.id}`}
                icon={<ViewIcon />}
                label="View Details"
                data-test={`author-result-view-${author.id}`}
              />
              {isValidUrl(author.grokipediaUrl) && (
                <IconButton
                  href={author.grokipediaUrl}
                  icon={<GrokipediaIcon />}
                  label="View on Grokipedia"
                  tone="warning"
                  data-test={`author-result-grokipedia-${author.id}`}
                />
              )}
              <IconButton
                to={`/authors/${author.id}`}
                icon={<BooksIcon />}
                label="See Books"
                tone="info"
                data-test={`author-result-see-books-${author.id}`}
              />
              {isLibrarian && (
                <>
                  <IconButton
                    to={`/authors/${author.id}/edit`}
                    icon={<EditIcon />}
                    label="Edit"
                    tone="primary"
                    data-test={`author-result-edit-${author.id}`}
                  />
                  <IconButton
                    icon={<DeleteIcon />}
                    label="Delete"
                    tone="danger"
                    onClick={() => setShowDeleteConfirm(true)}
                    data-test={`author-result-delete-${author.id}`}
                  />
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete Author"
        message="Are you sure you want to delete this author? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteAuthor.isPending}
      />
    </>
  )
}
