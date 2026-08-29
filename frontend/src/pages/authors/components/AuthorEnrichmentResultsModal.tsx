// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { EntityLink } from '@/components/ui/EntityLink'
import { PiCheckCircle, PiXCircle, PiMinusCircle } from 'react-icons/pi'
import type { AuthorEnrichmentResultDto } from '@/types/dtos'

interface AuthorEnrichmentResultsModalProps {
  isOpen: boolean
  onClose: () => void
  results: AuthorEnrichmentResultDto[]
}

const FIELD_LABELS: Record<string, string> = {
  dateOfBirth: 'Date of birth',
  dateOfDeath: 'Date of death',
  religiousAffiliation: 'Religious affiliation',
  birthCountry: 'Birth country',
  nationality: 'Nationality',
  biographicalEssay: 'Biography',
  briefBiography: 'Biography',
}

function fieldLabel(field: string) {
  return FIELD_LABELS[field] ?? field
}

export function AuthorEnrichmentResultsModal({
  isOpen,
  onClose,
  results,
}: AuthorEnrichmentResultsModalProps) {
  const filledCount = results.filter((r) => r.success && !r.skipped && r.filledFields.length > 0).length
  const skippedCount = results.filter((r) => r.skipped || (r.success && r.filledFields.length === 0)).length
  const failureCount = results.filter((r) => !r.success).length

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Generate Missing Data Results"
      size="lg"
      footer={
        <div className="flex justify-end">
          <Button variant="primary" onClick={onClose} data-test="close-author-enrichment-results">
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        <div className="bg-gray-50 rounded-lg p-4">
          <div className="grid grid-cols-3 gap-4">
            <div>
              <p className="text-sm font-medium text-gray-500">Filled</p>
              <p className="text-2xl font-bold text-green-600">{filledCount}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Skipped</p>
              <p className="text-2xl font-bold text-gray-700">{skippedCount}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Failed</p>
              <p className="text-2xl font-bold text-red-600">{failureCount}</p>
            </div>
          </div>
        </div>

        <div className="max-h-96 overflow-y-auto">
          <table className="min-w-full table-fixed divide-y divide-gray-200">
            <thead className="bg-gray-50 sticky top-0">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '10%' }}>
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '30%' }}>
                  Author
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" style={{ width: '60%' }}>
                  Details
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {results.map((result) => {
                const skipped = result.skipped || (result.success && result.filledFields.length === 0)
                return (
                  <tr key={result.authorId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 whitespace-nowrap">
                      {!result.success ? (
                        <PiXCircle className="w-5 h-5 text-red-600" />
                      ) : skipped ? (
                        <PiMinusCircle className="w-5 h-5 text-gray-400" />
                      ) : (
                        <PiCheckCircle className="w-5 h-5 text-green-600" />
                      )}
                    </td>
                    <td className="px-4 py-3 overflow-hidden truncate text-sm text-gray-900">
                      <EntityLink to={`/authors/${result.authorId}`}>
                        {result.name || `Author #${result.authorId}`}
                      </EntityLink>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      {!result.success ? (
                        result.errorMessage || 'Failed'
                      ) : skipped ? (
                        'No missing fields to fill'
                      ) : (
                        result.filledFields.map(fieldLabel).join(', ')
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
    </Modal>
  )
}
