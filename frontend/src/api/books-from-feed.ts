// (c) Copyright 2025 by Muczynski
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client'

export interface SavedBookDto {
  id: number
  title: string
  author?: string
  library?: string
  photoCount: number
  needsProcessing: boolean
  // Additional fields for Books page table
  locNumber?: string
  status?: string
  grokipediaUrl?: string
}

export interface PickerSessionDto {
  id: string
  pickerUri: string
}

export interface PickerSessionStatusDto {
  mediaItemsSet: boolean
  pollingInterval?: number
}

export interface PhotoFromPickerDto {
  id: string
  baseUrl: string
  filename: string
  mimeType: string
  mediaMetadata?: {
    width?: string
    height?: string
    creationTime?: string
  }
}

export interface SaveFromPickerResultDto {
  savedCount: number
  skippedCount: number
  totalPhotos: number
  message: string
  error?: string
}

export interface ProcessResultDto {
  success: boolean
  processedCount: number
  failedCount: number
  totalBooks: number
  message?: string
  error?: string
}

export interface SingleProcessResultDto {
  success: boolean
  bookId: number
  title?: string
  author?: string
  message: string
  error?: string
}

// Get saved books that need processing
export function useSavedBooks() {
  return useQuery({
    queryKey: ['books-from-feed', 'saved-books'],
    queryFn: () => api.get<SavedBookDto[]>('/books-from-feed/saved-books'),
    staleTime: 1000 * 30, // 30 seconds - refresh frequently
  })
}

// Create a Google Photos Picker session
export function useCreatePickerSession() {
  return useMutation({
    mutationFn: () => api.post<PickerSessionDto>('/books-from-feed/picker-session', {}),
  })
}

// Poll picker session status
export function usePickerSessionStatus(sessionId: string | null) {
  return useQuery({
    queryKey: ['books-from-feed', 'picker-session', sessionId],
    queryFn: () => api.get<PickerSessionStatusDto>(`/books-from-feed/picker-session/${sessionId}`),
    enabled: !!sessionId,
    refetchInterval: (query) => {
      // Keep polling if mediaItemsSet is false
      const data = query.state.data
      return data?.mediaItemsSet ? false : 2000 // Poll every 2 seconds
    },
  })
}

// Get media items from picker session
// Note: mediaItemsReady should be true only after the user has finished selecting photos
// (i.e., when session status shows mediaItemsSet === true)
export function usePickerMediaItems(sessionId: string | null, mediaItemsReady: boolean = false) {
  return useQuery({
    queryKey: ['books-from-feed', 'picker-media', sessionId],
    queryFn: () =>
      api.get<{ mediaItems: PhotoFromPickerDto[]; count: number }>(
        `/books-from-feed/picker-session/${sessionId}/media-items`
      ),
    enabled: !!sessionId && mediaItemsReady,
  })
}

// Save photos from picker to database
export function useSavePhotosFromPicker() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (photos: PhotoFromPickerDto[]) =>
      api.post<SaveFromPickerResultDto>('/books-from-feed/save-from-picker', { photos }),
    onSuccess: () => {
      // Invalidate saved books query to refresh the list
      queryClient.invalidateQueries({ queryKey: ['books-from-feed', 'saved-books'] })
      queryClient.invalidateQueries({ queryKey: ['books'] })
    },
  })
}

// Process a single book (standalone function for use in iterative processing)
export function processSingleBookApi(bookId: number) {
  return api.post<SingleProcessResultDto>(`/books-from-feed/process-single/${bookId}`, {})
}

// Process a single saved book
export function useProcessSingleBook() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (bookId: number) =>
      api.post<SingleProcessResultDto>(`/books-from-feed/process-single/${bookId}`, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['books-from-feed', 'saved-books'] })
      queryClient.invalidateQueries({ queryKey: ['books'] })
    },
  })
}
