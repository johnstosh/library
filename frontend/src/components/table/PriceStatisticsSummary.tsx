// (c) Copyright 2025 by Muczynski
import type { BookPriceStatistics } from '@/utils/priceStatistics'
import { formatUsd } from '@/utils/formatters'

export interface PriceStatisticsSummaryProps {
  stats: BookPriceStatistics
  isLoading?: boolean
}

function Stat({
  label,
  value,
  dataTest,
}: {
  label: string
  value: string
  dataTest: string
}) {
  return (
    <div className="min-w-0">
      <dt className="text-gray-500">{label}</dt>
      <dd className="font-semibold text-gray-900 tabular-nums" data-test={dataTest}>
        {value}
      </dd>
    </div>
  )
}

export function PriceStatisticsSummary({
  stats,
  isLoading = false,
}: PriceStatisticsSummaryProps) {
  if (isLoading) return null

  return (
    <div
      className="px-4 py-3 border-t border-gray-200 bg-gray-50"
      data-test="price-statistics"
    >
      <p className="text-sm font-medium text-gray-900">Price statistics</p>
      <p className="text-xs text-gray-500 mt-0.5 mb-3">
        Cheapest hardcover or softcover total (item + shipping) per book.
      </p>
      <dl className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 text-sm">
        <Stat
          label="Total cost"
          value={formatUsd(stats.totalCost)}
          dataTest="price-stats-total-cost"
        />
        <Stat
          label="Over $20"
          value={stats.booksOver20.toLocaleString('en-US')}
          dataTest="price-stats-over-20"
        />
        <Stat
          label="Over $40"
          value={stats.booksOver40.toLocaleString('en-US')}
          dataTest="price-stats-over-40"
        />
        <Stat
          label="Over $80"
          value={stats.booksOver80.toLocaleString('en-US')}
          dataTest="price-stats-over-80"
        />
        <Stat
          label="Total books"
          value={stats.totalBooks.toLocaleString('en-US')}
          dataTest="price-stats-total-books"
        />
        <Stat
          label="Without prices"
          value={stats.booksWithoutPrices.toLocaleString('en-US')}
          dataTest="price-stats-without-prices"
        />
      </dl>
    </div>
  )
}
