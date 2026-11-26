// (c) Copyright 2026 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import { Spinner } from '@/components/progress/Spinner'
import {
  useJoinMailingLists,
  type MailingFrequency,
  type MailingListId,
  type MailingListSignupLine,
} from '@/api/mailing-lists'
import { useUpdateUserSettings, useUserSettings } from '@/api/settings'
import { isValidOptionalEmail } from '@/utils/contact'
import { isSsoAccount } from '@/utils/sso'

const LISTS: { id: MailingListId; label: string }[] = [
  { id: 'LIVES_OF_SAINTS', label: 'Lives of Saints' },
  { id: 'CHILDRENS_FICTION', label: 'Childrens Fiction' },
  { id: 'READ_ALOUD_BOOKS', label: 'Read-aloud Books' },
  { id: 'NEW_CATALOG_ENTRIES', label: 'New Catalog Entries' },
  { id: 'ADULT_FICTION', label: 'Adult Fiction' },
]

const FREQUENCIES: { id: MailingFrequency; label: string }[] = [
  { id: 'DAILY', label: 'Daily' },
  { id: 'WEEKLY', label: 'Weekly' },
  { id: 'MONTHLY', label: 'Monthly' },
]

export function MailingListsPage() {
  const { data: userSettings, isLoading, isError } = useUserSettings()
  const updateUserSettings = useUpdateUserSettings()
  const [emailDraft, setEmailDraft] = useState<string | null>(null)
  const [selected, setSelected] = useState<MailingListId[]>([])
  const [frequency, setFrequency] = useState<MailingFrequency>('WEEKLY')
  const [error, setError] = useState('')
  const [results, setResults] = useState<MailingListSignupLine[] | null>(null)
  const join = useJoinMailingLists()

  const isSsoUser = isSsoAccount(userSettings)
  const accountEmail = (userSettings?.email || '').trim()
  const email = emailDraft ?? accountEmail

  const toggle = (id: MailingListId) => {
    setSelected((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    )
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError('')
    setResults(null)
    if (!userSettings) {
      setError('Could not load your email address')
      return
    }
    if (selected.length === 0) {
      setError('Choose at least one mailing list')
      return
    }
    if (isSsoUser) {
      if (!accountEmail) {
        setError('An email address cannot be added here.')
        return
      }
    } else {
      const address = email.trim()
      if (!address) {
        setError('Enter an email address and save it before joining a list')
        return
      }
      if (!isValidOptionalEmail(address)) {
        setError('Enter a valid email address')
        return
      }
      if (address !== accountEmail) {
        try {
          await updateUserSettings.mutateAsync({ email: address })
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Could not save your email address')
          return
        }
      }
    }
    try {
      const response = await join.mutateAsync({
        lists: selected,
        frequency,
      })
      setResults(response.results)
      setSelected([])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not sign up')
    }
  }

  const created = results?.filter((line) => line.status === 'created' || line.status === 'resubscribed') ?? []
  const already = results?.filter((line) => line.status === 'already_subscribed') ?? []
  const shownEmail = isSsoUser ? accountEmail : email
  const cannotAddSsoEmail = isSsoUser && !accountEmail

  return (
    <div className="mx-auto max-w-md w-full">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Mailing Lists</h1>
        <p className="text-gray-600">
          Sign up with the email in your settings. Each list has one frequency. To change it, unsubscribe and sign up again.
        </p>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        {isLoading ? (
          <div className="flex justify-center py-6">
            <Spinner />
          </div>
        ) : isError || !userSettings ? (
          <ErrorMessage message="Could not load your email address" data-test="mailing-list-error" />
        ) : (
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {error && <ErrorMessage message={error} data-test="mailing-list-error" />}
            {created.length > 0 && (
              <SuccessMessage
                message={created.map((line) => `Signed up for ${line.displayName}.`).join(' ')}
                data-test="mailing-list-success"
              />
            )}
            {already.map((line) => (
              <ErrorMessage
                key={line.list}
                message={line.message || 'Unsubscribe first to change frequency.'}
                data-test="mailing-list-already"
              />
            ))}

            {cannotAddSsoEmail ? (
              <ErrorMessage
                message="An email address cannot be added here."
                data-test="mailing-list-sso-email"
              />
            ) : (
              <Input
                label="Email"
                type="email"
                value={shownEmail}
                onChange={isSsoUser ? undefined : (event) => setEmailDraft(event.target.value)}
                readOnly={isSsoUser}
                disabled={isSsoUser}
                required={!isSsoUser}
                placeholder="you@example.com"
                data-test="mailing-list-email"
                autoComplete="email"
                helpText={
                  isSsoUser
                    ? 'This email comes from your sign-in provider and cannot be changed here.'
                    : 'This is the email in your settings. Saving a new address updates your account and moves your lists.'
                }
              />
            )}

            <fieldset className="space-y-2">
              <legend className="block text-sm font-medium text-gray-700 mb-1">Lists</legend>
              {LISTS.map((list) => (
                <label key={list.id} className="flex items-center gap-2 text-sm text-gray-800">
                  <input
                    type="checkbox"
                    className="h-4 w-4"
                    checked={selected.includes(list.id)}
                    onChange={() => toggle(list.id)}
                    data-test={`mailing-list-${list.id}`}
                  />
                  {list.label}
                </label>
              ))}
            </fieldset>

            <fieldset className="space-y-2">
              <legend className="block text-sm font-medium text-gray-700 mb-1">Frequency</legend>
              {FREQUENCIES.map((item) => (
                <label key={item.id} className="flex items-center gap-2 text-sm text-gray-800">
                  <input
                    type="radio"
                    name="frequency"
                    className="h-4 w-4"
                    checked={frequency === item.id}
                    onChange={() => setFrequency(item.id)}
                    data-test={`mailing-list-frequency-${item.id}`}
                  />
                  {item.label}
                </label>
              ))}
            </fieldset>

            <Button
              type="submit"
              isLoading={join.isPending || updateUserSettings.isPending}
              disabled={cannotAddSsoEmail}
              fullWidth
              data-test="mailing-list-submit"
            >
              Sign up
            </Button>
          </form>
        )}
      </div>
    </div>
  )
}
