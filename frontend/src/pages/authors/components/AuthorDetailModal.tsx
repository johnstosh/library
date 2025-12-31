// (c) Copyright 2025 by Muczynski
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { PhotoSection } from '@/components/photos/PhotoSection'
import { useAuthor } from '@/api/authors'
import { formatDate } from '@/utils/formatters'
import { Spinner } from '@/components/progress/Spinner'

interface AuthorDetailModalProps {
  isOpen: boolean
  onClose: () => void
  authorId: number | null
}

export function AuthorDetailModal({ isOpen, onClose, authorId }: AuthorDetailModalProps) {
  const { data: author, isLoading } = useAuthor(authorId || 0)

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={author ? `${author.name}` : 'Author Details'}
      size="xl"
      footer={
        <div className="flex justify-end">
          <Button variant="ghost" onClick={onClose} data-test="author-detail-close">
            Close
          </Button>
        </div>
      }
    >
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : author ? (
        <div className="space-y-6">
          {/* Author Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Full Name</p>
                <p className="text-gray-900">
                  {author.name} {}
                </p>
              </div>
              {author.dateOfBirth && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Birth Date</p>
                  <p className="text-gray-900">{formatDate(author.dateOfBirth)}</p>
                </div>
              )}
              {author.dateOfDeath && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Death Date</p>
                  <p className="text-gray-900">{formatDate(author.dateOfDeath)}</p>
                </div>
              )}
              {author.bookCount !== undefined && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Books</p>
                  <p className="text-gray-900">
                    {author.bookCount} {author.bookCount === 1 ? 'book' : 'books'}
                  </p>
                </div>
              )}
              {author.birthCountry && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Birth Country</p>
                  <p className="text-gray-900">{author.birthCountry}</p>
                </div>
              )}
              {author.nationality && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Nationality</p>
                  <p className="text-gray-900">{author.nationality}</p>
                </div>
              )}
              {author.grokipediaUrl && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Grokipedia</p>
                  <a
                    href={author.grokipediaUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-800 underline"
                    data-test="author-grokipedia-link"
                  >
                    View on Grokipedia
                  </a>
                </div>
              )}
            </div>
            {author.religiousAffiliation && (
              <div className="mt-4">
                <p className="text-sm font-medium text-gray-500 mb-2">Religious Affiliation</p>
                <p className="text-gray-900">{author.religiousAffiliation}</p>
              </div>
            )}
            {author.briefBiography && (
              <div className="mt-4">
                <p className="text-sm font-medium text-gray-500 mb-2">Biography</p>
                <p className="text-gray-900">{author.briefBiography}</p>
              </div>
            )}
          </div>

          {/* Photos */}
          <PhotoSection
            entityType="author"
            entityId={author.id}
            entityName={`${author.name}`}
          />
        </div>
      ) : (
        <p className="text-gray-500">Author not found</p>
      )}
    </Modal>
  )
}
