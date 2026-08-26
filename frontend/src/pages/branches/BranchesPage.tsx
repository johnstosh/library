// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import {
  useBranches,
  useBranchStatistics,
  useDeleteBranch,
} from '@/api/branches'
import type { BranchDto } from '@/types/dtos'
import { IconButton, TEXT_LINK_CLASS } from '@/components/ui/IconButton'
import { DeleteIcon, EditIcon, ViewIcon } from '@/components/ui/Icons'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TableSummary } from '@/components/table/TableSummary'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useToast } from '@/hooks/useToast'

export function BranchesPage() {
  const navigate = useNavigate()
  const [deleteBranchId, setDeleteBranchId] = useState<number | null>(null)

  const { data: branches = [], isLoading, isFetching, error } = useBranches()
  const toast = useToast()
  const { data: statistics = [] } = useBranchStatistics()
  const deleteBranch = useDeleteBranch()

  const handleAdd = () => {
    navigate('/branches/new')
  }

  const handleEdit = (branch: BranchDto) => {
    navigate(`/branches/${branch.id}/edit`)
  }

  const handleView = (branch: BranchDto) => {
    navigate(`/branches/${branch.id}`)
  }

  const handleDelete = async () => {
    if (deleteBranchId === null) return

    try {
      await deleteBranch.mutateAsync(deleteBranchId)
      setDeleteBranchId(null)
    } catch (error) {
      console.error('Failed to delete branch:', error)
      toast.error('Failed to delete branch')
    }
  }

  // Get statistics for a branch
  const getBranchStats = (branchId: number) => {
    return statistics.find((s) => s.branchId === branchId)
  }

  const columns: Column<BranchDto>[] = [
    {
      key: 'name',
      header: 'Branch Name',
      accessor: (branch) => (
        <button
          onClick={() => handleView(branch)}
          className={`font-medium ${TEXT_LINK_CLASS} text-left`}
          data-test={`view-branch-${branch.id}`}
        >
          {branch.branchName}
        </button>
      ),
      width: '30%',
    },
    {
      key: 'librarySystemName',
      header: 'Library System',
      accessor: (branch) => <div className="text-gray-600">{branch.librarySystemName}</div>,
      width: '30%',
    },
    {
      key: 'bookCount',
      header: 'Books',
      accessor: (branch) => {
        const stats = getBranchStats(branch.id)
        return (
          <div className="text-gray-900">
            {stats?.bookCount !== undefined ? stats.bookCount.toLocaleString() : '-'}
          </div>
        )
      },
      width: '15%',
    },
    {
      key: 'activeLoans',
      header: 'Active Loans',
      accessor: (branch) => {
        const stats = getBranchStats(branch.id)
        return (
          <div className="text-gray-900">
            {stats?.activeLoansCount !== undefined ? stats.activeLoansCount.toLocaleString() : '-'}
          </div>
        )
      },
      width: '10%',
    },
  ]

  return (
    <div>
      <PageHeader
        title="Branches"
        actions={
          <Button variant="primary" onClick={handleAdd} data-test="add-branch">
            Add Branch
          </Button>
        }
      />

      {error && (
        <ErrorMessage message={`Error loading branches: ${error.message}`} className="mb-4" />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4">
          <DataTable
            data={branches}
            columns={columns}
            keyExtractor={(branch) => branch.id}
            actions={(branch) => (
              <>
                <IconButton
                  icon={<ViewIcon />}
                  label="View Details"
                  onClick={() => handleView(branch)}
                  data-test={`view-branch-details-${branch.id}`}
                />
                <IconButton
                  icon={<EditIcon />}
                  label="Edit"
                  tone="primary"
                  onClick={() => handleEdit(branch)}
                  data-test={`edit-branch-${branch.id}`}
                />
                <IconButton
                  icon={<DeleteIcon />}
                  label="Delete"
                  tone="danger"
                  onClick={() => setDeleteBranchId(branch.id)}
                  data-test={`delete-branch-${branch.id}`}
                />
              </>
            )}
            isLoading={isLoading}
            emptyMessage="No branches found"
          />
        </div>

        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary count={branches.length} singular="branch" plural="branches" isLoading={isLoading} />
      </PageCard>

      <ConfirmDialog
        isOpen={deleteBranchId !== null}
        onClose={() => setDeleteBranchId(null)}
        onConfirm={handleDelete}
        title="Delete Branch"
        message="Are you sure you want to delete this branch? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteBranch.isPending}
      />
    </div>
  )
}
