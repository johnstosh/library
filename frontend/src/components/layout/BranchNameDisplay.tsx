// (c) Copyright 2025 by Muczynski
import { isDevSite } from '@/utils/environment'

export function BranchNameDisplay({
  branchName,
  librarySystemName,
  dataTest,
}: {
  branchName: string
  librarySystemName: string
  dataTest?: string
}) {
  if (isDevSite()) {
    return (
      <span className="flex flex-col items-start" data-test={dataTest}>
        <span className="text-xl font-bold text-gray-900 leading-tight">DEV</span>
      </span>
    )
  }

  return (
    <span className="flex flex-col items-start" data-test={dataTest}>
      <span className="text-base font-bold text-gray-900 leading-tight">
        The {branchName} Branch
      </span>
      <span className="text-xs text-gray-600 leading-tight">
        of the {librarySystemName}
      </span>
    </span>
  )
}
