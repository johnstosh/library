// (c) Copyright 2025 by Muczynski
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import type { UserDto } from '@/types/dtos'
import { PiEye, PiTrash } from 'react-icons/pi'

interface UserTableProps {
  users: UserDto[]
  isLoading: boolean
  onView: (user: UserDto) => void
  onDelete: (user: UserDto) => void
  selectedIds: Set<number>
  onSelectToggle: (id: number) => void
  onSelectAll: () => void
  selectAll: boolean
}

export function UserTable({
  users,
  isLoading,
  onView,
  onDelete,
  selectedIds,
  onSelectToggle,
  onSelectAll,
  selectAll,
}: UserTableProps) {
  const columns: Column<UserDto>[] = [
    {
      key: 'username',
      header: 'Username',
      accessor: (user) => (
        <div className="flex items-center gap-2">
          <span className="font-medium text-gray-900">{user.username}</span>
          {user.ssoSubjectId && (
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
              SSO
            </span>
          )}
        </div>
      ),
      width: '35%',
    },
    {
      key: 'authorities',
      header: 'Authority',
      accessor: (user) => {
        const isLibrarian = user.authorities?.includes('LIBRARIAN')
        const displayAuthority = user.authorities?.[0] || 'USER'
        return (
          <span
            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
              isLibrarian
                ? 'bg-purple-100 text-purple-800'
                : 'bg-gray-100 text-gray-800'
            }`}
          >
            {displayAuthority}
          </span>
        )
      },
      width: '20%',
    },
    {
      key: 'activeLoansCount',
      header: 'Active Loans',
      accessor: (user) => (
        <span
          className={`text-sm ${
            (user.activeLoansCount ?? 0) > 0
              ? 'text-blue-600 font-medium'
              : 'text-gray-500'
          }`}
        >
          {user.activeLoansCount ?? 0}
        </span>
      ),
      width: '15%',
    },
    {
      key: 'id',
      header: 'ID',
      accessor: (user) => (
        <span className="text-sm text-gray-500">{user.id}</span>
      ),
      width: '10%',
    },
  ]

  return (
    <DataTable
      data={users}
      columns={columns}
      keyExtractor={(user) => user.id}
      selectable
      selectedIds={selectedIds}
      onSelectToggle={onSelectToggle}
      onSelectAll={onSelectAll}
      selectAll={selectAll}
      onRowClick={onView}
      actions={(user) => (
        <>
          <button
            onClick={(e) => {
              e.stopPropagation()
              onView(user)
            }}
            className="text-gray-600 hover:text-gray-900 transition-colors"
            data-test={`view-user-${user.id}`}
            title="View Details"
          >
            <PiEye className="w-5 h-5" />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation()
              onDelete(user)
            }}
            className="text-red-600 hover:text-red-900 transition-colors"
            data-test={`delete-user-${user.id}`}
            title="Delete"
          >
            <PiTrash className="w-5 h-5" />
          </button>
        </>
      )}
      isLoading={isLoading}
      emptyMessage="No users found"
    />
  )
}
