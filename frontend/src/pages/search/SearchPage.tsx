// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/progress/Spinner'
import { useSearch } from '@/api/search'
import { formatBookStatus } from '@/utils/formatters'
import { PiMagnifyingGlass, PiBook, PiUser } from 'react-icons/pi'
import type { BookDto, AuthorDto } from '@/types/dtos'

export function SearchPage() {
  const [query, setQuery] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [page, setPage] = useState(0)
  const pageSize = 20

  const { data, isLoading, error } = useSearch(searchQuery, page, pageSize)

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (query.trim()) {
      setSearchQuery(query.trim())
      setPage(0)
    }
  }

  const handleClear = () => {
    setQuery('')
    setSearchQuery('')
    setPage(0)
  }

  const hasResults = data && (data.books.length > 0 || data.authors.length > 0)
  const noResults = searchQuery && data && data.books.length === 0 && data.authors.length === 0

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
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Enter book title or author name..."
              data-test="search-input"
              className="text-lg"
            />
          </div>
          <Button
            type="submit"
            variant="primary"
            size="lg"
            disabled={!query.trim() || isLoading}
            leftIcon={<PiMagnifyingGlass />}
            data-test="search-button"
          >
            Search
          </Button>
          {searchQuery && (
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
            No books or authors found for "{searchQuery}"
          </p>
        </div>
      )}

      {/* Search Results */}
      {hasResults && (
        <div className="space-y-8">
          {/* Books Results */}
          {data.books.length > 0 && (
            <div>
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
                    <BookResult key={book.id} book={book} />
                  ))}
                </div>

                {/* Books Pagination */}
                {data.bookPage.totalPages > 1 && (
                  <div className="px-4 py-3 bg-gray-50 border-t border-gray-200 flex items-center justify-between">
                    <div className="text-sm text-gray-700">
                      Page {data.bookPage.currentPage + 1} of {data.bookPage.totalPages}
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setPage(page - 1)}
                        disabled={page === 0}
                      >
                        Previous
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setPage(page + 1)}
                        disabled={page >= data.bookPage.totalPages - 1}
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
            <div>
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
                    <AuthorResult key={author.id} author={author} />
                  ))}
                </div>

                {/* Authors Pagination */}
                {data.authorPage.totalPages > 1 && (
                  <div className="px-4 py-3 bg-gray-50 border-t border-gray-200 flex items-center justify-between">
                    <div className="text-sm text-gray-700">
                      Page {data.authorPage.currentPage + 1} of {data.authorPage.totalPages}
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setPage(page - 1)}
                        disabled={page === 0}
                      >
                        Previous
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setPage(page + 1)}
                        disabled={page >= data.authorPage.totalPages - 1}
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
function BookResult({ book }: { book: BookDto }) {
  const statusColors = {
    AVAILABLE: 'bg-green-100 text-green-800',
    CHECKED_OUT: 'bg-blue-100 text-blue-800',
    LOST: 'bg-red-100 text-red-800',
    DAMAGED: 'bg-orange-100 text-orange-800',
  }

  return (
    <div className="p-4 hover:bg-gray-50 transition-colors" data-test={`book-result-${book.id}`}>
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">{book.title}</h3>
          <p className="text-gray-600 mt-1">by {book.authorName}</p>
          <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
            {book.publicationYear && <span>{book.publicationYear}</span>}
            {book.publisher && <span>{book.publisher}</span>}
            {book.libraryName && <span className="font-medium">{book.libraryName}</span>}
          </div>
          {book.locCallNumber && (
            <div className="mt-2 text-sm text-gray-500">
              <span className="font-medium">LOC:</span> {book.locCallNumber}
            </div>
          )}
        </div>
        <div>
          <span
            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
              statusColors[book.status as keyof typeof statusColors] || 'bg-gray-100 text-gray-800'
            }`}
          >
            {formatBookStatus(book.status)}
          </span>
        </div>
      </div>
    </div>
  )
}

// Author Result Component
function AuthorResult({ author }: { author: AuthorDto }) {
  return (
    <div className="p-4 hover:bg-gray-50 transition-colors" data-test={`author-result-${author.id}`}>
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">
            {author.firstName} {author.lastName}
          </h3>
          {(author.birthDate || author.deathDate) && (
            <p className="text-gray-600 mt-1">
              {author.birthDate && <span>{author.birthDate.split('-')[0]}</span>}
              {author.birthDate && author.deathDate && <span> - </span>}
              {author.deathDate && <span>{author.deathDate.split('-')[0]}</span>}
            </p>
          )}
          {author.briefBiography && (
            <p className="text-gray-700 mt-2 line-clamp-2">{author.briefBiography}</p>
          )}
        </div>
        {author.bookCount !== undefined && (
          <div className="ml-4">
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
              {author.bookCount} {author.bookCount === 1 ? 'book' : 'books'}
            </span>
          </div>
        )}
      </div>
    </div>
  )
}
