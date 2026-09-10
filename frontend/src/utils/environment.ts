// (c) Copyright 2025 by Muczynski

/**
 * True on the deployed development site (library-dev.*), not local or production.
 */
export function isDevSite(hostname: string = currentHostname()): boolean {
  return hostname.includes('library-dev')
}

function currentHostname(): string {
  if (typeof window === 'undefined') return ''
  return window.location.hostname
}
