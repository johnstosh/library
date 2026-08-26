// (c) Copyright 2025 by Muczynski
import { useNavigate } from 'react-router-dom'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { formatBookStatus } from '@/utils/formatters'
import type { BookDto } from '@/types/dtos'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { bookStatusTone } from '@/utils/status'
import { EmptyState } from '@/components/ui/EmptyState'
import { EntityLink } from '@/components/ui/EntityLink'
import { IconButton } from '@/components/ui/IconButton'
import { BookIcon, EditIcon } from '@/components/ui/Icons'

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
          <EntityLink to={`/books/${book.id}`} className="font-medium" data-test={`author-book-title-${book.id}`}>
            {book.title}
          </EntityLink>
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
        <StatusBadge tone={bookStatusTone(book.status)}>
          {formatBookStatus(book.status)}
        </StatusBadge>
      ),
      width: '15%',
    },
  ]

  const handleRowClick = (book: BookDto) => {
    navigate(`/books/${book.id}`)
  }

  if (!books || books.length === 0) {
    return (
      <EmptyState
        message="No books found for this author."
        data-test="author-books-empty"
      />
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
        actions={(book) => (
          <>
            <IconButton
              to={`/books/${book.id}`}
              icon={<BookIcon />}
              label="View Details"
              onClick={(e) => e.stopPropagation()}
              data-test={`author-book-view-${book.id}`}
            />
            <IconButton
              to={`/books/${book.id}/edit`}
              icon={<EditIcon />}
              label="Edit"
              tone="primary"
              onClick={(e) => e.stopPropagation()}
              data-test={`author-book-edit-${book.id}`}
            />
          </>
        )}
        isLoading={false}
        emptyMessage="No books found for this author."
      />
    </div>
  )
}
