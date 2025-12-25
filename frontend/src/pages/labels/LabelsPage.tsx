// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { Spinner } from '@/components/progress/Spinner'
import { useBooksForLabels, generateLabelsPdf, type BookLocStatusDto } from '@/api/labels'
import { PiFilePdf, PiCheckCircle, PiXCircle } from 'react-icons/pi'

export function LabelsPage() {
  const [filter, setFilter] = useState<'most-recent' | 'all'>('most-recent')
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [selectAll, setSelectAll] = useState(false)
  const [isGenerating, setIsGenerating] = useState(false)

  const { data: books = [], isLoading } = useBooksForLabels(filter)

  const handleSelectToggle = (id: number) => {
    const newSelected = new Set(selectedIds)
    if (newSelected.has(id)) {
      newSelected.delete(id)
    } else {
      newSelected.add(id)
    }
    setSelectedIds(newSelected)
  }

  const handleSelectAll = () => {
    if (selectAll) {
      setSelectedIds(new Set())
      setSelectAll(false)
    } else {
      const allIds = new Set(books.map((b) => b.id))
      setSelectedIds(allIds)
      setSelectAll(true)
    }
  }

  const handleGeneratePdf = async () => {
    if (selectedIds.size === 0) return

    setIsGenerating(true)
    try {
      const blob = await generateLabelsPdf(Array.from(selectedIds))

      // Create download link
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'book-labels.pdf'
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      console.error('Failed to generate labels PDF:', error)
      alert('Failed to generate labels PDF. Please try again.')
    } finally {
      setIsGenerating(false)
    }
  }

  const columns: Column<BookLocStatusDto>[] = [
    {
      key: 'title',
      header: 'Title',
      accessor: (book) => (
        <div>
          <div className="font-medium text-gray-900">{book.title}</div>
          <div className="text-sm text-gray-500">by {book.author}</div>
        </div>
      ),
      width: '40%',
    },
    {
      key: 'publicationYear',
      header: 'Year',
      accessor: (book) => (
        <span className="text-gray-700">{book.publicationYear || '-'}</span>
      ),
      width: '10%',
    },
    {
      key: 'currentLocNumber',
      header: 'LOC Call Number',
      accessor: (book) => (
        <div className="flex items-center gap-2">
          {book.hasLocNumber ? (
            <>
              <PiCheckCircle className="w-5 h-5 text-green-600" />
              <span className="font-mono text-sm text-gray-900">{book.currentLocNumber}</span>
            </>
          ) : (
            <>
              <PiXCircle className="w-5 h-5 text-red-600" />
              <span className="text-gray-400 text-sm">No LOC number</span>
            </>
          )}
        </div>
      ),
      width: '40%',
    },
    {
      key: 'dateAdded',
      header: 'Date Added',
      accessor: (book) => (
        <span className="text-sm text-gray-500">
          {book.dateAdded ? new Date(book.dateAdded).toLocaleDateString() : '-'}
        </span>
      ),
      width: '10%',
    },
  ]

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Book Labels</h1>
          <p className="text-gray-600 mt-1">
            Generate PDF labels for book spines with LOC call numbers
          </p>
        </div>
      </div>

      {/* Filter Radio Buttons */}
      <div className="bg-white rounded-lg shadow p-4 mb-4">
        <div className="flex items-center gap-6">
          <span className="text-sm font-medium text-gray-700">Filter:</span>
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="radio"
              name="labelFilter"
              value="most-recent"
              checked={filter === 'most-recent'}
              onChange={() => {
                setFilter('most-recent')
                setSelectedIds(new Set())
                setSelectAll(false)
              }}
              className="text-blue-600 focus:ring-blue-500"
              data-test="filter-most-recent"
            />
            <span className="text-sm text-gray-700">Most Recent Day</span>
          </label>
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="radio"
              name="labelFilter"
              value="all"
              checked={filter === 'all'}
              onChange={() => {
                setFilter('all')
                setSelectedIds(new Set())
                setSelectAll(false)
              }}
              className="text-blue-600 focus:ring-blue-500"
              data-test="filter-all"
            />
            <span className="text-sm text-gray-700">All Books</span>
          </label>
        </div>
      </div>

      {/* Bulk Actions */}
      {selectedIds.size > 0 && (
        <div className="mb-4 bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <span className="text-sm font-medium text-blue-900">
                {selectedIds.size} book{selectedIds.size === 1 ? '' : 's'} selected
              </span>
              <span className="text-sm text-blue-700">
                {Array.from(selectedIds).filter((id) => {
                  const book = books.find((b) => b.id === id)
                  return book?.hasLocNumber
                }).length}{' '}
                with LOC numbers
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSelectedIds(new Set())
                  setSelectAll(false)
                }}
              >
                Clear
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleGeneratePdf}
                isLoading={isGenerating}
                leftIcon={<PiFilePdf />}
                data-test="generate-labels-pdf"
              >
                Generate Labels PDF
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Books Table */}
      <div className="bg-white rounded-lg shadow">
        {isLoading ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : (
          <>
            <div className="p-4">
              <DataTable
                data={books}
                columns={columns}
                keyExtractor={(book) => book.id}
                selectable
                selectedIds={selectedIds}
                onSelectToggle={handleSelectToggle}
                onSelectAll={handleSelectAll}
                selectAll={selectAll}
                isLoading={isLoading}
                emptyMessage="No books found"
              />
            </div>

            {!isLoading && books.length > 0 && (
              <div className="px-4 py-3 border-t border-gray-200 bg-gray-50">
                <div className="flex items-center justify-between text-sm text-gray-700">
                  <p>
                    Showing {books.length} {books.length === 1 ? 'book' : 'books'}
                  </p>
                  <p>
                    {books.filter((b) => b.hasLocNumber).length} with LOC numbers
                  </p>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Help Text */}
      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="text-sm font-medium text-blue-900 mb-2">How to use:</h3>
        <ol className="text-sm text-blue-800 space-y-1 list-decimal list-inside">
          <li>Select books using the checkboxes (only books with LOC numbers will have useful labels)</li>
          <li>Click "Generate Labels PDF" to create a printable PDF</li>
          <li>Print the PDF on label sheets</li>
          <li>Cut and apply labels to book spines</li>
        </ol>
      </div>
    </div>
  )
}
