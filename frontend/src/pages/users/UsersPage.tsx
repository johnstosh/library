// (c) Copyright 2025 by Muczynski
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TableSummary } from '@/components/table/TableSummary'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useToast } from '@/hooks/useToast'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { UserTable } from './components/UserTable'
import { UserFilters } from './components/UserFilters'
import { applyUserFilters } from '@/utils/userChipFilters'
import {
  useUsers,
  useDeleteUser,
  useDeleteUsers,
} from '@/api/users'
import { useUiStore, useUsersChips, useUsersSearchQuery } from '@/stores/uiStore'
import type { UserDto } from '@/types/dtos'

export function UsersPage() {
  const navigate = useNavigate()
  const [deletingUser, setDeletingUser] = useState<UserDto | null>(null)
  const [showBulkDelete, setShowBulkDelete] = useState(false)

  const { data: allUsers = [], isLoading, isFetching, error } = useUsers()
  const deleteUser = useDeleteUser()
  const deleteUsers = useDeleteUsers()
  const toast = useToast()

  const chips = useUsersChips()
  const searchQuery = useUsersSearchQuery()
  const selectedIds = useUiStore((state) => state.usersTable.selectedIds)
  const selectAll = useUiStore((state) => state.usersTable.selectAll)
  const setSelectedIds = useUiStore((state) => state.setSelectedIds)
  const toggleSelectAll = useUiStore((state) => state.toggleSelectAll)
  const clearSelection = useUiStore((state) => state.clearSelection)
  const toggleUsersChip = useUiStore((state) => state.toggleUsersChip)
  const setUsersSearchQuery = useUiStore((state) => state.setUsersSearchQuery)

  const users = useMemo(
    () => applyUserFilters(allUsers, chips, searchQuery),
    [allUsers, chips, searchQuery]
  )

  const handleCreate = () => {
    navigate('/users/new')
  }

  const handleView = (user: UserDto) => {
    navigate(`/users/${user.id}`)
  }

  const handleDelete = (user: UserDto) => {
    setDeletingUser(user)
  }

  const confirmDelete = async () => {
    if (!deletingUser) return
    try {
      await deleteUser.mutateAsync(deletingUser.id)
      setDeletingUser(null)
    } catch (error) {
      console.error('Failed to delete user:', error)
      toast.error('Failed to delete user')
    }
  }

  const handleBulkDelete = () => {
    if (selectedIds.size === 0) return
    setShowBulkDelete(true)
  }

  const confirmBulkDelete = async () => {
    try {
      await deleteUsers.mutateAsync(Array.from(selectedIds))
      clearSelection('usersTable')
      setShowBulkDelete(false)
    } catch (error) {
      console.error('Failed to delete users:', error)
      toast.error('Failed to delete users')
    }
  }

  const handleSelectToggle = (id: number) => {
    const newSelected = new Set(selectedIds)
    if (newSelected.has(id)) {
      newSelected.delete(id)
    } else {
      newSelected.add(id)
    }
    setSelectedIds('usersTable', newSelected)
  }

  const handleSelectAll = () => {
    if (selectAll) {
      clearSelection('usersTable')
    } else {
      const allIds = new Set(users.map((u) => u.id))
      setSelectedIds('usersTable', allIds)
      toggleSelectAll('usersTable')
    }
  }

  return (
    <div>
      <PageHeader
        title="Users"
        actions={
          <Button variant="primary" onClick={handleCreate} data-test="create-user">
            Add User
          </Button>
        }
      />

      {error && (
        <ErrorMessage message={`Error loading users: ${error.message}`} className="mb-4" />
      )}

      <PageCard padding={false} className="relative">
        {selectedIds.size > 0 && (
          <div className="m-4 bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-blue-900">
                {selectedIds.size} user{selectedIds.size === 1 ? '' : 's'} selected
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => clearSelection('usersTable')}
                >
                  Clear Selection
                </Button>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={handleBulkDelete}
                  data-test="bulk-delete-users"
                >
                  Delete Selected
                </Button>
              </div>
            </div>
          </div>
        )}

        <div className="p-4 border-b border-gray-200 space-y-3">
          <Input
            type="search"
            label="Search users"
            hideLabel
            placeholder="Search by name, email, or SSO provider..."
            value={searchQuery}
            onChange={(e) => setUsersSearchQuery(e.target.value)}
            data-test="user-search"
          />
          <UserFilters chips={chips} onToggle={toggleUsersChip} />
        </div>

        <div className="p-4">
          <UserTable
            users={users}
            isLoading={isLoading}
            onView={handleView}
            onDelete={handleDelete}
            selectedIds={selectedIds}
            onSelectToggle={handleSelectToggle}
            onSelectAll={handleSelectAll}
            selectAll={selectAll}
          />
        </div>

        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary count={users.length} singular="user" plural="users" isLoading={isLoading} />
      </PageCard>

      {/* Delete Confirmation */}
      <ConfirmDialog
        isOpen={deletingUser !== null}
        onClose={() => setDeletingUser(null)}
        onConfirm={confirmDelete}
        title="Delete User"
        message={`Are you sure you want to delete user "${deletingUser?.username}"? This action cannot be undone.`}
        confirmText="Delete"
        variant="danger"
        isLoading={deleteUser.isPending}
      />

      {/* Bulk Delete Confirmation */}
      <ConfirmDialog
        isOpen={showBulkDelete}
        onClose={() => setShowBulkDelete(false)}
        onConfirm={confirmBulkDelete}
        title="Delete Users"
        message={`Are you sure you want to delete ${selectedIds.size} user${selectedIds.size === 1 ? '' : 's'}? This action cannot be undone.`}
        confirmText="Delete All"
        variant="danger"
        isLoading={deleteUsers.isPending}
      />
    </div>
  )
}
