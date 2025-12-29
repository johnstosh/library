// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { Spinner } from '@/components/progress/Spinner'
import { useGlobalSettings, useUpdateGlobalSettings } from '@/api/settings'
import { formatRelativeTime } from '@/utils/formatters'

interface GlobalSettingsForm {
  googleSsoClientId: string
  googleSsoClientSecret: string
  googleClientId: string
  googleClientSecret: string
}

export function GlobalSettingsPage() {
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const { data: settings, isLoading } = useGlobalSettings()
  const updateSettings = useUpdateGlobalSettings()

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<GlobalSettingsForm>()

  // Load current settings into form
  useEffect(() => {
    if (settings) {
      reset({
        googleSsoClientId: settings.googleSsoClientId || '',
        googleSsoClientSecret: '',
        googleClientId: settings.googleClientId || '',
        googleClientSecret: '',
      })
    }
  }, [settings, reset])

  const onSubmit = async (data: GlobalSettingsForm) => {
    setSuccessMessage('')
    setErrorMessage('')

    try {
      // Only send secrets if they were filled in (non-empty)
      const payload: Partial<GlobalSettingsForm> = {
        googleSsoClientId: data.googleSsoClientId,
        googleClientId: data.googleClientId,
      }

      if (data.googleSsoClientSecret) {
        payload.googleSsoClientSecret = data.googleSsoClientSecret
      }

      if (data.googleClientSecret) {
        payload.googleClientSecret = data.googleClientSecret
      }

      await updateSettings.mutateAsync(payload)
      setSuccessMessage('Settings updated successfully')

      // Clear secret fields after successful update
      reset({
        googleSsoClientId: data.googleSsoClientId,
        googleSsoClientSecret: '',
        googleClientId: data.googleClientId,
        googleClientSecret: '',
      })
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to update settings')
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <Spinner size="lg" />
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-6">Global Settings</h1>

      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-sm text-gray-600 mb-6">
          Configure OAuth credentials for Google integrations. Leave secret fields blank to keep existing values.
        </p>

        {successMessage && <SuccessMessage message={successMessage} className="mb-6" />}
        {errorMessage && <ErrorMessage message={errorMessage} className="mb-6" />}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
          {/* Google SSO Settings */}
          <div className="border-b border-gray-200 pb-8">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Google SSO (User Authentication)</h2>

            <div className="space-y-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <label className="block text-sm font-medium text-gray-700">
                    Client ID <span className="text-red-500">*</span>
                  </label>
                  {settings?.googleSsoClientIdConfigured ? (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Configured
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                      Not Configured
                    </span>
                  )}
                </div>
                <Input
                  {...register('googleSsoClientId', { required: 'Client ID is required' })}
                  error={errors.googleSsoClientId?.message}
                  data-test="sso-client-id"
                  hideLabel
                />
              </div>

              <div>
                <div className="flex items-center gap-2 mb-1">
                  <label className="block text-sm font-medium text-gray-700">Client Secret</label>
                  {settings?.googleSsoClientSecretConfigured ? (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Configured
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                      Not Configured
                    </span>
                  )}
                </div>
                <Input
                  type="password"
                  {...register('googleSsoClientSecret')}
                  error={errors.googleSsoClientSecret?.message}
                  data-test="sso-client-secret"
                  helpText={
                    settings?.googleSsoClientSecretPartial
                      ? `Current: ${settings.googleSsoClientSecretPartial} (Updated: ${
                          settings.googleSsoCredentialsUpdatedAt
                            ? formatRelativeTime(settings.googleSsoCredentialsUpdatedAt)
                            : 'Never'
                        })`
                      : 'Leave blank to keep existing value'
                  }
                  placeholder="Leave blank to keep existing value"
                  hideLabel
                />
                {settings?.googleSsoClientSecretValidation && (
                  <p
                    className={`text-sm font-medium mt-1 ${
                      settings.googleSsoClientSecretValidation === 'Valid'
                        ? 'text-green-600'
                        : settings.googleSsoClientSecretValidation.includes('Warning')
                        ? 'text-orange-600'
                        : 'text-red-600'
                    }`}
                  >
                    {settings.googleSsoClientSecretValidation}
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Google Photos API Settings */}
          <div className="border-b border-gray-200 pb-8">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Google Photos API</h2>

            <div className="space-y-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <label className="block text-sm font-medium text-gray-700">Client ID</label>
                  {settings?.googleClientSecretConfigured ? (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Configured
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                      Not Configured
                    </span>
                  )}
                </div>
                <Input
                  {...register('googleClientId')}
                  data-test="photos-client-id"
                  helpText={settings?.googleClientId || 'From application.properties'}
                  disabled
                  hideLabel
                />
              </div>

              <div>
                <div className="flex items-center gap-2 mb-1">
                  <label className="block text-sm font-medium text-gray-700">Client Secret</label>
                  {settings?.googleClientSecretConfigured ? (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Configured
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                      Not Configured
                    </span>
                  )}
                </div>
                <Input
                  type="password"
                  {...register('googleClientSecret')}
                  error={errors.googleClientSecret?.message}
                  data-test="photos-client-secret"
                  helpText={
                    settings?.googleClientSecretPartial
                      ? `Current: ${settings.googleClientSecretPartial} (Updated: ${
                          settings.googleClientSecretUpdatedAt
                            ? formatRelativeTime(settings.googleClientSecretUpdatedAt)
                            : 'Never'
                        })`
                      : 'Leave blank to keep existing value'
                  }
                  placeholder="Leave blank to keep existing value"
                  hideLabel
                />
                {settings?.googleClientSecretValidation && (
                  <p
                    className={`text-sm font-medium mt-1 ${
                      settings.googleClientSecretValidation === 'Valid'
                        ? 'text-green-600'
                        : settings.googleClientSecretValidation.includes('Warning')
                        ? 'text-orange-600'
                        : 'text-red-600'
                    }`}
                  >
                    {settings.googleClientSecretValidation}
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Redirect URI (read-only) */}
          <div>
            <h2 className="text-xl font-semibold text-gray-900 mb-4">OAuth Redirect URI</h2>
            <div className="bg-gray-50 rounded-lg p-4">
              <p className="text-sm font-medium text-gray-700 mb-2">Configured Redirect URI:</p>
              <p className="text-sm text-gray-900 font-mono" data-test="global-redirect-uri">
                {settings?.redirectUri || 'Not configured'}
              </p>
              <p className="text-xs text-gray-500 mt-2">
                Use this URI when configuring OAuth apps in Google Cloud Console
              </p>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                reset()
                setSuccessMessage('')
                setErrorMessage('')
              }}
              data-test="cancel-settings"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={updateSettings.isPending}
              data-test="save-settings"
            >
              Save Settings
            </Button>
          </div>
        </form>

        {/* Last Updated Info */}
        {settings?.lastUpdated && (
          <div className="mt-6 pt-6 border-t border-gray-200">
            <p className="text-sm text-gray-500">
              Last updated: {formatRelativeTime(settings.lastUpdated)}
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
