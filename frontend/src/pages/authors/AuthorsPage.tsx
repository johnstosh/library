// (c) Copyright 2025 by Muczynski
import { useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { PiMagnifyingGlass } from 'react-icons/pi'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import { PageCard } from '@/components/ui/PageCard'
import { CatalogLoadMore } from '@/components/table/CatalogLoadMore'
import { TableSummary } from '@/components/table/TableSummary'
import { LoadingOverlay } from '@/components/progress/LoadingOverlay'
import { TransientFetchErrorBanner } from '@/components/ui/TransientFetchErrorBanner'
import { queryKeys } from '@/config/queryClient'
import { useQueryClient } from '@tanstack/react-query'
import { AuthorFilters } from './components/AuthorFilters'
import { FavoriteListFilters } from '@/pages/books/components/FavoriteListFilters'
import { AuthorTable } from './components/AuthorTable'
import { AuthorBulkActionsToolbar } from './components/AuthorBulkActionsToolbar'
import { useAuthorCount, useFilteredAuthors, type AuthorListFilters } from '@/api/authors'
import { CATALOG_PAGE_SIZE } from '@/api/byIds'
import { favoriteListsFromSearchParams } from '@/utils/bookFilterParams'
import { favoriteListChips, useFavoriteSummary } from '@/api/favorites'
import { useUiStore, useAuthorsChips, useAuthorsTableSelection } from '@/stores/uiStore'
import type { AuthorDto } from '@/types/dtos'

export function AuthorsPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedFavoriteLists = favoriteListsFromSearchParams(searchParams)
  const urlQuery = searchParams.get('q') ?? ''
  const [inputValue, setInputValue] = useState(urlQuery)
  // React Router updates the address bar before this component renders. Copy an
  // outside query into the box. A query this page just wrote must not replace a
  // name typed after that search.
  const [syncedQuery, setSyncedQuery] = useState(urlQuery)
  const ownQuery = useRef<string | null>(null)
  if (urlQuery !== syncedQuery) {
    const own = ownQuery.current === urlQuery
    ownQuery.current = null
    setSyncedQuery(urlQuery)
    if (!own) setInputValue(urlQuery)
  }
  const { data: favoriteSummary } = useFavoriteSummary()
  const favoriteChips = favoriteListChips(favoriteSummary?.lists, 'authors')

  const chips = useAuthorsChips()
  const { selectedIds, selectAll } = useAuthorsTableSelection()
  const { toggleRowSelection, toggleSelectAll, clearSelection, setSelectedIds, toggleAuthorsChip } = useUiStore()

  const authorFilters = useMemo((): AuthorListFilters => ({
    chips,
    favoriteLists: selectedFavoriteLists,
    q: urlQuery,
  }), [chips, selectedFavoriteLists, urlQuery])

  const writeQuery = (q: string) => {
    const trimmed = q.trim()
    ownQuery.current = trimmed
    const nextParams = new URLSearchParams(searchParams)
    if (trimmed) nextParams.set('q', trimmed)
    else nextParams.delete('q')
    setSearchParams(nextParams)
  }

  const {
    data: authors = [],
    total = 0,
    hasMore = false,
    loadMore = () => {},
    isLoadingMore = false,
    isLoading,
    isFetching,
    error,
  } = useFilteredAuthors(authorFilters, { pageSize: CATALOG_PAGE_SIZE })
  const { data: authorCount } = useAuthorCount()

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

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const field = e.currentTarget.elements.namedItem('q')
    const typed = field instanceof HTMLInputElement ? field.value : inputValue
    writeQuery(typed)
  }

  const handleSearchKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key !== 'Enter') return
    // Read the box, not React state. A fast Enter can arrive before onChange commits.
    e.preventDefault()
    writeQuery(e.currentTarget.value)
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

      {error && (
        <TransientFetchErrorBanner
          error={error}
          onRetry={() => {
            queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
          }}
          className="mb-4"
        />
      )}

      <PageCard padding={false} className="relative">
        <div className="p-4 border-b border-gray-200 space-y-3">
          <form onSubmit={handleSearch} className="flex max-w-full gap-2 items-start" data-test="authors-search-controls">
            <div className="min-w-0 w-full flex-1">
              <Input
                type="search"
                name="q"
                label="Filter by name"
                hideLabel
                placeholder="Filter by name..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={handleSearchKeyDown}
                enterKeyHint="search"
                autoComplete="off"
                aria-label="Filter by name"
                className="text-base"
                data-test="authors-name-filter"
              />
            </div>
            <Button
              type="submit"
              variant="primary"
              className="shrink-0 whitespace-nowrap"
              leftIcon={<PiMagnifyingGlass />}
              data-test="authors-search-button"
            >
              Search
            </Button>
          </form>
          <AuthorFilters
            chips={chips}
            onToggle={toggleAuthorsChip}
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
            searchResultsCount={total}
            totalCount={authorCount?.count}
            isLoading={isLoading}
          />

          <AuthorTable
            authors={authors}
            isLoading={isLoading}
            selectedIds={selectedIds}
            selectAll={selectAll}
            onSelectToggle={handleSelectToggle}
            onSelectAll={handleSelectAll}
            onView={handleViewAuthor}
          />
        </div>

        <CatalogLoadMore
          hasMore={hasMore}
          isLoadingMore={isLoadingMore}
          onLoadMore={loadMore}
          data-test="authors-load-more"
        />
        <LoadingOverlay show={isFetching && !isLoading} />
        <TableSummary
          count={authors.length}
          total={total}
          singular="author"
          plural="authors"
          isLoading={isLoading}
        />
      </PageCard>
    </div>
  )
}
