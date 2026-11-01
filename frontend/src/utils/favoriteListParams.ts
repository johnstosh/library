// (c) Copyright 2025 by Muczynski

/**
 * Favorite list names travel in one `favoriteLists` query value, separated by
 * commas. A name that itself contains a comma, or that starts with "~", is
 * prefixed with "~" and percent-encoded so the separator split leaves it whole.
 * Names without those characters stay plain, so older links such as
 * `Have Read,Saints` still parse.
 */
const ENCODED_PREFIX = '~'

function needsEncoding(name: string): boolean {
  return name.includes(',') || name.startsWith(ENCODED_PREFIX)
}

export function joinFavoriteLists(lists: readonly string[]): string {
  return lists
    .map((name) => (needsEncoding(name) ? ENCODED_PREFIX + encodeURIComponent(name) : name))
    .join(',')
}

export function splitFavoriteLists(raw: string | null | undefined): string[] {
  if (!raw) return []
  const names: string[] = []
  for (const part of raw.split(',')) {
    const name = decodeFavoriteListToken(part.trim())
    if (name) names.push(name)
  }
  return names
}

function decodeFavoriteListToken(token: string): string {
  if (!token.startsWith(ENCODED_PREFIX)) return token
  try {
    return decodeURIComponent(token.slice(ENCODED_PREFIX.length))
  } catch {
    return token
  }
}
