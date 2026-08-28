// (c) Copyright 2025 by Muczynski

/**
 * Trailing copy-number suffix our catalog appends to disambiguate duplicate
 * titles, e.g. ", c. 2", ", c.2", ", c 3". Mirrors Book.stripCopySuffix on the
 * Java domain entity.
 */
const COPY_SUFFIX = /,\s*c\.?\s*\d+\s*$/i

/**
 * Strips a trailing copy-number suffix of the form ", c. N" (any spacing or
 * optional period after "c"). Returns the original string when there is no
 * suffix, or when stripping would leave the title empty.
 */
export function stripCopySuffix(title: string | null | undefined): string {
  if (title == null || title.trim() === '') {
    return title ?? ''
  }
  const trimmed = title.trim()
  const stripped = trimmed.replace(COPY_SUFFIX, '').trim()
  return stripped === '' ? trimmed : stripped
}

export function ydlCatalogSearchUrl(title: string): string {
  const queryTitle = stripCopySuffix(title)
  return `https://ypsilantidl.na4.iiivega.com/search?query=${encodeURIComponent(`"${queryTitle}"`)}&searchType=everything&pageSize=40`
}

export function emuCatalogSearchUrl(title: string): string {
  const queryTitle = stripCopySuffix(title)
  return `https://emich.primo.exlibrisgroup.com/discovery/search?query=${encodeURIComponent(`any,contains,"${queryTitle}"`)}&tab=Everything&search_scope=MyInst_and_CI&vid=01EMU_INST:EMU`
}
