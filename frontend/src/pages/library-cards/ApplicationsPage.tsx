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

export function ApplicationsPage() {
  const toast = useToast()
  const [approvingId, setApprovingId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const { data: applications = [], isLoading, isFetching, error } = useApplications()
  const approveApplication = useApproveApplication()
  const deleteApplication = useDeleteApplication()

  const handleApprove = async () => {
    if (approvingId === null) return

    try {
      await approveApplication.mutateAsync(approvingId)
      setApprovingId(null)
    } catch (error) {
      console.error('Failed to approve application:', error)
      toast.error('Failed to approve application. Please try again.')
    }
  }

  const handleDelete = async () => {
    if (deletingId === null) return

    try {
      await deleteApplication.mutateAsync(deletingId)
      setDeletingId(null)
    } catch (error) {
      console.error('Failed to delete application:', error)
      toast.error('Failed to delete application. Please try again.')
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
      width: '70%',
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
                  onClick={() => setApprovingId(app.id)}
                  leftIcon={<PiCheckCircle />}
                  data-test={`approve-application-${app.id}`}
                >
                  Approve
                </Button>
                <IconButton
                  icon={<DeleteIcon />}
                  label="Delete"
                  tone="danger"
                  onClick={() => setDeletingId(app.id)}
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
        onClose={() => setApprovingId(null)}
        onConfirm={handleApprove}
        title="Approve Application"
        message={`Approve library card application for "${applications.find(app => app.id === approvingId)?.name}"? This will create a new user account.`}
        confirmText="Approve"
        variant="primary"
        isLoading={approveApplication.isPending}
      />

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={deletingId !== null}
        onClose={() => setDeletingId(null)}
        onConfirm={handleDelete}
        title="Delete Application"
        message={`Delete application for "${deletingApplication?.name}"? This action cannot be undone.`}
        confirmText="Delete"
        variant="danger"
        isLoading={deleteApplication.isPending}
      />
    </div>
  )
}
