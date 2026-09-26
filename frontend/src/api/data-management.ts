// (c) Copyright 2025 by Muczynski
import React, { useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { api } from './client'
import { queryKeys } from '@/config/queryClient'
import { streamJsonImportChunks } from './jsonImportChunker'
import {
  runChunkedJsonExport,
  type ExportChunk,
  type ExportChunkPlan,
  type JsonExportProgress,
} from './jsonExportAssembler'
import { fetchComplete, readCompleteJson } from './fetchWithCompleteBody'

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
    favorites?: number
    prices?: number
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
  favoriteCount: number
  /** Unique books with at least one usable AbeBooks listing. */
  priceCount: number
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
  requested: number
  availableAtYdl: number
  ydlPaper: number
  ydlEbook: number
  ydlAudio: number
  availableAtEmu: number
  emuPaper: number
  emuEbook: number
  emuAudio: number
  availableAtAcla: number
  aclaPaper: number
  aclaEbook: number
  aclaAudio: number
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

export type { JsonExportProgress, ExportChunkPlan, ExportChunk } from './jsonExportAssembler'

async function fetchExportChunkPlan(): Promise<ExportChunkPlan> {
  const response = await fetchComplete('/api/import/json/chunk-plan', {
    credentials: 'include',
  })
  if (!response.ok) {
    throw new Error(`Failed to load export plan (HTTP ${response.status})`)
  }
  return readCompleteJson<ExportChunkPlan>(response)
}

async function fetchExportChunk(
  section: string,
  afterId: number,
  limit: number,
): Promise<ExportChunk> {
  const params = new URLSearchParams({
    section,
    afterId: String(afterId),
    limit: String(limit),
  })
  const response = await fetchComplete(`/api/import/json/chunk?${params}`, {
    credentials: 'include',
  })
  if (!response.ok) {
    throw new Error(
      `Failed to export "${section}" chunk afterId=${afterId} (HTTP ${response.status})`,
    )
  }
  return readCompleteJson<ExportChunk>(response)
}

/**
 * Chunked JSON export: ~33 sequential GETs with progress, assembled into one Blob.
 * Fails loudly if any chunk fails or assembled section counts mismatch the plan.
 * Full streaming GET /api/import/json remains available for tests/tools.
 */
export async function exportJsonData(
  onProgress?: (progress: Omit<JsonExportProgress, 'isExporting'>) => void,
): Promise<Blob> {
  return runChunkedJsonExport(
    {
      fetchPlan: fetchExportChunkPlan,
      fetchChunk: fetchExportChunk,
    },
    onProgress,
  )
}

export interface JsonImportProgress {
  percentage: number
  bytesRead: number
  totalBytes: number
  isImporting: boolean
}

function emptyCounts(): NonNullable<ImportResponseDto['counts']> {
  return {
    branches: 0,
    authors: 0,
    users: 0,
    books: 0,
    loans: 0,
    photos: 0,
    favorites: 0,
    prices: 0,
  }
}

function addCounts(
  dest: NonNullable<ImportResponseDto['counts']>,
  src?: ImportResponseDto['counts'],
) {
  if (!src) return
  dest.branches += src.branches || 0
  dest.authors += src.authors || 0
  dest.users += src.users || 0
  dest.books += src.books || 0
  dest.loans += src.loans || 0
  dest.photos += src.photos || 0
  dest.favorites = (dest.favorites || 0) + (src.favorites || 0)
  dest.prices = (dest.prices || 0) + (src.prices || 0)
}

/**
 * Chunked JSON import: streams the file client-side into ~33 sequential POSTs.
 * Retries each POST a few times (upsert-safe). Progress is bytesRead/file.size.
 */
export async function importJsonDataChunked(
  file: File,
  onProgress?: (bytesRead: number, totalBytes: number) => void,
): Promise<ImportResponseDto> {
  const totals = emptyCounts()
  const errors: ImportErrorDto[] = []
  let lastMessage = 'Import completed successfully'
  let anySuccess = false

  for await (const chunk of streamJsonImportChunks(file)) {
    onProgress?.(chunk.bytesRead, file.size)

    const response = await fetchComplete('/api/import/json', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: new Blob([Uint8Array.from(chunk.body)], { type: 'application/json' }),
    })

    let payload: ImportResponseDto | null = null
    try {
      payload = await readCompleteJson<ImportResponseDto>(response)
    } catch {
      payload = null
    }

    if (!response.ok || !payload?.success) {
      const msg =
        payload?.message ||
        `Import chunk failed with HTTP ${response.status}`
      throw new Error(msg)
    }

    anySuccess = true
    addCounts(totals, payload.counts)
    if (payload.errors?.length) errors.push(...payload.errors)
    if (payload.message) lastMessage = payload.message
  }

  onProgress?.(file.size, file.size)

  if (!anySuccess) {
    // Empty file / no sections — still hit the endpoint once with {}
    const response = await fetchComplete('/api/import/json', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: '{}',
    })
    let payload: ImportResponseDto
    try {
      payload = await readCompleteJson<ImportResponseDto>(response)
    } catch {
      throw new Error(`Import failed with HTTP ${response.status}`)
    }
    if (!response.ok || !payload.success) {
      throw new Error(payload.message || 'Import failed')
    }
    return payload
  }

  return {
    success: true,
    message:
      errors.length > 0
        ? `Import completed with ${errors.length} error(s)`
        : lastMessage,
    counts: totals,
    errors,
  }
}

// Import JSON data (chunked streaming POSTs with determinate progress)
export function useImportJsonData() {
  const queryClient = useQueryClient()
  const [progress, setProgress] = useState<JsonImportProgress>({
    percentage: 0,
    bytesRead: 0,
    totalBytes: 0,
    isImporting: false,
  })

  const mutation = useMutation({
    mutationFn: async (file: File) => {
      setProgress({
        percentage: 0,
        bytesRead: 0,
        totalBytes: file.size,
        isImporting: true,
      })
      try {
        const response = await importJsonDataChunked(file, (bytesRead, totalBytes) => {
          const percentage =
            totalBytes > 0 ? Math.min(100, (bytesRead / totalBytes) * 100) : 100
          setProgress({
            percentage,
            bytesRead,
            totalBytes,
            isImporting: true,
          })
        })
        return response
      } finally {
        setProgress((prev) => ({ ...prev, isImporting: false, percentage: 100 }))
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['database-stats'] })
      queryClient.invalidateQueries({ queryKey: ['availability-stats'] })
      queryClient.invalidateQueries({ queryKey: ['books'] })
      queryClient.invalidateQueries({ queryKey: ['authors'] })
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['loans'] })
      queryClient.invalidateQueries({ queryKey: ['branches'] })
    },
  })

  return { ...mutation, progress }
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

// Maintenance types for Illegal Genres cleanup (Issue #347)
export interface IllegalGenresMaintenanceDto {
  booksAffected: number
  booksScanned?: number
  booksUpdated?: number
  pluralCorrections?: number
  illegalRemoved?: number
  message?: string
  error?: string
}

// Maintenance hooks for DataManagementPage
export function useRecalcIllegalGenres() {
  return useMutation({
    mutationFn: () =>
      api.get<IllegalGenresMaintenanceDto>('/maintenance/illegal-genres/count'),
  })
}

export function useCleanupIllegalGenres() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () =>
      api.post<IllegalGenresMaintenanceDto>('/maintenance/illegal-genres/cleanup'),
    onSuccess: () => {
      // Refresh related stats after cleanup
      queryClient.invalidateQueries({ queryKey: ['label-counts'] })
      queryClient.invalidateQueries({ queryKey: ['database-stats'] })
    },
  })
}

// Duplicate catalog titles (Issue #351) — find-only
export interface DuplicateTitlePairDto {
  score: number
  bookAId: number
  bookATitle?: string
  bookAAlternateTitle?: string
  bookAAuthorName?: string
  bookAStatus?: string
  bookBId: number
  bookBTitle?: string
  bookBAlternateTitle?: string
  bookBAuthorName?: string
  bookBStatus?: string
  matchedTitleA?: string
  matchedTitleB?: string
}

export interface DuplicateTitlesResultDto {
  pairs: DuplicateTitlePairDto[]
  booksScanned: number
  representativesCompared: number
  message?: string
  error?: string
}

export function useFindDuplicateTitles() {
  return useMutation({
    mutationFn: () =>
      api.post<DuplicateTitlesResultDto>('/maintenance/duplicate-titles/find'),
  })
}
