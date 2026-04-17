// (c) Copyright 2025 by Muczynski
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import type { LibraryCardDesign } from '@/types/dtos'
import { useLibraryCardDesigns } from '@/api/library-cards'

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
  const { data: designs, isLoading } = useLibraryCardDesigns()

  const previewImageUrl = designs?.find(d => d.name === currentDesign)?.imageUrl
    ?? `/images/library-cards/${(currentDesign ?? 'CLASSICAL_DEVOTION').toLowerCase()}.jpg`

  return (
    <>
      {successMessage && <SuccessMessage message={successMessage} className="mb-4" />}
      {errorMessage && <ErrorMessage message={errorMessage} className="mb-4" />}

      {/* Large preview of currently selected design */}
      <div className="mb-4 flex justify-center">
        <img
          src={previewImageUrl}
          alt="Selected library card design preview"
          className="w-128 max-h-160 h-auto object-contain rounded-lg shadow"
          data-test="library-card-design-preview"
        />
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="border rounded-lg p-4 animate-pulse bg-gray-100 h-24" />
          ))}
        </div>
      ) : (
        <div className="space-y-3">
          {(designs ?? []).map((option) => (
            <div
              key={option.name}
              className={`border rounded-lg p-4 cursor-pointer transition-all ${
                currentDesign === option.name
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300'
              }`}
              onClick={() => onDesignChange(option.name)}
              data-test={`library-card-design-${option.name}`}
            >
              <div className="flex items-center gap-3">
                <img
                  src={option.imageUrl}
                  alt={option.displayName}
                  className="w-32 max-h-40 h-auto object-contain rounded flex-shrink-0"
                />
                <div className="flex items-start flex-1">
                  <input
                    type="radio"
                    name="libraryCardDesign"
                    value={option.name}
                    checked={currentDesign === option.name}
                    onChange={() => onDesignChange(option.name)}
                    className="mt-1 mr-3"
                  />
                  <div>
                    <div className="font-medium text-gray-900">{option.displayName}</div>
                    <div className="text-sm text-gray-600 mt-1">{option.description}</div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  )
}
