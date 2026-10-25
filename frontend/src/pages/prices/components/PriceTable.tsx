// (c) Copyright 2025 by Muczynski
import { DataTable } from '@/components/table/DataTable'
import type { Column } from '@/components/table/DataTable'
import { EntityLink } from '@/components/ui/EntityLink'
import type { BookPriceDto } from '@/types/dtos'
import { formatDateTime, formatUsd } from '@/utils/formatters'
import { coverAbbrev, coverLabel } from '@/utils/priceFilters'

interface PriceTableProps {
  prices: BookPriceDto[]
  isLoading: boolean
}

/**
 * Shares of the table. The previous widths summed to 110, so each old visual
 * share was width/110. Book, Seller, Cover, and Looked up are those shares
 * times 4, 1/3, 0.7, and 1/2. Price keeps the width of the old Price column
 * and stacks item, shipping, and total. Condition, Status, and Listing split
 * the remaining width in their old 11:16:10 ratio.
 */
const NARROW_HEADER = 'px-1 py-2 text-[10px] tracking-normal whitespace-normal leading-tight break-words'

function PriceAmounts({ price }: { price: BookPriceDto }) {
  if (price.priceDollars == null && price.shippingDollars == null && price.totalDollars == null) {
    return <span data-test={`price-total-${price.id}`}>—</span>
  }
  return (
    <div className="leading-tight tabular-nums whitespace-nowrap">
      <div>
        <span className="sr-only">Price </span>
        {formatUsd(price.priceDollars)}
      </div>
      <div className="text-gray-500">
        <span className="sr-only">Shipping </span>
        + {formatUsd(price.shippingDollars)}
      </div>
      <div className="font-medium">
        <span className="sr-only">Total </span>
        <span data-test={`price-total-${price.id}`}>{formatUsd(price.totalDollars)}</span>
      </div>
    </div>
  )
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
      width: '65.5%',
      cellClassName: 'px-3 py-2 max-w-0 align-top text-sm',
    },
    {
      key: 'seller',
      header: 'Seller',
      accessor: (price) =>
        price.sellerId ? (
          <div className="truncate" title={price.sellerName || 'Seller'}>
            <EntityLink to={`/sellers/${price.sellerId}`} data-test={`price-seller-${price.id}`}>
              {price.sellerName || 'Seller'}
            </EntityLink>
          </div>
        ) : (
          '—'
        ),
      width: '3.6%',
      headerClassName: NARROW_HEADER,
      cellClassName: 'px-1 py-2 max-w-0 overflow-hidden align-top text-sm',
    },
    {
      key: 'cover',
      header: 'Cover',
      accessor: (price) => (
        <span title={coverLabel(price.cover)} data-test={`price-cover-${price.id}`}>
          {coverAbbrev(price.cover)}
        </span>
      ),
      width: '5.7%',
      headerClassName: NARROW_HEADER,
      cellClassName: 'px-1 py-2 align-top text-sm whitespace-nowrap',
    },
    {
      key: 'condition',
      header: 'Condition',
      accessor: (price) => price.condition || '—',
      width: '4%',
      headerClassName: NARROW_HEADER,
      cellClassName: 'px-1 py-2 align-top text-sm break-words',
    },
    {
      key: 'price',
      header: 'Price',
      headerTitle: 'Item price, shipping, and total',
      accessor: (price) => <PriceAmounts price={price} />,
      width: '7.3%',
      headerClassName: NARROW_HEADER,
      cellClassName: 'px-1 py-2 max-w-0 overflow-hidden align-top text-sm whitespace-normal',
    },
    {
      key: 'lookedUpAt',
      header: 'Looked up',
      accessor: (price) => {
        const full = formatDateTime(price.lookedUpAt)
        if (!full) return '—'
        return (
          <span title={full} className="whitespace-nowrap">
            {formatDateTime(price.lookedUpAt, 'M/d/yy')}
          </span>
        )
      },
      width: '4.5%',
      headerClassName: NARROW_HEADER,
      cellClassName: 'px-1 py-2 align-top text-sm whitespace-nowrap',
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
      width: '5.8%',
      headerClassName: NARROW_HEADER,
      cellClassName: 'px-1 py-2 align-top text-sm',
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
            className="text-primary-700 hover:underline break-all"
            data-test={`price-listing-${price.id}`}
          >
            {price.lookupError ? 'Search' : 'AbeBooks'}
          </a>
        ) : (
          '—'
        ),
      width: '3.6%',
      headerClassName: NARROW_HEADER,
      cellClassName: 'px-1 py-2 align-top text-sm',
    },
  ]

  return (
    <DataTable
      data={prices}
      columns={columns}
      keyExtractor={(price) => price.id}
      emptyMessage="No prices match the current filters."
      isLoading={isLoading}
      tableClassName="min-w-[64rem]"
    />
  )
}
