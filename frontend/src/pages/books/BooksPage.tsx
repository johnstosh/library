// (c) Copyright 2025 by Muczynski
import { useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { BookFilters } from './components/BookFilters'
import { BookLabelFilters } from './components/BookLabelFilters'
import { BookTable } from './components/BookTable'
import { BulkActionsToolbar } from './components/BulkActionsToolbar'
import { useBooks } from '@/api/books'
import { useUiStore, useBooksChips, useBooksLabelFilter, useBooksTableSelection } from '@/stores/uiStore'
import { useIsLibrarian } from '@/stores/authStore'
import type { BookDto } from '@/types/dtos'

// ─── Client-side filter helpers ───────────────────────────────────────────────

/** True if locNumber starts with exactly three uppercase letters. */
function is3LetterLoc(locNumber: string | null | undefined): boolean {
  if (!locNumber) return false
  return /^[A-Z]{3}/.test(locNumber.substring(0, 3))
}

/**
 * Apply all active chip filters to the book list (AND logic).
 * A book must satisfy every active chip to be included.
 * The "Most Recent Day" chip mirrors the backend query:
 *   DATE(dateAddedToLibrary) >= max_date - 1 day  OR  temp-title regex.
 */
function applyChipFilters(books: BookDto[], chips: ReturnType<typeof useBooksChips>): BookDto[] {
  // Compute max date for mostRecent chip
  let maxDate: Date | null = null
  if (chips.mostRecent) {
    for (const b of books) {
      if (b.dateAddedToLibrary) {
        const d = new Date(b.dateAddedToLibrary)
        if (!maxDate || d > maxDate) maxDate = d
      }
    }
  }
  // Start of (maxDate - 1 day) in UTC
  let cutoff: Date | null = null
  if (maxDate) {
    cutoff = new Date(maxDate)
    cutoff.setUTCHours(0, 0, 0, 0)
    cutoff.setUTCDate(cutoff.getUTCDate() - 1)
  }

  const TEMP_TITLE_RE = /^\d{4}-\d{1,2}-\d{1,2}/

  return books.filter((book) => {
    // ── Row 1: search-style type chips ───────────────────────────────────
    if (chips.inLibrary && (!book.locNumber || book.locNumber.trim() === '')) return false
    if (chips.electronic && !book.electronicResource) return false
    if (chips.freeText && (!book.freeTextUrl || book.freeTextUrl.trim() === '')) return false
    if (chips.audio) {
      if (!book.freeTextUrl || !book.freeTextUrl.toLowerCase().includes('librivox')) return false
    }

    // ── Row 2: books-specific chips ──────────────────────────────────────
    if (chips.mostRecent) {
      const isTempTitle = TEMP_TITLE_RE.test(book.title ?? '')
      if (!isTempTitle) {
        if (!book.dateAddedToLibrary) return false
        const bookDate = new Date(book.dateAddedToLibrary)
        if (!cutoff || bookDate < cutoff) return false
      }
    }
    if (chips.withoutLoc && book.locNumber && book.locNumber.trim() !== '') return false
    if (chips.threeLetterLoc && !is3LetterLoc(book.locNumber)) return false
    if (chips.withoutGrokipedia && book.grokipediaUrl && book.grokipediaUrl.trim() !== '') return false
    if (chips.withoutGenres && book.tagsList && book.tagsList.length > 0) return false
    if (chips.notActiveStatus && book.status === 'ACTIVE') return false
    if (chips.withoutFreeTextUrls && book.freeTextUrl && book.freeTextUrl.trim() !== '') return false

    return true
  })
}

// ─── BooksPage ────────────────────────────────────────────────────────────────

export function BooksPage() {
  const navigate = useNavigate()
  const chips = useBooksChips()
  const selectedLabels = useBooksLabelFilter()
  const { selectedIds, selectAll } = useBooksTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds, toggleBooksLabel, clearBooksLabels } = useUiStore()
  const isLibrarian = useIsLibrarian()

  const { data: allBooks = [], isLoading, isFetching } = useBooks(selectedLabels)

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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6 gap-3">
        <h1 className="text-3xl font-bold text-gray-900">Books</h1>
        {isLibrarian && (
          <div className="flex gap-2">
            <Link to="/books-from-feed">
              <Button variant="outline" data-test="books-from-feed">
                Books from Feed
              </Button>
            </Link>
            <Button variant="primary" onClick={handleAddBook} data-test="add-book">
              Add Book
            </Button>
          </div>
        )}
      </div>

      <div className="bg-white rounded-lg shadow relative">
        <div className="p-4 border-b border-gray-200">
          <BookFilters />
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

        {isFetching && (
          <div className="absolute inset-0 flex items-center justify-center bg-white/60">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
          </div>
        )}

        {!isLoading && books.length > 0 && (
          <div className="px-4 py-3 border-t border-gray-200 bg-gray-50">
            <p className="text-sm text-gray-700">
              Showing {books.length} {books.length === 1 ? 'book' : 'books'}
            </p>
          </div>
        )}
      </div>

    </div>
  )
}
