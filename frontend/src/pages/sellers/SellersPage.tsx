// (c) Copyright 2025 by Muczynski
import { EntityLink } from '@/components/ui/EntityLink'
import { PageCard } from '@/components/ui/PageCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
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

export function SellersPage() {
  const { data: sellers = [], isLoading, error } = useSellers()

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
        <DataTable
          data={sellers}
          columns={columns}
          keyExtractor={(seller) => seller.id}
          emptyMessage="No sellers yet. Look up prices to capture AbeBooks sellers."
          isLoading={isLoading}
        />
      </PageCard>
    </div>
  )
}
