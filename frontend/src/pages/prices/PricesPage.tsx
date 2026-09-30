// (c) Copyright 2025 by Muczynski
import { useEffect, useMemo, useState, useTransition } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PiBooks, PiMagnifyingGlass } from 'react-icons/pi'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { PriceStatisticsSummary } from '@/components/table/PriceStatisticsSummary'
import { SelectionToolbar, TableCountPlaceholder } from '@/components/table/SelectionToolbar'
import { TransientFetchErrorBanner } from '@/components/ui/TransientFetchErrorBanner'
import { queryKeys } from '@/config/queryClient'
import { useQueryClient } from '@tanstack/react-query'
import { BookFilters } from '@/pages/books/components/BookFilters'
import { BookLabelFilters } from '@/pages/books/components/BookLabelFilters'
import { DesireToPurchaseFilters } from '@/pages/books/components/DesireToPurchaseFilters'
import { BindingFilters } from '@/pages/books/components/BindingFilters'
import { ReadingDifficultyFilters } from '@/pages/books/components/ReadingDifficultyFilters'
import { StatusFilters } from '@/pages/books/components/StatusFilters'
import { FavoriteListFilters } from '@/pages/books/components/FavoriteListFilters'
import { PriceFilters } from './components/PriceFilters'
import { PriceTable } from './components/PriceTable'
import { usePrices } from '@/api/prices'
import { useBookCount, useBooks, type BookListFilters } from '@/api/books'
import { isLookupError, type BookChipFilters } from '@/utils/bookChipFilters'
import {
  bookFilterParamsForUrl,
  booksPathFromPriceFilters,
  chipsFromSearchParams,
  favoriteListsFromSearchParams,
  labelsFromSearchParams,
  priceOlderDaysFromSearchParams,
} from '@/utils/bookFilterParams'
import { BookPriceFilters } from '@/pages/books/components/BookPriceFilters'
import {
  desireToPurchaseFromSearchParams,
  type DesireToPurchaseFilter,
} from '@/utils/desireToPurchase'
import { bindingsFromSearchParams } from '@/utils/bookBinding'
import { readingDifficultiesFromSearchParams } from '@/utils/readingDifficulty'
import {
  bookStatusesFromSearchParams,
  type BookStatusFilter,
} from '@/utils/bookStatus'
import type { BookCoverType, ReadingDifficulty } from '@/types/enums'
import { favoriteListChips, useFavoriteSummary } from '@/api/favorites'
import {
  applyPriceFilters,
  maxTotalFromSearchParams,
  priceChipsFromSearchParams,
  priceFilterParamsForUrl,
  recentHoursFromSearchParams,
  type PriceChipFilters,
} from '@/utils/priceFilters'
import { summarizeBookPrices } from '@/utils/priceStatistics'

export function PricesPage() {
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const chips = chipsFromSearchParams(searchParams, 'prices')
  const priceOlderDays = priceOlderDaysFromSearchParams(searchParams)
  const selectedLabels = labelsFromSearchParams(searchParams)
  const selectedDifficulties = readingDifficultiesFromSearchParams(searchParams)
  const selectedBindings = bindingsFromSearchParams(searchParams)
  const selectedDesireToPurchase = desireToPurchaseFromSearchParams(searchParams)
  const selectedStatuses = bookStatusesFromSearchParams(searchParams)
  const selectedFavoriteLists = favoriteListsFromSearchParams(searchParams)
  const urlQuery = searchParams.get('q') ?? ''
  const [inputValue, setInputValue] = useState(urlQuery)
  const priceChips = priceChipsFromSearchParams(searchParams)
  const maxTotal = maxTotalFromSearchParams(searchParams)
  const [maxTotalInput, setMaxTotalInput] = useState(maxTotal)
  const recentHours = recentHoursFromSearchParams(searchParams)
  const { data: favoriteSummary } = useFavoriteSummary()
  const [, startTransition] = useTransition()

  useEffect(() => {
    setInputValue(urlQuery)
  }, [urlQuery])

  useEffect(() => {
    setMaxTotalInput(maxTotal)
  }, [maxTotal])
  const favoriteChips = favoriteListChips(favoriteSummary?.lists, 'books')

  const writeUrl = (next: {
    chips?: BookChipFilters
    labels?: string[]
    readingDifficulties?: string[]
    bindings?: string[]
    desireToPurchase?: DesireToPurchaseFilter[]
    statuses?: string[]
    favoriteLists?: string[]
    q?: string
    priceChips?: PriceChipFilters
    maxTotal?: string
    priceOlderDays?: number
    recentHours?: number
  }) => {
    startTransition(() => {
      const bookParams = bookFilterParamsForUrl(
        {
          chips: next.chips ?? chips,
          labels: next.labels ?? selectedLabels,
          readingDifficulties: next.readingDifficulties ?? selectedDifficulties,
          bindings: next.bindings ?? selectedBindings,
          desireToPurchase: next.desireToPurchase ?? selectedDesireToPurchase,
          statuses: next.statuses ?? selectedStatuses,
          favoriteLists: next.favoriteLists ?? selectedFavoriteLists,
          q: next.q !== undefined ? next.q : urlQuery,
          priceOlderDays: next.priceOlderDays ?? priceOlderDays,
        },
        'prices',
      )
      const priceParams = priceFilterParamsForUrl({
        chips: next.priceChips ?? priceChips,
        maxTotal: next.maxTotal !== undefined ? next.maxTotal : maxTotal,
        recentHours: next.recentHours ?? recentHours,
      })
      setSearchParams({ ...bookParams, ...priceParams })
    })
  }

  const listFilters = useMemo((): BookListFilters => ({
    q: searchParams.get('q') ?? '',
    labels: labelsFromSearchParams(searchParams),
    statuses: bookStatusesFromSearchParams(searchParams),
    readingDifficulties: readingDifficultiesFromSearchParams(searchParams),
    bindings: bindingsFromSearchParams(searchParams),
    desireToPurchase: desireToPurchaseFromSearchParams(searchParams),
    favoriteLists: favoriteListsFromSearchParams(searchParams),
    priceOlderDays: priceOlderDaysFromSearchParams(searchParams),
    chips: chipsFromSearchParams(searchParams, 'prices'),
  }), [searchParams])

  const {
    data: matchingBooks = [],
    isLoading: booksLoading,
    isFetching: booksFetching,
    error: booksError,
  } = useBooks(listFilters)
  const { data: bookCount } = useBookCount()

  // Book-level chips, including price chips, are applied by filtered-summaries.
  // Price rows (max total, recent hours, cover chips) stay in the browser.
  const candidateBookIds = useMemo(
    () => matchingBooks.map((b) => b.id),
    [matchingBooks],
  )

  const { data: scopedPrices = [], isLoading: pricesLoading, isFetching, error } = usePrices({
    bookIds: candidateBookIds,
    enabled: !booksLoading,
  })

  const priceStatistics = useMemo(
    () => summarizeBookPrices(matchingBooks, scopedPrices),
    [matchingBooks, scopedPrices],
  )

  const prices = useMemo(() => {
    const matchingBookIds = new Set(matchingBooks.map((b) => b.id))
    // Scope price rows to matchingBooks (restores pre-#342 noPrices/withPrices/etc behavior).
    // lookupErrors row filter applies on top (shows only error rows, not real listings for those books).
    let rows = scopedPrices.filter((price) => matchingBookIds.has(price.bookId))
    if (chips.lookupErrors) {
      rows = rows.filter(isLookupError)
    }
    return applyPriceFilters(rows, priceChips, maxTotal, Date.now(), recentHours)
  }, [scopedPrices, matchingBooks, chips.lookupErrors, priceChips, maxTotal, recentHours])

  const displayedBookCount = useMemo(() => {
    const ids = new Set<number>()
    for (const price of prices) {
      ids.add(price.bookId)
    }
    return ids.size
  }, [prices])

  const isLoading = pricesLoading || booksLoading
  const isFilterPending = ((isFetching || booksFetching) && !isLoading) || (booksLoading && !pricesLoading)

  return (
    <div>
      <PageHeader
        title="Prices"
        description="AbeBooks used-book prices for hardcover, softcover, and library-binding copies in good condition or better."
      />

      {(booksError || error) && (
        <TransientFetchErrorBanner
          error={booksError ?? error}
          onRetry={() => {
            queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
            queryClient.invalidateQueries({ queryKey: queryKeys.prices.all })
          }}
          className="mb-4"
        />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200 space-y-3">
          <form
            onSubmit={(e) => {
              e.preventDefault()
              writeUrl({ q: inputValue.trim(), maxTotal: maxTotalInput.trim() })
            }}
            className="flex flex-col sm:flex-row gap-2 items-start"
          >
            <div className="flex-1 w-full">
              <Input
                type="search"
                label="Filter by title or author"
                hideLabel
                placeholder="Filter by title or author..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                data-test="prices-title-filter"
              />
            </div>
            <Button
              type="submit"
              variant="primary"
              leftIcon={<PiMagnifyingGlass />}
              data-test="prices-search-button"
            >
              Search
            </Button>
          </form>
          <Button
            variant="outline"
            to={booksPathFromPriceFilters({
              chips,
              labels: selectedLabels,
              readingDifficulties: selectedDifficulties,
              bindings: selectedBindings,
              statuses: selectedStatuses,
              favoriteLists: selectedFavoriteLists,
              q: inputValue.trim() || urlQuery,
              priceOlderDays,
            })}
            leftIcon={<PiBooks />}
            data-test="open-in-books"
          >
            Open in Books
          </Button>
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
          <DesireToPurchaseFilters
            selected={selectedDesireToPurchase}
            onToggle={(value: DesireToPurchaseFilter) => {
              const next = selectedDesireToPurchase.includes(value)
                ? selectedDesireToPurchase.filter((item) => item !== value)
                : [...selectedDesireToPurchase, value]
              writeUrl({ desireToPurchase: next })
            }}
            onClear={() => writeUrl({ desireToPurchase: [] })}
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
            listingFilters={
              <PriceFilters
                chips={priceChips}
                onToggle={(key) => writeUrl({ priceChips: { ...priceChips, [key]: !priceChips[key] } })}
                recentHours={recentHours}
                onRecentHoursChange={(hours) =>
                  writeUrl({ priceChips: { ...priceChips, recent: true }, recentHours: hours })
                }
              />
            }
            maxTotal={maxTotalInput}
            onMaxTotalChange={(value) => {
              setMaxTotalInput(value)
              writeUrl({ maxTotal: value.trim() })
            }}
          />
        </div>

        <div className="p-4">
          <SelectionToolbar dataTest="prices-stats" selected={false}>
            <TableCountPlaceholder
              tableCount={displayedBookCount}
              totalCount={bookCount?.count}
              singular="book"
              plural="books"
              isLoading={isLoading}
              extraTableCounts={[
                {
                  count: prices.length,
                  singular: 'price',
                  plural: 'prices',
                  dataTest: 'price-row-count',
                },
              ]}
            />
          </SelectionToolbar>
          <PriceTable prices={prices} isLoading={isLoading} />
        </div>

        <LoadingOverlay show={isFilterPending} />
        <PriceStatisticsSummary stats={priceStatistics} isLoading={isLoading} />
      </PageCard>
    </div>
  )
}
