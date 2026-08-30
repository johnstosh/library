// (c) Copyright 2025 by Muczynski

/** Build /loans/new query so the checkout form prefills this catalog title and patron. */
export function loansNewPathFromBook(
  book: {
    id: number
    title?: string | null
    author?: string | null
    locNumber?: string | null
  },
  patronUsername?: string | null,
): string {
  const params = new URLSearchParams()
  params.set('bookId', String(book.id))
  if (book.title) params.set('title', book.title)
  if (book.author) params.set('author', book.author)
  if (book.locNumber) params.set('locNumber', book.locNumber)
  if (patronUsername) params.set('borrower', patronUsername)
  return `/loans/new?${params.toString()}`
}
