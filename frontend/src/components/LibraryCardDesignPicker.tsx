// (c) Copyright 2025 by Muczynski
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import type { LibraryCardDesign } from '@/types/dtos'

export const LIBRARY_CARD_DESIGN_OPTIONS: Array<{ value: LibraryCardDesign; label: string; description: string }> = [
  {
    value: 'CLASSICAL_DEVOTION',
    label: 'Classical Devotion',
    description: 'Traditional design with classic typography'
  },
  {
    value: 'COUNTRYSIDE_YOUTH',
    label: 'Countryside Youth',
    description: 'Fresh, youthful design with natural elements'
  },
  {
    value: 'SACRED_HEART_PORTRAIT',
    label: 'Sacred Heart Portrait',
    description: 'Portrait-oriented design with sacred imagery'
  },
  {
    value: 'RADIANT_BLESSING',
    label: 'Radiant Blessing',
    description: 'Bright design with uplifting elements'
  },
  {
    value: 'PATRON_OF_CREATURES',
    label: 'Patron of Creatures',
    description: 'Nature-focused design with animal motifs'
  }
]

interface LibraryCardDesignPickerProps {
  currentDesign: LibraryCardDesign | undefined
  onDesignChange: (design: LibraryCardDesign) => Promise<void>
  successMessage?: string
  errorMessage?: string
}

export function LibraryCardDesignPicker({
  currentDesign,
  onDesignChange,
  successMessage,
  errorMessage,
}: LibraryCardDesignPickerProps) {
  return (
    <>
      {successMessage && <SuccessMessage message={successMessage} className="mb-4" />}
      {errorMessage && <ErrorMessage message={errorMessage} className="mb-4" />}

      {/* Large preview of currently selected design */}
      <div className="mb-4 flex justify-center">
        <img
          src={`/images/library-cards/${(currentDesign ?? 'CLASSICAL_DEVOTION').toLowerCase()}.jpg`}
          alt="Selected library card design preview"
          className="w-32 h-40 object-cover rounded-lg shadow"
          data-test="library-card-design-preview"
        />
      </div>

      <div className="space-y-3">
        {LIBRARY_CARD_DESIGN_OPTIONS.map((option) => (
          <div
            key={option.value}
            className={`border rounded-lg p-4 cursor-pointer transition-all ${
              currentDesign === option.value
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 hover:border-gray-300'
            }`}
            onClick={() => onDesignChange(option.value)}
            data-test={`library-card-design-${option.value}`}
          >
            <div className="flex items-center gap-3">
              <img
                src={`/images/library-cards/${option.value.toLowerCase()}.jpg`}
                alt={option.label}
                className="w-16 h-20 object-cover rounded flex-shrink-0"
              />
              <div className="flex items-start flex-1">
                <input
                  type="radio"
                  name="libraryCardDesign"
                  value={option.value}
                  checked={currentDesign === option.value}
                  onChange={() => onDesignChange(option.value)}
                  className="mt-1 mr-3"
                />
                <div>
                  <div className="font-medium text-gray-900">{option.label}</div>
                  <div className="text-sm text-gray-600 mt-1">{option.description}</div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </>
  )
}
