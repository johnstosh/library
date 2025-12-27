// (c) Copyright 2025 by Muczynski
import { useQuery } from '@tanstack/react-query'
import { api } from './client'

export interface BookLocStatusDto {
  id: number
  title: string
  authorName: string
  currentLocNumber?: string
  hasLocNumber: boolean
  publicationYear?: number
  firstPhotoId?: number
  firstPhotoChecksum?: string
  dateAdded?: string
}

export function useBooksForLabels(filter: 'most-recent' | 'all' = 'most-recent') {
  return useQuery({
    queryKey: ['labels', 'books', filter],
    queryFn: () => {
      const endpoint = filter === 'all' ? '/labels/books/all' : '/labels/books'
      return api.get<BookLocStatusDto[]>(endpoint)
    },
    staleTime: 1000 * 60 * 2, // 2 minutes
  })
}

export async function generateLabelsPdf(bookIds: number[]): Promise<Blob> {
  const params = bookIds.map((id) => `bookIds=${id}`).join('&')
  const response = await fetch(`/api/labels/generate?${params}`, {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Failed to generate labels PDF')
  }

  return response.blob()
}
