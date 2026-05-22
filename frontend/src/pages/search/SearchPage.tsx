// (c) Copyright 2025 by Muczynski
import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/progress/Spinner'
import { useSearch, type SearchFilters } from '@/api/search'
import { BookLabelFilters } from '@/pages/books/components/BookLabelFilters'
import { formatBookStatus } from '@/utils/formatters'
import { PiMagnifyingGlass, PiBook, PiUser, PiFunnel } from 'react-icons/pi'
import { useIsLibrarian } from '@/stores/authStore'
import type { BookDto, AuthorDto } from '@/types/dtos'

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
        <svg className="w-3.5 h-3.5 text-blue-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      ) : (
        <PiFunnel className="w-3.5 h-3.5 text-gray-400 shrink-0" />
      )}
      {label}
      <span className="text-gray-400 text-xs shrink-0" aria-hidden="true">ⓘ</span>
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
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Search Library</h1>
        <p className="text-gray-600">Search for books and authors by title or name</p>
      </div>

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
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-800">
          <p className="font-medium">Error performing search</p>
          <p className="text-sm mt-1">{error instanceof Error ? error.message : 'An error occurred'}</p>
        </div>
      )}

      {/* No Results */}
      {noResults && (
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-8 text-center">
          <p className="text-gray-600 text-lg">
            {urlQuery ? `No books or authors found for "${urlQuery}"` : 'No books or authors found'}
          </p>
        </div>
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

              <div className="bg-white rounded-lg shadow overflow-hidden">
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
              </div>
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

              <div className="bg-white rounded-lg shadow overflow-hidden">
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
              </div>
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
  const statusColors = {
    AVAILABLE: 'bg-green-100 text-green-800',
    CHECKED_OUT: 'bg-blue-100 text-blue-800',
    LOST: 'bg-red-100 text-red-800',
    DAMAGED: 'bg-orange-100 text-orange-800',
  }

  return (
    <div className="p-4 hover:bg-gray-50 transition-colors" data-test={`book-result-${book.id}`}>
      <div className="flex items-start justify-between gap-4">
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
        <div className="flex items-center gap-4">
          <span
            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
              statusColors[book.status as keyof typeof statusColors] || 'bg-gray-100 text-gray-800'
            }`}
          >
            {formatBookStatus(book.status)}
          </span>
          <div className="flex items-center gap-2">
            <Link
              to={`/books/${book.id}`}
              className="text-gray-600 hover:text-gray-900"
              data-test={`book-result-view-${book.id}`}
              title="View Details"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </Link>
            {isLibrarian && (
              <Link
                to={`/books/${book.id}/edit`}
                className="text-blue-600 hover:text-blue-900"
                data-test={`book-result-edit-${book.id}`}
                title="Edit"
              >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

// ─── Author Result Component ──────────────────────────────────────────────────

interface AuthorResultProps {
  author: AuthorDto
  isLibrarian: boolean
}

function AuthorResult({ author, isLibrarian }: AuthorResultProps) {
  return (
    <div className="p-4 hover:bg-gray-50 transition-colors" data-test={`author-result-${author.id}`}>
      <div className="flex items-start justify-between gap-4">
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
        <div className="flex items-center gap-4">
          {author.bookCount !== undefined && (
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
              {author.bookCount} {author.bookCount === 1 ? 'book' : 'books'}
            </span>
          )}
          <div className="flex items-center gap-2">
            <Link
              to={`/authors/${author.id}`}
              className="text-gray-600 hover:text-gray-900"
              data-test={`author-result-view-${author.id}`}
              title="View Details"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </Link>
            {isLibrarian && (
              <Link
                to={`/authors/${author.id}/edit`}
                className="text-blue-600 hover:text-blue-900"
                data-test={`author-result-edit-${author.id}`}
                title="Edit"
              >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
