// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { PiStar, PiStarFill } from 'react-icons/pi'
import { clsx } from 'clsx'
import { useIsAuthenticated } from '@/stores/authStore'
import { useFavoriteSummary, type FavoriteItemType } from '@/api/favorites'
import { FavoriteListsModal } from './FavoriteListsModal'

interface FavoriteStarProps {
  itemType: FavoriteItemType
  itemId: number
  className?: string
}

export function FavoriteStar({ itemType, itemId, className }: FavoriteStarProps) {
  const isAuthenticated = useIsAuthenticated()
  const { data: summary } = useFavoriteSummary()
  const [open, setOpen] = useState(false)

  if (!isAuthenticated || itemId <= 0) {
    return null
  }

  const ids = itemType === 'BOOK' ? summary?.favoriteBookIds : summary?.favoriteAuthorIds
  const isFavorite = Boolean(ids?.includes(itemId))
  const testId = itemType === 'BOOK' ? `favorite-star-book-${itemId}` : `favorite-star-author-${itemId}`

  return (
    <>
      <button
        type="button"
        className={clsx(
          'inline-flex items-center justify-center rounded-md p-1.5',
          'focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-primary-500',
          className,
        )}
        aria-label={isFavorite ? 'Edit favorite lists' : 'Add to favorites'}
        aria-pressed={isFavorite}
        data-test={testId}
        onClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
          setOpen(true)
        }}
      >
        {isFavorite ? (
          <PiStarFill className="w-6 h-6 text-red-600" data-test={`${testId}-filled`} />
        ) : (
          <PiStar className="w-5 h-5 text-black" data-test={`${testId}-outline`} />
        )}
      </button>
      <FavoriteListsModal
        isOpen={open}
        onClose={() => setOpen(false)}
        itemType={itemType}
        itemId={itemId}
      />
    </>
  )
}
