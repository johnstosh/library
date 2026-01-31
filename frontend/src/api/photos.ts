// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
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

export interface PhotoZipImportItemDto {
  filename: string
  status: 'SUCCESS' | 'FAILURE' | 'SKIPPED'
  entityType?: string
  entityName?: string
  entityId?: number
  photoId?: number
  errorMessage?: string
}

export interface PhotoZipImportResultDto {
  totalFiles: number
  successCount: number
  failureCount: number
  skippedCount: number
  items: PhotoZipImportItemDto[]
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

// Chunked upload progress state
export interface ChunkUploadProgress {
  mbSent: number
  totalMb: number
  percentage: number
  imagesProcessed: number
  imagesSuccess: number
  imagesFailure: number
  imagesSkipped: number
  isUploading: boolean
  currentItems: PhotoZipImportItemDto[]
}

interface ChunkUploadResultDto {
  uploadId: string
  chunkIndex: number
  processedPhotos: PhotoZipImportItemDto[]
  totalProcessedSoFar: number
  totalSuccessSoFar: number
  totalFailureSoFar: number
  totalSkippedSoFar: number
  complete: boolean
  finalResult?: PhotoZipImportResultDto
  errorMessage?: string
}

// Import photos from ZIP file using chunked upload (supports 2GB+ files with progress)
export function useImportPhotosFromZipChunked() {
  const queryClient = useQueryClient()
  const [progress, setProgress] = useState<ChunkUploadProgress>({
    mbSent: 0,
    totalMb: 0,
    percentage: 0,
    imagesProcessed: 0,
    imagesSuccess: 0,
    imagesFailure: 0,
    imagesSkipped: 0,
    isUploading: false,
    currentItems: [],
  })

  const mutation = useMutation({
    mutationFn: async (file: File): Promise<PhotoZipImportResultDto> => {
      const CHUNK_SIZE = 10 * 1024 * 1024 // 10MB
      const totalSize = file.size
      const totalMb = totalSize / (1024 * 1024)
      const uploadId = crypto.randomUUID()
      const allItems: PhotoZipImportItemDto[] = []

      setProgress({
        mbSent: 0,
        totalMb,
        percentage: 0,
        imagesProcessed: 0,
        imagesSuccess: 0,
        imagesFailure: 0,
        imagesSkipped: 0,
        isUploading: true,
        currentItems: [],
      })

      let chunkIndex = 0
      let finalResult: PhotoZipImportResultDto | undefined

      const cleanupUpload = () => {
        fetch(`/api/photos/import-zip-chunk/${uploadId}`, {
          method: 'DELETE',
          credentials: 'include',
        }).catch(() => {}) // best-effort cleanup
      }

      try {
        for (let offset = 0; offset < totalSize; offset += CHUNK_SIZE) {
          const end = Math.min(offset + CHUNK_SIZE, totalSize)
          const chunk = file.slice(offset, end)
          const isLastChunk = end >= totalSize
          const chunkBytes = await chunk.arrayBuffer()

          const response = await fetch('/api/photos/import-zip-chunk', {
            method: 'PUT',
            body: chunkBytes,
            headers: {
              'Content-Type': 'application/octet-stream',
              'X-Upload-Id': uploadId,
              'X-Chunk-Index': String(chunkIndex),
              'X-Is-Last-Chunk': String(isLastChunk),
            },
            credentials: 'include',
          })

          if (!response.ok) {
            const error = await response.json().catch(() => null)
            throw new Error(error?.message || `Server returned ${response.status}`)
          }

          const result: ChunkUploadResultDto = await response.json()
          allItems.push(...result.processedPhotos)

          const mbSent = end / (1024 * 1024)
          setProgress({
            mbSent,
            totalMb,
            percentage: (end / totalSize) * 100,
            imagesProcessed: result.totalProcessedSoFar,
            imagesSuccess: result.totalSuccessSoFar,
            imagesFailure: result.totalFailureSoFar,
            imagesSkipped: result.totalSkippedSoFar,
            isUploading: !result.complete,
            currentItems: allItems,
          })

          if (result.complete && result.finalResult) {
            finalResult = result.finalResult
          }

          if (result.errorMessage) {
            // Server returned stats with an error — build result from what we have, then throw
            if (!finalResult && result.finalResult) {
              finalResult = result.finalResult
            }
            const statsMsg = `${result.totalSuccessSoFar} succeeded, ${result.totalFailureSoFar} failed, ${result.totalSkippedSoFar} skipped`
            throw new Error(`Import failed after processing ${result.totalProcessedSoFar} photos (${statsMsg}): ${result.errorMessage}`)
          }

          chunkIndex++
        }

        return finalResult || {
          totalFiles: allItems.length,
          successCount: allItems.filter(i => i.status === 'SUCCESS').length,
          failureCount: allItems.filter(i => i.status === 'FAILURE').length,
          skippedCount: allItems.filter(i => i.status === 'SKIPPED').length,
          items: allItems,
        }
      } finally {
        cleanupUpload()
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.books.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.authors.all })
    },
    onSettled: () => {
      setProgress(prev => ({ ...prev, isUploading: false }))
    },
  })

  return { ...mutation, progress }
}

// Helper functions to get photo URLs
export function getPhotoUrl(photoId: number): string {
  return `/api/photos/${photoId}/image`
}

// Alias for getPhotoUrl for use in PhotoViewPage
export function getImageUrl(photoId: number): string {
  return `/api/photos/${photoId}/image`
}

export function getThumbnailUrl(photoId: number, width: number): string {
  return `/api/photos/${photoId}/thumbnail?width=${width}`
}

