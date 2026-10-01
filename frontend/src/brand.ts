// (c) Copyright 2026 by Muczynski
import { isDevSite } from '@/utils/environment'

export type AppBrand = {
  name: string
  subName: string
}

/**
 * App name and sub-name for the header and the browser tab.
 * The deployed dev site uses fixed names. Every other host, including
 * production and local, uses the first branch's name and library system.
 */
export function libraryBrand(
  branchName: string,
  librarySystemName: string,
  hostname?: string,
): AppBrand {
  if (isDevSite(hostname)) {
    return { name: 'library-dev', subName: 'DEV' }
  }
  return { name: branchName, subName: librarySystemName }
}
