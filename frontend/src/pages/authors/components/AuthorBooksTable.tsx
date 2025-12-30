// (c) Copyright 2025 by Muczynski
import { useNavigate } from 'react-router-dom'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { formatBookStatus } from '@/utils/formatters'
import type { BookDto } from '@/types/dtos'

interface AuthorBooksTableProps {
  books: BookDto[]
}

export function AuthorBooksTable({ books }: AuthorBooksTableProps) {
  const navigate = useNavigate()

  const columns: Column<BookDto>[] = [
    {
      key: 'title',
      header: 'Title',
      accessor: (book) => (
        <div>
          <div className="font-medium text-gray-900">{book.title}</div>
          {book.library && <div className="text-sm text-gray-500">{book.library}</div>}
        </div>
      ),
      width: '45%',
    },
    {
      key: 'locCallNumber',
      header: 'LOC',
      accessor: (book) => book.locNumber || '—',
      width: '20%',
    },
    {
      key: 'status',
      header: 'Status',
      accessor: (book) => (
        <span
          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
            book.status === 'ACTIVE'
              ? 'bg-green-100 text-green-800'
              : book.status === 'ON_ORDER'
              ? 'bg-blue-100 text-blue-800'
              : 'bg-red-100 text-red-800'
          }`}
        >
          {formatBookStatus(book.status)}
        </span>
      ),
      width: '15%',
    },
  ]

  const handleRowClick = (book: BookDto) => {
    navigate(`/books/${book.id}`)
  }

  if (!books || books.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500" data-test="author-books-empty">
        No books found for this author.
      </div>
    )
  }

  return (
    <div data-test="author-books-table">
      <DataTable
        data={books}
        columns={columns}
        keyExtractor={(book) => book.id}
        selectable={false}
        onRowClick={handleRowClick}
        isLoading={false}
        emptyMessage="No books found for this author."
      />
    </div>
  )
}
