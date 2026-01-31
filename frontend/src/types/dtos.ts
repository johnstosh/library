// (c) Copyright 2025 by Muczynski
import type { BookStatus } from './enums'

// Library Card Design Type
export type LibraryCardDesign =
  | 'COUNTRYSIDE_YOUTH'
  | 'SACRED_HEART_PORTRAIT'
  | 'RADIANT_BLESSING'
  | 'PATRON_OF_CREATURES'
  | 'CLASSICAL_DEVOTION'

// Common
export interface IdDto {
  id: number
}

export interface SummaryDto {
  id: number
  lastModified: string
}

// Library DTOs
export interface BranchDto {
  id: number
  branchName: string
  librarySystemName: string
}

export interface BranchStatisticsDto {
  branchId: number
  branchName: string
  bookCount: number
  activeLoansCount: number
}

// Author DTOs
export interface AuthorDto {
  id: number
  name: string
  dateOfBirth?: string
  dateOfDeath?: string
  religiousAffiliation?: string
  birthCountry?: string
  nationality?: string
  briefBiography?: string
  grokipediaUrl?: string
  firstPhotoId?: number
  firstPhotoChecksum?: string
  bookCount?: number
  lastModified: string
  books?: BookDto[]
}

export interface AuthorSummaryDto {
  id: number
  lastModified: string
}

// Book DTOs
export interface BookDto {
  id: number
  title: string
  publicationYear?: number
  publisher?: string
  plotSummary?: string
  relatedWorks?: string
  detailedDescription?: string
  grokipediaUrl?: string
  freeTextUrl?: string
  dateAddedToLibrary?: string
  status: BookStatus
  statusReason?: string
  locNumber?: string
  libraryId?: number
  library?: string
  authorId?: number
  author?: string
  firstPhotoId?: number
  firstPhotoChecksum?: string
  loanCount?: number
  lastModified: string
  tagsList?: string[]
}

export interface BookSummaryDto {
  id: number
  lastModified: string
}

// Photo DTOs
export interface PhotoDto {
  id: number
  checksum: string
  displayOrder: number
  rotation: number
  bookId?: number
  authorId?: number
  dateTaken?: string
}

// User DTOs
export interface UserDto {
  id: number
  username: string
  authorities: string[]
  xaiApiKey?: string
  ssoProvider?: string
  ssoSubjectId?: string
  email?: string
  libraryCardDesign?: LibraryCardDesign
  activeLoansCount?: number
  googlePhotosApiKey?: string
  googlePhotosRefreshToken?: string
  googlePhotosTokenExpiry?: string
  googleClientSecret?: string
  googlePhotosAlbumId?: string
  lastPhotoTimestamp?: string
  lastModified: string
}

// Loan DTOs
export interface LoanDto {
  id: number
  bookId: number
  bookTitle?: string
  userId: number
  userName?: string
  loanDate: string
  dueDate: string
  returnDate?: string
  lastModified: string
  // Checkout card photo (from loan-by-photo feature)
  photoId?: number
  photoChecksum?: string
}

export interface CheckoutCardTranscriptionDto {
  title: string
  author: string
  callNumber: string
  lastDate: string
  lastIssuedTo: string
  lastDue: string
}

// Library Card DTOs
export interface ApplicationDto {
  id: number
  userId: number
  username?: string
  status: string
  appliedDate: string
  processedDate?: string
}

// Bulk Delete Result DTOs
export interface BulkDeleteFailureDto {
  id: number
  title: string
  errorMessage: string
}

export interface BulkDeleteResultDto {
  deletedCount: number
  failedCount: number
  deletedIds: number[]
  failures: BulkDeleteFailureDto[]
}

// Genre Lookup DTOs
export interface GenreLookupResultDto {
  bookId: number
  title?: string
  success: boolean
  suggestedGenres?: string[]
  errorMessage?: string
}

// Chunked Upload DTOs
export interface ChunkUploadResultDto {
  uploadId: string
  chunkIndex: number
  processedPhotos: PhotoZipImportItemDto[]
  totalProcessedSoFar: number
  totalSuccessSoFar: number
  totalFailureSoFar: number
  totalSkippedSoFar: number
  complete: boolean
  finalResult?: PhotoZipImportResultDto
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

// Settings DTOs
export interface GlobalSettingsDto {
  googleClientSecret?: string
  googleClientSecretPartial?: string
  googleClientSecretUpdatedAt?: string
  googleClientId?: string
  redirectUri?: string
  googleSsoClientId?: string
  googleSsoClientSecret?: string
  googleSsoClientSecretPartial?: string
  googleSsoCredentialsUpdatedAt?: string
  googleSsoClientSecretConfigured: boolean
  googleSsoClientIdConfigured: boolean
  googleSsoClientSecretValidation?: string
  lastUpdated?: string
  googleClientSecretConfigured: boolean
  googleClientSecretValidation?: string
}
