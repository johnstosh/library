// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { UserTable } from './components/UserTable'
import { UserForm, type UserFormData } from './components/UserForm'
import {
  useUsers,
  useCreateUser,
  useUpdateUser,
  useDeleteUser,
  useDeleteUsers,
} from '@/api/users'
import { useUiStore } from '@/stores/uiStore'
import type { UserDto } from '@/types/dtos'
import { hashPassword } from '@/utils/auth'

export function UsersPage() {
  const [showForm, setShowForm] = useState(false)
  const [editingUser, setEditingUser] = useState<UserDto | undefined>()
  const [deletingUser, setDeletingUser] = useState<UserDto | null>(null)
  const [showBulkDelete, setShowBulkDelete] = useState(false)

  const { data: users = [], isLoading } = useUsers()
  const createUser = useCreateUser()
  const updateUser = useUpdateUser()
  const deleteUser = useDeleteUser()
  const deleteUsers = useDeleteUsers()

  const selectedIds = useUiStore((state) => state.usersTable.selectedIds)
  const selectAll = useUiStore((state) => state.usersTable.selectAll)
  const setSelectedIds = useUiStore((state) => state.setSelectedIds)
  const toggleSelectAll = useUiStore((state) => state.toggleSelectAll)
  const clearSelection = useUiStore((state) => state.clearSelection)

  const handleCreate = () => {
    setEditingUser(undefined)
    setShowForm(true)
  }

  const handleEdit = (user: UserDto) => {
    setEditingUser(user)
    setShowForm(true)
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setEditingUser(undefined)
  }

  const handleSubmit = async (data: UserFormData) => {
    // Hash password if provided
    const hashedPassword = data.password ? await hashPassword(data.password) : undefined

    if (editingUser) {
      // Update existing user
      const updateData = {
        username: data.username,
        authority: data.authority,
        ...(hashedPassword && { password: hashedPassword }),
      }
      await updateUser.mutateAsync({
        id: editingUser.id,
        user: updateData,
      })
    } else {
      // Create new user
      if (!hashedPassword) {
        throw new Error('Password is required for new users')
      }
      await createUser.mutateAsync({
        username: data.username,
        password: hashedPassword,
        authority: data.authority,
      })
    }
  }

  const handleDelete = (user: UserDto) => {
    setDeletingUser(user)
  }

  const confirmDelete = async () => {
    if (!deletingUser) return
    await deleteUser.mutateAsync(deletingUser.id)
    setDeletingUser(null)
  }

  const handleBulkDelete = () => {
    if (selectedIds.size === 0) return
    setShowBulkDelete(true)
  }

  const confirmBulkDelete = async () => {
    await deleteUsers.mutateAsync(Array.from(selectedIds))
    clearSelection('usersTable')
    setShowBulkDelete(false)
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

  const isFormLoading = createUser.isPending || updateUser.isPending

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Users</h1>
        <Button variant="primary" onClick={handleCreate} data-test="create-user">
          Create User
        </Button>
      </div>

      {/* Bulk Actions */}
      {selectedIds.size > 0 && (
        <div className="mb-4 bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <span className="text-sm font-medium text-blue-900">
                {selectedIds.size} user{selectedIds.size === 1 ? '' : 's'} selected
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => clearSelection('usersTable')}
              >
                Clear
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

      {/* Users Table */}
      <div className="bg-white rounded-lg shadow">
        <div className="p-4">
          <UserTable
            users={users}
            isLoading={isLoading}
            onEdit={handleEdit}
            onDelete={handleDelete}
            selectedIds={selectedIds}
            onSelectToggle={handleSelectToggle}
            onSelectAll={handleSelectAll}
            selectAll={selectAll}
          />
        </div>

        {!isLoading && users.length > 0 && (
          <div className="px-4 py-3 border-t border-gray-200 bg-gray-50">
            <p className="text-sm text-gray-700">
              Showing {users.length} {users.length === 1 ? 'user' : 'users'}
            </p>
          </div>
        )}
      </div>

      {/* Create/Edit Form */}
      <UserForm
        isOpen={showForm}
        onClose={handleCloseForm}
        onSubmit={handleSubmit}
        initialData={editingUser}
        isLoading={isFormLoading}
      />

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
