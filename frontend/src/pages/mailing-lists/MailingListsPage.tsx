// (c) Copyright 2026 by Muczynski
import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { SuccessMessage } from '@/components/ui/SuccessMessage'
import {
  useJoinMailingLists,
  type MailingFrequency,
  type MailingListId,
  type MailingListSignupLine,
} from '@/api/mailing-lists'
import { isValidOptionalEmail } from '@/utils/contact'

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
  const [email, setEmail] = useState('')
  const [selected, setSelected] = useState<MailingListId[]>([])
  const [frequency, setFrequency] = useState<MailingFrequency>('WEEKLY')
  const [error, setError] = useState('')
  const [results, setResults] = useState<MailingListSignupLine[] | null>(null)
  const join = useJoinMailingLists()

  const toggle = (id: MailingListId) => {
    setSelected((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    )
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError('')
    setResults(null)
    const address = email.trim()
    if (!address || !isValidOptionalEmail(address)) {
      setError('Enter a valid email address')
      return
    }
    if (selected.length === 0) {
      setError('Choose at least one mailing list')
      return
    }
    try {
      const response = await join.mutateAsync({
        email: address,
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

  return (
    <div className="mx-auto max-w-md w-full">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Mailing Lists</h1>
        <p className="text-gray-600">
          Sign up by email. Each list has one frequency. To change it, unsubscribe and sign up again.
        </p>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
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

          <Input
            label="Email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            placeholder="you@example.com"
            data-test="mailing-list-email"
            autoComplete="email"
          />

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

          <Button type="submit" isLoading={join.isPending} fullWidth data-test="mailing-list-submit">
            Sign up
          </Button>
        </form>
      </div>
    </div>
  )
}
