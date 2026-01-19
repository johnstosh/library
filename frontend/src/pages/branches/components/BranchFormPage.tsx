// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useCreateBranch, useUpdateBranch } from '@/api/branches'
import type { BranchDto } from '@/types/dtos'

interface BranchFormPageProps {
  title: string
  branch?: BranchDto
  onSuccess: () => void
  onCancel: () => void
}

export function BranchFormPage({ title, branch, onSuccess, onCancel }: BranchFormPageProps) {
  const isEditing = !!branch

  const [formData, setFormData] = useState({
    branchName: '',
    librarySystemName: '',
  })
  const [error, setError] = useState('')
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)

  const createBranch = useCreateBranch()
  const updateBranch = useUpdateBranch()

  useEffect(() => {
    if (branch) {
      setFormData({
        branchName: branch.branchName,
        librarySystemName: branch.librarySystemName,
      })
    }
  }, [branch])

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
        await updateBranch.mutateAsync({ id: branch.id, branch: formData })
      } else {
        await createBranch.mutateAsync(formData)
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

  const isLoading = createBranch.isPending || updateBranch.isPending

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
