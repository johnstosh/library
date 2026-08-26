// (c) Copyright 2025 by Muczynski
import { Spinner } from './Spinner'

export function PageLoading() {
  return (
    <div className="flex justify-center items-center min-h-[400px]">
      <Spinner size="lg" />
    </div>
  )
}
