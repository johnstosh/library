// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useDeleteAuthor } from '@/api/authors'
import { truncate, isValidUrl } from '@/utils/formatters'
import type { AuthorDto } from '@/types/dtos'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { IconButton } from '@/components/ui/IconButton'
import { EntityLink } from '@/components/ui/EntityLink'
import { AuthorIcon, BooksIcon, DeleteIcon, EditIcon, GrokipediaIcon } from '@/components/ui/Icons'
import { useToast } from '@/hooks/useToast'

interface AuthorTableProps {
  authors: AuthorDto[]
  isLoading: boolean
  selectedIds: Set<number>
  selectAll: boolean
  onSelectToggle: (id: number) => void
  onSelectAll: () => void
  onView: (author: AuthorDto) => void
}

export function AuthorTable({
  authors,
  isLoading,
  selectedIds,
  selectAll,
  onSelectToggle,
  onSelectAll,
  onView,
}: AuthorTableProps) {
  const [deleteAuthorId, setDeleteAuthorId] = useState<number | null>(null)
  const deleteAuthor = useDeleteAuthor()
  const toast = useToast()

  const handleDelete = async () => {
    if (deleteAuthorId === null) return

    try {
      await deleteAuthor.mutateAsync(deleteAuthorId)
      setDeleteAuthorId(null)
    } catch (error) {
      console.error('Failed to delete author:', error)
      toast.error('Failed to delete author')
    }
  }

  const columns: Column<AuthorDto>[] = [
    {
      key: 'name',
      header: 'Name',
      accessor: (author) => (
        <EntityLink to={`/authors/${author.id}`} className="font-medium" data-test={`author-name-link-${author.id}`}>
          {truncate(author.name, 30)}
        </EntityLink>
      ),
      width: '20%',
    },
    {
      key: 'religiousAffiliation',
      header: 'Religious Affiliation',
      accessor: (author) => truncate(author.religiousAffiliation, 25) || '—',
      width: '15%',
      hideOnMobile: true,
    },
    {
      key: 'biography',
      header: 'Biography',
      accessor: (author) => (
        <span className="text-gray-600">{truncate(author.briefBiography, 60) || '—'}</span>
      ),
      width: '35%',
      hideOnMobile: true,
    },
    {
      key: 'bookCount',
      header: 'Books',
      accessor: (author) => (
        <StatusBadge tone="neutral">{author.bookCount || 0}</StatusBadge>
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
            <IconButton
              to={`/authors/${author.id}`}
              icon={<AuthorIcon />}
              label="View Details"
              onClick={(e) => e.stopPropagation()}
              data-test={`view-author-${author.id}`}
            />
            {isValidUrl(author.grokipediaUrl) && (
              <IconButton
                href={author.grokipediaUrl}
                icon={<GrokipediaIcon />}
                label="View on Grokipedia"
                tone="warning"
                onClick={(e) => e.stopPropagation()}
                data-test={`grokipedia-author-${author.id}`}
              />
            )}
            <IconButton
              to={`/authors/${author.id}`}
              icon={<BooksIcon />}
              label="See Books"
              tone="info"
              onClick={(e) => e.stopPropagation()}
              data-test={`see-books-${author.id}`}
            />
            <IconButton
              to={`/authors/${author.id}/edit`}
              icon={<EditIcon />}
              label="Edit"
              tone="primary"
              onClick={(e) => e.stopPropagation()}
              data-test={`edit-author-${author.id}`}
            />
            <IconButton
              icon={<DeleteIcon />}
              label="Delete"
              tone="danger"
              onClick={(e) => {
                e.stopPropagation()
                setDeleteAuthorId(author.id)
              }}
              data-test={`delete-author-${author.id}`}
            />
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
