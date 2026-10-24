// (c) Copyright 2025 by Muczynski
import { useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { PiMagnifyingGlass } from 'react-icons/pi'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageCard } from '@/components/ui/PageCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { CatalogLoadMore } from '@/components/table/CatalogLoadMore'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { SelectionToolbar, TableCountPlaceholder } from '@/components/table/SelectionToolbar'
import { TableSummary } from '@/components/table/TableSummary'
import { CATALOG_PAGE_SIZE } from '@/api/byIds'
import { useBookCount, useBooks, type BookListFilters } from '@/api/books'
import { useBranches } from '@/api/branches'
import { favoriteListChips, useFavoriteSummary } from '@/api/favorites'
import { fetchListingsForBooks } from '@/api/sellers'
import { BookFilters } from '@/pages/books/components/BookFilters'
import { BookLabelFilters } from '@/pages/books/components/BookLabelFilters'
import { BindingFilters } from '@/pages/books/components/BindingFilters'
import { ReadingDifficultyFilters } from '@/pages/books/components/ReadingDifficultyFilters'
import { StatusFilters } from '@/pages/books/components/StatusFilters'
import { FavoriteListFilters } from '@/pages/books/components/FavoriteListFilters'
import { BookPriceFilters } from '@/pages/books/components/BookPriceFilters'
import type { BookListingDto } from '@/types/dtos'
import type { BookCoverType, ReadingDifficulty } from '@/types/enums'
import { bindingsFromSearchParams } from '@/utils/bookBinding'
import type { BookChipFilters } from '@/utils/bookChipFilters'
import {
  bookFilterParamsForUrl,
  chipsFromSearchParams,
  favoriteListsFromSearchParams,
  labelsFromSearchParams,
  priceOlderDaysFromSearchParams,
} from '@/utils/bookFilterParams'
import {
  bookStatusesFromSearchParams,
  type BookStatusFilter,
} from '@/utils/bookStatus'
import { formatUsd } from '@/utils/formatters'
import { coverLabel } from '@/utils/priceFilters'
import { readingDifficultiesFromSearchParams } from '@/utils/readingDifficulty'
import {
  computeShippingSavings,
  type SavingsListing,
  type SavingsReport,
} from '@/utils/shippingSavings'

interface SelectedTitle {
  id: number
  title: string
  author: string | null
}

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
  const [searchParams, setSearchParams] = useSearchParams()
  const chips = chipsFromSearchParams(searchParams, 'books')
  const priceOlderDays = priceOlderDaysFromSearchParams(searchParams)
  const selectedLabels = labelsFromSearchParams(searchParams)
  const selectedDifficulties = readingDifficultiesFromSearchParams(searchParams)
  const selectedBindings = bindingsFromSearchParams(searchParams)
  const selectedStatuses = bookStatusesFromSearchParams(searchParams)
  const selectedFavoriteLists = favoriteListsFromSearchParams(searchParams)
  const { data: favoriteSummary } = useFavoriteSummary()
  const favoriteChips = favoriteListChips(favoriteSummary?.lists, 'books')
  const urlQuery = searchParams.get('q') ?? ''
  const [inputValue, setInputValue] = useState(urlQuery)
  const [syncedQuery, setSyncedQuery] = useState(urlQuery)
  const ownQuery = useRef<string | null>(null)
  if (urlQuery !== syncedQuery) {
    const own = ownQuery.current === urlQuery
    ownQuery.current = null
    setSyncedQuery(urlQuery)
    if (!own) setInputValue(urlQuery)
  }

  const writeUrl = (next: {
    chips?: BookChipFilters
    labels?: string[]
    readingDifficulties?: string[]
    bindings?: string[]
    statuses?: string[]
    favoriteLists?: string[]
    q?: string
    priceOlderDays?: number
  }) => {
    const q = next.q !== undefined ? next.q : urlQuery
    if (next.q !== undefined) ownQuery.current = q
    setSearchParams(
      bookFilterParamsForUrl(
        {
          chips: next.chips ?? chips,
          labels: next.labels ?? selectedLabels,
          readingDifficulties: next.readingDifficulties ?? selectedDifficulties,
          bindings: next.bindings ?? selectedBindings,
          statuses: next.statuses ?? selectedStatuses,
          favoriteLists: next.favoriteLists ?? selectedFavoriteLists,
          q,
          priceOlderDays: next.priceOlderDays ?? priceOlderDays,
        },
        'books',
      ),
    )
  }

  const listFilters: BookListFilters = {
    q: searchParams.get('q') ?? '',
    labels: labelsFromSearchParams(searchParams),
    statuses: bookStatusesFromSearchParams(searchParams),
    readingDifficulties: readingDifficultiesFromSearchParams(searchParams),
    bindings: bindingsFromSearchParams(searchParams),
    favoriteLists: favoriteListsFromSearchParams(searchParams),
    priceOlderDays: priceOlderDaysFromSearchParams(searchParams),
    chips: chipsFromSearchParams(searchParams, 'books'),
  }

  const {
    data: books = [],
    total = 0,
    hasMore = false,
    loadMore = () => {},
    isLoadingMore = false,
    isLoading,
    isFetching,
    error: booksError,
  } = useBooks(listFilters, { pageSize: CATALOG_PAGE_SIZE })
  const { data: bookCount } = useBookCount()
  const { data: branches = [] } = useBranches()
  const firstBranch = branches[0]

  // Selection is not the loaded window. A title picked on another page or
  // search stays selected, and compute uses that stored title and author.
  const [selected, setSelected] = useState<Map<number, SelectedTitle>>(() => new Map())
  const [running, setRunning] = useState(false)
  const [phase, setPhase] = useState<'idle' | 'loading' | 'computing' | 'done'>('idle')
  const [elapsedMs, setElapsedMs] = useState(0)
  const [report, setReport] = useState<SavingsReport | null>(null)
  const [error, setError] = useState<string | null>(null)

  function remember(book: { id: number; title: string; author?: string | null }): SelectedTitle {
    return { id: book.id, title: book.title, author: book.author ?? null }
  }

  function toggle(book: { id: number; title: string; author?: string | null }) {
    setSelected((current) => {
      const next = new Map(current)
      if (next.has(book.id)) next.delete(book.id)
      else next.set(book.id, remember(book))
      return next
    })
  }

  function selectVisible() {
    setSelected((current) => {
      const next = new Map(current)
      for (const book of books) next.set(book.id, remember(book))
      return next
    })
  }

  async function compute() {
    const chosen = [...selected.values()]
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
              author: book.author,
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

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const field = event.currentTarget.elements.namedItem('q')
    const typed = field instanceof HTMLInputElement ? field.value : inputValue
    writeUrl({ q: typed.trim() })
  }

  return (
    <div>
      <PageHeader
        title="Savings"
        description="Pick any number of titles. Savings are computed from listings already stored by Lookup Prices. This page does not call AbeBooks."
      />
      {booksError && (
        <p className="mb-4 text-sm text-red-700" data-test="savings-books-error">
          Could not load the catalog.
        </p>
      )}
      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200 space-y-3">
          <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-2 items-start">
            <div className="flex-1 w-full">
              <Input
                type="search"
                name="q"
                label="Filter by title or author"
                hideLabel
                placeholder="Filter by title or author..."
                value={inputValue}
                onChange={(event) => setInputValue(event.target.value)}
                data-test="savings-search"
              />
            </div>
            <Button
              type="submit"
              variant="primary"
              leftIcon={<PiMagnifyingGlass />}
              data-test="savings-search-button"
            >
              Search
            </Button>
          </form>
          <BookFilters
            chips={chips}
            onToggle={(key) => writeUrl({ chips: { ...chips, [key]: !chips[key] } })}
            showAvailabilityFilters
            showCatalogerFilters
          />
          <BookLabelFilters
            selectedLabels={selectedLabels}
            onToggleLabel={(label) => {
              const nextLabels = selectedLabels.includes(label)
                ? selectedLabels.filter((item) => item !== label)
                : [...selectedLabels, label]
              writeUrl({ labels: nextLabels })
            }}
            onClearLabels={() => writeUrl({ labels: [] })}
          />
          <StatusFilters
            selected={selectedStatuses}
            onToggle={(value: BookStatusFilter) => {
              const next = selectedStatuses.includes(value)
                ? selectedStatuses.filter((item) => item !== value)
                : [...selectedStatuses, value]
              writeUrl({ statuses: next })
            }}
            onClear={() => writeUrl({ statuses: [] })}
          />
          <BindingFilters
            selected={selectedBindings}
            onToggle={(value: BookCoverType) => {
              const next = selectedBindings.includes(value)
                ? selectedBindings.filter((item) => item !== value)
                : [...selectedBindings, value]
              writeUrl({ bindings: next })
            }}
            onClear={() => writeUrl({ bindings: [] })}
          />
          <ReadingDifficultyFilters
            selected={selectedDifficulties}
            onToggle={(value: ReadingDifficulty) => {
              const next = selectedDifficulties.includes(value)
                ? selectedDifficulties.filter((item) => item !== value)
                : [...selectedDifficulties, value]
              writeUrl({ readingDifficulties: next })
            }}
            onClear={() => writeUrl({ readingDifficulties: [] })}
          />
          <FavoriteListFilters
            lists={favoriteChips}
            selected={selectedFavoriteLists}
            onToggle={(listName) => {
              const next = selectedFavoriteLists.includes(listName)
                ? selectedFavoriteLists.filter((name) => name !== listName)
                : [...selectedFavoriteLists, listName]
              writeUrl({ favoriteLists: next })
            }}
            onClear={() => writeUrl({ favoriteLists: [] })}
          />
          <BookPriceFilters
            chips={chips}
            onToggle={(key) => writeUrl({ chips: { ...chips, [key]: !chips[key] } })}
            priceOlderDays={priceOlderDays}
            onPriceOlderDaysChange={(days) =>
              writeUrl({ chips: { ...chips, priceOlder: true }, priceOlderDays: days })
            }
          />
          <div className="flex flex-wrap gap-2">
            <Button type="button" variant="secondary" onClick={selectVisible} data-test="savings-select-visible">
              Select shown
            </Button>
            <Button type="button" variant="secondary" onClick={() => setSelected(new Map())} data-test="savings-clear">
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
          <p className="text-sm text-gray-600" data-test="savings-selected-count">
            {selected.size} {selected.size === 1 ? 'title' : 'titles'} selected
          </p>
          {(phase === 'loading' || phase === 'computing' || phase === 'done') && (
            <p className="text-sm font-medium text-gray-900" data-test="savings-timer">
              {phase === 'loading' && 'Loading listings…'}
              {phase === 'computing' && `Computing… ${seconds(elapsedMs)}s`}
              {phase === 'done' && `Computed in ${seconds(elapsedMs)}s`}
            </p>
          )}
          {error && <p className="text-sm text-red-700" data-test="savings-error">{error}</p>}
        </div>
        <div className="p-4">
          <SelectionToolbar dataTest="savings-stats" selected={false}>
            <TableCountPlaceholder
              tableCount={books.length}
              searchResultsCount={total}
              totalCount={bookCount?.count}
              singular="book"
              plural="books"
              isLoading={isLoading}
              branchName={firstBranch?.branchName}
              librarySystemName={firstBranch?.librarySystemName}
            />
          </SelectionToolbar>
          <div data-test="savings-book-list">
            {isLoading && <p className="py-3 text-sm text-gray-600">Loading titles…</p>}
            {!isLoading && books.length === 0 && (
              <p className="py-3 text-sm text-gray-600">No titles match.</p>
            )}
            <ul className="divide-y divide-gray-100">
              {books.map((book) => (
                <li key={book.id}>
                  <label className="flex items-start gap-3 px-3 py-2 text-sm cursor-pointer hover:bg-gray-50">
                    <input
                      type="checkbox"
                      className="mt-1"
                      checked={selected.has(book.id)}
                      onChange={() => toggle(book)}
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
        </div>
        <CatalogLoadMore
          hasMore={hasMore}
          isLoadingMore={isLoadingMore}
          shownCount={books.length}
          onLoadMore={loadMore}
          data-test="savings-load-more"
        />
        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary
          count={books.length}
          total={total}
          singular="book"
          plural="books"
          isLoading={isLoading}
        />
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
