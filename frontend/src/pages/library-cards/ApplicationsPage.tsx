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
import { PiCheckCircle, PiTrash } from 'react-icons/pi'

export function ApplicationsPage() {
  const [approvingId, setApprovingId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const { data: applications = [], isLoading } = useApplications()
  const approveApplication = useApproveApplication()
  const deleteApplication = useDeleteApplication()

  const handleApprove = async () => {
    if (approvingId === null) return

    try {
      await approveApplication.mutateAsync(approvingId)
      setApprovingId(null)
    } catch (error) {
      console.error('Failed to approve application:', error)
      alert('Failed to approve application. Please try again.')
    }
  }

  const handleDelete = async () => {
    if (deletingId === null) return

    try {
      await deleteApplication.mutateAsync(deletingId)
      setDeletingId(null)
    } catch (error) {
      console.error('Failed to delete application:', error)
      alert('Failed to delete application. Please try again.')
    }
  }

  const columns: Column<AppliedDto>[] = [
    {
      key: 'id',
      header: 'ID',
      accessor: (app) => (
        <span className="font-mono text-sm text-gray-500">{app.id}</span>
      ),
      width: '10%',
    },
    {
      key: 'name',
      header: 'Applicant Name',
      accessor: (app) => (
        <span className="font-medium text-gray-900">{app.name}</span>
      ),
      width: '60%',
    },
    {
      key: 'actions',
      header: 'Actions',
      accessor: () => null,
      width: '30%',
    },
  ]

  const deletingApplication = applications.find((app) => app.id === deletingId)

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Library Card Applications</h1>
          <p className="text-gray-600 mt-1">
            Review and approve library card applications
          </p>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow">
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
                <button
                  onClick={() => setDeletingId(app.id)}
                  className="text-red-600 hover:text-red-900 transition-colors"
                  data-test={`delete-application-${app.id}`}
                  title="Delete"
                >
                  <PiTrash className="w-5 h-5" />
                </button>
              </>
            )}
            isLoading={isLoading}
            emptyMessage="No pending applications"
          />
        </div>

        {!isLoading && applications.length > 0 && (
          <div className="px-4 py-3 border-t border-gray-200 bg-gray-50">
            <p className="text-sm text-gray-700">
              Showing {applications.length} pending {applications.length === 1 ? 'application' : 'applications'}
            </p>
          </div>
        )}
      </div>

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
