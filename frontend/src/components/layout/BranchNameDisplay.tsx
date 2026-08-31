// (c) Copyright 2025 by Muczynski

export function BranchNameDisplay({
  branchName,
  librarySystemName,
  dataTest,
}: {
  branchName: string
  librarySystemName: string
  dataTest?: string
}) {
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
