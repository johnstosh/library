// (c) Copyright 2025 by Muczynski
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageCard } from '@/components/ui/PageCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { useBooks } from '@/api/books'
import { fetchListingsForBooks } from '@/api/sellers'
import type { BookListingDto } from '@/types/dtos'
import { formatUsd } from '@/utils/formatters'
import { coverLabel } from '@/utils/priceFilters'
import {
  computeShippingSavings,
  type SavingsListing,
  type SavingsReport,
} from '@/utils/shippingSavings'

function toSavingsListing(row: BookListingDto): SavingsListing {
  return {
    bookId: row.bookId,
    listingId: row.id,
    listingTitle: row.listingTitle ?? null,
    sellerId: row.sellerId,
    sellerName: row.sellerName || 'Seller',
    storefrontUrl: row.storefrontUrl ?? null,
    detailsUrl: row.detailsUrl ?? null,
    priceDollars: Number(row.priceDollars ?? 0),
    shippingDollars: Number(row.shippingDollars ?? 0),
    additionalShippingDollars: row.additionalShippingDollars == null ? null : Number(row.additionalShippingDollars),
    additionalShippingSource: row.additionalShippingSource ?? 'UNKNOWN',
    condition: row.condition ?? null,
    binding: row.binding ?? null,
  }
}

function seconds(ms: number): string {
  return (ms / 1000).toFixed(1)
}

export function SavingsPage() {
  const { data: books = [], isLoading, error: booksError } = useBooks()
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<Set<number>>(() => new Set())
  const [running, setRunning] = useState(false)
  const [phase, setPhase] = useState<'idle' | 'loading' | 'computing' | 'done'>('idle')
  const [elapsedMs, setElapsedMs] = useState(0)
  const [report, setReport] = useState<SavingsReport | null>(null)
  const [error, setError] = useState<string | null>(null)

  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase()
    if (!needle) return books
    return books.filter((book) => {
      const title = book.title.toLowerCase()
      const author = (book.author ?? '').toLowerCase()
      return title.includes(needle) || author.includes(needle)
    })
  }, [books, query])

  function toggle(id: number) {
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function selectVisible() {
    setSelected((current) => {
      const next = new Set(current)
      for (const book of visible) next.add(book.id)
      return next
    })
  }

  async function compute() {
    const chosen = books.filter((book) => selected.has(book.id))
    if (chosen.length === 0 || running) return
    setRunning(true)
    setError(null)
    setReport(null)
    setPhase('loading')
    setElapsedMs(0)
    try {
      const rows = await fetchListingsForBooks(chosen.map((book) => book.id))
      const started = performance.now()
      setPhase('computing')
      const timer = window.setInterval(() => {
        setElapsedMs(performance.now() - started)
      }, 100)
      try {
        const result = await computeShippingSavings(
          {
            books: chosen.map((book) => ({
              id: book.id,
              title: book.title,
              author: book.author ?? null,
            })),
            listings: rows.map(toSavingsListing),
          },
          () => new Promise((resolve) => {
            window.setTimeout(resolve, 0)
          }),
        )
        setElapsedMs(performance.now() - started)
        setReport(result)
        setPhase('done')
      } finally {
        window.clearInterval(timer)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not compute savings')
      setPhase('idle')
    } finally {
      setRunning(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Savings"
        description="Pick any number of titles. Savings are computed from listings already stored by Lookup Prices. This page does not call AbeBooks."
      />
      <PageCard>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="flex-1">
            <Input
              label="Find titles"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Title or author"
              data-test="savings-search"
            />
          </div>
          <div className="flex flex-wrap gap-2">
            <Button type="button" variant="secondary" onClick={selectVisible} data-test="savings-select-visible">
              Select shown
            </Button>
            <Button type="button" variant="secondary" onClick={() => setSelected(new Set())} data-test="savings-clear">
              Clear
            </Button>
            <Button
              type="button"
              onClick={() => { void compute() }}
              disabled={selected.size === 0 || running}
              isLoading={running}
              data-test="savings-compute"
            >
              Compute savings
            </Button>
          </div>
        </div>
        <p className="mt-3 text-sm text-gray-600" data-test="savings-selected-count">
          {selected.size} {selected.size === 1 ? 'title' : 'titles'} selected
          {query.trim() ? ` · ${visible.length} shown` : ''}
        </p>
        {(phase === 'loading' || phase === 'computing' || phase === 'done') && (
          <p className="mt-2 text-sm font-medium text-gray-900" data-test="savings-timer">
            {phase === 'loading' && 'Loading listings…'}
            {phase === 'computing' && `Computing… ${seconds(elapsedMs)}s`}
            {phase === 'done' && `Computed in ${seconds(elapsedMs)}s`}
          </p>
        )}
        {error && <p className="mt-2 text-sm text-red-700" data-test="savings-error">{error}</p>}
        {booksError && <p className="mt-2 text-sm text-red-700">Could not load the catalog.</p>}
        <div className="mt-4 max-h-[28rem] overflow-y-auto border border-gray-200 rounded-md" data-test="savings-book-list">
          {isLoading && <p className="p-3 text-sm text-gray-600">Loading titles…</p>}
          {!isLoading && visible.length === 0 && (
            <p className="p-3 text-sm text-gray-600">No titles match.</p>
          )}
          <ul className="divide-y divide-gray-100">
            {visible.map((book) => (
              <li key={book.id}>
                <label className="flex items-start gap-3 px-3 py-2 text-sm cursor-pointer hover:bg-gray-50">
                  <input
                    type="checkbox"
                    className="mt-1"
                    checked={selected.has(book.id)}
                    onChange={() => toggle(book.id)}
                    data-test={`savings-book-${book.id}`}
                  />
                  <span>
                    <span className="font-medium text-gray-900">{book.title}</span>
                    {book.author && <span className="block text-gray-500">{book.author}</span>}
                  </span>
                </label>
              </li>
            ))}
          </ul>
        </div>
      </PageCard>
      {report && <SavingsReportView report={report} />}
    </div>
  )
}

function SavingsReportView({ report }: { report: SavingsReport }) {
  return (
    <div className="mt-6 space-y-6" data-test="savings-report">
      <PageCard>
        <h2 className="text-lg font-semibold text-gray-900">Total savings</h2>
        <p className="mt-1 text-2xl font-semibold" data-test="savings-total">{formatUsd(report.totalSavings)}</p>
        <p className="mt-1 text-sm text-gray-600">
          Compared with buying each book on its own at the cheapest stored good-or-better total.
        </p>
      </PageCard>
      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-3">Buy together</h2>
        {report.sellers.length === 0 && (
          <p className="text-sm text-gray-600" data-test="savings-no-bundles">
            No seller&apos;s extra-book rate beats buying these titles separately.
          </p>
        )}
        <div className="space-y-4">
          {report.sellers.map((seller) => (
            <PageCard key={seller.sellerId}>
              <div className="flex flex-wrap items-baseline justify-between gap-2">
                <h3 className="text-base font-semibold">
                  <Link to={`/sellers/${seller.sellerId}`} className="text-primary-700 hover:underline" data-test={`savings-seller-${seller.sellerId}`}>
                    {seller.sellerName}
                  </Link>
                </h3>
                <p className="text-sm" data-test={`savings-seller-amount-${seller.sellerId}`}>
                  Save {formatUsd(seller.savingsDollars)} · basket {formatUsd(seller.basketDollars)} · separate {formatUsd(seller.baselineDollars)}
                </p>
              </div>
              <p className="mt-1 text-sm text-gray-600">
                First book {formatUsd(seller.firstShippingDollars)}, then {formatUsd(seller.additionalShippingDollars)} each.
                {seller.shippingEstimated && ' First-book rates differ, so shipping uses the highest of them and is an estimate.'}
                {seller.storefrontUrl && (
                  <>
                    {' '}
                    <a href={seller.storefrontUrl} target="_blank" rel="noreferrer" className="text-primary-700 hover:underline">
                      Storefront
                    </a>
                  </>
                )}
              </p>
              <ul className="mt-3 divide-y divide-gray-100 text-sm">
                {seller.lines.map((line) => (
                  <li key={line.bookId} className="py-2" data-test={`savings-line-${seller.sellerId}-${line.bookId}`}>
                    <div className="font-medium">{line.catalogTitle}</div>
                    <div className="text-gray-600">
                      {line.listingTitle || 'Listing'}
                      {line.condition ? ` · ${line.condition}` : ''}
                      {line.binding ? ` · ${coverLabel(line.binding)}` : ''}
                      {' · '}
                      {formatUsd(line.itemDollars)}
                      {line.detailsUrl && (
                        <>
                          {' · '}
                          <a href={line.detailsUrl} target="_blank" rel="noreferrer" className="text-primary-700 hover:underline">
                            AbeBooks
                          </a>
                        </>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </PageCard>
          ))}
        </div>
      </section>
      {report.shared.length > 0 && (
        <section data-test="savings-shared">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Rate not published</h2>
          <div className="space-y-4">
            {report.shared.map((seller) => (
              <PageCard key={seller.sellerId}>
                <h3 className="font-semibold">
                  <Link to={`/sellers/${seller.sellerId}`} className="text-primary-700 hover:underline">
                    {seller.sellerName}
                  </Link>
                </h3>
                <p className="mt-1 text-sm text-gray-700">{seller.note}</p>
                <p className="mt-1 text-sm text-gray-600">{seller.catalogTitles.join(', ')}</p>
              </PageCard>
            ))}
          </div>
        </section>
      )}
      {report.individual.length > 0 && (
        <section data-test="savings-individual">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Buy individually</h2>
          <PageCard>
            <ul className="divide-y divide-gray-100 text-sm">
              {report.individual.map((book) => (
                <li key={book.bookId} className="py-2 flex justify-between gap-3">
                  <span>{book.catalogTitle}</span>
                  <span>{formatUsd(book.baselineDollars)}</span>
                </li>
              ))}
            </ul>
          </PageCard>
        </section>
      )}
      {report.booksWithoutListings.length > 0 && (
        <section data-test="savings-missing">
          <h2 className="text-lg font-semibold text-gray-900 mb-3">Look up prices first</h2>
          <PageCard>
            <ul className="text-sm text-gray-700 list-disc pl-5">
              {report.booksWithoutListings.map((book) => (
                <li key={book.bookId}>{book.catalogTitle}</li>
              ))}
            </ul>
          </PageCard>
        </section>
      )}
    </div>
  )
}
