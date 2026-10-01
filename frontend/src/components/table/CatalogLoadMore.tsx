// (c) Copyright 2025 by Muczynski
import { useEffect, useRef } from 'react'
import { Button } from '@/components/ui/Button'

interface CatalogLoadMoreProps {
  hasMore: boolean
  isLoadingMore: boolean
  onLoadMore: () => void
  'data-test'?: string
}

/**
 * Loads the next page when the user reaches the bottom of the window,
 * and offers the same action as a button. Scroll events do not fire in tests.
 */
export function CatalogLoadMore({
  hasMore,
  isLoadingMore,
  onLoadMore,
  'data-test': dataTest = 'catalog-load-more',
}: CatalogLoadMoreProps) {
  const onLoadMoreRef = useRef(onLoadMore)
  const hasMoreRef = useRef(hasMore)
  const loadingRef = useRef(isLoadingMore)
  onLoadMoreRef.current = onLoadMore
  hasMoreRef.current = hasMore
  loadingRef.current = isLoadingMore

  useEffect(() => {
    const onScroll = () => {
      if (!hasMoreRef.current || loadingRef.current) return
      const remaining = document.documentElement.scrollHeight - (window.innerHeight + window.scrollY)
      if (remaining <= 400) onLoadMoreRef.current()
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
