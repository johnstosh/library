// (c) Copyright 2025 by Muczynski
import React, { useMemo, useRef } from 'react'
import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'

export interface ImportExportStats {
  branches: number
  authors: number
  books: number
  users: number
  loans: number
}

// Import error for per-entity error reporting
export interface ImportErrorDto {
  entityType: string
  entityName: string
  errorMessage: string
}

// Import response from backend
export interface ImportResponseDto {
  success: boolean
  message: string
  counts?: {
    branches: number
    authors: number
    users: number
    books: number
    loans: number
    photos: number
  }
  errors?: ImportErrorDto[]
}

// Database statistics from the backend (total counts from database)
export interface DatabaseStatsDto {
  branchCount: number
  bookCount: number
  authorCount: number
  userCount: number
  loanCount: number
}

// Label count from backend
export interface LabelCountDto {
  label: string
  count: number
}

// Named book-count statistics for the Data Management availability section
export interface BookAvailabilityStatsDto {
  electronicResource: number
  hasCallNumber: number
  hasFreeOnlineText: number
  hasFreeOnlineAudio: number
  withdrawn: number
  availableAtYdl: number
  ydlPaper: number
  ydlEbook: number
  ydlAudio: number
  availableAtEmu: number
  emuPaper: number
  emuEbook: number
  emuAudio: number
}

// Photo Export Types
export interface PhotoExportStatsDto {
  total: number
  exported: number
  imported: number
  pendingExport: number
  pendingImport: number
  failed: number
  inProgress: number
  completed: number
  pending: number
  albumName?: string
  albumId?: string
}

export interface PhotoSummaryDto {
  id: number
  lastModified: string
}

export interface PhotoExportInfoDto {
  id: number
  lastModified: string
  caption?: string
  exportStatus: string
  exportedAt?: string
  permanentId?: string
  exportErrorMessage?: string
  contentType?: string
  hasImage: boolean
  checksum?: string
  bookTitle?: string
  bookId?: number
  bookLocNumber?: string
  bookDateAdded?: string
  bookAuthorId?: number
  bookAuthorName?: string
  authorName?: string
  authorId?: number
}

export interface PhotoExportResponseDto {
  message: string
  photoId?: number
  stats?: PhotoExportStatsDto
}

export interface PhotoVerifyResultDto {
  valid: boolean
  message: string
  filename?: string
}

export interface PhotoZipPartDto {
  partNumber: number
  totalParts: number
  rangeLabel: string
  photoCount: number
  estimatedMb: number
  startKey: string
  endKey: string
}

// Export JSON data
export async function exportJsonData(): Promise<Blob> {
  const response = await fetch('/api/import/json', {
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error('Failed to export data')
  }

  const data = await response.json()
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: 'application/json',
  })

  return blob
}

// Import JSON data
export function useImportJsonData() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (file: File) => {
      const text = await file.text()
      let data
      try {
        data = JSON.parse(text)
      } catch (parseError) {
        console.error('Failed to parse import file as JSON:', parseError)
        throw new Error('Invalid JSON file format. Please check the file and try again.')
      }
      console.log('Importing data:', {
        branches: data.libraries?.length || 0,
        authors: data.authors?.length || 0,
        users: data.users?.length || 0,
        books: data.books?.length || 0,
        loans: data.loans?.length || 0,
        photos: data.photos?.length || 0,
      })
      const response = await api.post<ImportResponseDto>('/import/json', data)
      console.log('Import response:', response)
      if (!response.success) {
        throw new Error(response.message || 'Import failed')
      }
      return response
    },
    onSuccess: () => {
      // Invalidate all queries to refresh data after import
      queryClient.invalidateQueries({ queryKey: ['database-stats'] })
      queryClient.invalidateQueries({ queryKey: ['availability-stats'] })
      queryClient.invalidateQueries({ queryKey: ['books'] })
      queryClient.invalidateQueries({ queryKey: ['authors'] })
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['loans'] })
      queryClient.invalidateQueries({ queryKey: ['branches'] })
    },
  })
}

// Get database statistics (total counts from database)
export function useDatabaseStats() {
  return useQuery({
    queryKey: ['database-stats'],
    queryFn: () => api.get<DatabaseStatsDto>('/import/stats'),
  })
}

// Get label counts (books per label)
export function useLabelCounts() {
  return useQuery({
    queryKey: ['label-counts'],
    queryFn: () => api.get<LabelCountDto[]>('/import/label-counts'),
  })
}

// Get book availability counts (electronic, call number, withdrawn, YDL, EMU)
export function useAvailabilityStats() {
  return useQuery({
    queryKey: ['availability-stats'],
    queryFn: () => api.get<BookAvailabilityStatsDto>('/import/availability-stats'),
  })
}

// Export photos as ZIP
export async function exportPhotos(): Promise<Blob> {
  const response = await fetch('/api/photo-export', {
    credentials: 'include',
  })

  if (!response.ok) {
    // Try to get error message from response body
    const contentType = response.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      const errorData = await response.json()
      throw new Error(errorData.message || errorData.error || 'Failed to export photos')
    }
    // Handle common HTTP errors
    if (response.status === 401) {
      throw new Error('Authentication required. Please log in.')
    }
    if (response.status === 403) {
      throw new Error('Permission denied. Librarian access required.')
    }
    throw new Error(`Failed to export photos (HTTP ${response.status})`)
  }

  return response.blob()
}

// Photo Export Status Hooks

// Get photo export statistics
export function usePhotoExportStats() {
  return useQuery({
    queryKey: queryKeys.photos.exportStats(),
    queryFn: () => api.get<PhotoExportStatsDto>('/photo-export/stats'),
  })
}

// Get all photos with export info using lastModified summaries → by-ids caching.
export function usePhotoExportList() {
  const queryClient = useQueryClient()

  const {
    data: summaries,
    isLoading: summariesLoading,
    isFetching: summariesFetching,
    error: summariesError,
    refetch,
  } = useQuery({
    queryKey: queryKeys.photos.summaries(),
    queryFn: () => api.get<PhotoSummaryDto[]>('/photo-export/summaries'),
    staleTime: 30 * 1000,
    refetchOnMount: true,
    placeholderData: keepPreviousData,
  })

  const photosToFetch = useMemo(() => {
    if (!summaries) return []

    return summaries
      .filter((summary) => {
        const cached = queryClient.getQueryData<PhotoExportInfoDto>(queryKeys.photos.detail(summary.id))
        return !cached || cached.lastModified !== summary.lastModified
      })
      .map((s) => s.id)
  }, [summaries, queryClient])

  const {
    data: fetchedPhotos,
    isLoading: fetchingPhotos,
    isFetching: byIdsFetching,
    error: byIdsError,
  } = useQuery({
    queryKey: queryKeys.photos.byIds(photosToFetch),
    queryFn: async () => {
      if (photosToFetch.length > 0) {
        return api.post<PhotoExportInfoDto[]>('/photo-export/by-ids', photosToFetch)
      }
      return []
    },
    enabled: summaries !== undefined && photosToFetch.length > 0,
    placeholderData: keepPreviousData,
  })

  React.useEffect(() => {
    fetchedPhotos?.forEach((photo) => {
      queryClient.setQueryData(queryKeys.photos.detail(photo.id), photo)
    })
  }, [fetchedPhotos, queryClient])

  const allPhotos = useMemo(() => {
    if (!summaries) return []

    const fetchedPhotosMap = new Map<number, PhotoExportInfoDto>()
    fetchedPhotos?.forEach((photo) => {
      fetchedPhotosMap.set(photo.id, photo)
    })

    const photos = summaries
      .map((summary) => {
        const fetched = fetchedPhotosMap.get(summary.id)
        if (fetched) return fetched
        return queryClient.getQueryData<PhotoExportInfoDto>(queryKeys.photos.detail(summary.id))
      })
      .filter((photo): photo is PhotoExportInfoDto => photo !== undefined)

    return photos.sort((a, b) => {
      const dateA = a.bookDateAdded ? new Date(a.bookDateAdded).getTime() : 0
      const dateB = b.bookDateAdded ? new Date(b.bookDateAdded).getTime() : 0
      if (dateB !== dateA) return dateB - dateA
      return a.id - b.id
    })
  }, [summaries, queryClient, fetchedPhotos])

  const previousPhotosRef = useRef<PhotoExportInfoDto[]>([])
  React.useEffect(() => {
    if (allPhotos.length > 0) {
      previousPhotosRef.current = allPhotos
    }
  }, [allPhotos])

  const stablePhotos = allPhotos.length > 0 ? allPhotos : previousPhotosRef.current
  const isFetching = summariesFetching || byIdsFetching

  return {
    data: stablePhotos,
    isLoading: stablePhotos.length === 0 && (summariesLoading || fetchingPhotos),
    isFetching,
    error: summariesError || byIdsError,
    refetch,
  }
}

// Compute how the photo collection splits into ZIP parts.
// staleTime: Infinity so it never re-fetches automatically once loaded;
// the page auto-triggers this on mount (strictly once per session).
export function usePhotoZipParts() {
  return useQuery({
    queryKey: ['photo-zip-parts'],
    queryFn: () => api.get<PhotoZipPartDto[]>('/photo-export/zip-parts'),
    staleTime: Infinity,
  })
}

// Export single photo to Google Photos
export function useExportSinglePhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.post<PhotoExportInfoDto>(`/photo-export/export/${photoId}`)
    },
    onSuccess: (updatedPhoto) => {
      queryClient.setQueryData(queryKeys.photos.detail(updatedPhoto.id), updatedPhoto)
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.exportStats() })
    },
  })
}

// Import single photo from Google Photos
export function useImportSinglePhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.post<PhotoExportInfoDto>(`/photo-export/import/${photoId}`)
    },
    onSuccess: (updatedPhoto) => {
      queryClient.setQueryData(queryKeys.photos.detail(updatedPhoto.id), updatedPhoto)
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.exportStats() })
    },
  })
}

// Verify photo permanent ID
export function useVerifyPhoto() {
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.post<PhotoVerifyResultDto>(`/photo-export/verify/${photoId}`)
    },
  })
}

// Unlink photo (remove permanent ID)
export function useUnlinkPhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.post<PhotoExportInfoDto>(`/photo-export/unlink/${photoId}`)
    },
    onSuccess: (updatedPhoto) => {
      queryClient.setQueryData(queryKeys.photos.detail(updatedPhoto.id), updatedPhoto)
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.exportStats() })
    },
  })
}

// Delete photo
export function useDeletePhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (photoId: number) => {
      return api.delete(`/photos/${photoId}`)
    },
    onSuccess: (_, photoId) => {
      queryClient.removeQueries({ queryKey: queryKeys.photos.detail(photoId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.exportStats() })
    },
  })
}

// Upload/replace photo image
export function useUploadPhotoImage() {
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
        throw new Error('Failed to upload photo image')
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.summaries() })
      queryClient.invalidateQueries({ queryKey: queryKeys.photos.exportStats() })
    },
  })
}
