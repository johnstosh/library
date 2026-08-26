// (c) Copyright 2025 by Muczynski
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { TableSummary } from '@/components/table/TableSummary'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useToast } from '@/hooks/useToast'
import { GrokipediaIcon } from '@/components/ui/Icons'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { AuthorFilters } from './components/AuthorFilters'
import { AuthorTable } from './components/AuthorTable'
import { useAuthors, useDeleteAuthors, useMostRecentAuthorSummaries } from '@/api/authors'
import { applyAuthorChipFilters } from '@/utils/authorChipFilters'
import { useLookupBulkAuthorsGrokipedia, type GrokipediaLookupResultDto } from '@/api/grokipedia-lookup'
import { GrokipediaLookupResultsModal } from '@/components/GrokipediaLookupResultsModal'
import { useUiStore, useAuthorsChips, useAuthorsTableSelection } from '@/stores/uiStore'
import type { AuthorDto } from '@/types/dtos'

export function AuthorsPage() {
  const navigate = useNavigate()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showGrokipediaResults, setShowGrokipediaResults] = useState(false)
  const [grokipediaResults, setGrokipediaResults] = useState<GrokipediaLookupResultDto[]>([])

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
  const deleteAuthors = useDeleteAuthors()
  const lookupGrokipedia = useLookupBulkAuthorsGrokipedia()
  const toast = useToast()

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

  const handleBulkDelete = async () => {
    try {
      await deleteAuthors.mutateAsync(Array.from(selectedIds))
      handleClearSelection()
      setShowDeleteConfirm(false)
    } catch (error) {
      console.error('Failed to delete authors:', error)
      toast.error('Failed to delete authors')
    }
  }

  const handleGrokipediaLookup = async () => {
    try {
      const results = await lookupGrokipedia.mutateAsync(Array.from(selectedIds))
      setGrokipediaResults(results)
      setShowGrokipediaResults(true)
    } catch (error) {
      console.error('Failed to lookup Grokipedia URLs:', error)
      toast.error('Failed to lookup Grokipedia URLs')
    }
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
          {selectedIds.size > 0 && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <span className="text-sm font-medium text-blue-900">
                    {selectedIds.size} {selectedIds.size === 1 ? 'author' : 'authors'} selected
                  </span>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleClearSelection}
                    data-test="clear-selection"
                  >
                    Clear Selection
                  </Button>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleGrokipediaLookup}
                    isLoading={lookupGrokipedia.isPending}
                    disabled={lookupGrokipedia.isPending}
                    leftIcon={<GrokipediaIcon />}
                    data-test="bulk-lookup-grokipedia"
                  >
                    Find Grokipedia URLs
                  </Button>
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => setShowDeleteConfirm(true)}
                    data-test="bulk-delete"
                  >
                    Delete Selected
                  </Button>
                </div>
              </div>
            </div>
          )}

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

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleBulkDelete}
        title="Delete Authors"
        message={`Are you sure you want to delete ${selectedIds.size} ${
          selectedIds.size === 1 ? 'author' : 'authors'
        }? This action cannot be undone.`}
        confirmText="Delete"
        variant="danger"
        isLoading={deleteAuthors.isPending}
      />

      <GrokipediaLookupResultsModal
        isOpen={showGrokipediaResults}
        onClose={() => setShowGrokipediaResults(false)}
        results={grokipediaResults}
        entityType="author"
      />
    </div>
  )
}
