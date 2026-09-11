// (c) Copyright 2025 by Muczynski
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TableSummary } from '@/components/table/TableSummary'
import { ErrorMessage } from '@/components/ui/ErrorMessage'
import { BookFilters } from '@/pages/books/components/BookFilters'
import { BookLabelFilters } from '@/pages/books/components/BookLabelFilters'
import { ReadingDifficultyFilters } from '@/pages/books/components/ReadingDifficultyFilters'
import { FavoriteListFilters } from '@/pages/books/components/FavoriteListFilters'
import { PriceFilters } from './components/PriceFilters'
import { PriceTable } from './components/PriceTable'
import { usePrices } from '@/api/prices'
import { useBooks } from '@/api/books'
import { applyChipFilters } from '@/utils/bookChipFilters'
import {
  bookFilterParamsForUrl,
  chipsFromSearchParams,
  favoriteListsFromSearchParams,
  labelsFromSearchParams,
  matchesBookQuery,
} from '@/utils/bookFilterParams'
import {
  applyReadingDifficultyFilter,
  readingDifficultiesFromSearchParams,
} from '@/utils/readingDifficulty'
import type { ReadingDifficulty } from '@/types/enums'
import { favoriteItemIdsForLists, favoriteListChips, useFavoriteSummary } from '@/api/favorites'
import type { BookChipFilters } from '@/utils/bookChipFilters'
import {
  applyPriceFilters,
  maxTotalFromSearchParams,
  priceChipsFromSearchParams,
  priceFilterParamsForUrl,
  type PriceChipFilters,
} from '@/utils/priceFilters'

export function PricesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const chips = chipsFromSearchParams(searchParams, 'prices')
  const selectedLabels = labelsFromSearchParams(searchParams)
  const selectedDifficulties = readingDifficultiesFromSearchParams(searchParams)
  const selectedFavoriteLists = favoriteListsFromSearchParams(searchParams)
  const urlQuery = searchParams.get('q') ?? ''
  const [inputValue, setInputValue] = useState(urlQuery)
  const priceChips = priceChipsFromSearchParams(searchParams)
  const maxTotal = maxTotalFromSearchParams(searchParams)
  const [maxTotalInput, setMaxTotalInput] = useState(maxTotal)
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
    favoriteLists?: string[]
    q?: string
    priceChips?: PriceChipFilters
    maxTotal?: string
  }) => {
    const bookParams = bookFilterParamsForUrl(
      {
        chips: next.chips ?? chips,
        labels: next.labels ?? selectedLabels,
        readingDifficulties: next.readingDifficulties ?? selectedDifficulties,
        favoriteLists: next.favoriteLists ?? selectedFavoriteLists,
        q: next.q !== undefined ? next.q : urlQuery,
      },
      'prices',
    )
    const priceParams = priceFilterParamsForUrl({
      chips: next.priceChips ?? priceChips,
      maxTotal: next.maxTotal !== undefined ? next.maxTotal : maxTotal,
    })
    setSearchParams({ ...bookParams, ...priceParams })
  }

  const { data: allPrices = [], isLoading: pricesLoading, isFetching, error } = usePrices()
  const { data: allBooks = [], isLoading: booksLoading } = useBooks(selectedLabels, chips.mostRecent)

  const matchingBookIds = useMemo(() => {
    const favoriteIds = favoriteItemIdsForLists(
      favoriteSummary?.lists,
      selectedFavoriteLists,
      'bookIds',
    )
    return new Set(
      applyReadingDifficultyFilter(applyChipFilters(allBooks, chips), selectedDifficulties)
        .filter((book) => matchesBookQuery(book, urlQuery))
        .filter((book) => favoriteIds.size === 0 || favoriteIds.has(book.id))
        .map((book) => book.id),
    )
  }, [allBooks, chips, favoriteSummary?.lists, selectedDifficulties, selectedFavoriteLists, urlQuery])

  const bookFiltersActive =
    urlQuery.trim().length > 0 ||
    selectedLabels.length > 0 ||
    selectedDifficulties.length > 0 ||
    selectedFavoriteLists.length > 0 ||
    Object.entries(chips).some(([, on]) => on)

  const prices = useMemo(() => {
    const scoped = bookFiltersActive
      ? allPrices.filter((price) => matchingBookIds.has(price.bookId))
      : allPrices
    return applyPriceFilters(scoped, priceChips, maxTotal)
  }, [allPrices, bookFiltersActive, matchingBookIds, priceChips, maxTotal])

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
            <div className="w-full sm:w-48">
              <Input
                type="number"
                min="0"
                step="0.01"
                label="Total less than"
                hideLabel
                placeholder="Total less than $"
                value={maxTotalInput}
                onChange={(e) => setMaxTotalInput(e.target.value)}
                data-test="prices-max-total"
              />
            </div>
            <Button type="submit" variant="primary" data-test="prices-search-button">
              Apply
            </Button>
          </form>
          <PriceFilters
            chips={priceChips}
            onToggle={(key) => writeUrl({ priceChips: { ...priceChips, [key]: !priceChips[key] } })}
          />
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
        </div>

        <div className="p-4">
          <PriceTable prices={prices} isLoading={isLoading} />
        </div>

        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary count={prices.length} singular="price" plural="prices" isLoading={isLoading} />
      </PageCard>
    </div>
  )
}
