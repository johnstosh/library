// (c) Copyright 2025 by Muczynski
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { EntityLink } from '@/components/ui/EntityLink'
import type { BookPriceDto } from '@/types/dtos'
import { formatDateTime, formatUsd } from '@/utils/formatters'
import { coverLabel } from '@/utils/priceFilters'

interface PriceTableProps {
  prices: BookPriceDto[]
  isLoading: boolean
}

export function PriceTable({ prices, isLoading }: PriceTableProps) {
  const columns: Column<BookPriceDto>[] = [
    {
      key: 'bookTitle',
      header: 'Book',
      accessor: (price) => (
        <div className="break-words">
          <EntityLink
            to={`/books/${price.bookId}`}
            className="font-medium"
            data-test={`price-book-${price.id}`}
          >
            {price.bookTitle ?? 'Unknown book'}
          </EntityLink>
          {price.author && <div className="text-sm text-gray-500">{price.author}</div>}
        </div>
      ),
      width: '20%',
      cellClassName: 'px-3 sm:px-6 py-3 sm:py-4 max-w-0 align-top text-sm',
    },
    {
      key: 'cover',
      header: 'Cover',
      accessor: (price) => coverLabel(price.cover),
      width: '9%',
    },
    {
      key: 'condition',
      header: 'Condition',
      accessor: (price) => price.condition || '—',
      width: '11%',
    },
    {
      key: 'price',
      header: 'Price',
      accessor: (price) => formatUsd(price.priceDollars),
      width: '8%',
    },
    {
      key: 'shipping',
      header: 'Shipping',
      accessor: (price) => formatUsd(price.shippingDollars),
      width: '8%',
    },
    {
      key: 'total',
      header: 'Total',
      accessor: (price) => (
        <span className="font-medium" data-test={`price-total-${price.id}`}>
          {formatUsd(price.totalDollars)}
        </span>
      ),
      width: '8%',
    },
    {
      key: 'lookedUpAt',
      header: 'Looked up',
      accessor: (price) => formatDateTime(price.lookedUpAt),
      width: '10%',
      hideOnMobile: true,
    },
    {
      key: 'status',
      header: 'Status',
      accessor: (price) =>
        price.lookupError ? (
          <span className="text-sm text-red-700 break-words" data-test={`price-status-${price.id}`}>
            {price.lookupError}
          </span>
        ) : (
          <span data-test={`price-status-${price.id}`}>—</span>
        ),
      width: '16%',
      cellClassName: 'px-3 sm:px-6 py-3 sm:py-4 align-top text-sm',
    },
    {
      key: 'details',
      header: 'Listing',
      accessor: (price) =>
        price.detailsUrl ? (
          <a
            href={price.detailsUrl}
            target="_blank"
            rel="noreferrer"
            className="text-primary-700 hover:underline"
            data-test={`price-listing-${price.id}`}
          >
            {price.lookupError ? 'Search' : 'AbeBooks'}
          </a>
        ) : (
          '—'
        ),
      width: '10%',
    },
  ]

  return (
    <DataTable
      data={prices}
      columns={columns}
      keyExtractor={(price) => price.id}
      emptyMessage="No prices match the current filters."
      isLoading={isLoading}
    />
  )
}
