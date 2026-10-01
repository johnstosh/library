// (c) Copyright 2025 by Muczynski

export function BranchNameDisplay({
  name,
  subName,
  dataTest,
}: {
  name: string
  subName: string
  dataTest?: string
}) {
  return (
    <span className="flex flex-col items-start" data-test={dataTest}>
      <span className="text-base font-bold text-gray-900 leading-tight" data-test="app-name">
        {name}
      </span>
      {subName ? (
        <span className="text-xs text-gray-600 leading-tight" data-test="app-sub-name">
          {subName}
        </span>
      ) : null}
    </span>
  )
}
