// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { PhotoSection } from '@/components/photos/PhotoSection'
import { AuthorBooksTable } from './components/AuthorBooksTable'
import { useAuthor, useDeleteAuthor } from '@/api/authors'
import { Spinner } from '@/components/progress/Spinner'
import { PiPencil, PiTrash, PiArrowLeft } from 'react-icons/pi'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'

export function AuthorViewPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const authorId = id ? parseInt(id, 10) : 0
  const { data: author, isLoading } = useAuthor(authorId)
  const deleteAuthor = useDeleteAuthor()
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
    navigate('/authors')
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!author) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-lg shadow p-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Author Not Found</h1>
          <p className="text-gray-600 mb-4">The author you're looking for doesn't exist.</p>
          <button
            onClick={handleBack}
            className="text-blue-600 hover:text-blue-800"
          >
            Return to Authors
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Button
          variant="ghost"
          onClick={handleBack}
          leftIcon={<PiArrowLeft />}
          data-test="back-to-authors"
        >
          Back to Authors
        </Button>
      </div>

      <div className="bg-white rounded-lg shadow">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-start justify-between">
            <h1 className="text-2xl font-bold text-gray-900" data-test="author-name">
              {author.name}
            </h1>
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
          </div>
        </div>

        {/* Body */}
        <div className="px-6 py-6 space-y-6">
          {error && <ErrorMessage message={error} />}

          {/* Delete Confirmation */}
          {showDeleteConfirm && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <p className="text-red-900 font-semibold mb-3">
                Are you sure you want to delete this author?
              </p>
              <p className="text-red-700 mb-4">This action cannot be undone.</p>
              <div className="flex gap-3">
                <Button
                  variant="danger"
                  onClick={handleDelete}
                  isLoading={deleteAuthor.isPending}
                  data-test="confirm-delete-author"
                >
                  Yes, Delete
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => setShowDeleteConfirm(false)}
                  data-test="cancel-delete-author"
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}

          {/* Author Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-2 gap-4">
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
            <AuthorBooksTable books={author.books || []} />
          </div>
        </div>
      </div>
    </div>
  )
}
