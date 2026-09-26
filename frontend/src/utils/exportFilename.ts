// (c) Copyright 2025 by Muczynski
import { isDevSite } from './environment'

export interface ExportFilenameStats {
  branchName: string
  bookCount: number
  authorCount: number
  userCount: number
  loanCount: number
  favoriteCount: number
  priceCount: number
  photoCount: number
  /** ISO date yyyy-mm-dd; defaults to today UTC. */
  date?: string
  /** Override hostname detection (tests). */
  hostname?: string
}

/** Sanitize branch/library name for download filenames. */
export function sanitizeExportBranchName(branchName: string): string {
  return branchName
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

/**
 * Catalog JSON export filename.
 * On library-dev hosts: `{date}-DEV-{branch}-…json`
 * Elsewhere: `{date}-{branch}-…json`
 */
export function buildExportFilename(stats: ExportFilenameStats): string {
  const date = stats.date ?? new Date().toISOString().split('T')[0]
  const branch = sanitizeExportBranchName(stats.branchName || 'branch') || 'branch'
  const envTag = isDevSite(stats.hostname) ? 'DEV-' : ''
  return `${date}-${envTag}${branch}-${stats.bookCount}-books-${stats.authorCount}-authors-${stats.userCount}-users-${stats.loanCount}-loans-${stats.favoriteCount}-favorites-${stats.priceCount}-prices-${stats.photoCount}-photos.json`
}
