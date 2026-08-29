// (c) Copyright 2025 by Muczynski
import { useMemo, useState } from 'react'
import { Button } from '@/components/ui/Button'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Input } from '@/components/ui/Input'
import {
  useApplications,
  useApproveApplication,
  useDeleteApplication,
  useUpdateApplicationStatus,
  type AppliedDto,
} from '@/api/library-cards'
import { PiArrowCounterClockwise, PiCheckCircle, PiQuestion, PiXCircle } from 'react-icons/pi'
import { IconButton } from '@/components/ui/IconButton'
import { DeleteIcon } from '@/components/ui/Icons'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TableSummary } from '@/components/table/TableSummary'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { useToast } from '@/hooks/useToast'
import { ApplicationFilters } from './components/ApplicationFilters'
import {
  applyApplicationFilters,
  isApprovedStatus,
  isAwaitingReview,
  isNotApprovedStatus,
} from '@/utils/applicationChipFilters'
import { applicationStatusLabel, applicationStatusTone } from '@/utils/status'
import {
  useApplicationsChips,
  useApplicationsSearchQuery,
  useUiStore,
} from '@/stores/uiStore'

function toActionError(error: unknown, fallback: string): string {
  return error instanceof Error && error.message.trim() ? error.message : fallback
}

type PendingAction =
  | { kind: 'approve'; application: AppliedDto }
  | { kind: 'question'; application: AppliedDto }
  | { kind: 'notApprove'; application: AppliedDto }
  | { kind: 'reopen'; application: AppliedDto }
  | { kind: 'delete'; application: AppliedDto }

export function ApplicationsPage() {
  const toast = useToast()
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null)
  const [actionError, setActionError] = useState('')

  const { data: allApplications = [], isLoading, isFetching, error } = useApplications()
  const approveApplication = useApproveApplication()
  const updateStatus = useUpdateApplicationStatus()
  const deleteApplication = useDeleteApplication()

  const chips = useApplicationsChips()
  const searchQuery = useApplicationsSearchQuery()
  const toggleApplicationsChip = useUiStore((state) => state.toggleApplicationsChip)
  const setApplicationsSearchQuery = useUiStore((state) => state.setApplicationsSearchQuery)

  const applications = useMemo(
    () => applyApplicationFilters(allApplications, chips, searchQuery),
    [allApplications, chips, searchQuery]
  )

  const isBusy =
    approveApplication.isPending || updateStatus.isPending || deleteApplication.isPending

  const openAction = (action: PendingAction) => {
    setActionError('')
    setPendingAction(action)
  }

  const closeAction = () => {
    if (isBusy) return
    setPendingAction(null)
  }

  const handleConfirm = async () => {
    if (!pendingAction) return
    const { kind, application } = pendingAction

    try {
      if (kind === 'approve') {
        await approveApplication.mutateAsync(application.id)
      } else if (kind === 'question') {
        await updateStatus.mutateAsync({ id: application.id, status: 'QUESTION' })
      } else if (kind === 'notApprove') {
        await updateStatus.mutateAsync({ id: application.id, status: 'NOT_APPROVED' })
      } else if (kind === 'reopen') {
        await updateStatus.mutateAsync({ id: application.id, status: 'PENDING' })
      } else {
        await deleteApplication.mutateAsync(application.id)
      }
      setPendingAction(null)
      setActionError('')
    } catch (error) {
      console.error(`Failed to ${kind} application:`, error)
      const fallback =
        kind === 'approve'
          ? 'Failed to approve application. Please try again.'
          : kind === 'delete'
            ? 'Failed to delete application. Please try again.'
            : 'Failed to update application. Please try again.'
      const message = toActionError(error, fallback)
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
      width: '10%',
    },
    {
      key: 'name',
      header: 'Applicant Name',
      accessor: (app) => (
        <span className="font-medium text-gray-900">{app.name}</span>
      ),
      width: '28%',
    },
    {
      key: 'email',
      header: 'Email',
      accessor: (app) => (
        <span className="text-sm text-gray-700">{app.email || '—'}</span>
      ),
      width: '24%',
    },
    {
      key: 'status',
      header: 'Status',
      accessor: (app) => (
        <StatusBadge
          tone={applicationStatusTone(app.status)}
          data-test={`application-status-${app.id}`}
        >
          {applicationStatusLabel(app.status)}
        </StatusBadge>
      ),
      width: '14%',
    },
  ]

  const dialogCopy = (() => {
    if (!pendingAction) {
      return { title: '', message: '', confirmText: 'Confirm', variant: 'primary' as const }
    }
    const name = pendingAction.application.name
    switch (pendingAction.kind) {
      case 'approve':
        return {
          title: 'Approve Application',
          message: `Approve library card application for "${name}"? This will create a new user account.`,
          confirmText: 'Approve',
          variant: 'primary' as const,
        }
      case 'question':
        return {
          title: 'Mark as Question',
          message: `Mark the application for "${name}" as needing more information?`,
          confirmText: 'Mark as Question',
          variant: 'primary' as const,
        }
      case 'notApprove':
        return {
          title: 'Do Not Approve',
          message: `Decline the application for "${name}" without creating an account?`,
          confirmText: 'Do Not Approve',
          variant: 'danger' as const,
        }
      case 'reopen':
        return {
          title: 'Restore to Pending',
          message: `Put the application for "${name}" back in the review queue?`,
          confirmText: 'Restore to Pending',
          variant: 'primary' as const,
        }
      case 'delete':
        return {
          title: 'Delete Application',
          message: `Delete application for "${name}"? This action cannot be undone.`,
          confirmText: 'Delete',
          variant: 'danger' as const,
        }
    }
  })()

  return (
    <div>
      <PageHeader
        title="Library Card Applications"
        description="Review and process library card applications"
      />

      {error && (
        <ErrorMessage message={`Error loading applications: ${error.message}`} className="mb-4" />
      )}

      {actionError && (
        <ErrorMessage message={actionError} className="mb-4" data-test="application-action-error" />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200 space-y-3">
          <Input
            type="search"
            label="Search applications"
            hideLabel
            placeholder="Search by name or email..."
            value={searchQuery}
            onChange={(e) => setApplicationsSearchQuery(e.target.value)}
            data-test="application-search"
          />
          <ApplicationFilters chips={chips} onToggle={toggleApplicationsChip} />
        </div>

        <div className="p-4">
          <DataTable
            data={applications}
            columns={columns}
            keyExtractor={(app) => app.id}
            actions={(app) => (
              <>
                {!isApprovedStatus(app.status) && (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => openAction({ kind: 'approve', application: app })}
                    leftIcon={<PiCheckCircle />}
                    data-test={`approve-application-${app.id}`}
                  >
                    Approve
                  </Button>
                )}
                {app.status !== 'QUESTION' && !isApprovedStatus(app.status) && (
                  <IconButton
                    icon={<PiQuestion className="w-5 h-5" />}
                    label="Mark as Question"
                    tone="info"
                    onClick={() => openAction({ kind: 'question', application: app })}
                    data-test={`question-application-${app.id}`}
                  />
                )}
                {isAwaitingReview(app.status) && (
                  <IconButton
                    icon={<PiXCircle className="w-5 h-5" />}
                    label="Do not approve"
                    tone="warning"
                    onClick={() => openAction({ kind: 'notApprove', application: app })}
                    data-test={`not-approve-application-${app.id}`}
                  />
                )}
                {(app.status === 'QUESTION' || isNotApprovedStatus(app.status)) && (
                  <IconButton
                    icon={<PiArrowCounterClockwise className="w-5 h-5" />}
                    label="Restore to pending"
                    tone="primary"
                    onClick={() => openAction({ kind: 'reopen', application: app })}
                    data-test={`reopen-application-${app.id}`}
                  />
                )}
                <IconButton
                  icon={<DeleteIcon />}
                  label="Delete"
                  tone="danger"
                  onClick={() => openAction({ kind: 'delete', application: app })}
                  data-test={`delete-application-${app.id}`}
                />
              </>
            )}
            isLoading={isLoading}
            emptyMessage="No applications match the current filters"
          />
        </div>

        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary
          count={applications.length}
          singular="application"
          plural="applications"
          isLoading={isLoading}
        />
      </PageCard>

      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="text-sm font-medium text-blue-900 mb-2">Managing Applications:</h3>
        <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
          <li><strong>Needs approval</strong> starts on so you see applications waiting for a decision.</li>
          <li><strong>Approve:</strong> Creates a new user account with USER authority. The applicant can then log in.</li>
          <li><strong>Question:</strong> Marks the application as needing more information without creating an account.</li>
          <li><strong>Do not approve:</strong> Declines the application and keeps the record for later review.</li>
          <li><strong>Restore to pending:</strong> Puts a questioned or declined application back in the review queue.</li>
          <li><strong>Delete:</strong> Removes the application. This cannot be undone.</li>
        </ul>
      </div>

      <ConfirmDialog
        isOpen={pendingAction !== null}
        onClose={closeAction}
        onConfirm={handleConfirm}
        title={dialogCopy.title}
        message={dialogCopy.message}
        error={pendingAction !== null ? actionError : undefined}
        confirmText={dialogCopy.confirmText}
        variant={dialogCopy.variant}
        isLoading={isBusy}
      />
    </div>
  )
}
