// (c) Copyright 2025 by Muczynski
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PiMagnifyingGlass } from 'react-icons/pi'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TableSummary } from '@/components/table/TableSummary'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { BookFilters } from './components/BookFilters'
import { BookLabelFilters } from './components/BookLabelFilters'
import { BookTable } from './components/BookTable'
import { BulkActionsToolbar } from './components/BulkActionsToolbar'
import { useBookCount, useBooks } from '@/api/books'
import { useUiStore, useBooksTableSelection } from '@/stores/uiStore'
import { applyChipFilters } from '@/utils/bookChipFilters'
import {
  bookFilterParamsForUrl,
  chipsFromSearchParams,
  isBooksIntakeConstrained,
  labelsFromSearchParams,
  matchesBookQuery,
} from '@/utils/bookFilterParams'
import { useIsLibrarian } from '@/stores/authStore'
import type { BookChipFilters } from '@/utils/bookChipFilters'
import type { BookDto } from '@/types/dtos'

export function BooksPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const chips = chipsFromSearchParams(searchParams, 'books')
  const selectedLabels = labelsFromSearchParams(searchParams)
  const urlQuery = searchParams.get('q') ?? ''
  const [inputValue, setInputValue] = useState(urlQuery)
  const { selectedIds, selectAll } = useBooksTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds } = useUiStore()
  const isLibrarian = useIsLibrarian()

  useEffect(() => {
    setInputValue(urlQuery)
  }, [urlQuery])

  const writeUrl = (next: { chips?: BookChipFilters; labels?: string[]; q?: string }) => {
    setSearchParams(
      bookFilterParamsForUrl(
        {
          chips: next.chips ?? chips,
          labels: next.labels ?? selectedLabels,
          q: next.q !== undefined ? next.q : urlQuery,
        },
        'books',
      ),
    )
  }

  const { data: allBooks = [], isLoading, isFetching, error } = useBooks(selectedLabels, chips.mostRecent)
  const { data: bookCount } = useBookCount()

  const books = useMemo(
    () => applyChipFilters(allBooks, chips).filter((book) => matchesBookQuery(book, urlQuery)),
    [allBooks, chips, urlQuery],
  )

  const intakeConstrained = isBooksIntakeConstrained(chips, selectedLabels, urlQuery)

  const handleSelectToggle = (id: number) => {
    toggleRowSelection('booksTable', id)
  }

  const handleSelectAll = () => {
    if (selectAll) {
      clearSelection('booksTable')
    } else {
      const allIds = new Set(books.map((b) => b.id))
      setSelectedIds('booksTable', allIds)
      toggleSelectAll('booksTable')
    }
  }

  const handleClearSelection = () => {
    clearSelection('booksTable')
  }

  const handleAddBook = () => {
    navigate('/books/new')
  }

  const handleViewBook = (book: BookDto) => {
    navigate(`/books/${book.id}`)
  }

  const handleToggleChip = (key: keyof BookChipFilters) => {
    if (key === 'mostRecent' && intakeConstrained) return
    writeUrl({ chips: { ...chips, [key]: !chips[key] } })
  }

  const handleToggleLabel = (label: string) => {
    const nextLabels = selectedLabels.includes(label)
      ? selectedLabels.filter((l) => l !== label)
      : [...selectedLabels, label]
    writeUrl({ labels: nextLabels })
  }

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    writeUrl({ q: inputValue.trim() })
  }

  return (
    <div>
      <PageHeader
        title="Books"
        actions={
          isLibrarian ? (
            <>
              <Link to="/books-from-feed">
                <Button variant="outline" data-test="books-from-feed">
                  Books from Feed
                </Button>
              </Link>
              <Button variant="primary" onClick={handleAddBook} data-test="add-book">
                Add Book
              </Button>
            </>
          ) : undefined
        }
      />

      {error && (
        <ErrorMessage message={`Error loading books: ${error.message}`} className="mb-4" />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200 space-y-3">
          <form onSubmit={handleSearch} className="flex gap-2 items-start">
            <div className="flex-1">
              <Input
                type="search"
                label="Filter by title or author"
                hideLabel
                placeholder="Filter by title or author..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                data-test="books-title-filter"
              />
            </div>
            <Button
              type="submit"
              variant="primary"
              leftIcon={<PiMagnifyingGlass />}
              data-test="books-search-button"
            >
              Search
            </Button>
          </form>
          <BookFilters
            chips={chips}
            onToggle={handleToggleChip}
            showAvailabilityFilters
            mostRecentDisabled={intakeConstrained}
          />
          <BookLabelFilters
            selectedLabels={selectedLabels}
            onToggleLabel={handleToggleLabel}
            onClearLabels={() => writeUrl({ labels: [] })}
          />
        </div>

        <div className="p-4">
          <BulkActionsToolbar
            selectedIds={selectedIds}
            onClearSelection={handleClearSelection}
            tableCount={books.length}
            totalCount={bookCount?.count}
            isLoading={isLoading}
          />

          <BookTable
            books={books}
            isLoading={isLoading}
            selectedIds={selectedIds}
            selectAll={selectAll}
            onSelectToggle={handleSelectToggle}
            onSelectAll={handleSelectAll}
            onView={handleViewBook}
          />
        </div>

        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary count={books.length} singular="book" plural="books" isLoading={isLoading} />
      </PageCard>

    </div>
  )
}
