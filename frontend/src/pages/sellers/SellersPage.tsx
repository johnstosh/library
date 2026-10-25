// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
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
import { useSellers } from '@/api/sellers'
import type { AbeSellerDto, AdditionalShippingSource } from '@/types/dtos'
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

/** True when the Each extra book column shows "Not published". */
export function isRateNotPublished(
  seller: Pick<AbeSellerDto, 'additionalShippingDollars' | 'additionalShippingSource'>,
): boolean {
  return additionalRateLabel(seller) === 'Not published'
}

export function SellersPage() {
  const { data: sellers = [], isLoading, error } = useSellers()
  const { data: branches = [] } = useBranches()
  const firstBranch = branches[0]
  const [includeNotPublished, setIncludeNotPublished] = useState(false)
  const [loadedCount, setLoadedCount] = useState(CATALOG_PAGE_SIZE)
  const filtered = includeNotPublished ? sellers : sellers.filter((seller) => !isRateNotPublished(seller))
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
      />
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
              sellers.length > 0
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
