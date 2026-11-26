// (c) Copyright 2026 by Muczynski

/** SSO when the provider is set and is not local, or when a subject id is set. */
export function isSsoAccount(account?: {
  ssoProvider?: string | null
  ssoSubjectId?: string | null
} | null): boolean {
  if (!account) return false
  const provider = account.ssoProvider
  if (provider && provider !== 'local') return true
  return !!account.ssoSubjectId
}
