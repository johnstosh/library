// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { BookFilters } from './components/BookFilters'
import { BookTable } from './components/BookTable'
import { BookForm } from './components/BookForm'
import { BulkActionsToolbar } from './components/BulkActionsToolbar'
import { useBooks } from '@/api/books'
import { useUiStore, useBooksFilter, useBooksTableSelection } from '@/stores/uiStore'
import type { BookDto } from '@/types/dtos'

export function BooksPage() {
  const [showForm, setShowForm] = useState(false)
  const [editingBook, setEditingBook] = useState<BookDto | null>(null)

  const filter = useBooksFilter()
  const { selectedIds, selectAll } = useBooksTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds } = useUiStore()

  const { data: books = [], isLoading } = useBooks(filter)

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
    setEditingBook(null)
    setShowForm(true)
  }

  const handleEditBook = (book: BookDto) => {
    setEditingBook(book)
    setShowForm(true)
  }

  const handleViewBook = (book: BookDto) => {
    // For now, just open edit form in view mode
    // Later this can navigate to a dedicated view page
    console.log('View book:', book)
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setEditingBook(null)
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Books</h1>
        <Button variant="primary" onClick={handleAddBook} data-test="add-book">
          Add Book
        </Button>
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
            isLoading={isLoading}
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

      <BookForm isOpen={showForm} onClose={handleCloseForm} book={editingBook} />
    </div>
  )
}
