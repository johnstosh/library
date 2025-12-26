// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import type { UserDto } from '@/types/dtos'
import { UserAuthority } from '@/types/enums'

interface UserFormProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (data: UserFormData) => Promise<void>
  initialData?: UserDto
  isLoading: boolean
}

export interface UserFormData {
  username: string
  password: string
  authority: string
}

export function UserForm({
  isOpen,
  onClose,
  onSubmit,
  initialData,
  isLoading,
}: UserFormProps) {
  const [formData, setFormData] = useState<UserFormData>({
    username: '',
    password: '',
    authority: UserAuthority.USER,
  })
  const [error, setError] = useState('')

  const isEditMode = !!initialData

  useEffect(() => {
    if (initialData) {
      setFormData({
        username: initialData.username,
        password: '', // Password is optional for updates
        authority: initialData.authorities?.[0] || UserAuthority.USER,
      })
    } else {
      setFormData({
        username: '',
        password: '',
        authority: UserAuthority.USER,
      })
    }
    setError('')
  }, [initialData, isOpen])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    // Validation
    if (!formData.username.trim()) {
      setError('Username is required')
      return
    }

    if (!isEditMode && !formData.password) {
      setError('Password is required for new users')
      return
    }

    try {
      await onSubmit(formData)
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    }
  }

  const authorityOptions = [
    { value: UserAuthority.USER, label: 'User' },
    { value: UserAuthority.LIBRARIAN, label: 'Librarian' },
  ]

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditMode ? 'Edit User' : 'Create User'}
      size="md"
      footer={
        <div className="flex justify-end gap-3">
          <Button variant="ghost" onClick={onClose} disabled={isLoading} data-test="user-cancel">
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            isLoading={isLoading}
            data-test="user-submit"
          >
            {isEditMode ? 'Update' : 'Create'}
          </Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && <ErrorMessage message={error} />}

        <Input
          label="Username"
          value={formData.username}
          onChange={(e) =>
            setFormData({ ...formData, username: e.target.value })
          }
          required
          placeholder="Enter username"
          data-test="user-username"
          disabled={isEditMode && !!initialData?.ssoSubjectId} // Disable username edit for SSO users
          helpText={
            isEditMode && initialData?.ssoSubjectId
              ? 'Cannot change username for SSO users'
              : undefined
          }
        />

        <Input
          label={isEditMode ? 'Password (leave blank to keep current)' : 'Password'}
          type="password"
          value={formData.password}
          onChange={(e) =>
            setFormData({ ...formData, password: e.target.value })
          }
          required={!isEditMode}
          placeholder={isEditMode ? 'Leave blank to keep current password' : 'Enter password'}
          data-test="user-password"
          disabled={!!initialData?.ssoSubjectId} // Disable password for SSO users
          helpText={
            initialData?.ssoSubjectId
              ? 'SSO users authenticate via Google'
              : isEditMode
                ? 'Leave blank to keep current password'
                : 'Minimum 6 characters'
          }
        />

        <Select
          label="Authority"
          value={formData.authority}
          onChange={(e) =>
            setFormData({ ...formData, authority: e.target.value })
          }
          options={authorityOptions}
          required
          helpText="LIBRARIAN has full access, USER has limited access"
          data-test="user-authority"
        />

        {initialData?.ssoSubjectId && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-900">
              <strong>SSO User:</strong> This user authenticates via Google OAuth.
              Username and password cannot be changed.
            </p>
          </div>
        )}
      </form>
    </Modal>
  )
}
