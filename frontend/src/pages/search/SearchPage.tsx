// (c) Copyright 2025 by Muczynski
import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/progress/Spinner'
import { useSearch } from '@/api/search'
import { formatBookStatus } from '@/utils/formatters'
import { PiMagnifyingGlass, PiBook, PiUser } from 'react-icons/pi'
import { useIsLibrarian } from '@/stores/authStore'
import type { BookDto, AuthorDto } from '@/types/dtos'

export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const urlQuery = searchParams.get('q') || ''
  const urlPage = parseInt(searchParams.get('page') || '0', 10)

  // Local input state (for typing before submit)
  const [inputValue, setInputValue] = useState(urlQuery)
  const pageSize = 20

  // Sync input value when URL changes (e.g., browser back/forward)
  useEffect(() => {
    setInputValue(urlQuery)
  }, [urlQuery])

  const { data, isLoading, error } = useSearch(urlQuery, urlPage, pageSize)
  const isLibrarian = useIsLibrarian()

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (inputValue.trim()) {
      setSearchParams({ q: inputValue.trim() })
    }
  }

  const handleClear = () => {
    setInputValue('')
    setSearchParams({})
  }

  const handlePageChange = (newPage: number) => {
    const params: Record<string, string> = { q: urlQuery }
    if (newPage > 0) {
      params.page = String(newPage)
    }
    setSearchParams(params)
  }

  const hasResults = data && (data.books.length > 0 || data.authors.length > 0)
  const noResults = urlQuery && data && data.books.length === 0 && data.authors.length === 0

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
            disabled={!inputValue.trim() || isLoading}
            leftIcon={<PiMagnifyingGlass />}
            data-test="search-button"
          >
            Search
          </Button>
          {urlQuery && (
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
            No books or authors found for "{urlQuery}"
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

// Book Result Component
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

// Author Result Component
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
