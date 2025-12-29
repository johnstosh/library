// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useDeleteAuthor } from '@/api/authors'
import { truncate } from '@/utils/formatters'
import type { AuthorDto } from '@/types/dtos'

interface AuthorTableProps {
  authors: AuthorDto[]
  isLoading: boolean
  selectedIds: Set<number>
  selectAll: boolean
  onSelectToggle: (id: number) => void
  onSelectAll: () => void
  onEdit: (author: AuthorDto) => void
  onView: (author: AuthorDto) => void
}

export function AuthorTable({
  authors,
  isLoading,
  selectedIds,
  selectAll,
  onSelectToggle,
  onSelectAll,
  onEdit,
  onView,
}: AuthorTableProps) {
  const [deleteAuthorId, setDeleteAuthorId] = useState<number | null>(null)
  const deleteAuthor = useDeleteAuthor()

  const handleDelete = async () => {
    if (deleteAuthorId === null) return

    try {
      await deleteAuthor.mutateAsync(deleteAuthorId)
      setDeleteAuthorId(null)
    } catch (error) {
      console.error('Failed to delete author:', error)
    }
  }

  const columns: Column<AuthorDto>[] = [
    {
      key: 'name',
      header: 'Name',
      accessor: (author) => (
        <div className="font-medium text-gray-900">
          {author.name}
        </div>
      ),
      width: '20%',
    },
    {
      key: 'religiousAffiliation',
      header: 'Religious Affiliation',
      accessor: (author) => author.religiousAffiliation || '—',
      width: '15%',
    },
    {
      key: 'biography',
      header: 'Biography',
      accessor: (author) => (
        <span className="text-gray-600">{truncate(author.briefBiography, 60) || '—'}</span>
      ),
      width: '35%',
    },
    {
      key: 'bookCount',
      header: 'Books',
      accessor: (author) => (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
          {author.bookCount || 0}
        </span>
      ),
      width: '10%',
    },
  ]

  return (
    <>
      <DataTable
        data={authors}
        columns={columns}
        keyExtractor={(author) => author.id}
        selectable
        selectedIds={selectedIds}
        selectAll={selectAll}
        onSelectToggle={onSelectToggle}
        onSelectAll={onSelectAll}
        onRowClick={onView}
        actions={(author) => (
          <>
            <button
              onClick={(e) => {
                e.stopPropagation()
                onEdit(author)
              }}
              className="text-blue-600 hover:text-blue-900"
              data-test={`edit-author-${author.id}`}
              title="Edit"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                />
              </svg>
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation()
                setDeleteAuthorId(author.id)
              }}
              className="text-red-600 hover:text-red-900"
              data-test={`delete-author-${author.id}`}
              title="Delete"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                />
              </svg>
            </button>
          </>
        )}
        isLoading={isLoading}
        emptyMessage="No authors found"
      />

      <ConfirmDialog
        isOpen={deleteAuthorId !== null}
        onClose={() => setDeleteAuthorId(null)}
        onConfirm={handleDelete}
        title="Delete Author"
        message="Are you sure you want to delete this author? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteAuthor.isPending}
      />
    </>
  )
}
