// (c) Copyright 2025 by Muczynski
import { ThrottledThumbnail } from '@/components/ui/ThrottledThumbnail'
import { getThumbnailUrl } from '@/api/photos'

interface CoverThumbnailProps {
  photoId?: number
  checksum?: string
  alt: string
  /** Prevent table row click when opening the full-size photo. */
  stopPropagation?: boolean
  dataTest?: string
}

export function CoverThumbnail({
  photoId,
  checksum,
  alt,
  stopPropagation = false,
  dataTest,
}: CoverThumbnailProps) {
  if (!photoId) {
    return (
      <div
        className="w-14 h-20 bg-gray-100 rounded flex items-center justify-center text-gray-400 shrink-0"
        style={{ width: '3.5rem', minWidth: '3.5rem' }}
        data-test={dataTest}
      >
        -
      </div>
    )
  }

  return (
    <a
      href={`/photos/${photoId}`}
      target="_blank"
      rel="noopener noreferrer"
      onClick={stopPropagation ? (e) => e.stopPropagation() : undefined}
      className="block shrink-0"
      style={{ width: '3.5rem', minWidth: '3.5rem' }}
      title="View full-size photo"
      data-test={dataTest}
    >
      <ThrottledThumbnail
        photoId={photoId}
        url={getThumbnailUrl(photoId, 70, checksum)}
        alt={alt}
        className="w-14 max-h-20 h-auto object-contain rounded hover:opacity-80 transition-opacity cursor-pointer"
      />
    </a>
  )
}
