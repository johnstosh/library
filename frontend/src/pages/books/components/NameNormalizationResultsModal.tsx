// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { EntityLink } from '@/components/ui/EntityLink'
import type { NameNormalizationResultDto } from '@/types/dtos'

interface NameNormalizationResultsModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  results: NameNormalizationResultDto[]
}

export function NameNormalizationResultsModal({
  isOpen,
  onClose,
  title,
  results,
}: NameNormalizationResultsModalProps) {
  const changedCount = results.filter((result) => result.changed).length
  const failedCount = results.filter((result) => !result.success).length
  const unchangedCount = results.length - changedCount - failedCount

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="primary" onClick={onClose} data-test="name-normalization-results-close">
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-4" data-test="name-normalization-results">
        <div className="bg-primary-50 border border-primary-200 rounded-lg p-3">
          <p className="text-primary-800 font-medium">
            {changedCount} {changedCount === 1 ? 'book' : 'books'} rewritten
            {unchangedCount > 0 && `, ${unchangedCount} already conformed`}
            {failedCount > 0 && `, ${failedCount} skipped or failed`}
          </p>
        </div>

        <div className="max-h-96 overflow-y-auto space-y-3">
          {results.map((result) => (
            <div
              key={result.bookId}
              className={`rounded-lg p-3 border ${
                !result.success
                  ? 'bg-yellow-50 border-yellow-200'
                  : result.changed
                    ? 'bg-green-50 border-green-200'
                    : 'bg-gray-50 border-gray-200'
              }`}
              data-test={`name-normalization-result-${result.bookId}`}
            >
              <p className="font-medium">
                <EntityLink to={`/books/${result.bookId}`}>
                  {result.title || `Book #${result.bookId}`}
                </EntityLink>
              </p>
              {result.success && result.changed ? (
                <p className="text-sm text-green-800 mt-1" data-test="name-normalization-before-after">
                  <span className="line-through">{result.before || '(blank)'}</span>
                  {' → '}
                  <span className="font-medium">{result.after}</span>
                </p>
              ) : result.success ? (
                <p className="text-sm text-gray-600 mt-1">
                  {result.before ? 'Already conforms' : 'Nothing to rewrite'}
                </p>
              ) : (
                <p className="text-sm text-yellow-700 mt-1">{result.errorMessage || 'Skipped'}</p>
              )}
            </div>
          ))}
        </div>
      </div>
    </Modal>
  )
}
