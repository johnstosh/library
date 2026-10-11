// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { EntityLink } from '@/components/ui/EntityLink'
import type { AuthorNameNormalizationResultDto, NameNormalizationResultDto } from '@/types/dtos'

type NormalizationResult = NameNormalizationResultDto | AuthorNameNormalizationResultDto

function isAuthorResult(result: NormalizationResult): result is AuthorNameNormalizationResultDto {
  return 'authorId' in result
}

interface NameNormalizationResultsModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  /** Plural subject in the summary line. Books is the default. */
  subject?: 'book' | 'author'
  results: NormalizationResult[]
}

export function NameNormalizationResultsModal({
  isOpen,
  onClose,
  title,
  subject = 'book',
  results,
}: NameNormalizationResultsModalProps) {
  const changedCount = results.filter((result) => result.changed).length
  const failedCount = results.filter((result) => !result.success).length
  const unchangedCount = results.length - changedCount - failedCount
  const mergedCount = results.filter(
    (result) => isAuthorResult(result) && result.mergedIntoAuthorId != null
  ).length
  const rewrittenCount = changedCount - mergedCount
  const singular = subject
  const plural = subject === 'author' ? 'authors' : 'books'
  const summary = [
    rewrittenCount > 0 ? `${rewrittenCount} ${rewrittenCount === 1 ? singular : plural} rewritten` : null,
    mergedCount > 0
      ? `${mergedCount} ${mergedCount === 1 ? singular : plural} merged into an existing author`
      : null,
    rewrittenCount === 0 && mergedCount === 0 ? `0 ${plural} rewritten` : null,
    unchangedCount > 0 ? `${unchangedCount} already conformed` : null,
    failedCount > 0 ? `${failedCount} skipped or failed` : null,
  ]
    .filter((part) => part != null)
    .join(', ')

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
            {summary}
          </p>
        </div>

        <div className="max-h-96 overflow-y-auto space-y-3">
          {results.map((result) => {
            const id = isAuthorResult(result) ? result.authorId : result.bookId
            const label = isAuthorResult(result) ? result.name : result.title
            const mergedInto = isAuthorResult(result) ? result.mergedIntoAuthorId : undefined
            const href = isAuthorResult(result) ? `/authors/${mergedInto ?? id}` : `/books/${id}`
            const fallback = isAuthorResult(result) ? `Author #${id}` : `Book #${id}`
            return (
            <div
              key={id}
              className={`rounded-lg p-3 border ${
                !result.success
                  ? 'bg-yellow-50 border-yellow-200'
                  : result.changed
                    ? 'bg-green-50 border-green-200'
                    : 'bg-gray-50 border-gray-200'
              }`}
              data-test={`name-normalization-result-${id}`}
            >
              <p className="font-medium">
                <EntityLink to={href}>
                  {label || fallback}
                </EntityLink>
              </p>
              {result.success && result.changed ? (
                <>
                  <p className="text-sm text-green-800 mt-1" data-test="name-normalization-before-after">
                    <span className="line-through">{result.before || '(blank)'}</span>
                    {' → '}
                    <span className="font-medium">{result.after}</span>
                  </p>
                  {mergedInto != null && (
                    <p className="text-sm text-green-800 mt-1" data-test="name-normalization-merged">
                      Merged into the existing author, and this duplicate was removed.
                    </p>
                  )}
                </>
              ) : result.success ? (
                <p className="text-sm text-gray-600 mt-1">
                  {result.before ? 'Already conforms' : 'Nothing to rewrite'}
                </p>
              ) : (
                <p className="text-sm text-yellow-700 mt-1">{result.errorMessage || 'Skipped'}</p>
              )}
            </div>
            )
          })}
        </div>
      </div>
    </Modal>
  )
}
