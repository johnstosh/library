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
export interface LibraryDto {
  id: number
  name: string
  hostname: string
}

export interface LibraryStatisticsDto {
  libraryId: number
  libraryName: string
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
  name: string
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
