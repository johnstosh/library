// (c) Copyright 2025 by Muczynski
import { create } from 'zustand'
import { api } from '@/api/client'

export type UserAuthority = 'LIBRARIAN' | 'USER'

export interface CurrentUser {
  id: number
  username: string
  authority: UserAuthority
  ssoSubjectId?: string
}

interface AuthState {
  isAuthenticated: boolean
  user: CurrentUser | null
  isLoading: boolean
  returnUrl: string | null // URL to return to after login (Alternative 5: state parameter)

  // Actions
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  checkAuth: () => Promise<void>
  setUser: (user: CurrentUser | null) => void
  setReturnUrl: (url: string | null) => void
  getAndClearReturnUrl: () => string | null
}

export const useAuthStore = create<AuthState>((set, get) => ({
  isAuthenticated: false,
  user: null,
  isLoading: true,
  returnUrl: null,

  login: async (username: string, password: string) => {
    // Hash password client-side using SHA-256
    const encoder = new TextEncoder()
    const data = encoder.encode(password)
    const hashBuffer = await crypto.subtle.digest('SHA-256', data)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    const hashedPassword = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')

    // Send login request
    const response = await api.post<CurrentUser>('/auth/login', {
      username,
      password: hashedPassword,
    }, { requireAuth: false })

    set({ isAuthenticated: true, user: response, isLoading: false })
  },

  logout: () => {
    // Call logout endpoint to clear server-side session
    api.post('/auth/logout', {}, { requireAuth: false }).catch(() => {
      // Ignore errors on logout
    })

    set({ isAuthenticated: false, user: null, isLoading: false })
  },

  checkAuth: async () => {
    try {
      const user = await api.get<CurrentUser>('/auth/me', { requireAuth: false })
      set({ isAuthenticated: true, user, isLoading: false })
    } catch (error) {
      set({ isAuthenticated: false, user: null, isLoading: false })
    }
  },

  setUser: (user: CurrentUser | null) => {
    set({ isAuthenticated: !!user, user, isLoading: false })
  },

  setReturnUrl: (url: string | null) => {
    // Security: Only allow relative URLs to prevent open redirect attacks
    if (url && (url.startsWith('/') && !url.startsWith('//'))) {
      set({ returnUrl: url })
      // Also persist to sessionStorage for OAuth flows (browser redirect loses state)
      sessionStorage.setItem('auth_return_url', url)
    } else {
      set({ returnUrl: null })
      sessionStorage.removeItem('auth_return_url')
    }
  },

  getAndClearReturnUrl: () => {
    // Check both store and sessionStorage (for OAuth flows)
    let { returnUrl } = get()
    if (!returnUrl) {
      returnUrl = sessionStorage.getItem('auth_return_url')
    }
    // Clear both
    set({ returnUrl: null })
    sessionStorage.removeItem('auth_return_url')
    return returnUrl
  },
}))

// Helper hook to check if user is librarian
export const useIsLibrarian = () => {
  const user = useAuthStore((state) => state.user)
  return user?.authority === 'LIBRARIAN'
}

// Helper hook to check if user is authenticated
export const useIsAuthenticated = () => {
  return useAuthStore((state) => state.isAuthenticated)
}
