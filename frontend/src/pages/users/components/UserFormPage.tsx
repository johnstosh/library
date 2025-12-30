// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { useCreateUser, useUpdateUser } from '@/api/users'
import type { UserDto } from '@/types/dtos'
import { UserAuthority } from '@/types/enums'
import { hashPassword } from '@/utils/auth'

interface UserFormPageProps {
  title: string
  user?: UserDto
  onSuccess: () => void
  onCancel: () => void
}

export function UserFormPage({ title, user, onSuccess, onCancel }: UserFormPageProps) {
  const isEditing = !!user

  const [formData, setFormData] = useState({
    username: '',
    password: '',
    authority: UserAuthority.USER as string,
  })
  const [error, setError] = useState('')
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)

  const createUser = useCreateUser()
  const updateUser = useUpdateUser()

  useEffect(() => {
    if (user) {
      setFormData({
        username: user.username,
        password: '', // Password is optional for updates
        authority: user.authorities?.[0] || UserAuthority.USER,
      })
    }
  }, [user])

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

    // Validation
    if (!formData.username.trim()) {
      setError('Username is required')
      return
    }

    if (!isEditing && !formData.password) {
      setError('Password is required for new users')
      return
    }

    try {
      // Hash password if provided
      const hashedPassword = formData.password ? await hashPassword(formData.password) : undefined

      if (isEditing) {
        // Update existing user
        const updateData = {
          username: formData.username,
          authority: formData.authority,
          ...(hashedPassword && { password: hashedPassword }),
        }
        await updateUser.mutateAsync({
          id: user.id,
          user: updateData,
        })
      } else {
        // Create new user
        if (!hashedPassword) {
          setError('Password is required for new users')
          return
        }
        await createUser.mutateAsync({
          username: formData.username,
          password: hashedPassword,
          authority: formData.authority,
        })
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

  const authorityOptions = [
    { value: UserAuthority.USER, label: 'User' },
    { value: UserAuthority.LIBRARIAN, label: 'Librarian' },
  ]

  const isLoading = createUser.isPending || updateUser.isPending
  const isSsoUser = !!user?.ssoSubjectId

  return (
    <div className="bg-white rounded-lg shadow">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="px-6 py-6 space-y-4">
        {error && <ErrorMessage message={error} />}

        {isSsoUser && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-900">
              <strong>SSO User:</strong> This user authenticates via Google OAuth.
              Username and password cannot be changed.
            </p>
          </div>
        )}

        <Input
          label="Username"
          value={formData.username}
          onChange={(e) => handleFieldChange('username', e.target.value)}
          required
          placeholder="Enter username"
          data-test="user-username"
          disabled={isSsoUser}
          {...(isSsoUser && { helpText: 'Cannot change username for SSO users' })}
        />

        <Input
          label={isEditing ? 'Password (leave blank to keep current)' : 'Password'}
          type="password"
          value={formData.password}
          onChange={(e) => handleFieldChange('password', e.target.value)}
          required={!isEditing}
          placeholder={isEditing ? 'Leave blank to keep current password' : 'Enter password'}
          data-test="user-password"
          disabled={isSsoUser}
          helpText={
            isSsoUser
              ? 'SSO users authenticate via Google'
              : isEditing
                ? 'Leave blank to keep current password'
                : 'Minimum 6 characters'
          }
        />

        <Select
          label="Authority"
          value={formData.authority}
          onChange={(e) => handleFieldChange('authority', e.target.value)}
          options={authorityOptions}
          required
          helpText="LIBRARIAN has full access, USER has limited access"
          data-test="user-authority"
        />
      </form>

      {/* Footer */}
      <div className="px-6 py-4 bg-gray-50 border-t rounded-b-lg">
        <div className="flex justify-end gap-3">
          <Button
            variant="ghost"
            onClick={handleCancel}
            disabled={isLoading}
            data-test="user-form-cancel"
          >
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            isLoading={isLoading}
            data-test="user-form-submit"
          >
            {isEditing ? 'Update' : 'Create'}
          </Button>
        </div>
      </div>
    </div>
  )
}
