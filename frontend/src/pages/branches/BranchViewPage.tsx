// (c) Copyright 2025 by Muczynski
import { useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useBranch, useBranchStatistics, useDeleteBranch } from '@/api/branches'
import { PageLoading } from '@/components/progress/PageLoading'
import { PiPencil, PiTrash } from 'react-icons/pi'
import { useState } from 'react'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { BackLink } from '@/components/ui/BackLink'
import { EntityNotFound } from '@/components/ui/EntityNotFound'
import { PageCard } from '@/components/ui/PageCard'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'

export function BranchViewPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const branchId = id ? parseInt(id, 10) : 0
  const { data: branch, isLoading } = useBranch(branchId)
  const { data: statistics = [] } = useBranchStatistics()
  const deleteBranch = useDeleteBranch()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [error, setError] = useState('')

  const handleDelete = async () => {
    try {
      await deleteBranch.mutateAsync(branchId)
      navigate('/branches')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete branch')
      setShowDeleteConfirm(false)
    }
  }

  const handleEdit = () => {
    navigate(`/branches/${branchId}/edit`)
  }

  const handleBack = () => {
    navigate('/branches')
  }

  if (isLoading) {
    return <PageLoading />
  }

  if (!branch) {
    return (
      <EntityNotFound
        title="Branch Not Found"
        entityLabel="branch"
        onBack={handleBack}
        backLabel="Return to Branches"
      />
    )
  }

  // Get statistics for this branch
  const stats = statistics.find((s) => s.branchId === branchId)

  return (
    <div className="max-w-4xl mx-auto">
      <BackLink onClick={handleBack} data-test="back-to-branches">
        Back to Branches
      </BackLink>

      <PageCard padding={false}>
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-start justify-between">
            <h1 className="text-2xl font-bold text-gray-900" data-test="branch-name">
              {branch.branchName}
            </h1>
            <div className="flex gap-3">
              <Button
                variant="outline"
                onClick={handleEdit}
                leftIcon={<PiPencil />}
                data-test="branch-view-edit"
              >
                Edit
              </Button>
              <Button
                variant="danger"
                onClick={() => setShowDeleteConfirm(true)}
                leftIcon={<PiTrash />}
                data-test="branch-view-delete"
              >
                Delete
              </Button>
            </div>
          </div>
        </div>

        {/* Body */}
        <div className="px-6 py-6 space-y-6">
          {error && <ErrorMessage message={error} />}

          {/* Branch Info */}
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-500">Library System Name</p>
                <p className="text-gray-900">{branch.librarySystemName}</p>
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
      </PageCard>

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete Branch"
        message="Are you sure you want to delete this branch? This action cannot be undone."
        confirmText="Yes, Delete"
        variant="danger"
        isLoading={deleteBranch.isPending}
        confirmDataTest="confirm-delete-branch"
        cancelDataTest="cancel-delete-branch"
      />
    </div>
  )
}
