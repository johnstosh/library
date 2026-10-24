// (c) Copyright 2025 by Muczynski
import { useEffect, useLayoutEffect, useRef } from 'react'
import { Button } from '@/components/ui/Button'

interface CatalogLoadMoreProps {
  hasMore: boolean
  isLoadingMore: boolean
  onLoadMore: () => void
  /** Rows currently shown. A drop (a new search) clears the scroll gate. */
  shownCount?: number
  'data-test'?: string
}

/**
 * Loads the next page when the user reaches the bottom of the window,
 * and offers the same action as a button.
 * One scroll can start only one page. The next automatic page waits until
 * isLoadingMore finishes and the user scrolls farther down, so a short page
 * cannot chain the whole list before those rows are on screen.
 */
export function CatalogLoadMore({
  hasMore,
  isLoadingMore,
  onLoadMore,
  shownCount = 0,
  'data-test': dataTest = 'catalog-load-more',
}: CatalogLoadMoreProps) {
  const onLoadMoreRef = useRef(onLoadMore)
  const hasMoreRef = useRef(hasMore)
  const loadingRef = useRef(isLoadingMore)
  const lockRef = useRef(false)
  const minScrollYRef = useRef(0)
  const shownCountRef = useRef(shownCount)
  onLoadMoreRef.current = onLoadMore
  hasMoreRef.current = hasMore
  loadingRef.current = isLoadingMore

  useLayoutEffect(() => {
    if (shownCount < shownCountRef.current) {
      minScrollYRef.current = 0
      lockRef.current = false
    }
    shownCountRef.current = shownCount
    if (isLoadingMore) {
      lockRef.current = true
      return
    }
    if (!lockRef.current) return
    lockRef.current = false
    minScrollYRef.current = window.scrollY + 24
  })

  useEffect(() => {
    const onScroll = () => {
      if (lockRef.current || loadingRef.current || !hasMoreRef.current) return
      if (window.scrollY < minScrollYRef.current) return
      const remaining = document.documentElement.scrollHeight - (window.innerHeight + window.scrollY)
      if (remaining > 400) return
      lockRef.current = true
      onLoadMoreRef.current()
      // A refused load does not re-render. Drop the lock on the next turn
      // when the parent is still idle so a later scroll can try again.
      window.setTimeout(() => {
        if (!loadingRef.current) lockRef.current = false
      }, 0)
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  if (!hasMore && !isLoadingMore) return null

  return (
    <div className="px-4 py-3 border-t border-gray-200 flex justify-center">
      <Button
        type="button"
        variant="outline"
        onClick={onLoadMore}
        disabled={!hasMore}
        isLoading={isLoadingMore}
        data-test={dataTest}
      >
        Load more
      </Button>
    </div>
  )
}
