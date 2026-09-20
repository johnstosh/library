// (c) Copyright 2025 by Muczynski
import { useState } from 'react'
import { Button } from './Button'
import { ErrorMessage } from './ErrorMessage'
import { isTransientApiError, getTransientErrorMessage } from '@/utils/api'
import { PiArrowClockwise, PiArrowsClockwise } from 'react-icons/pi'

export interface TransientFetchErrorBannerProps {
  error: unknown
  onRetry?: () => void
  className?: string
  'data-test'?: string
}

/**
 * Banner for transient API/query errors (A in Issue #319).
 * Shows friendly message + Refresh page (primary) and optional Retry button.
 * For non-transient errors, falls back to standard ErrorMessage.
 */
export function TransientFetchErrorBanner({
  error,
  onRetry,
  className,
  'data-test': dataTest = 'transient-error-banner',
}: TransientFetchErrorBannerProps) {
  const [isRefreshing, setIsRefreshing] = useState(false)

  const isTransient = isTransientApiError(error)
  const message = getTransientErrorMessage(error)

  const handleRefresh = () => {
    setIsRefreshing(true)
    window.location.reload()
  }

  if (!error) return null

  if (!isTransient) {
    return <ErrorMessage message={message} className={className} data-test={dataTest} />
  }

  return (
    <div
      className={`bg-amber-50 border border-amber-200 rounded-lg p-4 ${className || ''}`}
      role="alert"
      data-test={dataTest}
    >
      <div className="flex flex-col sm:flex-row sm:items-start gap-3">
        <div className="flex-1">
          <div className="flex items-center">
            <div className="w-5 h-5 mr-2 text-amber-600">
              <PiArrowsClockwise className="w-full h-full" />
            </div>
            <p className="text-amber-800 font-medium">{message}</p>
          </div>
          <p className="text-amber-700 text-sm mt-1">
            This is usually temporary. Try refreshing the page.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-2 sm:shrink-0 pt-1">
          {onRetry && (
            <Button
              variant="outline"
              size="sm"
              onClick={onRetry}
              leftIcon={<PiArrowClockwise />}
              data-test="transient-retry-button"
            >
              Retry
            </Button>
          )}
          <Button
            variant="primary"
            size="sm"
            onClick={handleRefresh}
            isLoading={isRefreshing}
            leftIcon={<PiArrowsClockwise />}
            data-test="transient-refresh-button"
          >
            Refresh Page
          </Button>
        </div>
      </div>
    </div>
  )
}
