// (c) Copyright 2025 by Muczynski
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PiBooks, PiMagnifyingGlass } from 'react-icons/pi'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TableSummary } from '@/components/table/TableSummary'
import { SelectionToolbar, TableCountPlaceholder } from '@/components/table/SelectionToolbar'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { BookFilters } from '@/pages/books/components/BookFilters'
import { BookLabelFilters } from '@/pages/books/components/BookLabelFilters'
import { DesireToPurchaseFilters } from '@/pages/books/components/DesireToPurchaseFilters'
import { ReadingDifficultyFilters } from '@/pages/books/components/ReadingDifficultyFilters'
import { StatusFilters } from '@/pages/books/components/StatusFilters'
import { FavoriteListFilters } from '@/pages/books/components/FavoriteListFilters'
import { PriceFilters } from './components/PriceFilters'
import { PriceTable } from './components/PriceTable'
import { usePrices } from '@/api/prices'
import { useBookCount, useBooks } from '@/api/books'
import { applyBookPriceFilters, applyChipFilters, isLookupError, type BookChipFilters } from '@/utils/bookChipFilters'
import {
  bookFilterParamsForUrl,
  booksPathFromPriceFilters,
  chipsFromSearchParams,
  favoriteListsFromSearchParams,
  labelsFromSearchParams,
  matchesBookQuery,
  priceOlderDaysFromSearchParams,
} from '@/utils/bookFilterParams'
import { BookPriceFilters } from '@/pages/books/components/BookPriceFilters'
import {
  applyDesireToPurchaseFilter,
  desireToPurchaseFromSearchParams,
  type DesireToPurchaseFilter,
} from '@/utils/desireToPurchase'
import {
  applyReadingDifficultyFilter,
  readingDifficultiesFromSearchParams,
} from '@/utils/readingDifficulty'
import {
  applyBookStatusFilter,
  bookStatusesFromSearchParams,
  type BookStatusFilter,
} from '@/utils/bookStatus'
import type { ReadingDifficulty } from '@/types/enums'
import { favoriteItemIdsForLists, favoriteListChips, useFavoriteSummary } from '@/api/favorites'
import {
  applyPriceFilters,
  maxTotalFromSearchParams,
  priceChipsFromSearchParams,
  priceFilterParamsForUrl,
  recentHoursFromSearchParams,
  type PriceChipFilters,
} from '@/utils/priceFilters'

export function PricesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const chips = chipsFromSearchParams(searchParams, 'prices')
  const priceOlderDays = priceOlderDaysFromSearchParams(searchParams)
  const selectedLabels = labelsFromSearchParams(searchParams)
  const selectedDifficulties = readingDifficultiesFromSearchParams(searchParams)
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
    desireToPurchase?: DesireToPurchaseFilter[]
    statuses?: string[]
    favoriteLists?: string[]
    q?: string
    priceChips?: PriceChipFilters
    maxTotal?: string
    priceOlderDays?: number
    recentHours?: number
  }) => {
    const bookParams = bookFilterParamsForUrl(
      {
        chips: next.chips ?? chips,
        labels: next.labels ?? selectedLabels,
        readingDifficulties: next.readingDifficulties ?? selectedDifficulties,
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
  }

  const { data: allPrices = [], isLoading: pricesLoading, isFetching, error } = usePrices()
  const { data: allBooks = [], isLoading: booksLoading } = useBooks(selectedLabels, chips.mostRecent)
  const { data: bookCount } = useBookCount()

  const matchingBookIds = useMemo(() => {
    const favoriteIds = favoriteItemIdsForLists(
      favoriteSummary?.lists,
      selectedFavoriteLists,
      'bookIds',
    )
    return new Set(
      applyBookPriceFilters(
        applyDesireToPurchaseFilter(
          applyReadingDifficultyFilter(
            applyBookStatusFilter(applyChipFilters(allBooks, chips), selectedStatuses),
            selectedDifficulties,
          ),
          selectedDesireToPurchase,
        )
          .filter((book) => matchesBookQuery(book, urlQuery))
          .filter((book) => favoriteIds.size === 0 || favoriteIds.has(book.id)),
        allPrices,
        {
          withPrices: chips.withPrices,
          noPrices: chips.noPrices,
          priceOlder: chips.priceOlder,
          priceOlderDays,
          lookupErrors: chips.lookupErrors,
        },
      ).map((book) => book.id),
    )
  }, [allBooks, allPrices, chips, favoriteSummary?.lists, priceOlderDays, selectedDesireToPurchase, selectedDifficulties, selectedFavoriteLists, selectedStatuses, urlQuery])

  const bookFiltersActive =
    urlQuery.trim().length > 0 ||
    selectedLabels.length > 0 ||
    selectedDifficulties.length > 0 ||
    selectedDesireToPurchase.length > 0 ||
    selectedStatuses.length > 0 ||
    selectedFavoriteLists.length > 0 ||
    Object.entries(chips).some(([, on]) => on)

  const prices = useMemo(() => {
    const scoped = bookFiltersActive
      ? allPrices.filter((price) => matchingBookIds.has(price.bookId))
      : allPrices
    const rows = chips.lookupErrors ? scoped.filter(isLookupError) : scoped
    return applyPriceFilters(rows, priceChips, maxTotal, Date.now(), recentHours)
  }, [allPrices, bookFiltersActive, chips.lookupErrors, matchingBookIds, maxTotal, priceChips, recentHours])

  const displayedBookCount = useMemo(() => {
    const ids = new Set<number>()
    for (const price of prices) {
      ids.add(price.bookId)
    }
    return ids.size
  }, [prices])

  const isLoading = pricesLoading || booksLoading

  return (
    <div>
      <PageHeader
        title="Prices"
        description="AbeBooks used-book prices for hardcover and softcover copies in good condition or better."
      />

      {error && (
        <ErrorMessage message={`Error loading prices: ${error.message}`} className="mb-4" />
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

        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary count={prices.length} singular="price" plural="prices" isLoading={isLoading} />
      </PageCard>
    </div>
  )
}
