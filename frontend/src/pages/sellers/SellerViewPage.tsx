// (c) Copyright 2025 by Muczynski
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageCard } from '@/components/ui/PageCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { useSeller, useUpdateAdditionalShipping } from '@/api/sellers'
import { formatDateTime, formatUsd } from '@/utils/formatters'
import { additionalRateLabel, sourceLabel } from './SellersPage'

function dollarsToInput(value: number | null | undefined): string {
  if (value == null) return ''
  return value.toFixed(2)
}

export function SellerViewPage() {
  const { id } = useParams()
  const sellerId = Number(id)
  const navigate = useNavigate()
  const { data: seller, isLoading, error } = useSeller(sellerId)
  const update = useUpdateAdditionalShipping()
  const [amount, setAmount] = useState('')
  const [savedAmount, setSavedAmount] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    const next = dollarsToInput(seller?.additionalShippingDollars)
    setAmount(next)
    setSavedAmount(next)
  }, [seller?.id, seller?.additionalShippingDollars, seller?.additionalShippingSource])

  const dirty = amount !== savedAmount

  useEffect(() => {
    if (!dirty) return
    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
    }
    window.addEventListener('beforeunload', onBeforeUnload)
    return () => window.removeEventListener('beforeunload', onBeforeUnload)
  }, [dirty])

  function leave() {
    if (dirty && !window.confirm('Leave without saving the extra-book rate?')) return
    navigate('/sellers')
  }

  async function save(additionalShippingDollars: number | null) {
    setFormError(null)
    try {
      const updated = await update.mutateAsync({ id: sellerId, additionalShippingDollars })
      const next = dollarsToInput(updated.additionalShippingDollars)
      setAmount(next)
      setSavedAmount(next)
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Could not save the rate')
    }
  }

  function saveTyped() {
    const trimmed = amount.trim()
    if (!trimmed) {
      void save(null)
      return
    }
    const parsed = Number(trimmed)
    if (!Number.isFinite(parsed) || parsed < 0) {
      setFormError('Enter a dollar amount, or clear the field to read the seller terms again.')
      return
    }
    void save(Math.round(parsed * 100) / 100)
  }

  const place = [seller?.city, seller?.region, seller?.country].filter(Boolean).join(', ')

  return (
    <div>
      <PageHeader
        title={seller?.name || 'Seller'}
        description="Shipping terms captured from AbeBooks. A rate entered here is kept when prices are looked up again."
        actions={(
          <Button variant="secondary" type="button" onClick={leave} data-test="seller-back">
            Back to sellers
          </Button>
        )}
      />
      {isLoading && <p className="text-gray-600">Loading seller…</p>}
      {error && <p className="text-red-700" data-test="seller-missing">Seller not found.</p>}
      {seller && (
        <div className="space-y-6">
          <PageCard>
            <dl className="grid gap-4 sm:grid-cols-2 text-sm">
              <div>
                <dt className="text-gray-500">Location</dt>
                <dd data-test="seller-place">{place || '—'}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Stars</dt>
                <dd>{seller.starRating ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-gray-500">First book, standard / priority</dt>
                <dd>
                  {formatUsd(seller.sampleStandardShipping)} / {formatUsd(seller.samplePriorityShipping)}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500">Listings stored</dt>
                <dd data-test="seller-listing-count">{seller.listingCount}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Storefront</dt>
                <dd>
                  {seller.storefrontUrl ? (
                    <a href={seller.storefrontUrl} target="_blank" rel="noreferrer" className="text-primary-700 hover:underline" data-test="seller-storefront">
                      {seller.storefrontUrl}
                    </a>
                  ) : '—'}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500">Ship rates page</dt>
                <dd>
                  {seller.shipRatesUrl ? (
                    <a href={seller.shipRatesUrl} target="_blank" rel="noreferrer" className="text-primary-700 hover:underline" data-test="seller-ship-rates">
                      AbeBooks shipping
                    </a>
                  ) : '—'}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500">Rates read</dt>
                <dd>{seller.ratesLookedUpAt ? formatDateTime(seller.ratesLookedUpAt) : '—'}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Current extra-book rate</dt>
                <dd data-test="seller-current-rate">
                  {additionalRateLabel(seller)} ({sourceLabel(seller.additionalShippingSource)})
                </dd>
              </div>
            </dl>
            {seller.shippingTerms && (
              <div className="mt-4">
                <h2 className="text-sm font-medium text-gray-700">Seller terms</h2>
                <p className="mt-1 whitespace-pre-wrap text-sm text-gray-800" data-test="seller-terms">
                  {seller.shippingTerms}
                </p>
              </div>
            )}
          </PageCard>
          <PageCard>
            <h2 className="text-lg font-semibold text-gray-900">Extra book shipping</h2>
            <p className="mt-1 text-sm text-gray-600">
              AbeBooks usually applies this only in the basket. Enter the amount from the seller&apos;s terms, or clear it to read those terms again.
            </p>
            <div className="mt-4 max-w-xs">
              <Input
                label="Each additional book (USD)"
                inputMode="decimal"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                data-test="seller-additional-input"
                helpText="Blank uses the stored terms. 0 means later copies ship free."
              />
            </div>
            {formError && <p className="mt-2 text-sm text-red-700" data-test="seller-save-error">{formError}</p>}
            <div className="mt-4 flex flex-wrap gap-2">
              <Button type="button" onClick={saveTyped} isLoading={update.isPending} data-test="seller-save-rate">
                Save rate
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => { setAmount(''); void save(null) }}
                disabled={update.isPending}
                data-test="seller-clear-rate"
              >
                Use seller terms
              </Button>
              <Link to="/savings" className="inline-flex items-center text-sm text-primary-700 hover:underline" data-test="seller-to-savings">
                Compute savings
              </Link>
            </div>
          </PageCard>
        </div>
      )}
    </div>
  )
}
