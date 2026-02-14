// (c) Copyright 2025 by Muczynski
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { BookFilters } from './components/BookFilters'
import { BookTable } from './components/BookTable'
import { BulkActionsToolbar } from './components/BulkActionsToolbar'
import { useBooks } from '@/api/books'
import { useUiStore, useBooksFilter, useBooksTableSelection } from '@/stores/uiStore'
import { useIsLibrarian } from '@/stores/authStore'
import type { BookDto } from '@/types/dtos'

export function BooksPage() {
  const navigate = useNavigate()
  const filter = useBooksFilter()
  const { selectedIds, selectAll } = useBooksTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds } = useUiStore()
  const isLibrarian = useIsLibrarian()

  const { data: books = [], isLoading, isFetching } = useBooks(filter)

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

  const handleEditBook = (book: BookDto) => {
    navigate(`/books/${book.id}/edit`)
  }

  const handleViewBook = (book: BookDto) => {
    navigate(`/books/${book.id}`)
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
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

      <div className="bg-white rounded-lg shadow">
        <div className="p-4 border-b border-gray-200">
          <BookFilters />
        </div>

        <div className="p-4">
          <BulkActionsToolbar
            selectedIds={selectedIds}
            onClearSelection={handleClearSelection}
          />

          <BookTable
            books={books}
            isLoading={isLoading || isFetching}
            selectedIds={selectedIds}
            selectAll={selectAll}
            onSelectToggle={handleSelectToggle}
            onSelectAll={handleSelectAll}
            onEdit={handleEditBook}
            onView={handleViewBook}
          />
        </div>

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
