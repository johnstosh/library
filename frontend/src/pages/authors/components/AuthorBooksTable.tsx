// (c) Copyright 2025 by Muczynski
import { useNavigate } from 'react-router-dom'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { formatBookStatus } from '@/utils/formatters'
import type { BookDto } from '@/types/dtos'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { bookStatusTone } from '@/utils/status'
import { EntityLink } from '@/components/ui/EntityLink'
import { IconButton } from '@/components/ui/IconButton'
import { BookIcon, EditIcon } from '@/components/ui/Icons'
import { useIsLibrarian } from '@/stores/authStore'

interface AuthorBooksTableProps {
  books: BookDto[]
  isLoading?: boolean
}

export function AuthorBooksTable({ books, isLoading = false }: AuthorBooksTableProps) {
  const navigate = useNavigate()
  const isLibrarian = useIsLibrarian()

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

  return (
    <div data-test={isLoading || books.length > 0 ? 'author-books-table' : 'author-books-empty'}>
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
            {isLibrarian && (
              <IconButton
                to={`/books/${book.id}/edit`}
                icon={<EditIcon />}
                label="Edit"
                tone="primary"
                onClick={(e) => e.stopPropagation()}
                data-test={`author-book-edit-${book.id}`}
              />
            )}
          </>
        )}
        isLoading={isLoading}
        emptyMessage="No books found for this author."
      />
    </div>
  )
}
