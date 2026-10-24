// (c) Copyright 2025 by Muczynski

export type AdditionalShippingSource = 'PARSED' | 'LIBRARIAN' | 'FREE' | 'UNKNOWN'

export interface SavingsBook {
  id: number
  title: string
  author: string | null
}

export interface SavingsListing {
  bookId: number
  listingId: number
  listingTitle: string | null
  sellerId: number
  sellerName: string
  storefrontUrl: string | null
  detailsUrl: string | null
  priceDollars: number
  shippingDollars: number
  additionalShippingDollars: number | null
  additionalShippingSource: AdditionalShippingSource
  condition: string | null
  binding: string | null
}

export interface SavingsLine {
  bookId: number
  catalogTitle: string
  author: string | null
  listingTitle: string | null
  condition: string | null
  binding: string | null
  itemDollars: number
  firstShippingDollars: number
  detailsUrl: string | null
  baselineDollars: number
}

export interface SellerBundle {
  sellerId: number
  sellerName: string
  storefrontUrl: string | null
  shippingEstimated: boolean
  firstShippingDollars: number
  additionalShippingDollars: number
  basketDollars: number
  baselineDollars: number
  savingsDollars: number
  lines: SavingsLine[]
}

export interface SharedSeller {
  sellerId: number
  sellerName: string
  storefrontUrl: string | null
  note: string
  bookIds: number[]
  catalogTitles: string[]
}

export interface IndividualBuy {
  bookId: number
  catalogTitle: string
  author: string | null
  baselineDollars: number
}

export interface BookWithoutListings {
  bookId: number
  catalogTitle: string
}

export interface SavingsReport {
  totalSavings: number
  sellers: SellerBundle[]
  shared: SharedSeller[]
  individual: IndividualBuy[]
  booksWithoutListings: BookWithoutListings[]
}

export const UNPUBLISHED_RATE_NOTE = "AbeBooks does not publish this seller's extra-book rate."

interface Cand {
  bookId: number
  item: number
  first: number
  baseline: number
  addOn: number
  listingTitle: string | null
  condition: string | null
  binding: string | null
  detailsUrl: string | null
}

interface Option {
  sellerId: number
  sellerName: string
  storefrontUrl: string | null
  additional: number
  books: Map<number, Cand>
}

interface Subset {
  savings: number
  first: number
  estimated: boolean
  lines: Cand[]
}

interface Bundle {
  option: Option
  subset: Subset
}

function cents(dollars: number): number {
  return Math.round(dollars * 100)
}

function money(amount: number): number {
  return amount / 100
}

function bestSubset(cands: Cand[], additional: number): Subset | null {
  const positive = cands.filter((cand) => cand.addOn > 0)
  if (positive.length === 0) return null
  const thresholds = [...new Set(positive.map((cand) => cand.first))].sort((a, b) => a - b)
  let best: Subset | null = null
  for (const threshold of thresholds) {
    const chosen = positive.filter((cand) => cand.first <= threshold)
    const savings = chosen.reduce((sum, cand) => sum + cand.addOn, 0) - (threshold - additional)
    if (savings <= 0) continue
    const estimated = chosen.some((cand) => cand.first !== threshold)
    if (
      !best
      || savings > best.savings
      || (savings === best.savings && chosen.length > best.lines.length)
    ) {
      best = { savings, first: threshold, estimated, lines: chosen }
    }
  }
  return best
}

function pickListing(rows: SavingsListing[]): SavingsListing {
  return rows.reduce((best, row) => {
    const item = cents(row.priceDollars)
    const ship = cents(row.shippingDollars)
    const bestItem = cents(best.priceDollars)
    const bestShip = cents(best.shippingDollars)
    if (item < bestItem || (item === bestItem && ship < bestShip)) return row
    return best
  })
}

/** Known extra-book cents, or null when the seller cannot be priced. */
function additionalCents(rows: SavingsListing[]): number | null {
  const source = rows[0]?.additionalShippingSource ?? 'UNKNOWN'
  if (source === 'FREE') return 0
  if (source === 'PARSED' || source === 'LIBRARIAN') {
    const amount = rows[0]?.additionalShippingDollars
    return amount == null ? null : cents(amount)
  }
  if (rows.every((row) => cents(row.shippingDollars) === 0)) return 0
  return null
}

function shares(left: Option, right: Option): boolean {
  for (const bookId of left.books.keys()) {
    if (right.books.has(bookId)) return true
  }
  return false
}

function realize(openIds: Set<number>, options: Option[]): { total: number; bundles: Bundle[] } {
  const open = options.filter((option) => openIds.has(option.sellerId))
  const winner = new Map<number, { sellerId: number; addOn: number }>()
  for (const option of open) {
    for (const [bookId, cand] of option.books) {
      const current = winner.get(bookId)
      if (
        !current
        || cand.addOn > current.addOn
        || (cand.addOn === current.addOn && option.sellerId < current.sellerId)
      ) {
        winner.set(bookId, { sellerId: option.sellerId, addOn: cand.addOn })
      }
    }
  }
  const bundles: Bundle[] = []
  let total = 0
  for (const option of open) {
    const assigned = [...option.books.values()].filter(
      (cand) => winner.get(cand.bookId)?.sellerId === option.sellerId,
    )
    const subset = bestSubset(assigned, option.additional)
    if (!subset) continue
    total += subset.savings
    bundles.push({ option, subset })
  }
  return { total, bundles }
}

function components(options: Option[]): Option[][] {
  const parent = new Map<number, number>()
  const find = (id: number): number => {
    const current = parent.get(id)
    if (current == null) {
      parent.set(id, id)
      return id
    }
    if (current === id) return id
    const root = find(current)
    parent.set(id, root)
    return root
  }
  const union = (left: number, right: number) => {
    const a = find(left)
    const b = find(right)
    if (a !== b) parent.set(a, b)
  }
  for (const option of options) {
    const ids = [...option.books.keys()]
    for (let i = 1; i < ids.length; i++) union(ids[0], ids[i])
  }
  const groups = new Map<number, Option[]>()
  for (const option of options) {
    const ids = [...option.books.keys()]
    if (ids.length === 0) continue
    const root = find(ids[0])
    const list = groups.get(root) ?? []
    list.push(option)
    groups.set(root, list)
  }
  return [...groups.values()]
}

/**
 * Basket savings versus buying each book from its cheapest single listing.
 * Yields so a live timer can paint. There is no title cap.
 */
export async function computeShippingSavings(
  input: { books: SavingsBook[]; listings: SavingsListing[] },
  yieldToUi?: () => Promise<void>,
): Promise<SavingsReport> {
  let lastYield = 0
  const maybeYield = async (force = false) => {
    if (!yieldToUi) return
    const now = performance.now()
    if (force || now - lastYield >= 50) {
      await yieldToUi()
      lastYield = performance.now()
    }
  }

  await maybeYield(true)

  const bookById = new Map(input.books.map((book) => [book.id, book]))
  const listings = input.listings.filter((row) => bookById.has(row.bookId))
  const listingsByBook = new Map<number, SavingsListing[]>()
  const listingsBySeller = new Map<number, SavingsListing[]>()
  for (const row of listings) {
    const forBook = listingsByBook.get(row.bookId) ?? []
    forBook.push(row)
    listingsByBook.set(row.bookId, forBook)
    const forSeller = listingsBySeller.get(row.sellerId) ?? []
    forSeller.push(row)
    listingsBySeller.set(row.sellerId, forSeller)
  }

  const baseline = new Map<number, number>()
  const booksWithoutListings: BookWithoutListings[] = []
  for (const book of input.books) {
    const rows = listingsByBook.get(book.id) ?? []
    if (rows.length === 0) {
      booksWithoutListings.push({ bookId: book.id, catalogTitle: book.title })
      continue
    }
    let cheapest = Number.POSITIVE_INFINITY
    for (const row of rows) {
      cheapest = Math.min(cheapest, cents(row.priceDollars) + cents(row.shippingDollars))
    }
    baseline.set(book.id, cheapest)
  }

  const options: Option[] = []
  const shared: SharedSeller[] = []
  for (const [sellerId, rows] of listingsBySeller) {
    const additional = additionalCents(rows)
    const bookIds = [...new Set(rows.map((row) => row.bookId))]
    if (additional == null) {
      if (bookIds.length >= 2) {
        shared.push({
          sellerId,
          sellerName: rows[0]?.sellerName || 'Seller',
          storefrontUrl: rows[0]?.storefrontUrl ?? null,
          note: UNPUBLISHED_RATE_NOTE,
          bookIds,
          catalogTitles: bookIds
            .map((id) => bookById.get(id)?.title ?? '')
            .sort((a, b) => a.localeCompare(b)),
        })
      }
      continue
    }
    const byBook = new Map<number, SavingsListing[]>()
    for (const row of rows) {
      const group = byBook.get(row.bookId) ?? []
      group.push(row)
      byBook.set(row.bookId, group)
    }
    const books = new Map<number, Cand>()
    for (const [bookId, bookRows] of byBook) {
      const base = baseline.get(bookId)
      if (base == null) continue
      const chosen = pickListing(bookRows)
      const item = cents(chosen.priceDollars)
      const first = cents(chosen.shippingDollars)
      books.set(bookId, {
        bookId,
        item,
        first,
        baseline: base,
        addOn: base - item - additional,
        listingTitle: chosen.listingTitle,
        condition: chosen.condition,
        binding: chosen.binding,
        detailsUrl: chosen.detailsUrl,
      })
    }
    if (books.size === 0) continue
    options.push({
      sellerId,
      sellerName: rows[0]?.sellerName || 'Seller',
      storefrontUrl: rows[0]?.storefrontUrl ?? null,
      additional,
      books,
    })
  }

  const open = await solve(options, maybeYield)
  const { bundles } = realize(open, options)
  const bundledBookIds = new Set<number>()
  const sellers: SellerBundle[] = bundles.map(({ option, subset }) => {
    const lines = subset.lines
      .map((cand) => {
        bundledBookIds.add(cand.bookId)
        const book = bookById.get(cand.bookId)
        return {
          bookId: cand.bookId,
          catalogTitle: book?.title ?? '',
          author: book?.author ?? null,
          listingTitle: cand.listingTitle,
          condition: cand.condition,
          binding: cand.binding,
          itemDollars: money(cand.item),
          firstShippingDollars: money(cand.first),
          detailsUrl: cand.detailsUrl,
          baselineDollars: money(cand.baseline),
        }
      })
      .sort((a, b) => a.catalogTitle.localeCompare(b.catalogTitle) || a.bookId - b.bookId)
    const itemSum = subset.lines.reduce((sum, cand) => sum + cand.item, 0)
    const baselineSum = subset.lines.reduce((sum, cand) => sum + cand.baseline, 0)
    const count = subset.lines.length
    const basket = itemSum + subset.first + (count - 1) * option.additional
    return {
      sellerId: option.sellerId,
      sellerName: option.sellerName,
      storefrontUrl: option.storefrontUrl,
      shippingEstimated: subset.estimated,
      firstShippingDollars: money(subset.first),
      additionalShippingDollars: money(option.additional),
      basketDollars: money(basket),
      baselineDollars: money(baselineSum),
      savingsDollars: money(subset.savings),
      lines,
    }
  })
  sellers.sort(
    (a, b) => b.savingsDollars - a.savingsDollars
      || a.sellerName.localeCompare(b.sellerName)
      || a.sellerId - b.sellerId,
  )
  shared.sort((a, b) => a.sellerName.localeCompare(b.sellerName) || a.sellerId - b.sellerId)

  const individual: IndividualBuy[] = input.books
    .filter((book) => baseline.has(book.id) && !bundledBookIds.has(book.id))
    .map((book) => ({
      bookId: book.id,
      catalogTitle: book.title,
      author: book.author,
      baselineDollars: money(baseline.get(book.id) ?? 0),
    }))
    .sort((a, b) => a.catalogTitle.localeCompare(b.catalogTitle) || a.bookId - b.bookId)

  booksWithoutListings.sort((a, b) => a.catalogTitle.localeCompare(b.catalogTitle) || a.bookId - b.bookId)

  return {
    totalSavings: money(sellers.reduce((sum, seller) => sum + cents(seller.savingsDollars), 0)),
    sellers,
    shared,
    individual,
    booksWithoutListings,
  }
}

async function solve(
  options: Option[],
  maybeYield: (force?: boolean) => Promise<void>,
): Promise<Set<number>> {
  const chosen = new Set<number>()
  if (options.length === 0) return chosen
  for (const group of components(options)) {
    await maybeYield()
    const starts = [
      await greedy(group, false, maybeYield),
      await greedy(group, true, maybeYield),
      await dropFromAll(group, maybeYield),
    ]
    let best = starts[0]
    let bestTotal = realize(best, group).total
    for (const start of starts) {
      const improved = await localSearch(start, group, maybeYield)
      const total = realize(improved, group).total
      if (total > bestTotal) {
        best = improved
        bestTotal = total
      }
    }
    for (const sellerId of best) chosen.add(sellerId)
  }
  return chosen
}

async function greedy(
  options: Option[],
  smallFirst: boolean,
  maybeYield: (force?: boolean) => Promise<void>,
): Promise<Set<number>> {
  const open = new Set<number>()
  const bySize = [...options].sort((a, b) => {
    if (a.books.size !== b.books.size) return a.books.size - b.books.size
    return sellerRank(b) - sellerRank(a) || a.sellerId - b.sellerId
  })
  let guard = 0
  while (guard++ <= options.length) {
    await maybeYield()
    const before = realize(open, options).total
    let bestId = -1
    let bestGain = 0
    const scan = smallFirst
      ? bySize
      : [...options].sort((a, b) => sellerRank(b) - sellerRank(a) || a.sellerId - b.sellerId)
    for (const option of scan) {
      await maybeYield()
      if (open.has(option.sellerId)) continue
      open.add(option.sellerId)
      const gain = realize(open, options).total - before
      open.delete(option.sellerId)
      if (smallFirst) {
        if (gain > 0) {
          bestId = option.sellerId
          break
        }
      } else if (gain > bestGain) {
        bestGain = gain
        bestId = option.sellerId
      }
    }
    if (bestId < 0) break
    open.add(bestId)
  }
  return open
}

async function dropFromAll(
  options: Option[],
  maybeYield: (force?: boolean) => Promise<void>,
): Promise<Set<number>> {
  const open = new Set(options.map((option) => option.sellerId))
  let changed = true
  let guard = 0
  while (changed && guard++ <= options.length) {
    changed = false
    await maybeYield()
    for (const sellerId of [...open]) {
      await maybeYield()
      const before = realize(open, options).total
      open.delete(sellerId)
      if (realize(open, options).total > before) changed = true
      else open.add(sellerId)
    }
  }
  return open
}

async function localSearch(
  initial: Set<number>,
  options: Option[],
  maybeYield: (force?: boolean) => Promise<void>,
): Promise<Set<number>> {
  const open = new Set(initial)
  let changed = true
  let guard = 0
  while (changed && guard++ < 24) {
    changed = false
    await maybeYield()
    for (const sellerId of [...open]) {
      await maybeYield()
      const before = realize(open, options).total
      open.delete(sellerId)
      if (realize(open, options).total > before) changed = true
      else open.add(sellerId)
    }
    for (const option of options) {
      await maybeYield()
      if (open.has(option.sellerId)) continue
      const before = realize(open, options).total
      open.add(option.sellerId)
      if (realize(open, options).total > before) changed = true
      else open.delete(option.sellerId)
    }
    const closed = options.filter((option) => !open.has(option.sellerId))
    for (const sellerId of [...open]) {
      await maybeYield()
      let swapped = false
      for (const option of closed) {
        await maybeYield()
        const before = realize(open, options).total
        open.delete(sellerId)
        open.add(option.sellerId)
        if (realize(open, options).total > before) {
          changed = true
          swapped = true
          break
        }
        open.delete(option.sellerId)
        open.add(sellerId)
      }
      if (swapped) break
    }
    for (const sellerId of [...open]) {
      await maybeYield()
      const seller = options.find((option) => option.sellerId === sellerId)
      if (!seller) continue
      const partners = options.filter((option) => !open.has(option.sellerId) && shares(seller, option))
      if (partners.length === 0) continue
      const before = realize(open, options).total
      open.delete(sellerId)
      for (const partner of partners) open.add(partner.sellerId)
      if (realize(open, options).total > before) {
        changed = true
        continue
      }
      for (const partner of partners) open.delete(partner.sellerId)
      open.add(sellerId)
    }
  }
  return open
}

function sellerRank(option: Option): number {
  return bestSubset([...option.books.values()], option.additional)?.savings ?? 0
}
