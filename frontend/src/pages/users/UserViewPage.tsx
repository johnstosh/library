// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useUser, useDeleteUser } from '@/api/users'
import { PageLoading } from '@/components/progress/PageLoading'
import { PiPencil, PiTrash } from 'react-icons/pi'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { BackLink } from '@/components/ui/BackLink'
import { EntityNotFound } from '@/components/ui/EntityNotFound'
import { PageCard } from '@/components/ui/PageCard'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'

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
    return <PageLoading />
  }

  if (!user) {
    return (
      <EntityNotFound
        title="User Not Found"
        entityLabel="user"
        onBack={handleBack}
        backLabel="Return to Users"
      />
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <BackLink onClick={handleBack} data-test="back-to-users">
        Back to Users
      </BackLink>

      <PageCard padding={false}>
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

          {/* User Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Name</p>
                <div className="flex items-center gap-2">
                  <p className="text-gray-900">{user.username}</p>
                  {user.ssoSubjectId && (
                    <StatusBadge tone="info" shape="rounded">SSO</StatusBadge>
                  )}
                </div>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Authority</p>
                <p className="text-gray-900">
                  <StatusBadge tone={user.authorities?.includes('LIBRARIAN') ? 'emphasis' : 'neutral'}>
                    {user.authorities?.[0] || 'USER'}
                  </StatusBadge>
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Email</p>
                <p className="text-gray-900" data-test="user-email">{user.email?.trim() ? user.email : '—'}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Phone</p>
                <p className="text-gray-900" data-test="user-phone">{user.phone?.trim() ? user.phone : '—'}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-gray-500">Active Loans</p>
                <p
                  className={`text-sm ${
                    (user.activeLoansCount ?? 0) > 0
                      ? 'text-primary-600 font-medium'
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
      </PageCard>

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete User"
        message="Are you sure you want to delete this user? This action cannot be undone."
        confirmText="Yes, Delete"
        variant="danger"
        isLoading={deleteUser.isPending}
        confirmDataTest="confirm-delete-user"
        cancelDataTest="cancel-delete-user"
      />
    </div>
  )
}
