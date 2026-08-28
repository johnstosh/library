// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/hooks/useToast'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Modal } from '@/components/ui/Modal'
import { useDeleteAuthors, useGenerateAuthorsMissingDataWithProgress } from '@/api/authors'
import { useLookupBulkAuthorsGrokipediaWithProgress, type GrokipediaLookupResultDto } from '@/api/grokipedia-lookup'
import { GrokipediaLookupResultsModal } from '@/components/GrokipediaLookupResultsModal'
import { AuthorEnrichmentResultsModal } from './AuthorEnrichmentResultsModal'
import { AiIcon, GrokipediaIcon } from '@/components/ui/Icons'
import type { AuthorEnrichmentResultDto, BulkDeleteResultDto } from '@/types/dtos'

interface AuthorBulkActionsToolbarProps {
  selectedIds: Set<number>
  onClearSelection: () => void
}

function progressLabel(idle: string, running: string, isPending: boolean, completed: number, total: number) {
  return isPending ? `${running} (${completed}/${total})` : idle
}

export function AuthorBulkActionsToolbar({ selectedIds, onClearSelection }: AuthorBulkActionsToolbarProps) {
  const toast = useToast()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showDeleteResults, setShowDeleteResults] = useState(false)
  const [deleteResults, setDeleteResults] = useState<BulkDeleteResultDto | null>(null)
  const [showGrokipediaResults, setShowGrokipediaResults] = useState(false)
  const [grokipediaResults, setGrokipediaResults] = useState<GrokipediaLookupResultDto[]>([])
  const [grokipediaProgress, setGrokipediaProgress] = useState(0)
  const [showEnrichmentResults, setShowEnrichmentResults] = useState(false)
  const [enrichmentResults, setEnrichmentResults] = useState<AuthorEnrichmentResultDto[]>([])
  const [enrichmentProgress, setEnrichmentProgress] = useState(0)

  const deleteAuthors = useDeleteAuthors()
  const lookupGrokipedia = useLookupBulkAuthorsGrokipediaWithProgress((completed) => {
    setGrokipediaProgress(completed)
  })
  const generateMissing = useGenerateAuthorsMissingDataWithProgress((completed) => {
    setEnrichmentProgress(completed)
  })

  const selectedCount = selectedIds.size
  const isOperationPending = lookupGrokipedia.isPending || generateMissing.isPending || deleteAuthors.isPending

  const handleBulkDelete = async () => {
    try {
      const result = await deleteAuthors.mutateAsync(Array.from(selectedIds))
      setShowDeleteConfirm(false)
      onClearSelection()
      if (result.failedCount > 0) {
        setDeleteResults(result)
        setShowDeleteResults(true)
      }
    } catch (error) {
      console.error('Failed to delete authors:', error)
      setShowDeleteConfirm(false)
      toast.error('Failed to delete authors')
    }
  }

  const handleGrokipediaLookup = async () => {
    setGrokipediaProgress(0)
    try {
      const results = await lookupGrokipedia.mutateAsync(Array.from(selectedIds))
      setGrokipediaResults(results)
      setShowGrokipediaResults(true)
    } catch (error) {
      console.error('Failed to lookup Grokipedia URLs:', error)
      toast.error('Failed to lookup Grokipedia URLs')
    }
  }

  const handleGenerateMissing = async () => {
    setEnrichmentProgress(0)
    try {
      const results = await generateMissing.mutateAsync(Array.from(selectedIds))
      setEnrichmentResults(results)
      setShowEnrichmentResults(true)
    } catch (error) {
      console.error('Failed to generate missing author data:', error)
      toast.error('Failed to generate missing author data')
    }
  }

  if (selectedIds.size === 0) return null

  return (
    <>
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-4 shrink-0">
            <span className="text-sm font-medium text-blue-900">
              {selectedIds.size} {selectedIds.size === 1 ? 'author' : 'authors'} selected
            </span>
            <Button
              variant="ghost"
              size="sm"
              onClick={onClearSelection}
              data-test="clear-selection"
            >
              Clear Selection
            </Button>
          </div>
          <div className="flex gap-2 overflow-x-auto flex-nowrap">
            <Button
              variant="outline"
              size="sm"
              className="shrink-0"
              onClick={handleGenerateMissing}
              isLoading={generateMissing.isPending}
              disabled={isOperationPending}
              leftIcon={<AiIcon />}
              data-test="bulk-generate-missing"
            >
              {progressLabel(
                'Generate missing data',
                'Generating...',
                generateMissing.isPending,
                enrichmentProgress,
                selectedCount
              )}
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="shrink-0"
              onClick={handleGrokipediaLookup}
              isLoading={lookupGrokipedia.isPending}
              disabled={isOperationPending}
              leftIcon={<GrokipediaIcon />}
              data-test="bulk-lookup-grokipedia"
            >
              {progressLabel(
                'Find Grokipedia URLs',
                'Grokipedia...',
                lookupGrokipedia.isPending,
                grokipediaProgress,
                selectedCount
              )}
            </Button>
            <Button
              variant="danger"
              size="sm"
              className="shrink-0"
              onClick={() => setShowDeleteConfirm(true)}
              disabled={isOperationPending}
              data-test="bulk-delete"
            >
              Delete Selected
            </Button>
          </div>
        </div>
      </div>

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

      <AuthorEnrichmentResultsModal
        isOpen={showEnrichmentResults}
        onClose={() => setShowEnrichmentResults(false)}
        results={enrichmentResults}
      />

      <Modal
        isOpen={showDeleteResults}
        onClose={() => setShowDeleteResults(false)}
        title="Delete Results"
        size="md"
        footer={
          <div className="flex justify-end">
            <Button variant="primary" onClick={() => setShowDeleteResults(false)} data-test="delete-results-close">
              Close
            </Button>
          </div>
        }
      >
        {deleteResults && (
          <div className="space-y-4">
            {deleteResults.deletedCount > 0 && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                <p className="text-green-800 font-medium">
                  {deleteResults.deletedCount} {deleteResults.deletedCount === 1 ? 'author' : 'authors'} deleted successfully
                </p>
              </div>
            )}
            {deleteResults.failedCount > 0 && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="text-red-800 font-medium mb-2">
                  {deleteResults.failedCount} {deleteResults.failedCount === 1 ? 'author' : 'authors'} could not be deleted:
                </p>
                <ul className="list-disc list-inside space-y-1">
                  {deleteResults.failures.map((failure) => (
                    <li key={failure.id} className="text-red-700 text-sm">
                      <span className="font-medium">{failure.title}</span>: {failure.errorMessage}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </Modal>
    </>
  )
}
