// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useUser, useDeleteUser } from '@/api/users'
import { Spinner } from '@/components/progress/Spinner'
import { PiPencil, PiTrash, PiArrowLeft } from 'react-icons/pi'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'

export function UserViewPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const userId = id ? parseInt(id, 10) : 0
  const { data: user, isLoading } = useUser(userId)
  const deleteUser = useDeleteUser()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [error, setError] = useState('')

  const handleDelete = async () => {
    try {
      await deleteUser.mutateAsync(userId)
      navigate('/users')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete user')
      setShowDeleteConfirm(false)
    }
  }

  const handleEdit = () => {
    navigate(`/users/${userId}/edit`)
  }

  const handleBack = () => {
    navigate('/users')
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!user) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-lg shadow p-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">User Not Found</h1>
          <p className="text-gray-600 mb-4">The user you're looking for doesn't exist.</p>
          <button
            onClick={handleBack}
            className="text-blue-600 hover:text-blue-800"
          >
            Return to Users
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
          data-test="back-to-users"
        >
          Back to Users
        </Button>
      </div>

      <div className="bg-white rounded-lg shadow">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-start justify-between">
            <h1 className="text-2xl font-bold text-gray-900" data-test="user-username">
              {user.username}
            </h1>
            <div className="flex gap-3">
              <Button
                variant="outline"
                onClick={handleEdit}
                leftIcon={<PiPencil />}
                data-test="user-view-edit"
              >
                Edit
              </Button>
              <Button
                variant="danger"
                onClick={() => setShowDeleteConfirm(true)}
                leftIcon={<PiTrash />}
                data-test="user-view-delete"
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
                Are you sure you want to delete this user?
              </p>
              <p className="text-red-700 mb-4">This action cannot be undone.</p>
              <div className="flex gap-3">
                <Button
                  variant="danger"
                  onClick={handleDelete}
                  isLoading={deleteUser.isPending}
                  data-test="confirm-delete-user"
                >
                  Yes, Delete
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => setShowDeleteConfirm(false)}
                  data-test="cancel-delete-user"
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}

          {/* User Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Username</p>
                <div className="flex items-center gap-2">
                  <p className="text-gray-900">{user.username}</p>
                  {user.ssoSubjectId && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                      SSO
                    </span>
                  )}
                </div>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Authority</p>
                <p className="text-gray-900">
                  <span
                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      user.authorities?.includes('LIBRARIAN')
                        ? 'bg-purple-100 text-purple-800'
                        : 'bg-gray-100 text-gray-800'
                    }`}
                  >
                    {user.authorities?.[0] || 'USER'}
                  </span>
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Active Loans</p>
                <p
                  className={`text-sm ${
                    (user.activeLoansCount ?? 0) > 0
                      ? 'text-blue-600 font-medium'
                      : 'text-gray-500'
                  }`}
                >
                  {user.activeLoansCount ?? 0}
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">User ID</p>
                <p className="text-gray-900">{user.id}</p>
              </div>
            </div>

            {user.ssoSubjectId && (
              <div className="mt-4 pt-4 border-t border-gray-200">
                <p className="text-sm font-medium text-gray-500">SSO Subject ID</p>
                <p className="text-gray-900 font-mono text-sm break-all">{user.ssoSubjectId}</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
