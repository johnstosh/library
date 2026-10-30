// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { EntityLink } from '@/components/ui/EntityLink'
import { FilterChip } from '@/components/ui/FilterChip'
import { PageCard } from '@/components/ui/PageCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { CatalogLoadMore } from '@/components/table/CatalogLoadMore'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { SelectionToolbar, TableCountPlaceholder } from '@/components/table/SelectionToolbar'
import { TableSummary } from '@/components/table/TableSummary'
import { CATALOG_PAGE_SIZE } from '@/api/byIds'
import { useBranches } from '@/api/branches'
import { useReparseSellers, useSellers } from '@/api/sellers'
import type { AbeSellerDto, AdditionalShippingSource, SellerReparseResult } from '@/types/dtos'
import { formatUsd } from '@/utils/formatters'

function place(seller: AbeSellerDto): string {
  const parts = [seller.city, seller.region, seller.country].filter((part) => part && part.trim())
  return parts.length > 0 ? parts.join(', ') : '—'
}

export function additionalRateLabel(seller: Pick<AbeSellerDto, 'additionalShippingDollars' | 'additionalShippingSource'>): string {
  if (seller.additionalShippingSource === 'FREE') return 'Free'
  if (seller.additionalShippingDollars == null) return 'Not published'
  return formatUsd(seller.additionalShippingDollars)
}

export function sourceLabel(source: AdditionalShippingSource | null | undefined): string {
  if (source === 'LIBRARIAN') return 'Entered here'
  if (source === 'PARSED') return 'Seller terms'
  if (source === 'FREE') return 'Ships free'
  return 'Not published'
}

/** Extra-book shipping is cheaper than the stored first-book rate. */
export function hasMultiBookDiscount(
  seller: Pick<AbeSellerDto, 'additionalShippingDollars' | 'sampleStandardShipping'>,
): boolean {
  const extra = seller.additionalShippingDollars
  const first = seller.sampleStandardShipping
  if (extra == null || first == null) return false
  return extra < first
}

export function reparseMessage(result: SellerReparseResult): string {
  const updated = result.updated === 1
    ? 'Updated 1 seller from the stored terms.'
    : `Updated ${result.updated} sellers from the stored terms.`
  const kept = result.keptLibrarian === 1
    ? '1 rate entered here was kept.'
    : `${result.keptLibrarian} rates entered here were kept.`
  return `${updated} ${kept}`
}

/** True when the Each extra book column shows "Not published". */
export function isRateNotPublished(
  seller: Pick<AbeSellerDto, 'additionalShippingDollars' | 'additionalShippingSource'>,
): boolean {
  return additionalRateLabel(seller) === 'Not published'
}

export function SellersPage() {
  const { data: sellers = [], isLoading, error } = useSellers()
  const reparse = useReparseSellers()
  const { data: branches = [] } = useBranches()
  const firstBranch = branches[0]
  const [searchParams, setSearchParams] = useSearchParams()
  const multiBookOnly = searchParams.get('multiBook') === '1'
  const [includeNotPublished, setIncludeNotPublished] = useState(false)
  const [loadedCount, setLoadedCount] = useState(CATALOG_PAGE_SIZE)
  const [reparseNote, setReparseNote] = useState<string | null>(null)
  const [reparseError, setReparseError] = useState<string | null>(null)
  const filtered = sellers.filter((seller) => {
    if (!includeNotPublished && isRateNotPublished(seller)) return false
    if (multiBookOnly && !hasMultiBookDiscount(seller)) return false
    return true
  })
  const visible = filtered.slice(0, loadedCount)
  const hasMore = loadedCount < filtered.length
  const loadMore = () => {
    if (!hasMore) return
    setLoadedCount((count) => count + CATALOG_PAGE_SIZE)
  }
  const toggleNotPublished = () => {
    setIncludeNotPublished((on) => !on)
    setLoadedCount(CATALOG_PAGE_SIZE)
  }

  const toggleMultiBook = () => {
    const next = new URLSearchParams(searchParams)
    if (multiBookOnly) next.delete('multiBook')
    else next.set('multiBook', '1')
    setSearchParams(next)
    setLoadedCount(CATALOG_PAGE_SIZE)
  }

  async function reparseTerms() {
    setReparseError(null)
    try {
      const result = await reparse.mutateAsync()
      setReparseNote(reparseMessage(result))
    } catch (err) {
      setReparseNote(null)
      setReparseError(err instanceof Error ? err.message : 'Could not re-parse seller terms')
    }
  }

  const columns: Column<AbeSellerDto>[] = [
    {
      key: 'name',
      header: 'Seller',
      accessor: (seller) => (
        <EntityLink to={`/sellers/${seller.id}`} data-test={`seller-link-${seller.id}`}>
          {seller.name || `Seller ${seller.vendorId}`}
        </EntityLink>
      ),
      width: '24%',
    },
    {
      key: 'place',
      header: 'Location',
      accessor: (seller) => place(seller),
      width: '22%',
    },
    {
      key: 'additional',
      header: 'Each extra book',
      accessor: (seller) => (
        <span data-test={`seller-additional-${seller.id}`}>{additionalRateLabel(seller)}</span>
      ),
      width: '14%',
    },
    {
      key: 'source',
      header: 'Rate source',
      accessor: (seller) => sourceLabel(seller.additionalShippingSource),
      width: '14%',
    },
    {
      key: 'listings',
      header: 'Listings',
      accessor: (seller) => seller.listingCount,
      width: '10%',
    },
    {
      key: 'storefront',
      header: 'Storefront',
      accessor: (seller) =>
        seller.storefrontUrl ? (
          <a
            href={seller.storefrontUrl}
            target="_blank"
            rel="noreferrer"
            className="text-primary-700 hover:underline"
            data-test={`seller-storefront-${seller.id}`}
          >
            AbeBooks
          </a>
        ) : (
          '—'
        ),
      width: '16%',
    },
  ]

  return (
    <div>
      <PageHeader
        title="Sellers"
        description="AbeBooks booksellers seen during price lookup. The extra-book shipping rate is edited on each seller."
        actions={(
          <Button
            type="button"
            variant="outline"
            onClick={() => { void reparseTerms() }}
            isLoading={reparse.isPending}
            data-test="reparse-sellers"
          >
            Re-parse terms
          </Button>
        )}
      />
      {reparseNote && (
        <p className="mb-4 text-sm text-gray-700" data-test="reparse-result">{reparseNote}</p>
      )}
      {reparseError && (
        <p className="mb-4 text-sm text-red-700" data-test="reparse-error">{reparseError}</p>
      )}
      {error && (
        <p className="mb-4 text-sm text-red-700" data-test="sellers-error">
          Could not load sellers.
        </p>
      )}
      <PageCard padding={false}>
        <div className="p-4 border-b border-gray-200">
          <div className="flex flex-wrap gap-2" data-test="seller-filter-chips">
            <FilterChip
              label="Not published"
              active={includeNotPublished}
              onClick={toggleNotPublished}
              tooltip="Include sellers whose extra-book rate is not published. Off hides them."
              dataTest="filter-not-published"
            />
            <FilterChip
              label="Multi-book discount"
              active={multiBookOnly}
              onClick={toggleMultiBook}
              tooltip="Sellers whose extra book ships for less than the first book."
              dataTest="filter-multi-book"
            />
          </div>
        </div>
        <div className="p-4">
          <SelectionToolbar dataTest="sellers-stats" selected={false}>
            <TableCountPlaceholder
              tableCount={visible.length}
              searchResultsCount={filtered.length}
              totalCount={sellers.length}
              singular="seller"
              plural="sellers"
              isLoading={isLoading}
              branchName={firstBranch?.branchName}
              librarySystemName={firstBranch?.librarySystemName}
            />
          </SelectionToolbar>
          <DataTable
            data={visible}
            columns={columns}
            keyExtractor={(seller) => seller.id}
            emptyMessage={
              multiBookOnly
                ? 'No sellers with a multi-book shipping discount.'
                : sellers.length > 0
                  ? 'No sellers with a published extra-book rate.'
                  : 'No sellers yet. Look up prices to capture AbeBooks sellers.'
            }
            isLoading={isLoading}
          />
        </div>
        <CatalogLoadMore
          hasMore={hasMore}
          isLoadingMore={false}
          shownCount={visible.length}
          onLoadMore={loadMore}
          data-test="sellers-load-more"
        />
        <TableSummary
          count={visible.length}
          total={filtered.length}
          singular="seller"
          plural="sellers"
          isLoading={isLoading}
        />
      </PageCard>
    </div>
  )
}
