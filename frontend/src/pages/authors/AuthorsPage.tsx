// (c) Copyright 2025 by Muczynski
import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { TableSummary } from '@/components/table/TableSummary'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { AuthorFilters } from './components/AuthorFilters'
import { AuthorTable } from './components/AuthorTable'
import { AuthorBulkActionsToolbar } from './components/AuthorBulkActionsToolbar'
import { useAuthors, useMostRecentAuthorSummaries } from '@/api/authors'
import { applyAuthorChipFilters } from '@/utils/authorChipFilters'
import { useUiStore, useAuthorsChips, useAuthorsTableSelection } from '@/stores/uiStore'
import type { AuthorDto } from '@/types/dtos'

export function AuthorsPage() {
  const navigate = useNavigate()

  const chips = useAuthorsChips()
  const { selectedIds, selectAll } = useAuthorsTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds, toggleAuthorsChip } = useUiStore()

  const { data: allAuthors = [], isLoading, isFetching, error } = useAuthors()
  const {
    data: mostRecentSummaries = [],
    isLoading: mostRecentLoading,
    isFetching: mostRecentFetching,
    error: mostRecentError,
  } = useMostRecentAuthorSummaries(chips.mostRecent)

  const authors = useMemo(() => {
    if (chips.mostRecent && mostRecentLoading) return []
    const mostRecentIds = chips.mostRecent
      ? new Set(mostRecentSummaries.map((summary) => summary.id))
      : undefined
    return applyAuthorChipFilters(allAuthors, chips, mostRecentIds)
  }, [allAuthors, chips, mostRecentLoading, mostRecentSummaries])

  const handleSelectToggle = (id: number) => {
    toggleRowSelection('authorsTable', id)
  }

  const handleSelectAll = () => {
    if (selectAll) {
      clearSelection('authorsTable')
    } else {
      const allIds = new Set(authors.map((a) => a.id))
      setSelectedIds('authorsTable', allIds)
      toggleSelectAll('authorsTable')
    }
  }

  const handleClearSelection = () => {
    clearSelection('authorsTable')
  }

  const handleAddAuthor = () => {
    navigate('/authors/new')
  }

  const handleViewAuthor = (author: AuthorDto) => {
    navigate(`/authors/${author.id}`)
  }

  return (
    <div>
      <PageHeader
        title="Authors"
        actions={
          <Button variant="primary" onClick={handleAddAuthor} data-test="add-author">
            Add Author
          </Button>
        }
      />

      {(error || mostRecentError) && (
        <ErrorMessage
          message={`Error loading authors: ${(error ?? mostRecentError)?.message}`}
          className="mb-4"
        />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200">
          <AuthorFilters chips={chips} onToggle={toggleAuthorsChip} />
        </div>

        <div className="p-4">
          <AuthorBulkActionsToolbar
            selectedIds={selectedIds}
            onClearSelection={handleClearSelection}
          />

          <AuthorTable
            authors={authors}
            isLoading={isLoading || (chips.mostRecent && mostRecentLoading)}
            selectedIds={selectedIds}
            selectAll={selectAll}
            onSelectToggle={handleSelectToggle}
            onSelectAll={handleSelectAll}
            onView={handleViewAuthor}
          />
        </div>

        <LoadingOverlay
          show={
            (isFetching && !isLoading) ||
            (chips.mostRecent && mostRecentFetching && !mostRecentLoading)
          }
        />
        <TableSummary
          count={authors.length}
          singular="author"
          plural="authors"
          isLoading={isLoading || (chips.mostRecent && mostRecentLoading)}
        />
      </PageCard>
    </div>
  )
}
