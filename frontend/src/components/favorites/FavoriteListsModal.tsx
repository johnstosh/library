// (c) Copyright 2025 by Muczynski
import { useEffect, useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Checkbox } from '@/components/ui/Checkbox'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { useIsLibrarian } from '@/stores/authStore'
import {
  itemListsFromSummary,
  listNameToTestId,
  useFavoriteSummary,
  useReplaceFavoriteItem,
  type FavoriteItemType,
} from '@/api/favorites'

interface FavoriteListsModalProps {
  isOpen: boolean
  onClose: () => void
  itemType: FavoriteItemType
  itemId: number
}

export function FavoriteListsModal({ isOpen, onClose, itemType, itemId }: FavoriteListsModalProps) {
  const isLibrarian = useIsLibrarian()
  const { data: summary } = useFavoriteSummary()
  const replaceItem = useReplaceFavoriteItem()
  const [newListName, setNewListName] = useState('')
  const [override, setOverride] = useState<{ available: string[]; selected: string[] } | null>(null)

  useEffect(() => {
    if (!isOpen) {
      setOverride(null)
      setNewListName('')
    }
  }, [isOpen])

  const derived = itemListsFromSummary(summary, itemType, itemId, isLibrarian)
  const available = override?.available ?? derived.availableLists
  const selected = override?.selected ?? derived.selectedLists

  const persist = (nextSelected: string[], nextAvailable = available) => {
    setOverride({ available: nextAvailable, selected: nextSelected })
    replaceItem.mutate({ itemType, itemId, listNames: nextSelected })
  }

  const toggleList = (listName: string) => {
    const next = selected.includes(listName)
      ? selected.filter((name) => name !== listName)
      : [...selected, listName]
    persist(next)
  }

  const addCustomList = () => {
    const name = newListName.trim()
    if (!name) return
    const existing = available.find((list) => list.toLowerCase() === name.toLowerCase())
    const canonical = existing ?? name
    const nextAvailable = existing ? available : [...available, canonical]
    const nextSelected = selected.some((list) => list.toLowerCase() === canonical.toLowerCase())
      ? selected
      : [...selected, canonical]
    setNewListName('')
    persist(nextSelected, nextAvailable)
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Favorite lists" size="sm">
      <div data-test="favorite-modal" className="space-y-4">
        <div className="space-y-2" data-test="favorite-list-checkboxes">
          {available.map((listName) => (
            <Checkbox
              key={listName}
              label={listName}
              checked={selected.includes(listName)}
              onChange={() => toggleList(listName)}
              data-test={`favorite-list-${listNameToTestId(listName)}`}
            />
          ))}
        </div>

        <div className="flex gap-2 items-start pt-2 border-t border-gray-200">
          <Input
            value={newListName}
            onChange={(e) => setNewListName(e.target.value)}
            placeholder="New list name"
            data-test="favorite-add-list-name"
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault()
                addCustomList()
              }
            }}
          />
          <Button
            type="button"
            variant="outline"
            onClick={addCustomList}
            data-test="favorite-add-list"
          >
            Add Favorite List
          </Button>
        </div>
      </div>
    </Modal>
  )
}
