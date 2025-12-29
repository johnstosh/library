// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import {
  useLibraries,
  useLibraryStatistics,
  useCreateLibrary,
  useUpdateLibrary,
  useDeleteLibrary,
} from '@/api/libraries'
import type { LibraryDto } from '@/types/dtos'

export function LibrariesPage() {
  const [showForm, setShowForm] = useState(false)
  const [editingLibrary, setEditingLibrary] = useState<LibraryDto | null>(null)
  const [deleteLibraryId, setDeleteLibraryId] = useState<number | null>(null)

  const [formData, setFormData] = useState({
    name: '',
    hostname: '',
  })
  const [error, setError] = useState('')

  const { data: libraries = [], isLoading } = useLibraries()
  const { data: statistics = [] } = useLibraryStatistics()
  const createLibrary = useCreateLibrary()
  const updateLibrary = useUpdateLibrary()
  const deleteLibrary = useDeleteLibrary()

  const handleAdd = () => {
    setEditingLibrary(null)
    setFormData({ name: '', hostname: '' })
    setError('')
    setShowForm(true)
  }

  const handleEdit = (library: LibraryDto) => {
    setEditingLibrary(library)
    setFormData({
      name: library.name,
      hostname: library.hostname,
    })
    setError('')
    setShowForm(true)
  }

  const handleCloseForm = () => {
    setShowForm(false)
    setEditingLibrary(null)
    setFormData({ name: '', hostname: '' })
    setError('')
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!formData.name || !formData.hostname) {
      setError('Name and hostname are required')
      return
    }

    try {
      if (editingLibrary) {
        await updateLibrary.mutateAsync({ id: editingLibrary.id, library: formData })
      } else {
        await createLibrary.mutateAsync(formData)
      }
      handleCloseForm()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    }
  }

  const handleDelete = async () => {
    if (deleteLibraryId === null) return

    try {
      await deleteLibrary.mutateAsync(deleteLibraryId)
      setDeleteLibraryId(null)
    } catch (error) {
      console.error('Failed to delete library:', error)
    }
  }

  // Get statistics for a library
  const getLibraryStats = (libraryId: number) => {
    return statistics.find((s) => s.libraryId === libraryId)
  }

  const columns: Column<LibraryDto>[] = [
    {
      key: 'name',
      header: 'Name',
      accessor: (library) => <div className="font-medium text-gray-900">{library.name}</div>,
      width: '30%',
    },
    {
      key: 'hostname',
      header: 'Hostname',
      accessor: (library) => <div className="text-gray-600">{library.hostname}</div>,
      width: '30%',
    },
    {
      key: 'bookCount',
      header: 'Books',
      accessor: (library) => {
        const stats = getLibraryStats(library.id)
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
      accessor: (library) => {
        const stats = getLibraryStats(library.id)
        return (
          <div className="text-gray-900">
            {stats?.activeLoansCount !== undefined ? stats.activeLoansCount.toLocaleString() : '-'}
          </div>
        )
      },
      width: '10%',
    },
  ]

  const isSubmitting = createLibrary.isPending || updateLibrary.isPending

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Libraries</h1>
        <Button variant="primary" onClick={handleAdd} data-test="add-library">
          Add Library
        </Button>
      </div>

      <div className="bg-white rounded-lg shadow">
        <div className="p-4">
          <DataTable
            data={libraries}
            columns={columns}
            keyExtractor={(library) => library.id}
            actions={(library) => (
              <>
                <button
                  onClick={() => handleEdit(library)}
                  className="text-blue-600 hover:text-blue-900"
                  data-test={`edit-library-${library.id}`}
                  title="Edit"
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                    />
                  </svg>
                </button>
                <button
                  onClick={() => setDeleteLibraryId(library.id)}
                  className="text-red-600 hover:text-red-900"
                  data-test={`delete-library-${library.id}`}
                  title="Delete"
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                    />
                  </svg>
                </button>
              </>
            )}
            isLoading={isLoading}
            emptyMessage="No libraries found"
          />
        </div>

        {!isLoading && libraries.length > 0 && (
          <div className="px-4 py-3 border-t border-gray-200 bg-gray-50">
            <p className="text-sm text-gray-700">
              Showing {libraries.length} {libraries.length === 1 ? 'library' : 'libraries'}
            </p>
          </div>
        )}
      </div>

      <Modal
        isOpen={showForm}
        onClose={handleCloseForm}
        title={editingLibrary ? 'Edit Library' : 'Add New Library'}
        size="md"
        footer={
          <div className="flex justify-end gap-3">
            <Button variant="ghost" onClick={handleCloseForm} disabled={isSubmitting} data-test="library-form-cancel">
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleSubmit}
              isLoading={isSubmitting}
              data-test="library-form-submit"
            >
              {editingLibrary ? 'Update' : 'Create'}
            </Button>
          </div>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {error && <ErrorMessage message={error} />}

          <Input
            label="Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            required
            data-test="library-name"
          />

          <Input
            label="Hostname"
            value={formData.hostname}
            onChange={(e) => setFormData({ ...formData, hostname: e.target.value })}
            required
            helpText="The domain name for this library (e.g., mylibrary.com)"
            data-test="library-hostname"
          />
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={deleteLibraryId !== null}
        onClose={() => setDeleteLibraryId(null)}
        onConfirm={handleDelete}
        title="Delete Library"
        message="Are you sure you want to delete this library? This action cannot be undone."
        confirmText="Delete"
        variant="danger"
        isLoading={deleteLibrary.isPending}
      />
    </div>
  )
}
