// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { PhotoSection } from '@/components/photos/PhotoSection'
import { AuthorBooksTable } from './components/AuthorBooksTable'
import { useAuthor, useAuthorBooks, useDeleteAuthor } from '@/api/authors'
import { PageLoading } from '@/components/progress/PageLoading'
import { PiPencil, PiTrash } from 'react-icons/pi'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { BackLink } from '@/components/ui/BackLink'
import { EntityNotFound } from '@/components/ui/EntityNotFound'
import { PageCard } from '@/components/ui/PageCard'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { useIsAuthenticated, useIsLibrarian } from '@/stores/authStore'
import { isValidUrl } from '@/utils/formatters'

export function AuthorViewPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const authorId = id ? parseInt(id, 10) : 0
  const { data: author, isLoading } = useAuthor(authorId)
  const { data: authorBooks = [], isLoading: booksLoading } = useAuthorBooks(authorId)
  const deleteAuthor = useDeleteAuthor()
  const isLibrarian = useIsLibrarian()
  const isAuthenticated = useIsAuthenticated()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [error, setError] = useState('')

  const handleDelete = async () => {
    try {
      await deleteAuthor.mutateAsync(authorId)
      navigate('/authors')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete author')
      setShowDeleteConfirm(false)
    }
  }

  const handleEdit = () => {
    navigate(`/authors/${authorId}/edit`)
  }

  const handleBack = () => {
    navigate(isAuthenticated ? '/authors' : '/search')
  }

  if (isLoading) {
    return <PageLoading />
  }

  if (!author) {
    return (
      <EntityNotFound
        title="Author Not Found"
        entityLabel="author"
        onBack={handleBack}
        backLabel={isAuthenticated ? 'Return to Authors' : 'Return to Search'}
      />
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      {isAuthenticated ? (
        <BackLink onClick={handleBack} data-test="back-to-authors">
          Back to Authors
        </BackLink>
      ) : (
        <BackLink onClick={handleBack} data-test="back-to-search">
          Back to Search
        </BackLink>
      )}

      <PageCard padding={false}>
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-start justify-between">
            <h1 className="text-2xl font-bold text-gray-900" data-test="author-name">
              {author.name}
            </h1>
            {isLibrarian && (
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={handleEdit}
                  leftIcon={<PiPencil />}
                  data-test="author-view-edit"
                >
                  Edit
                </Button>
                <Button
                  variant="danger"
                  onClick={() => setShowDeleteConfirm(true)}
                  leftIcon={<PiTrash />}
                  data-test="author-view-delete"
                >
                  Delete
                </Button>
              </div>
            )}
          </div>
        </div>

        {/* Body */}
        <div className="px-6 py-6 space-y-6">
          {error && <ErrorMessage message={error} />}

          {/* Author Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {author.dateOfBirth && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Date of Birth</p>
                  <p className="text-gray-900">{author.dateOfBirth}</p>
                </div>
              )}
              {author.dateOfDeath && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Date of Death</p>
                  <p className="text-gray-900">{author.dateOfDeath}</p>
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
              {author.religiousAffiliation && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Religious Affiliation</p>
                  <p className="text-gray-900">{author.religiousAffiliation}</p>
                </div>
              )}
              {isValidUrl(author.grokipediaUrl) && (
                <div>
                  <p className="text-sm font-medium text-gray-500">Grokipedia</p>
                  <a
                    href={author.grokipediaUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary-600 hover:text-primary-800 underline"
                    data-test="author-grokipedia-link"
                  >
                    View on Grokipedia
                  </a>
                </div>
              )}
            </div>

            {author.briefBiography && (
              <div className="mt-4 pt-4 border-t border-gray-200">
                <p className="text-sm font-medium text-gray-500">Brief Biography</p>
                <p className="text-gray-900 whitespace-pre-wrap">{author.briefBiography}</p>
              </div>
            )}
          </div>

          {/* Photos */}
          <PhotoSection
            entityType="author"
            entityId={author.id}
            entityName={author.name}
          />

          {/* Books */}
          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-4" data-test="author-books-heading">
              Books by {author.name}
            </h2>
            <AuthorBooksTable books={authorBooks} isLoading={booksLoading} />
          </div>
        </div>
      </PageCard>

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete Author"
        message="Are you sure you want to delete this author? This action cannot be undone."
        confirmText="Yes, Delete"
        variant="danger"
        isLoading={deleteAuthor.isPending}
        confirmDataTest="confirm-delete-author"
        cancelDataTest="cancel-delete-author"
      />
    </div>
  )
}
