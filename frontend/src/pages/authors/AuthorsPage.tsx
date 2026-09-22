// (c) Copyright 2025 by Muczynski
import { useMemo } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { TableSummary } from '@/components/table/TableSummary'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TransientFetchErrorBanner } from '@/components/ui/TransientFetchErrorBanner'
import { queryKeys } from '@/config/queryClient'
import { useQueryClient } from '@tanstack/react-query'
import { AuthorFilters } from './components/AuthorFilters'
import { FavoriteListFilters } from '@/pages/books/components/FavoriteListFilters'
import { AuthorTable } from './components/AuthorTable'
import { AuthorBulkActionsToolbar } from './components/AuthorBulkActionsToolbar'
import { useAuthorAvailability, useAuthorCount, useAuthors } from '@/api/authors'
import { applyAuthorChipFilters, isAvailabilityChipActive, isOtherAuthorChipActive } from '@/utils/authorChipFilters'
import { favoriteListsFromSearchParams } from '@/utils/bookFilterParams'
import { favoriteItemIdsForLists, favoriteListChips, useFavoriteSummary } from '@/api/favorites'
import { useUiStore, useAuthorsChips, useAuthorsTableSelection } from '@/stores/uiStore'
import type { AuthorDto } from '@/types/dtos'

export function AuthorsPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedFavoriteLists = favoriteListsFromSearchParams(searchParams)
  const { data: favoriteSummary } = useFavoriteSummary()
  const favoriteChips = favoriteListChips(favoriteSummary?.lists, 'authors')

  const chips = useAuthorsChips()
  const { selectedIds, selectAll } = useAuthorsTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds, toggleAuthorsChip } = useUiStore()

  // mostRecent defaults on in uiStore so this uses GET /authors/most-recent-day
  // when it is the only filter (fast path). When combined with other filters,
  // we load summaries (or most-recent-day) then apply intersection client-side.
  const favoriteFilterOn = selectedFavoriteLists.length > 0
  const { data: allAuthors = [], isLoading, isFetching, error } = useAuthors(
    chips.mostRecent && !isOtherAuthorChipActive(chips) && !favoriteFilterOn
      ? 'most-recent'
      : undefined
  )
  const { data: authorCount } = useAuthorCount()
  const {
    data: availability = [],
    isLoading: availabilityLoading,
    isFetching: availabilityFetching,
    error: availabilityError,
  } = useAuthorAvailability()

  const availabilityByAuthorId = useMemo(() => {
    const map = new Map<number, (typeof availability)[number]>()
    availability.forEach((row) => map.set(row.authorId, row))
    return map
  }, [availability])

  const authors = useMemo(() => {
    if (isAvailabilityChipActive(chips) && availabilityLoading) return []
    // mostRecent now participates in intersection (AND) with other chips.
    // When mostRecent-only, backend /most-recent-day already filtered it.
    // Otherwise, pass full chips so applyAuthorChipFilters applies mostRecentIds
    // (computed from allAuthors) on top of other filters.
    const favoriteIds = favoriteItemIdsForLists(
      favoriteSummary?.lists,
      selectedFavoriteLists,
      'authorIds',
    )
    const mostRecentIds = chips.mostRecent
      ? new Set(allAuthors.map((a) => a.id))
      : undefined
    return applyAuthorChipFilters(
      allAuthors,
      chips,
      mostRecentIds,
      availabilityByAuthorId,
    ).filter((author) => favoriteIds.size === 0 || favoriteIds.has(author.id))
  }, [allAuthors, availabilityByAuthorId, availabilityLoading, chips, favoriteSummary?.lists, selectedFavoriteLists])

  const handleSelectToggle = (id: number) => {
    toggleRowSelection('authorsTable', id)
  }

  const handleSelectAll = () => {
    if (selectAll) {
      clearSelection('authorsTable')
    } else {
      const allIds = new Set(authors.map((a) => a.id))
      setSelectedIds('authorsTable', allIds)
      toggleSelectAll('authorsTable')
    }
  }

  const handleClearSelection = () => {
    clearSelection('authorsTable')
  }

  const handleAddAuthor = () => {
    navigate('/authors/new')
  }

  const handleViewAuthor = (author: AuthorDto) => {
    navigate(`/authors/${author.id}`)
  }

  return (
    <div>
      <PageHeader
        title="Authors"
        actions={
          <Button variant="primary" onClick={handleAddAuthor} data-test="add-author">
            Add Author
          </Button>
        }
      />

      {(error || availabilityError) && (
        <TransientFetchErrorBanner
          error={error ?? availabilityError}
          onRetry={() => {
            queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
          }}
          className="mb-4"
        />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200">
          <AuthorFilters
            chips={chips}
            onToggle={toggleAuthorsChip}
            mostRecentDisabled={false}
          />
          <FavoriteListFilters
            lists={favoriteChips}
            selected={selectedFavoriteLists}
            onToggle={(listName) => {
              const next = selectedFavoriteLists.includes(listName)
                ? selectedFavoriteLists.filter((name) => name !== listName)
                : [...selectedFavoriteLists, listName]
              const nextParams = new URLSearchParams(searchParams)
              if (next.length > 0) nextParams.set('favoriteLists', next.join(','))
              else nextParams.delete('favoriteLists')
              setSearchParams(nextParams)
            }}
            onClear={() => {
              const nextParams = new URLSearchParams(searchParams)
              nextParams.delete('favoriteLists')
              setSearchParams(nextParams)
            }}
          />
        </div>

        <div className="p-4">
          <AuthorBulkActionsToolbar
            selectedIds={selectedIds}
            onClearSelection={handleClearSelection}
            tableCount={authors.length}
            totalCount={authorCount?.count}
            isLoading={
              isLoading
              || (isAvailabilityChipActive(chips) && availabilityLoading)
            }
          />

          <AuthorTable
            authors={authors}
            isLoading={
              isLoading
              || (isAvailabilityChipActive(chips) && availabilityLoading)
            }
            selectedIds={selectedIds}
            selectAll={selectAll}
            onSelectToggle={handleSelectToggle}
            onSelectAll={handleSelectAll}
            onView={handleViewAuthor}
          />
        </div>

        <LoadingOverlay
          show={
            (isFetching && !isLoading) ||
            (isAvailabilityChipActive(chips) && availabilityFetching && !availabilityLoading)
          }
        />
        <TableSummary
          count={authors.length}
          singular="author"
          plural="authors"
          isLoading={
            isLoading
            || (isAvailabilityChipActive(chips) && availabilityLoading)
          }
        />
      </PageCard>
    </div>
  )
}
