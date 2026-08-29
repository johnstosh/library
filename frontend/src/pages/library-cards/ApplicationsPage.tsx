// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import {
  useApplications,
  useApproveApplication,
  useDeleteApplication,
  type AppliedDto,
} from '@/api/library-cards'
import { PiCheckCircle } from 'react-icons/pi'
import { IconButton } from '@/components/ui/IconButton'
import { DeleteIcon } from '@/components/ui/Icons'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TableSummary } from '@/components/table/TableSummary'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useToast } from '@/hooks/useToast'

function toActionError(error: unknown, fallback: string): string {
  return error instanceof Error && error.message.trim() ? error.message : fallback
}

export function ApplicationsPage() {
  const toast = useToast()
  const [approvingId, setApprovingId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [actionError, setActionError] = useState('')

  const { data: applications = [], isLoading, isFetching, error } = useApplications()
  const approveApplication = useApproveApplication()
  const deleteApplication = useDeleteApplication()

  const openApproveDialog = (id: number) => {
    setActionError('')
    setApprovingId(id)
  }

  const closeApproveDialog = () => {
    if (approveApplication.isPending) return
    setApprovingId(null)
  }

  const openDeleteDialog = (id: number) => {
    setActionError('')
    setDeletingId(id)
  }

  const closeDeleteDialog = () => {
    if (deleteApplication.isPending) return
    setDeletingId(null)
  }

  const handleApprove = async () => {
    if (approvingId === null) return

    try {
      await approveApplication.mutateAsync(approvingId)
      setApprovingId(null)
      setActionError('')
    } catch (error) {
      console.error('Failed to approve application:', error)
      const message = toActionError(error, 'Failed to approve application. Please try again.')
      setActionError(message)
      toast.error(message)
    }
  }

  const handleDelete = async () => {
    if (deletingId === null) return

    try {
      await deleteApplication.mutateAsync(deletingId)
      setDeletingId(null)
      setActionError('')
    } catch (error) {
      console.error('Failed to delete application:', error)
      const message = toActionError(error, 'Failed to delete application. Please try again.')
      setActionError(message)
      toast.error(message)
    }
  }

  const columns: Column<AppliedDto>[] = [
    {
      key: 'id',
      header: 'ID',
      accessor: (app) => (
        <span className="font-mono text-sm text-gray-500">{app.id}</span>
      ),
      width: '15%',
    },
    {
      key: 'name',
      header: 'Applicant Name',
      accessor: (app) => (
        <span className="font-medium text-gray-900">{app.name}</span>
      ),
      width: '40%',
    },
    {
      key: 'email',
      header: 'Email',
      accessor: (app) => (
        <span className="text-sm text-gray-700">{app.email || '—'}</span>
      ),
      width: '30%',
    },
  ]

  const deletingApplication = applications.find((app) => app.id === deletingId)

  return (
    <div>
      <PageHeader
        title="Library Card Applications"
        description="Review and approve library card applications"
      />

      {error && (
        <ErrorMessage message={`Error loading applications: ${error.message}`} className="mb-4" />
      )}

      {actionError && (
        <ErrorMessage message={actionError} className="mb-4" data-test="application-action-error" />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4">
          <DataTable
            data={applications}
            columns={columns}
            keyExtractor={(app) => app.id}
            actions={(app) => (
              <>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => openApproveDialog(app.id)}
                  leftIcon={<PiCheckCircle />}
                  data-test={`approve-application-${app.id}`}
                >
                  Approve
                </Button>
                <IconButton
                  icon={<DeleteIcon />}
                  label="Delete"
                  tone="danger"
                  onClick={() => openDeleteDialog(app.id)}
                  data-test={`delete-application-${app.id}`}
                />
              </>
            )}
            isLoading={isLoading}
            emptyMessage="No pending applications"
          />
        </div>

        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary
          count={applications.length}
          singular="pending application"
          plural="pending applications"
          isLoading={isLoading}
        />
      </PageCard>

      {/* Help Text */}
      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="text-sm font-medium text-blue-900 mb-2">Managing Applications:</h3>
        <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
          <li><strong>Approve:</strong> Creates a new user account with USER authority. The applicant can then log in.</li>
          <li><strong>Delete:</strong> Rejects the application without creating an account.</li>
          <li>Approved applications are automatically removed from this list.</li>
        </ul>
      </div>

      {/* Approve Confirmation */}
      <ConfirmDialog
        isOpen={approvingId !== null}
        onClose={closeApproveDialog}
        onConfirm={handleApprove}
        title="Approve Application"
        message={`Approve library card application for "${applications.find(app => app.id === approvingId)?.name}"? This will create a new user account.`}
        error={approvingId !== null ? actionError : undefined}
        confirmText="Approve"
        variant="primary"
        isLoading={approveApplication.isPending}
      />

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={deletingId !== null}
        onClose={closeDeleteDialog}
        onConfirm={handleDelete}
        title="Delete Application"
        message={`Delete application for "${deletingApplication?.name}"? This action cannot be undone.`}
        error={deletingId !== null ? actionError : undefined}
        confirmText="Delete"
        variant="danger"
        isLoading={deleteApplication.isPending}
      />
    </div>
  )
}
