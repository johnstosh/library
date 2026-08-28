// (c) Copyright 2025 by Muczynski
import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Checkbox } from '@/components/ui/Checkbox'
import { Textarea } from '@/components/ui/Textarea'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { PageLoading } from '@/components/progress/PageLoading'
import { useGlobalSettings, useUpdateGlobalSettings, useSendTestEmail } from '@/api/settings'
import { formatRelativeTime } from '@/utils/formatters'
import type { EmailMethod } from '@/types/dtos'

interface GlobalSettingsForm {
  googleSsoClientId: string
  googleSsoClientSecret: string
  googleClientId: string
  googleClientSecret: string
  emailMethod: EmailMethod
  emailFromAddress: string
  emailFromName: string
  emailNotifyLibrariansOnPending: boolean
  emailNotifyApplicantOnPending: boolean
  emailLibrarianRecipients: string
  emailIncludeLibrarianUserEmails: boolean
  smtpHost: string
  smtpPort: number
  smtpUsername: string
  smtpPassword: string
  smtpStartTls: boolean
  smtpSsl: boolean
  sendGridApiKey: string
  webhookUrl: string
  webhookBearerToken: string
}

const EMAIL_METHOD_OPTIONS = [
  { value: 'DISABLED', label: 'Disabled — do not send email' },
  { value: 'LOG', label: 'Log only — write messages to application logs' },
  { value: 'SMTP', label: 'SMTP — Gmail, Fastmail, Mailgun, etc.' },
  { value: 'SENDGRID', label: 'SendGrid — HTTPS API (Cloud Run friendly)' },
  { value: 'WEBHOOK', label: 'Webhook — POST JSON to Zapier, n8n, Make, or a Cloud Function' },
]

export function GlobalSettingsPage() {
  const [successMessage, setSuccessMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const { data: settings, isLoading } = useGlobalSettings()
  const updateSettings = useUpdateGlobalSettings()
  const sendTestEmail = useSendTestEmail()
  const [testEmailTo, setTestEmailTo] = useState('')
  const [testEmailMessage, setTestEmailMessage] = useState('')
  const [testEmailError, setTestEmailError] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    watch,
  } = useForm<GlobalSettingsForm>()

  const emailMethod = watch('emailMethod')

  // Load current settings into form
  useEffect(() => {
    if (settings) {
      reset({
        googleSsoClientId: settings.googleSsoClientId || '',
        googleSsoClientSecret: '',
        googleClientId: settings.googleClientId || '',
        googleClientSecret: '',
        emailMethod: settings.emailMethod || 'DISABLED',
        emailFromAddress: settings.emailFromAddress || '',
        emailFromName: settings.emailFromName || '',
        emailNotifyLibrariansOnPending: settings.emailNotifyLibrariansOnPending ?? true,
        emailNotifyApplicantOnPending: settings.emailNotifyApplicantOnPending ?? false,
        emailLibrarianRecipients: settings.emailLibrarianRecipients || '',
        emailIncludeLibrarianUserEmails: settings.emailIncludeLibrarianUserEmails ?? true,
        smtpHost: settings.smtpHost || '',
        smtpPort: settings.smtpPort || 587,
        smtpUsername: settings.smtpUsername || '',
        smtpPassword: '',
        smtpStartTls: settings.smtpStartTls ?? true,
        smtpSsl: settings.smtpSsl ?? false,
        sendGridApiKey: '',
        webhookUrl: settings.webhookUrl || '',
        webhookBearerToken: '',
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
        emailMethod: data.emailMethod,
        emailFromAddress: data.emailFromAddress,
        emailFromName: data.emailFromName,
        emailNotifyLibrariansOnPending: data.emailNotifyLibrariansOnPending,
        emailNotifyApplicantOnPending: data.emailNotifyApplicantOnPending,
        emailLibrarianRecipients: data.emailLibrarianRecipients,
        emailIncludeLibrarianUserEmails: data.emailIncludeLibrarianUserEmails,
        smtpHost: data.smtpHost,
        smtpPort: Number(data.smtpPort) || 587,
        smtpUsername: data.smtpUsername,
        smtpStartTls: data.smtpStartTls,
        smtpSsl: data.smtpSsl,
        webhookUrl: data.webhookUrl,
      }

      if (data.googleSsoClientSecret) {
        payload.googleSsoClientSecret = data.googleSsoClientSecret
      }

      if (data.googleClientSecret) {
        payload.googleClientSecret = data.googleClientSecret
      }

      if (data.smtpPassword) {
        payload.smtpPassword = data.smtpPassword
      }

      if (data.sendGridApiKey) {
        payload.sendGridApiKey = data.sendGridApiKey
      }

      if (data.webhookBearerToken) {
        payload.webhookBearerToken = data.webhookBearerToken
      }

      await updateSettings.mutateAsync(payload)
      setSuccessMessage('Settings updated successfully')
      setTestEmailMessage('')
      setTestEmailError('')

      // Clear secret fields after successful update
      reset({
        ...data,
        googleSsoClientSecret: '',
        googleClientSecret: '',
        smtpPassword: '',
        sendGridApiKey: '',
        webhookBearerToken: '',
      })
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to update settings')
    }
  }

  const handleSendTestEmail = async () => {
    setTestEmailMessage('')
    setTestEmailError('')
    try {
      const result = await sendTestEmail.mutateAsync(testEmailTo.trim() || undefined)
      if (result.sent) {
        setTestEmailMessage(result.message)
      } else {
        setTestEmailError(result.message)
      }
    } catch (error) {
      setTestEmailError(error instanceof Error ? error.message : 'Failed to send test email')
    }
  }

  if (isLoading) {
    return <PageLoading />
  }

  return (
    <div className="max-w-4xl mx-auto">
      <PageHeader title="Global Settings" />

      <PageCard>
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
                    <StatusBadge tone="success">Configured</StatusBadge>
                  ) : (
                    <StatusBadge tone="neutral">Not Configured</StatusBadge>
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
                    <StatusBadge tone="success">Configured</StatusBadge>
                  ) : (
                    <StatusBadge tone="neutral">Not Configured</StatusBadge>
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
                    <StatusBadge tone="success">Configured</StatusBadge>
                  ) : (
                    <StatusBadge tone="neutral">Not Configured</StatusBadge>
                  )}
                </div>
                <Input
                  {...register('googleClientId')}
                  data-test="photos-client-id"
                  placeholder="Enter Google Photos OAuth Client ID"
                  helpText={
                    settings?.googleClientId
                      ? `Current: ${settings.googleClientId}`
                      : 'From application.properties'
                  }
                  hideLabel
                />
              </div>

              <div>
                <div className="flex items-center gap-2 mb-1">
                  <label className="block text-sm font-medium text-gray-700">Client Secret</label>
                  {settings?.googleClientSecretConfigured ? (
                    <StatusBadge tone="success">Configured</StatusBadge>
                  ) : (
                    <StatusBadge tone="neutral">Not Configured</StatusBadge>
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

          {/* Email notifications */}
          <div className="border-b border-gray-200 pb-8">
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Email Notifications</h2>
            <p className="text-sm text-gray-600 mb-4">
              Sent when a library card application is pending. Pick a method that fits this
              deployment: log-only for Cloud Run verification, SMTP for Gmail, SendGrid for HTTPS
              on Cloud Run, or a webhook for Zapier / n8n / a Cloud Function.
            </p>

            <div className="flex items-center gap-2 mb-4">
              {settings?.emailMethodConfigured ? (
                <StatusBadge tone="success" data-test="email-method-status">
                  {settings.emailMethodStatus || 'Ready'}
                </StatusBadge>
              ) : (
                <StatusBadge tone="neutral" data-test="email-method-status">
                  {settings?.emailMethodStatus || 'Not configured'}
                </StatusBadge>
              )}
            </div>

            <div className="space-y-4">
              <Select
                label="Email method"
                options={EMAIL_METHOD_OPTIONS}
                {...register('emailMethod')}
                data-test="email-method"
              />

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="From address"
                  type="email"
                  {...register('emailFromAddress')}
                  data-test="email-from-address"
                  placeholder="library@example.com"
                  helpText="Required for SMTP and SendGrid"
                />
                <Input
                  label="From name"
                  {...register('emailFromName')}
                  data-test="email-from-name"
                  placeholder="Library"
                />
              </div>

              <Checkbox
                label="Email librarians when a card application is pending"
                {...register('emailNotifyLibrariansOnPending')}
                data-test="email-notify-librarians"
              />
              <Checkbox
                label="Email the applicant a confirmation (requires email on the application)"
                {...register('emailNotifyApplicantOnPending')}
                data-test="email-notify-applicant"
              />
              <Checkbox
                label="Also notify librarian users who have an email on their account"
                {...register('emailIncludeLibrarianUserEmails')}
                data-test="email-include-librarian-users"
              />

              <Textarea
                label="Additional librarian recipients"
                {...register('emailLibrarianRecipients')}
                data-test="email-librarian-recipients"
                rows={3}
                placeholder="librarian@example.com, other@example.com"
                helpText="Comma, semicolon, or newline separated"
              />

              {emailMethod === 'SMTP' && (
                <div className="space-y-4 rounded-lg border border-gray-200 p-4 bg-gray-50">
                  <h3 className="text-sm font-medium text-gray-900">SMTP</h3>
                  <p className="text-sm text-gray-600">
                    Gmail: smtp.gmail.com, port 587, STARTTLS, use an App Password. Cloud Run
                    blocks port 25 — use 587 or 465.
                  </p>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Input
                      label="SMTP host"
                      {...register('smtpHost')}
                      data-test="smtp-host"
                      placeholder="smtp.gmail.com"
                    />
                    <Input
                      label="SMTP port"
                      type="number"
                      {...register('smtpPort', { valueAsNumber: true })}
                      data-test="smtp-port"
                    />
                    <Input
                      label="SMTP username"
                      {...register('smtpUsername')}
                      data-test="smtp-username"
                    />
                    <Input
                      label="SMTP password"
                      type="password"
                      {...register('smtpPassword')}
                      data-test="smtp-password"
                      placeholder="Leave blank to keep existing value"
                      helpText={
                        settings?.smtpPasswordConfigured
                          ? `Current: ${settings.smtpPasswordPartial}`
                          : 'Leave blank to keep existing value'
                      }
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <Checkbox
                      label="STARTTLS"
                      {...register('smtpStartTls')}
                      data-test="smtp-starttls"
                    />
                    <Checkbox
                      label="SSL (port 465)"
                      {...register('smtpSsl')}
                      data-test="smtp-ssl"
                    />
                  </div>
                </div>
              )}

              {emailMethod === 'SENDGRID' && (
                <div className="space-y-4 rounded-lg border border-gray-200 p-4 bg-gray-50">
                  <h3 className="text-sm font-medium text-gray-900">SendGrid</h3>
                  <p className="text-sm text-gray-600">
                    Uses HTTPS (api.sendgrid.com), so it works on Cloud Run. Create an API key
                    with Mail Send permission.
                  </p>
                  <Input
                    label="SendGrid API key"
                    type="password"
                    {...register('sendGridApiKey')}
                    data-test="sendgrid-api-key"
                    placeholder="Leave blank to keep existing value"
                    helpText={
                      settings?.sendGridApiKeyConfigured
                        ? `Current: ${settings.sendGridApiKeyPartial}`
                        : 'Leave blank to keep existing value'
                    }
                  />
                </div>
              )}

              {emailMethod === 'WEBHOOK' && (
                <div className="space-y-4 rounded-lg border border-gray-200 p-4 bg-gray-50">
                  <h3 className="text-sm font-medium text-gray-900">Webhook</h3>
                  <p className="text-sm text-gray-600">
                    POSTs JSON (event, to, subject, text, html, payload) to this URL. Point it at
                    Zapier, n8n, Make, Google Apps Script, or a Cloud Function that sends the
                    actual email.
                  </p>
                  <Input
                    label="Webhook URL"
                    {...register('webhookUrl')}
                    data-test="webhook-url"
                    placeholder="https://hooks.example.com/library-email"
                  />
                  <Input
                    label="Bearer token (optional)"
                    type="password"
                    {...register('webhookBearerToken')}
                    data-test="webhook-bearer-token"
                    placeholder="Leave blank to keep existing value"
                    helpText={
                      settings?.webhookBearerTokenConfigured
                        ? `Current: ${settings.webhookBearerTokenPartial}`
                        : 'Sent as Authorization: Bearer … if set'
                    }
                  />
                </div>
              )}

              {emailMethod === 'LOG' && (
                <p className="text-sm text-gray-600" data-test="email-log-help">
                  Messages are written to application logs (visible in Cloud Run logs). Nothing
                  is delivered to an inbox.
                </p>
              )}

              <div className="rounded-lg border border-gray-200 p-4 space-y-3">
                <h3 className="text-sm font-medium text-gray-900">Send a test email</h3>
                <p className="text-sm text-gray-600">
                  Uses the last saved settings, not unsaved form values. Save first if you just
                  changed the method.
                </p>
                {testEmailMessage && (
                  <SuccessMessage message={testEmailMessage} data-test="test-email-success" />
                )}
                {testEmailError && (
                  <ErrorMessage message={testEmailError} data-test="test-email-error" />
                )}
                <div className="flex flex-col sm:flex-row gap-3 items-end">
                  <Input
                    label="To (optional)"
                    type="email"
                    value={testEmailTo}
                    onChange={(e) => setTestEmailTo(e.target.value)}
                    data-test="test-email-to"
                    placeholder="Defaults to librarian recipients"
                  />
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={handleSendTestEmail}
                    isLoading={sendTestEmail.isPending}
                    data-test="send-test-email"
                  >
                    Send test
                  </Button>
                </div>
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
      </PageCard>
    </div>
  )
}
