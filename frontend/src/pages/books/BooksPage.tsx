// (c) Copyright 2025 by Muczynski
import { useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
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
import { useUiStore, useBooksChips, useBooksLabelFilter, useBooksTableSelection } from '@/stores/uiStore'
import { useIsLibrarian } from '@/stores/authStore'
import { applyChipFilters, isOtherBookChipActive } from '@/utils/bookChipFilters'
import type { BookDto } from '@/types/dtos'

// ─── BooksPage ────────────────────────────────────────────────────────────────

export function BooksPage() {
  const navigate = useNavigate()
  const chips = useBooksChips()
  const selectedLabels = useBooksLabelFilter()
  const { selectedIds, selectAll } = useBooksTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds, toggleBooksLabel, clearBooksLabels, toggleBooksChip } = useUiStore()
  const isLibrarian = useIsLibrarian()

  // mostRecent defaults on in uiStore so this uses GET /books/most-recent-day
  // (faster than GET /books/summaries). Remaining chips still apply client-side.
  const { data: allBooks = [], isLoading, isFetching, error } = useBooks(selectedLabels, chips.mostRecent)
  const { data: bookCount } = useBookCount()

  // Apply all chip filters client-side (AND logic)
  const books = useMemo(() => applyChipFilters(allBooks, chips), [allBooks, chips])

  const handleSelectToggle = (id: number) => {
    toggleRowSelection('booksTable', id)
  }

  const handleSelectAll = () => {
    if (selectAll) {
      // Deselect all
      clearSelection('booksTable')
    } else {
      // Select all visible books
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
        <div className="p-4 border-b border-gray-200">
          <BookFilters
            chips={chips}
            onToggle={toggleBooksChip}
            showAvailabilityFilters
            mostRecentDisabled={isOtherBookChipActive(chips) || selectedLabels.length > 0}
          />
          <BookLabelFilters
            selectedLabels={selectedLabels}
            onToggleLabel={toggleBooksLabel}
            onClearLabels={clearBooksLabels}
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
