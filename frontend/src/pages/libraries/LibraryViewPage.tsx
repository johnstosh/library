// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useLibrary, useLibraryStatistics, useDeleteLibrary } from '@/api/libraries'
import { Spinner } from '@/components/progress/Spinner'
import { PiPencil, PiTrash, PiArrowLeft } from 'react-icons/pi'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'

export function LibraryViewPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const libraryId = id ? parseInt(id, 10) : 0
  const { data: library, isLoading } = useLibrary(libraryId)
  const { data: statistics = [] } = useLibraryStatistics()
  const deleteLibrary = useDeleteLibrary()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [error, setError] = useState('')

  const handleDelete = async () => {
    try {
      await deleteLibrary.mutateAsync(libraryId)
      navigate('/libraries')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete library')
      setShowDeleteConfirm(false)
    }
  }

  const handleEdit = () => {
    navigate(`/libraries/${libraryId}/edit`)
  }

  const handleBack = () => {
    navigate('/libraries')
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!library) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-lg shadow p-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Library Not Found</h1>
          <p className="text-gray-600 mb-4">The library you're looking for doesn't exist.</p>
          <button
            onClick={handleBack}
            className="text-blue-600 hover:text-blue-800"
          >
            Return to Libraries
          </button>
        </div>
      </div>
    )
  }

  // Get statistics for this library
  const stats = statistics.find((s) => s.libraryId === libraryId)

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Button
          variant="ghost"
          onClick={handleBack}
          leftIcon={<PiArrowLeft />}
          data-test="back-to-libraries"
        >
          Back to Libraries
        </Button>
      </div>

      <div className="bg-white rounded-lg shadow">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-start justify-between">
            <h1 className="text-2xl font-bold text-gray-900" data-test="library-name">
              {library.branchName}
            </h1>
            <div className="flex gap-3">
              <Button
                variant="outline"
                onClick={handleEdit}
                leftIcon={<PiPencil />}
                data-test="library-view-edit"
              >
                Edit
              </Button>
              <Button
                variant="danger"
                onClick={() => setShowDeleteConfirm(true)}
                leftIcon={<PiTrash />}
                data-test="library-view-delete"
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
                Are you sure you want to delete this library?
              </p>
              <p className="text-red-700 mb-4">This action cannot be undone.</p>
              <div className="flex gap-3">
                <Button
                  variant="danger"
                  onClick={handleDelete}
                  isLoading={deleteLibrary.isPending}
                  data-test="confirm-delete-library"
                >
                  Yes, Delete
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => setShowDeleteConfirm(false)}
                  data-test="cancel-delete-library"
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}

          {/* Library Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Library System Name</p>
                <p className="text-gray-900">{library.librarySystemName}</p>
              </div>
              {stats && (
                <>
                  <div>
                    <p className="text-sm font-medium text-gray-500">Total Books</p>
                    <p className="text-gray-900">{stats.bookCount.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-500">Active Loans</p>
                    <p className="text-gray-900">{stats.activeLoansCount.toLocaleString()}</p>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
