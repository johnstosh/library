// (c) Copyright 2025 by Muczynski

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

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...fetchOptions.headers,
  }

  // Note: Authentication is cookie-based, no manual token handling needed
  const config: RequestInit = {
    ...fetchOptions,
    headers,
    credentials: 'include', // Important for cookie-based auth
  }

  try {
    const response = await fetch(url, config)

    // Handle 401 Unauthorized
    if (response.status === 401 && requireAuth) {
      // Import dynamically to avoid circular dependency
      const { useAuthStore } = await import('@/stores/authStore')
      useAuthStore.getState().logout()
      window.location.href = '/login'
      throw new ApiError('Unauthorized', 401, response.statusText)
    }

    // Handle other error responses
    if (!response.ok) {
      const errorText = await response.text()
      let errorMessage = 'An error occurred'

      // Try to parse JSON error response
      try {
        const errorData = JSON.parse(errorText)
        errorMessage = errorData.message || errorText
      } catch {
        // If not JSON, use raw text
        errorMessage = errorText || errorMessage
      }

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
      body: data ? JSON.stringify(data) : undefined,
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
  postFormData: <T>(endpoint: string, formData: FormData, options?: RequestOptions) =>
    apiClient<T>(endpoint, {
      ...options,
      method: 'POST',
      body: formData,
      headers: {
        // Don't set Content-Type, let browser set it with boundary
        ...options?.headers,
        'Content-Type': undefined,
      } as HeadersInit,
    }),
}
