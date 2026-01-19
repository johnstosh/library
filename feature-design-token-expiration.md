# Token Expiration Handling Alternatives

<!-- (c) Copyright 2025 by Muczynski -->

## Problem Statement

When a user returns to the browser after hours of inactivity, their authentication token expires. Currently, when the browser tries to access a protected page like `/data-management`, it receives a 401 Unauthorized response, resulting in a dead-stop where the user sees an error instead of being redirected to login.

The desired behavior is:
1. Detect the 401 response
2. Redirect to the login page
3. After successful login, redirect back to the original page the user was trying to access

## Alternatives

### Alternative 1: Frontend Interceptor with Return URL

**How it works:**
- Add a response interceptor to the API client (fetch wrapper or axios)
- On 401 response, save the current URL to sessionStorage or URL parameter
- Redirect to login page with the return URL as a parameter
- Login page reads the return URL and redirects there after successful authentication

**Pros:**
- Clean separation of concerns
- Works with any authentication mechanism
- Can be implemented entirely in frontend

**Cons:**
- Requires changes to both API client and login page
- May miss 401s from non-API requests (e.g., direct navigation)
- Race condition risk if multiple requests fail simultaneously

**Implementation:**
```typescript
// In api/client.ts
async function handleResponse(response: Response, url: string) {
  if (response.status === 401) {
    const returnUrl = window.location.pathname + window.location.search
    window.location.href = `/login?returnUrl=${encodeURIComponent(returnUrl)}`
    throw new Error('Session expired')
  }
  return response
}

// In login page
const searchParams = new URLSearchParams(window.location.search)
const returnUrl = searchParams.get('returnUrl') || '/books'
// After successful login:
navigate(returnUrl)
```

### Alternative 2: React Query Error Handler

**How it works:**
- Configure TanStack Query's global error handler
- Detect 401 errors and redirect to login
- Store return URL in query params or sessionStorage

**Pros:**
- Centralized error handling
- Works with React Query's retry logic
- Can distinguish between different error types

**Cons:**
- Only handles errors from React Query hooks
- Doesn't cover direct fetch calls outside React Query

**Implementation:**
```typescript
// In config/queryClient.ts
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (error instanceof Error && error.message.includes('401')) {
          return false // Don't retry auth errors
        }
        return failureCount < 3
      },
    },
  },
})

// Use onError callback in mutations/queries
```

### Alternative 3: Auth Context with Token Refresh

**How it works:**
- Wrap app in an AuthProvider context
- Monitor token expiration proactively (using JWT exp claim or server timestamp)
- Show warning before expiration, offer to refresh
- On expiration, redirect to login with return URL

**Pros:**
- Better UX with proactive warnings
- Can attempt silent token refresh
- Single source of truth for auth state

**Cons:**
- More complex implementation
- Requires backend support for token refresh
- May not work with all auth mechanisms (e.g., session-based)

**Implementation:**
```typescript
// AuthProvider component
useEffect(() => {
  const checkExpiration = setInterval(() => {
    if (isTokenExpired()) {
      // Save current location
      sessionStorage.setItem('returnUrl', window.location.href)
      // Redirect to login
      logout()
      navigate('/login')
    }
  }, 60000) // Check every minute
  return () => clearInterval(checkExpiration)
}, [])
```

### Alternative 4: Service Worker Interception

**How it works:**
- Use a service worker to intercept all network requests
- Detect 401 responses at the network level
- Post message to main thread to trigger redirect

**Pros:**
- Catches all network requests, not just API calls
- Works even when page is backgrounded
- Can cache the intended destination

**Cons:**
- Complex implementation
- Service worker lifecycle complexity
- Not all browsers support service workers equally

### Alternative 5: Backend Redirect with State

**How it works:**
- Backend returns 302 redirect to login page on expired session
- Login page receives original URL in state parameter (OAuth style)
- After auth, redirect back to original URL

**Pros:**
- Works at HTTP level, before JavaScript executes
- Standard OAuth pattern (well-understood)
- Works even with JavaScript disabled

**Cons:**
- Requires backend changes
- API endpoints typically return JSON, not redirects
- May not work well with SPA architecture where API returns JSON

### Alternative 6: Zustand Store with Persistence

**How it works:**
- Store intended destination in Zustand with persistence
- Protected routes check auth status and store destination if unauthenticated
- After login, check for stored destination and navigate there

**Pros:**
- Persists across page reloads (localStorage)
- Integrates with existing Zustand state management
- Clean separation from routing logic

**Cons:**
- Requires changes to all protected routes
- Persistence can lead to stale destinations
- Need to clear stored destination appropriately

**Implementation:**
```typescript
// In authStore
interface AuthState {
  isAuthenticated: boolean
  returnUrl: string | null
  setReturnUrl: (url: string | null) => void
}

// In ProtectedRoute
if (!isAuthenticated) {
  setReturnUrl(location.pathname + location.search)
  return <Navigate to="/login" />
}

// In Login page after success
if (returnUrl) {
  setReturnUrl(null)
  navigate(returnUrl)
} else {
  navigate('/books')
}
```

## Recommended Approach

For this application, **Alternative 1 (Frontend Interceptor) combined with Alternative 6 (Zustand Store)** is recommended:

1. Add a 401 interceptor to the API client
2. Store return URL in the auth Zustand store (with localStorage persistence)
3. Redirect to login page
4. After successful login, read return URL from store and navigate

This approach:
- Is straightforward to implement
- Works with the existing architecture
- Handles both API and navigation scenarios
- Persists across page refreshes if needed

## Security Considerations

- **Validate return URLs**: Only allow relative URLs or URLs from the same origin to prevent open redirect vulnerabilities
- **Clear return URL**: Always clear the stored return URL after using it
- **Timeout**: Consider clearing stale return URLs after a period (e.g., 1 hour)
- **Sanitize**: Don't include sensitive data in return URL parameters
