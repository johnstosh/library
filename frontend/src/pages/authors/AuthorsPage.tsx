// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
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
import { useAuthors, useDeleteAuthors } from '@/api/authors'
import { useLookupBulkAuthorsGrokipedia, type GrokipediaLookupResultDto } from '@/api/grokipedia-lookup'
import { GrokipediaLookupResultsModal } from '@/components/GrokipediaLookupResultsModal'
import { useUiStore, useAuthorsFilter, useAuthorsTableSelection } from '@/stores/uiStore'
import type { AuthorDto } from '@/types/dtos'

export function AuthorsPage() {
  const navigate = useNavigate()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showGrokipediaResults, setShowGrokipediaResults] = useState(false)
  const [grokipediaResults, setGrokipediaResults] = useState<GrokipediaLookupResultDto[]>([])

  const filter = useAuthorsFilter()
  const { selectedIds, selectAll } = useAuthorsTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds } = useUiStore()

  const { data: authors = [], isLoading, isFetching, error } = useAuthors(filter)
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

      {error && (
        <ErrorMessage message={`Error loading authors: ${error.message}`} className="mb-4" />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200">
          <AuthorFilters />
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
            isLoading={isLoading}
            selectedIds={selectedIds}
            selectAll={selectAll}
            onSelectToggle={handleSelectToggle}
            onSelectAll={handleSelectAll}
            onView={handleViewAuthor}
          />
        </div>

        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary count={authors.length} singular="author" plural="authors" isLoading={isLoading} />
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
