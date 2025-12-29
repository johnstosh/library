// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'

export interface PhotoDto {
  id: number
  contentType: string
  caption?: string
  bookId?: number
  authorId?: number
  imageChecksum: string
  dateTaken?: string // ISO-8601 datetime string
}

// Get photos for a book
export function useBookPhotos(bookId: number) {
  return useQuery({
    queryKey: queryKeys.photos.book(bookId),
    queryFn: () => api.get<PhotoDto[]>(`/books/${bookId}/photos`, { requireAuth: false }),
    enabled: !!bookId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  })
}

// Get photos for an author
export function useAuthorPhotos(authorId: number) {
  return useQuery({
    queryKey: queryKeys.photos.author(authorId),
    queryFn: () => api.get<PhotoDto[]>(`/authors/${authorId}/photos`, { requireAuth: false }),
    enabled: !!authorId,
    staleTime: 1000 * 60 * 5,
  })
}

// Upload photo for a book
export function useUploadBookPhoto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ bookId, file }: { bookId: number; file: File }) => {
      const formData = new FormData()
      formData.append('file', file)

      const response = await fetch(`/api/books/${bookId}/photos`, {
        method: 'POST',
        body: formData,
        credentials: 'include',
      })

      if (!response.ok) {
        throw new Error('Failed to upload photo')
      }

      return response.json()
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.book(variables.bookId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.detail(variables.bookId) })
    },
  })
}

// Upload photo for an author
export function useUploadAuthorPhoto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ authorId, file }: { authorId: number; file: File }) => {
      const formData = new FormData()
      formData.append('file', file)

      const response = await fetch(`/api/authors/${authorId}/photos`, {
        method: 'POST',
        body: formData,
        credentials: 'include',
      })

      if (!response.ok) {
        throw new Error('Failed to upload photo')
      }

      return response.json()
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.author(variables.authorId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.detail(variables.authorId) })
    },
  })
}

// Delete photo
export function useDeletePhoto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (photoId: number) => api.delete(`/photos/${photoId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
  })
}

// Restore photo
export function useRestorePhoto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (photoId: number) => api.post(`/photos/${photoId}/restore`, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all })
    },
  })
}

// Rotate photo clockwise
export function useRotatePhotoCW() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ bookId, photoId, authorId }: { bookId?: number; photoId: number; authorId?: number }) => {
      if (bookId) {
        return api.put(`/books/${bookId}/photos/${photoId}/rotate-cw`, {})
      } else if (authorId) {
        return api.put(`/authors/${authorId}/photos/${photoId}/rotate-cw`, {})
      }
      throw new Error('Either bookId or authorId must be provided')
    },
    onSuccess: (_, variables) => {
      if (variables.bookId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.photos.book(variables.bookId) })
      } else if (variables.authorId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.photos.author(variables.authorId) })
      }
    },
  })
}

// Rotate photo counter-clockwise
export function useRotatePhotoCCW() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ bookId, photoId, authorId }: { bookId?: number; photoId: number; authorId?: number }) => {
      if (bookId) {
        return api.put(`/books/${bookId}/photos/${photoId}/rotate-ccw`, {})
      } else if (authorId) {
        return api.put(`/authors/${authorId}/photos/${photoId}/rotate-ccw`, {})
      }
      throw new Error('Either bookId or authorId must be provided')
    },
    onSuccess: (_, variables) => {
      if (variables.bookId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.photos.book(variables.bookId) })
      } else if (variables.authorId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.photos.author(variables.authorId) })
      }
    },
  })
}

// Move photo left
export function useMovePhotoLeft() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ bookId, photoId, authorId }: { bookId?: number; photoId: number; authorId?: number }) => {
      if (bookId) {
        return api.put(`/books/${bookId}/photos/${photoId}/move-left`, {})
      } else if (authorId) {
        return api.put(`/authors/${authorId}/photos/${photoId}/move-left`, {})
      }
      throw new Error('Either bookId or authorId must be provided')
    },
    onSuccess: (_, variables) => {
      if (variables.bookId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.photos.book(variables.bookId) })
      } else if (variables.authorId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.photos.author(variables.authorId) })
      }
    },
  })
}

// Move photo right
export function useMovePhotoRight() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ bookId, photoId, authorId }: { bookId?: number; photoId: number; authorId?: number }) => {
      if (bookId) {
        return api.put(`/books/${bookId}/photos/${photoId}/move-right`, {})
      } else if (authorId) {
        return api.put(`/authors/${authorId}/photos/${photoId}/move-right`, {})
      }
      throw new Error('Either bookId or authorId must be provided')
    },
    onSuccess: (_, variables) => {
      if (variables.bookId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.photos.book(variables.bookId) })
      } else if (variables.authorId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.photos.author(variables.authorId) })
      }
    },
  })
}

// Crop photo
export function useCropPhoto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ photoId, file }: { photoId: number; file: File }) => {
      const formData = new FormData()
      formData.append('file', file)

      const response = await fetch(`/api/photos/${photoId}/crop`, {
        method: 'PUT',
        body: formData,
        credentials: 'include',
      })

      if (!response.ok) {
        throw new Error('Failed to crop photo')
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all })
    },
  })
}

// Helper functions to get photo URLs
export function getPhotoUrl(photoId: number): string {
  return `/api/photos/${photoId}/image`
}

export function getThumbnailUrl(photoId: number, width: number): string {
  return `/api/photos/${photoId}/thumbnail?width=${width}`
}
