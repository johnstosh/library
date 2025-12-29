// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { useUserSettings, useUpdateUserSettings } from '@/api/settings'
import { hashPassword } from '@/utils/auth'
import { useAuthStore } from '@/stores/authStore'
import type { LibraryCardDesign } from '@/types/dtos'

interface PasswordChangeForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const LIBRARY_CARD_DESIGN_OPTIONS: Array<{ value: LibraryCardDesign; label: string; description: string }> = [
  {
    value: 'CLASSICAL_DEVOTION',
    label: 'Classical Devotion',
    description: 'Traditional design with classic typography'
  },
  {
    value: 'COUNTRYSIDE_YOUTH',
    label: 'Countryside Youth',
    description: 'Fresh, youthful design with natural elements'
  },
  {
    value: 'SACRED_HEART_PORTRAIT',
    label: 'Sacred Heart Portrait',
    description: 'Portrait-oriented design with sacred imagery'
  },
  {
    value: 'RADIANT_BLESSING',
    label: 'Radiant Blessing',
    description: 'Bright design with uplifting elements'
  },
  {
    value: 'PATRON_OF_CREATURES',
    label: 'Patron of Creatures',
    description: 'Nature-focused design with animal motifs'
  }
]

export function UserSettingsPage() {
  const { user } = useAuthStore()
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [cardDesignSuccess, setCardDesignSuccess] = useState('')
  const [cardDesignError, setCardDesignError] = useState('')
  const [xaiApiKey, setXaiApiKey] = useState('')
  const [googlePhotosAlbumId, setGooglePhotosAlbumId] = useState('')

  const { data: userSettings, refetch } = useUserSettings()
  const updateUserSettings = useUpdateUserSettings()

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<PasswordChangeForm>()

  const handleLibraryCardDesignChange = async (design: LibraryCardDesign) => {
    setCardDesignSuccess('')
    setCardDesignError('')

    try {
      await updateUserSettings.mutateAsync({ libraryCardDesign: design })
      setCardDesignSuccess('Library card design updated successfully')
    } catch (error) {
      setCardDesignError(error instanceof Error ? error.message : 'Failed to update library card design')
    }
  }

  const onSubmit = async (data: PasswordChangeForm) => {
    setSuccessMessage('')
    setErrorMessage('')

    // Validate passwords match
    if (data.newPassword !== data.confirmPassword) {
      setErrorMessage('New passwords do not match')
      return
    }

    // Hash passwords before sending
    const currentPasswordHashed = await hashPassword(data.currentPassword)
    const newPasswordHashed = await hashPassword(data.newPassword)

    try {
      await updateUserSettings.mutateAsync({
        currentPassword: currentPasswordHashed,
        password: newPasswordHashed,
      })
      setSuccessMessage('Password changed successfully')
      reset()
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to change password')
    }
  }

  const handleAuthorizeGooglePhotos = () => {
    window.location.href = '/authorize-google-photos'
  }

  const handleRevokeGooglePhotos = async () => {
    if (!confirm('Are you sure you want to revoke Google Photos access?')) {
      return
    }

    try {
      await updateUserSettings.mutateAsync({
        googlePhotosApiKey: '',
        googlePhotosRefreshToken: '',
        googlePhotosTokenExpiry: '',
      })
      setSuccessMessage('Google Photos access revoked')
    } catch (error) {
      setErrorMessage('Failed to revoke Google Photos access')
    }
  }

  const handleSaveXaiApiKey = async () => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      await updateUserSettings.mutateAsync({
        xaiApiKey: xaiApiKey || '',
      })
      setSuccessMessage('XAI API Key updated successfully')
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to update XAI API Key')
    }
  }

  const handleSaveGooglePhotosAlbumId = async () => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      await updateUserSettings.mutateAsync({
        googlePhotosAlbumId: googlePhotosAlbumId || '',
      })
      setSuccessMessage('Google Photos Album ID updated successfully')
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to update Album ID')
    }
  }

  // Initialize form fields from userSettings
  useEffect(() => {
    if (userSettings) {
      setXaiApiKey(userSettings.xaiApiKey || '')
      setGooglePhotosAlbumId(userSettings.googlePhotosAlbumId || '')
    }
  }, [userSettings])

  // Handle OAuth callback messages
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const oauthSuccess = params.get('oauth_success')
    const oauthError = params.get('oauth_error')

    if (oauthSuccess === 'true') {
      setSuccessMessage('Google Photos authorized successfully')
      refetch() // Reload user settings
      // Clean URL
      window.history.replaceState({}, '', '/settings')
    } else if (oauthError) {
      setErrorMessage(`OAuth error: ${oauthError}`)
      window.history.replaceState({}, '', '/settings')
    }
  }, [refetch])

  // If user is SSO user, they cannot change password
  if (user?.ssoSubjectId) {
    return (
      <div className="max-w-2xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">User Settings</h1>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="mb-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Account Information</h2>
            <div className="space-y-2">
              <div>
                <span className="text-sm font-medium text-gray-500">Username:</span>
                <span className="ml-2 text-gray-900">{user?.username}</span>
              </div>
              <div>
                <span className="text-sm font-medium text-gray-500">Account Type:</span>
                <span className="ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                  Google SSO
                </span>
              </div>
              <div>
                <span className="text-sm font-medium text-gray-500">Authority:</span>
                <span className="ml-2 text-gray-900">{user?.authority}</span>
              </div>
              {userSettings?.email && (
                <div>
                  <span className="text-sm font-medium text-gray-500">Email:</span>
                  <span className="ml-2 text-gray-900">{userSettings.email}</span>
                </div>
              )}
              {userSettings?.activeLoansCount !== undefined && (
                <div>
                  <span className="text-sm font-medium text-gray-500">Active Loans:</span>
                  <span className="ml-2 text-gray-900">{userSettings.activeLoansCount}</span>
                </div>
              )}
            </div>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
            <p className="text-sm text-blue-900">
              You are signed in with Google. Password changes are managed through your Google account.
            </p>
          </div>

          {/* Library Card Design Section */}
          <div className="border-t pt-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Library Card Design</h2>

            {cardDesignSuccess && <SuccessMessage message={cardDesignSuccess} className="mb-4" />}
            {cardDesignError && <ErrorMessage message={cardDesignError} className="mb-4" />}

            <div className="space-y-3">
              {LIBRARY_CARD_DESIGN_OPTIONS.map((option) => (
                <div
                  key={option.value}
                  className={`border rounded-lg p-4 cursor-pointer transition-all ${
                    userSettings?.libraryCardDesign === option.value
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                  onClick={() => handleLibraryCardDesignChange(option.value)}
                  data-test={`library-card-design-${option.value}`}
                >
                  <div className="flex items-start">
                    <input
                      type="radio"
                      name="libraryCardDesign"
                      value={option.value}
                      checked={userSettings?.libraryCardDesign === option.value}
                      onChange={() => handleLibraryCardDesignChange(option.value)}
                      className="mt-1 mr-3"
                    />
                    <div>
                      <div className="font-medium text-gray-900">{option.label}</div>
                      <div className="text-sm text-gray-600 mt-1">{option.description}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* XAI API Configuration - Librarian Only */}
          {user?.authority === 'LIBRARIAN' && (
            <div className="border-t pt-6">
              <h2 className="text-xl font-semibold mb-4">XAI Configuration</h2>
              <div className="space-y-4">
                <div>
                  <label htmlFor="xai-api-key" className="block text-sm font-medium mb-1">
                    XAI API Key
                  </label>
                  <div className="flex gap-3">
                    <Input
                      id="xai-api-key"
                      type="password"
                      placeholder="Enter XAI API key"
                      value={xaiApiKey}
                      onChange={(e) => setXaiApiKey(e.target.value)}
                      data-test="xai-api-key-input"
                      className="flex-1"
                    />
                    <Button onClick={handleSaveXaiApiKey} data-test="save-xai-api-key-button">
                      Save
                    </Button>
                  </div>
                  <p className="text-sm text-gray-600 mt-1">
                    API key for XAI integration (optional)
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Google Photos Configuration - Librarian Only */}
          {user?.authority === 'LIBRARIAN' && (
            <div className="border-t pt-6">
              <h2 className="text-xl font-semibold mb-4">Google Photos Integration</h2>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-2">
                    Authorization Status
                  </label>
                  {userSettings?.googlePhotosApiKey ? (
                    <div className="flex items-center gap-3">
                      <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm">
                        Authorized
                      </span>
                      <Button
                        variant="secondary"
                        onClick={handleRevokeGooglePhotos}
                        data-test="revoke-google-photos-button"
                      >
                        Revoke Access
                      </Button>
                    </div>
                  ) : (
                    <div className="flex items-center gap-3">
                      <span className="px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-sm">
                        Not Authorized
                      </span>
                      <Button
                        onClick={handleAuthorizeGooglePhotos}
                        data-test="authorize-google-photos-button"
                      >
                        Authorize Google Photos
                      </Button>
                    </div>
                  )}
                  {userSettings?.googlePhotosTokenExpiry && (
                    <p className="text-sm text-gray-600 mt-2">
                      Token expires: {new Date(userSettings.googlePhotosTokenExpiry).toLocaleString()}
                    </p>
                  )}
                </div>

                <div>
                  <label htmlFor="google-photos-album-id" className="block text-sm font-medium mb-1">
                    Google Photos Album ID
                  </label>
                  <div className="flex gap-3">
                    <Input
                      id="google-photos-album-id"
                      type="text"
                      placeholder="Album ID from Google Photos"
                      value={googlePhotosAlbumId}
                      onChange={(e) => setGooglePhotosAlbumId(e.target.value)}
                      className="font-mono flex-1"
                      data-test="google-photos-album-id-input"
                    />
                    <Button onClick={handleSaveGooglePhotosAlbumId} data-test="save-album-id-button">
                      Save
                    </Button>
                  </div>
                  <p className="text-sm text-gray-600 mt-1">
                    Permanent album ID for photo exports
                  </p>
                </div>

                {userSettings?.lastPhotoTimestamp && (
                  <div>
                    <label className="block text-sm font-medium mb-1">
                      Last Photo Sync
                    </label>
                    <p className="text-sm text-gray-700">
                      {new Date(userSettings.lastPhotoTimestamp).toLocaleString()}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-6">User Settings</h1>

      <div className="bg-white rounded-lg shadow p-6">
        {/* Account Information */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Account Information</h2>
          <div className="space-y-2">
            <div>
              <span className="text-sm font-medium text-gray-500">Username:</span>
              <span className="ml-2 text-gray-900">{user?.username}</span>
            </div>
            <div>
              <span className="text-sm font-medium text-gray-500">Authority:</span>
              <span className="ml-2 text-gray-900">{user?.authority}</span>
            </div>
            {userSettings?.email && (
              <div>
                <span className="text-sm font-medium text-gray-500">Email:</span>
                <span className="ml-2 text-gray-900">{userSettings.email}</span>
              </div>
            )}
            {userSettings?.activeLoansCount !== undefined && (
              <div>
                <span className="text-sm font-medium text-gray-500">Active Loans:</span>
                <span className="ml-2 text-gray-900">{userSettings.activeLoansCount}</span>
              </div>
            )}
          </div>
        </div>

        {/* Library Card Design Section */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Library Card Design</h2>

          {cardDesignSuccess && <SuccessMessage message={cardDesignSuccess} className="mb-4" />}
          {cardDesignError && <ErrorMessage message={cardDesignError} className="mb-4" />}

          <div className="space-y-3">
            {LIBRARY_CARD_DESIGN_OPTIONS.map((option) => (
              <div
                key={option.value}
                className={`border rounded-lg p-4 cursor-pointer transition-all ${
                  userSettings?.libraryCardDesign === option.value
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
                onClick={() => handleLibraryCardDesignChange(option.value)}
                data-test={`library-card-design-${option.value}`}
              >
                <div className="flex items-start">
                  <input
                    type="radio"
                    name="libraryCardDesign"
                    value={option.value}
                    checked={userSettings?.libraryCardDesign === option.value}
                    onChange={() => handleLibraryCardDesignChange(option.value)}
                    className="mt-1 mr-3"
                  />
                  <div>
                    <div className="font-medium text-gray-900">{option.label}</div>
                    <div className="text-sm text-gray-600 mt-1">{option.description}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Change Password Form */}
        <div className="border-t pt-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Change Password</h2>

          {successMessage && <SuccessMessage message={successMessage} className="mb-4" />}
          {errorMessage && <ErrorMessage message={errorMessage} className="mb-4" />}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Current Password"
              type="password"
              {...register('currentPassword', { required: 'Current password is required' })}
              error={errors.currentPassword?.message}
              data-test="current-password"
              required
            />

            <Input
              label="New Password"
              type="password"
              {...register('newPassword', {
                required: 'New password is required',
                minLength: {
                  value: 8,
                  message: 'Password must be at least 8 characters',
                },
              })}
              error={errors.newPassword?.message}
              data-test="new-password"
              helpText="Password must be at least 8 characters"
              required
            />

            <Input
              label="Confirm New Password"
              type="password"
              {...register('confirmPassword', { required: 'Please confirm your new password' })}
              error={errors.confirmPassword?.message}
              data-test="confirm-password"
              required
            />

            <div className="flex justify-end gap-3 pt-4">
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  reset()
                  setSuccessMessage('')
                  setErrorMessage('')
                }}
                data-test="cancel-password-change"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                isLoading={updateUserSettings.isPending}
                data-test="change-password-submit"
              >
                Change Password
              </Button>
            </div>
          </form>
        </div>

        {/* XAI API Configuration - Librarian Only */}
        {user?.authority === 'LIBRARIAN' && (
          <div className="border-t pt-6">
            <h2 className="text-xl font-semibold mb-4">XAI Configuration</h2>
            <div className="space-y-4">
              <div>
                <label htmlFor="xai-api-key" className="block text-sm font-medium mb-1">
                  XAI API Key
                </label>
                <div className="flex gap-3">
                  <Input
                    id="xai-api-key"
                    type="password"
                    placeholder="Enter XAI API key"
                    value={xaiApiKey}
                    onChange={(e) => setXaiApiKey(e.target.value)}
                    data-test="xai-api-key-input"
                    className="flex-1"
                  />
                  <Button onClick={handleSaveXaiApiKey} data-test="save-xai-api-key-button">
                    Save
                  </Button>
                </div>
                <p className="text-sm text-gray-600 mt-1">
                  API key for XAI integration (optional)
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Google Photos Configuration - Librarian Only */}
        {user?.authority === 'LIBRARIAN' && (
          <div className="border-t pt-6">
            <h2 className="text-xl font-semibold mb-4">Google Photos Integration</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-2">
                  Authorization Status
                </label>
                {userSettings?.googlePhotosApiKey ? (
                  <div className="flex items-center gap-3">
                    <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm">
                      Authorized
                    </span>
                    <Button
                      variant="secondary"
                      onClick={handleRevokeGooglePhotos}
                      data-test="revoke-google-photos-button"
                    >
                      Revoke Access
                    </Button>
                  </div>
                ) : (
                  <div className="flex items-center gap-3">
                    <span className="px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-sm">
                      Not Authorized
                    </span>
                    <Button
                      onClick={handleAuthorizeGooglePhotos}
                      data-test="authorize-google-photos-button"
                    >
                      Authorize Google Photos
                    </Button>
                  </div>
                )}
                {userSettings?.googlePhotosTokenExpiry && (
                  <p className="text-sm text-gray-600 mt-2">
                    Token expires: {new Date(userSettings.googlePhotosTokenExpiry).toLocaleString()}
                  </p>
                )}
              </div>

              <div>
                <label htmlFor="google-photos-album-id" className="block text-sm font-medium mb-1">
                  Google Photos Album ID
                </label>
                <div className="flex gap-3">
                  <Input
                    id="google-photos-album-id"
                    type="text"
                    placeholder="Album ID from Google Photos"
                    value={googlePhotosAlbumId}
                    onChange={(e) => setGooglePhotosAlbumId(e.target.value)}
                    className="font-mono flex-1"
                    data-test="google-photos-album-id-input"
                  />
                  <Button onClick={handleSaveGooglePhotosAlbumId} data-test="save-album-id-button">
                    Save
                  </Button>
                </div>
                <p className="text-sm text-gray-600 mt-1">
                  Permanent album ID for photo exports
                </p>
              </div>

              {userSettings?.lastPhotoTimestamp && (
                <div>
                  <label className="block text-sm font-medium mb-1">
                    Last Photo Sync
                  </label>
                  <p className="text-sm text-gray-700">
                    {new Date(userSettings.lastPhotoTimestamp).toLocaleString()}
                  </p>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
