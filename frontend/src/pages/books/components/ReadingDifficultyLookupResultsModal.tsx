// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { EntityLink } from '@/components/ui/EntityLink'
import type { ReadingDifficultyLookupResultDto } from '@/types/dtos'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { READING_DIFFICULTY_FILTER_LABELS, normalizeReadingDifficulty } from '@/utils/readingDifficulty'

interface ReadingDifficultyLookupResultsModalProps {
  isOpen: boolean
  onClose: () => void
  results: ReadingDifficultyLookupResultDto[]
  isRunning?: boolean
}

export function ReadingDifficultyLookupResultsModal({
  isOpen,
  onClose,
  results,
  isRunning,
}: ReadingDifficultyLookupResultsModalProps) {
  const successCount = results.filter((r) => r.success).length
  const failedCount = results.filter((r) => !r.success).length

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Reading Difficulty Results"
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="primary" onClick={onClose} data-test="reading-difficulty-lookup-results-close">
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        {results.length === 0 && isRunning ? (
          <p className="text-primary-800 font-medium" data-test="reading-difficulty-lookup-standby">
            Please stand by...
          </p>
        ) : (
          <>
            <div className="bg-primary-50 border border-primary-200 rounded-lg p-3">
              <p className="text-primary-800 font-medium">
                {successCount} {successCount === 1 ? 'book' : 'books'} processed successfully
                {failedCount > 0 && `, ${failedCount} skipped or failed`}
              </p>
            </div>

            <div className="max-h-96 overflow-y-auto space-y-3">
              {results.map((result) => (
                <div
                  key={result.bookId}
                  className={`rounded-lg p-3 border ${
                    result.success ? 'bg-green-50 border-green-200' : 'bg-yellow-50 border-yellow-200'
                  }`}
                  data-test={`reading-difficulty-lookup-result-${result.bookId}`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="font-medium">
                        <EntityLink to={`/books/${result.bookId}`}>
                          {result.title || `Book #${result.bookId}`}
                        </EntityLink>
                      </p>
                      {result.success && result.suggestedDifficulty ? (
                        <div className="flex flex-wrap gap-1 mt-2">
                          <StatusBadge tone="accent" shape="rounded">
                            {READING_DIFFICULTY_FILTER_LABELS[normalizeReadingDifficulty(result.suggestedDifficulty)]}
                          </StatusBadge>
                        </div>
                      ) : (
                        <p className="text-sm text-yellow-700 mt-1">
                          {result.errorMessage || 'No reading difficulty suggested'}
                        </p>
                      )}
                    </div>
                    <span className={`text-sm ${result.success ? 'text-green-600' : 'text-yellow-600'}`}>
                      {result.success ? 'Filled' : 'Skipped'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </Modal>
  )
}
