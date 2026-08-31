// (c) Copyright 2025 by Muczynski
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import type { UserDto } from '@/types/dtos'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { IconButton } from '@/components/ui/IconButton'
import { DeleteIcon, ViewIcon } from '@/components/ui/Icons'

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
      header: 'Name',
      accessor: (user) => (
        <div className="flex items-center gap-2">
          <span className="font-medium text-gray-900">{user.username}</span>
          {user.ssoSubjectId && (
            <StatusBadge tone="info" shape="rounded">SSO</StatusBadge>
          )}
        </div>
      ),
      width: '20%',
    },
    {
      key: 'email',
      header: 'Email',
      accessor: (user) => (
        <span className="text-gray-700" data-test={`user-email-${user.id}`}>
          {user.email?.trim() ? user.email : '—'}
        </span>
      ),
      width: '20%',
      hideOnMobile: true,
    },
    {
      key: 'phone',
      header: 'Phone',
      accessor: (user) => (
        <span className="text-gray-700" data-test={`user-phone-${user.id}`}>
          {user.phone?.trim() ? user.phone : '—'}
        </span>
      ),
      width: '16%',
      hideOnMobile: true,
    },
    {
      key: 'authorities',
      header: 'Authority',
      accessor: (user) => {
        const isLibrarian = user.authorities?.includes('LIBRARIAN')
        const displayAuthority = user.authorities?.[0] || 'USER'
        return (
          <StatusBadge tone={isLibrarian ? 'emphasis' : 'neutral'}>
            {displayAuthority}
          </StatusBadge>
        )
      },
      width: '15%',
    },
    {
      key: 'activeLoansCount',
      header: 'Active Loans',
      accessor: (user) => (
        <span
          className={`text-sm ${
            (user.activeLoansCount ?? 0) > 0
              ? 'text-primary-600 font-medium'
              : 'text-gray-500'
          }`}
        >
          {user.activeLoansCount ?? 0}
        </span>
      ),
      width: '12%',
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
          <IconButton
            to={`/users/${user.id}`}
            icon={<ViewIcon />}
            label="View Details"
            onClick={(e) => e.stopPropagation()}
            data-test={`view-user-${user.id}`}
          />
          <IconButton
            icon={<DeleteIcon />}
            label="Delete"
            tone="danger"
            onClick={(e) => {
              e.stopPropagation()
              onDelete(user)
            }}
            data-test={`delete-user-${user.id}`}
          />
        </>
      )}
      isLoading={isLoading}
      emptyMessage="No users found"
    />
  )
}
