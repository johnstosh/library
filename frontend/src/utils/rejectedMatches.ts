// (c) Copyright 2025 by Muczynski
import type { CatalogMatchRef, PublicLibrary, RejectedHolding, RejectedUrl } from '@/types/dtos'

const URL_PATTERN = /https?:\/\/[^\s]+/g
const TRAILING_PUNCTUATION = /[.,;:)\]}]+$/

/** http(s) URLs in a space-separated field, with trailing punctuation removed. */
export function httpUrls(text: string): string[] {
  if (!text.trim()) return []
  const found: string[] = []
  for (const match of text.matchAll(URL_PATTERN)) {
    const url = match[0].replace(TRAILING_PUNCTUATION, '')
    if (!found.some((existing) => existing.toLowerCase() === url.toLowerCase())) {
      found.push(url)
    }
  }
  return found
}

function urlKey(value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  const withoutHash = trimmed.split('#')[0].split('?')[0].replace(/\/+$/, '')
  return withoutHash.toLowerCase()
}

function grokipediaSlug(value: string): string | null {
  const key = urlKey(value)
  if (!key) return null
  const marker = 'grokipedia.com/page/'
  const at = key.indexOf(marker)
  if (at < 0) return null
  const slug = key.slice(at + marker.length).replace(/\/+$/, '')
  return slug || null
}

function bareSlug(value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed || /[\s/:]/.test(trimmed)) return null
  return trimmed.toLowerCase()
}

export function isLibriVox(url: string): boolean {
  const key = urlKey(url)
  return key != null && key.includes('librivox.org')
}

export function rejectsUrl(rows: RejectedUrl[] | undefined, candidate: string): boolean {
  if (!candidate.trim() || !rows || rows.length === 0) return false
  const candidateKey = urlKey(candidate)
  const candidateSlug = grokipediaSlug(candidate)
  return rows.some((row) => {
    if (!row.url?.trim()) return false
    if (candidateKey && candidateKey === urlKey(row.url)) return true
    const storedSlug = grokipediaSlug(row.url)
    if (candidateSlug && storedSlug && candidateSlug === storedSlug) return true
    const bare = bareSlug(row.url)
    return candidateSlug != null && bare != null && candidateSlug === bare
  })
}

export type LiveMatch = 'any' | 'text' | 'audio'

export function liveUrlIsRejected(live: string, rows: RejectedUrl[], kind: LiveMatch): boolean {
  return httpUrls(live).some((url) => {
    if (kind === 'audio' && !isLibriVox(url)) return false
    if (kind === 'text' && isLibriVox(url)) return false
    return rejectsUrl(rows, url)
  })
}

/** A second copy of the same URL replaces the reason. Date and by stay for the server. */
export function appendRejectedUrls(rows: RejectedUrl[], urls: string[], reason: string): RejectedUrl[] {
  const next = [...rows]
  const trimmedReason = reason.trim().slice(0, 500)
  for (const url of urls) {
    const trimmed = url.trim()
    if (!trimmed) continue
    const index = next.findIndex((row) => rejectsUrl([row], trimmed) || rejectsUrl([{ url: trimmed }], row.url))
    const entry: RejectedUrl = { url: trimmed, reason: trimmedReason }
    if (index >= 0) next[index] = entry
    else next.push(entry)
  }
  return next
}

export function appendHoldings(
  rows: RejectedHolding[],
  library: PublicLibrary,
  matches: CatalogMatchRef[],
  reason: string,
): RejectedHolding[] {
  const next = [...rows]
  const trimmedReason = reason.trim().slice(0, 500)
  for (const match of matches) {
    const recordKey = match.recordKey?.trim()
    if (!recordKey) continue
    const index = next.findIndex(
      (row) => row.library === library && row.recordKey.trim().toLowerCase() === recordKey.toLowerCase(),
    )
    const entry: RejectedHolding = {
      library,
      recordKey,
      url: match.url?.trim() || undefined,
      reason: trimmedReason,
    }
    if (index >= 0) next[index] = entry
    else next.push(entry)
  }
  return next
}
