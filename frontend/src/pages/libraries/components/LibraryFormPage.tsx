// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useCreateLibrary, useUpdateLibrary } from '@/api/libraries'
import type { LibraryDto } from '@/types/dtos'

interface LibraryFormPageProps {
  title: string
  library?: LibraryDto
  onSuccess: () => void
  onCancel: () => void
}

export function LibraryFormPage({ title, library, onSuccess, onCancel }: LibraryFormPageProps) {
  const isEditing = !!library

  const [formData, setFormData] = useState({
    branchName: '',
    librarySystemName: '',
  })
  const [error, setError] = useState('')
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)

  const createLibrary = useCreateLibrary()
  const updateLibrary = useUpdateLibrary()

  useEffect(() => {
    if (library) {
      setFormData({
        branchName: library.branchName,
        librarySystemName: library.librarySystemName,
      })
    }
  }, [library])

  // Warn user about unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges) {
        e.preventDefault()
        e.returnValue = ''
      }
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [hasUnsavedChanges])

  const handleFieldChange = (field: string, value: string) => {
    setFormData({ ...formData, [field]: value })
    setHasUnsavedChanges(true)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!formData.branchName || !formData.librarySystemName) {
      setError('Branch name and library system name are required')
      return
    }

    try {
      if (isEditing) {
        await updateLibrary.mutateAsync({ id: library.id, library: formData })
      } else {
        await createLibrary.mutateAsync(formData)
      }

      setHasUnsavedChanges(false)
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    }
  }

  const handleCancel = () => {
    if (hasUnsavedChanges) {
      if (window.confirm('You have unsaved changes. Are you sure you want to leave?')) {
        onCancel()
      }
    } else {
      onCancel()
    }
  }

  const isLoading = createLibrary.isPending || updateLibrary.isPending

  return (
    <div className="bg-white rounded-lg shadow">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="px-6 py-6 space-y-4">
        {error && <ErrorMessage message={error} />}

        <Input
          label="Branch Name"
          value={formData.branchName}
          onChange={(e) => handleFieldChange('branchName', e.target.value)}
          required
          data-test="library-branch-name"
        />

        <Input
          label="Library System Name"
          value={formData.librarySystemName}
          onChange={(e) => handleFieldChange('librarySystemName', e.target.value)}
          required
          helpText="The name of the library system this branch belongs to"
          data-test="library-system-name"
        />
      </form>

      {/* Footer */}
      <div className="px-6 py-4 bg-gray-50 border-t rounded-b-lg">
        <div className="flex justify-end gap-3">
          <Button
            variant="ghost"
            onClick={handleCancel}
            disabled={isLoading}
            data-test="library-form-cancel"
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            isLoading={isLoading}
            data-test="library-form-submit"
          >
            {isEditing ? 'Update' : 'Create'}
          </Button>
        </div>
      </div>
    </div>
  )
}
