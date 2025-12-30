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
    name: '',
    hostname: '',
  })
  const [error, setError] = useState('')
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)

  const createLibrary = useCreateLibrary()
  const updateLibrary = useUpdateLibrary()

  useEffect(() => {
    if (library) {
      setFormData({
        name: library.name,
        hostname: library.hostname,
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

    if (!formData.name || !formData.hostname) {
      setError('Name and hostname are required')
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
          label="Name"
          value={formData.name}
          onChange={(e) => handleFieldChange('name', e.target.value)}
          required
          data-test="library-name"
        />

        <Input
          label="Hostname"
          value={formData.hostname}
          onChange={(e) => handleFieldChange('hostname', e.target.value)}
          required
          helpText="The domain name for this library (e.g., mylibrary.com)"
          data-test="library-hostname"
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
