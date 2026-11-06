// (c) Copyright 2025 by Muczynski

export function parseApiErrorMessage(errorText: string, fallback = 'An error occurred'): string {
  if (!errorText) {
    return fallback
  }

  try {
    const errorData = JSON.parse(errorText) as unknown
    if (typeof errorData === 'string' && errorData.trim()) {
      return errorData
    }
    if (errorData && typeof errorData === 'object') {
      const record = errorData as { message?: unknown; error?: unknown }
      if (typeof record.message === 'string' && record.message.trim()) {
        return record.message
      }
      if (typeof record.error === 'string' && record.error.trim()) {
        return record.error
      }
    }
  } catch {
    // Response was not JSON; use the raw body below.
  }

  return errorText
}

export class ApiError extends Error {
  status: number
  statusText: string

  constructor(message: string, status: number, statusText: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.statusText = statusText
  }
}

/**
 * Browser fetch rejects with a generic message ("Failed to fetch", Firefox's
 * "NetworkError when attempting to fetch resource.", Safari's "Load failed")
 * when the connection closes before any HTTP status arrives. The status code
 * is hidden on purpose. Say that no response arrived and how long the call was open.
 */
export function describeTransportFailure(
  error: unknown,
  method: string,
  endpoint: string,
  elapsedMs: number,
): string {
  const errorName = readErrorField(error, 'name')
  const raw = readErrorField(error, 'message') || 'Network error'
  const target = `${method.toUpperCase()} ${endpoint}`
  const immediate = elapsedMs < 1500
  const when = immediate ? 'immediately' : `after ${Math.round(elapsedMs / 1000)}s`

  if (errorName === 'AbortError' || /aborted|abort/i.test(raw)) {
    return immediate
      ? `Request cancelled for ${target}.`
      : `Request cancelled for ${target} after ${Math.round(elapsedMs / 1000)}s.`
  }

  const reported = /failed to fetch|networkerror when attempting to fetch resource\.?|load failed/i.test(raw)
    ? 'The'
    : `${raw}: the`
  const cause = error instanceof Error && error.cause instanceof Error && error.cause.message.trim()
    ? ` Cause: ${error.cause.message.trim()}.`
    : ''
  return `No response (${when}). ${reported} connection closed before a status code arrived.${cause}`
}

function readErrorField(error: unknown, field: 'name' | 'message'): string {
  if (typeof error !== 'object' || error === null || !(field in error)) {
    return ''
  }
  const value = (error as Record<string, unknown>)[field]
  return typeof value === 'string' ? value.trim() : ''
}

interface RequestOptions extends RequestInit {
  requireAuth?: boolean
}

export async function apiClient<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { requireAuth = true, ...fetchOptions } = options

  const url = endpoint.startsWith('http')
    ? endpoint
    : `${import.meta.env.VITE_API_BASE_URL || ''}/api${endpoint}`

  // Build headers: skip default Content-Type for Blob/File (raw JSON body for streaming import) or FormData
  const headers: HeadersInit = {
    ...(fetchOptions.body instanceof FormData || fetchOptions.body instanceof Blob || fetchOptions.body instanceof File
      ? {}
      : { 'Content-Type': 'application/json' }),
    ...fetchOptions.headers,
  }

  // Note: Authentication is cookie-based, no manual token handling needed
  const config: RequestInit = {
    ...fetchOptions,
    headers,
    credentials: 'include', // Important for cookie-based auth
  }

  const method = (fetchOptions.method || 'GET').toUpperCase()
  const started = Date.now()
  let response: Response
  try {
    response = await fetch(url, config)
  } catch (error) {
    throw new ApiError(
      describeTransportFailure(error, method, endpoint, Date.now() - started),
      0,
      'Network Error'
    )
  }

  try {

    // Handle 401 Unauthorized - redirect to login with state (Alternative 5)
    if (response.status === 401 && requireAuth) {
      // Import dynamically to avoid circular dependency
      const { useAuthStore } = await import('@/stores/authStore')
      const authStore = useAuthStore.getState()

      // Save current URL as return URL (OAuth-style state parameter)
      const currentPath = window.location.pathname + window.location.search
      authStore.setReturnUrl(currentPath)

      // Logout and redirect to login
      authStore.logout()
      window.location.href = '/login'
      throw new ApiError('Unauthorized', 401, response.statusText)
    }

    // Handle other error responses
    if (!response.ok) {
      const errorText = await response.text()
      let errorMessage = 'An error occurred'

      errorMessage = parseApiErrorMessage(errorText, errorMessage)

      throw new ApiError(
        errorMessage,
        response.status,
        response.statusText
      )
    }

    // Handle empty responses (204 No Content or 200 with empty body)
    if (response.status === 204) {
      return null as T
    }

    // Check for empty body before parsing JSON
    const contentLength = response.headers.get('Content-Length')
    if (contentLength === '0') {
      return null as T
    }

    // Parse JSON response, handling empty body gracefully
    const text = await response.text()
    if (!text) {
      return null as T
    }

    const data = JSON.parse(text) as T
    return data
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    throw new ApiError(
      error instanceof Error ? error.message : 'Network error',
      0,
      'Network Error'
    )
  }
}

// Helper functions for different HTTP methods
export const api = {
  get: <T>(endpoint: string, options?: RequestOptions) =>
    apiClient<T>(endpoint, { ...options, method: 'GET' }),

  post: <T>(endpoint: string, data?: unknown, options?: RequestOptions) =>
    apiClient<T>(endpoint, {
      ...options,
      method: 'POST',
      // Support raw File/Blob bodies for streaming JSON import (no JSON.stringify)
      body: data instanceof Blob || data instanceof File ? data : (data ? JSON.stringify(data) : undefined),
    }),

  put: <T>(endpoint: string, data?: unknown, options?: RequestOptions) =>
    apiClient<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    }),

  delete: <T>(endpoint: string, options?: RequestOptions) =>
    apiClient<T>(endpoint, { ...options, method: 'DELETE' }),

  // Special handler for multipart/form-data (file uploads)
  // Note: Content-Type is automatically omitted for FormData by apiClient
  postFormData: <T>(endpoint: string, formData: FormData, options?: RequestOptions) =>
    apiClient<T>(endpoint, {
      ...options,
      method: 'POST',
      body: formData,
    }),
}
