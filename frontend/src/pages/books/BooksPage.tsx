// (c) Copyright 2025 by Muczynski
import { useEffect, useMemo, useState, useTransition } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PiCurrencyDollar, PiMagnifyingGlass } from 'react-icons/pi'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TableSummary } from '@/components/table/TableSummary'
import { TransientFetchErrorBanner } from '@/components/ui/TransientFetchErrorBanner'
import { BookFilters } from './components/BookFilters'
import { BookPriceFilters } from './components/BookPriceFilters'
import { BookLabelFilters } from './components/BookLabelFilters'
import { BindingFilters } from './components/BindingFilters'
import { ReadingDifficultyFilters } from './components/ReadingDifficultyFilters'
import { StatusFilters } from './components/StatusFilters'
import { FavoriteListFilters } from './components/FavoriteListFilters'
import { BookTable } from './components/BookTable'
import { BulkActionsToolbar } from './components/BulkActionsToolbar'
import { useBookCount, useBooks } from '@/api/books'
import { useUiStore, useBooksTableSelection } from '@/stores/uiStore'
import { useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/config/queryClient'
import { applyBookPriceFilters, applyChipFilters } from '@/utils/bookChipFilters'
import {
  bookFilterParamsForUrl,
  chipsFromSearchParams,
  favoriteListsFromSearchParams,
  labelsFromSearchParams,
  matchesBookQuery,
  priceOlderDaysFromSearchParams,
  pricesPathFromFilters,
} from '@/utils/bookFilterParams'
import { usePrices } from '@/api/prices'
import {
  applyBookBindingFilter,
  bindingsFromSearchParams,
} from '@/utils/bookBinding'
import {
  applyReadingDifficultyFilter,
  readingDifficultiesFromSearchParams,
} from '@/utils/readingDifficulty'
import {
  applyBookStatusFilter,
  bookStatusesFromSearchParams,
  type BookStatusFilter,
} from '@/utils/bookStatus'
import type { BookCoverType, ReadingDifficulty } from '@/types/enums'
import { useIsLibrarian } from '@/stores/authStore'
import { favoriteItemIdsForLists, favoriteListChips, useFavoriteSummary } from '@/api/favorites'
import type { BookChipFilters } from '@/utils/bookChipFilters'
import type { BookDto } from '@/types/dtos'

export function BooksPage() {
  const navigate = useNavigate()
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
  const { selectedIds, selectAll } = useBooksTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds } = useUiStore()
  const isLibrarian = useIsLibrarian()
  const queryClient = useQueryClient()
  const [, startTransition] = useTransition()

  useEffect(() => {
    setInputValue(urlQuery)
  }, [urlQuery])

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
    startTransition(() => {
      setSearchParams(
        bookFilterParamsForUrl(
          {
            chips: next.chips ?? chips,
            labels: next.labels ?? selectedLabels,
            readingDifficulties: next.readingDifficulties ?? selectedDifficulties,
            bindings: next.bindings ?? selectedBindings,
            statuses: next.statuses ?? selectedStatuses,
            favoriteLists: next.favoriteLists ?? selectedFavoriteLists,
            q: next.q !== undefined ? next.q : urlQuery,
            priceOlderDays: next.priceOlderDays ?? priceOlderDays,
          },
          'books',
        ),
      )
    })
  }

  const { data: allBooks = [], isLoading, isFetching, error } = useBooks(selectedLabels, chips.mostRecent)
  const { data: bookCount } = useBookCount()

  const isFilterPending = isFetching && !isLoading

  // Compute non-price filtered books for scoping price fetch (when librarian).
  const nonPriceFilteredBooks = useMemo(() => {
    const favoriteIds = favoriteItemIdsForLists(
      favoriteSummary?.lists,
      selectedFavoriteLists,
      'bookIds',
    )
    return applyBookBindingFilter(
      applyReadingDifficultyFilter(
        applyBookStatusFilter(applyChipFilters(allBooks, chips), selectedStatuses),
        selectedDifficulties,
      ),
      selectedBindings,
    )
      .filter((book) => matchesBookQuery(book, urlQuery))
      .filter((book) => favoriteIds.size === 0 || favoriteIds.has(book.id))
  }, [allBooks, chips, selectedStatuses, selectedDifficulties, selectedBindings, selectedFavoriteLists, favoriteSummary?.lists, urlQuery])

  // Use scoped prices (via /by-book-ids) for price chips when librarian (avoids full catalog on filtered views).
  // Non-librarian and full-catalog still use default usePrices().
  const priceBookIds = isLibrarian ? nonPriceFilteredBooks.map((b) => b.id) : undefined
  const { data: allPrices = [] } = usePrices({ enabled: isLibrarian, bookIds: priceBookIds })

  const books = useMemo(() => {
    return applyBookPriceFilters(
      nonPriceFilteredBooks,
      allPrices,
      {
        withPrices: chips.withPrices,
        noPrices: chips.noPrices,
        priceOlder: chips.priceOlder,
        priceOlderDays,
        lookupErrors: chips.lookupErrors,
      },
    )
  }, [nonPriceFilteredBooks, allPrices, chips, priceOlderDays])


  const handleSelectToggle = (id: number) => {
    toggleRowSelection('booksTable', id)
  }

  const handleSelectAll = () => {
    if (selectAll) {
      clearSelection('booksTable')
    } else {
      const allIds = new Set(books.map((b) => b.id))
      setSelectedIds('booksTable', allIds)
      toggleSelectAll('booksTable')
    }
  }

  const handleClearSelection = () => {
    clearSelection('booksTable')
  }

  const handleAddBook = () => {
    navigate('/books/new')
  }

  const handleViewBook = (book: BookDto) => {
    navigate(`/books/${book.id}`)
  }

  const handleToggleChip = (key: keyof BookChipFilters) => {
    writeUrl({ chips: { ...chips, [key]: !chips[key] } })
  }

  const handleToggleLabel = (label: string) => {
    const nextLabels = selectedLabels.includes(label)
      ? selectedLabels.filter((l) => l !== label)
      : [...selectedLabels, label]
    writeUrl({ labels: nextLabels })
  }

  const handleToggleDifficulty = (value: ReadingDifficulty) => {
    const next = selectedDifficulties.includes(value)
      ? selectedDifficulties.filter((item) => item !== value)
      : [...selectedDifficulties, value]
    writeUrl({ readingDifficulties: next })
  }

  const handleToggleBinding = (value: BookCoverType) => {
    const next = selectedBindings.includes(value)
      ? selectedBindings.filter((item) => item !== value)
      : [...selectedBindings, value]
    writeUrl({ bindings: next })
  }

  const handleToggleStatus = (value: BookStatusFilter) => {
    const next = selectedStatuses.includes(value)
      ? selectedStatuses.filter((item) => item !== value)
      : [...selectedStatuses, value]
    writeUrl({ statuses: next })
  }

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    writeUrl({ q: inputValue.trim() })
  }

  return (
    <div>
      <PageHeader
        title="Books"
        actions={
          isLibrarian ? (
            <>
              <Link to="/books-from-feed">
                <Button variant="outline" data-test="books-from-feed">
                  Books from Feed
                </Button>
              </Link>
              <Button variant="primary" onClick={handleAddBook} data-test="add-book">
                Add Book
              </Button>
            </>
          ) : undefined
        }
      />

      {error && (
        <TransientFetchErrorBanner
          error={error}
          onRetry={() => {
            // Refetch both queries used by useBooks
            queryClient.invalidateQueries({ queryKey: queryKeys.books.summaries() })
            queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
          }}
          className="mb-4"
          data-test="books-error-banner"
        />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200 space-y-3">
          <form onSubmit={handleSearch} className="flex gap-2 items-start">
            <div className="flex-1">
              <Input
                type="search"
                label="Filter by title or author"
                hideLabel
                placeholder="Filter by title or author..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                data-test="books-title-filter"
              />
            </div>
            <Button
              type="submit"
              variant="primary"
              leftIcon={<PiMagnifyingGlass />}
              data-test="books-search-button"
            >
              Search
            </Button>
          </form>
          {isLibrarian && (
            <Button
              variant="outline"
              to={pricesPathFromFilters({
                chips,
                labels: selectedLabels,
                readingDifficulties: selectedDifficulties,
                bindings: selectedBindings,
                statuses: selectedStatuses,
                favoriteLists: selectedFavoriteLists,
                q: inputValue.trim() || urlQuery,
                priceOlderDays,
              })}
              leftIcon={<PiCurrencyDollar />}
              data-test="open-in-prices"
            >
              Open in Prices
            </Button>
          )}
          <BookFilters
            chips={chips}
            onToggle={handleToggleChip}
            showAvailabilityFilters
            showCatalogerFilters
          />
          <BookLabelFilters
            selectedLabels={selectedLabels}
            onToggleLabel={handleToggleLabel}
            onClearLabels={() => writeUrl({ labels: [] })}
          />
          <StatusFilters
            selected={selectedStatuses}
            onToggle={handleToggleStatus}
            onClear={() => writeUrl({ statuses: [] })}
          />
          <BindingFilters
            selected={selectedBindings}
            onToggle={handleToggleBinding}
            onClear={() => writeUrl({ bindings: [] })}
          />
          <ReadingDifficultyFilters
            selected={selectedDifficulties}
            onToggle={handleToggleDifficulty}
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
          {isLibrarian && (
            <BookPriceFilters
              chips={chips}
              onToggle={handleToggleChip}
              priceOlderDays={priceOlderDays}
              onPriceOlderDaysChange={(days) =>
                writeUrl({ chips: { ...chips, priceOlder: true }, priceOlderDays: days })
              }
            />
          )}
        </div>

        <div className="p-4">
          <BulkActionsToolbar
            selectedIds={selectedIds}
            onClearSelection={handleClearSelection}
            tableCount={books.length}
            totalCount={bookCount?.count}
            isLoading={isLoading}
          />

          <BookTable
            books={books}
            isLoading={isLoading}
            selectedIds={selectedIds}
            selectAll={selectAll}
            onSelectToggle={handleSelectToggle}
            onSelectAll={handleSelectAll}
            onView={handleViewBook}
          />
        </div>

        <LoadingOverlay show={isFilterPending} />
        <TableSummary count={books.length} singular="book" plural="books" isLoading={isLoading} />
      </PageCard>

    </div>
  )
}
