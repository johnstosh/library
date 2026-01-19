# Security & Authentication Design

## Authority System

### Two-Tier Authority System
- **LIBRARIAN authority**: Full CRUD access to all resources (books, authors, libraries, users, settings, etc.)
- **USER authority**: Limited access
  - Can view books/authors/libraries
  - Can check out books to themselves
  - Can view their own loans only
  - Cannot create/edit/delete books, authors, or libraries
  - Cannot view other users' data or access admin features

### CRITICAL: Authorities vs Roles
- We use **authorities**, NOT roles
- Use `hasAuthority('LIBRARIAN')` or `hasAuthority('USER')` in `@PreAuthorize` annotations
- **NEVER** use `hasRole()` - it expects a `ROLE_` prefix which we don't use
- In tests: use `@WithMockUser(authorities = "LIBRARIAN")` without `ROLE_` prefix
- In tests: use `new SimpleGrantedAuthority("LIBRARIAN")` without `ROLE_` prefix

### Checking Authorities Programmatically in Controllers
When you need to check authorities in controller code (not via `@PreAuthorize`), use the **SecurityUtils** utility class:

**RECOMMENDED** - Use SecurityUtils utility class:
```java
import com.muczynski.library.util.SecurityUtils;

boolean isLibrarian = SecurityUtils.isLibrarian(authentication);
boolean isUser = SecurityUtils.isUser(authentication);
boolean hasCustomAuthority = SecurityUtils.hasAuthority(authentication, "CUSTOM_AUTHORITY");
```

**ALTERNATIVE** - Direct stream-based authority check (if SecurityUtils is not available):
```java
boolean isLibrarian = authentication.getAuthorities().stream()
        .anyMatch(auth -> "LIBRARIAN".equals(auth.getAuthority()));
```

**INCORRECT** - Do NOT use `.contains()` with new SimpleGrantedAuthority:
```java
// DON'T DO THIS - may fail due to object equality issues
boolean isLibrarian = authentication.getAuthorities().contains(new SimpleGrantedAuthority("LIBRARIAN"));
```

Why? The `.contains()` method relies on object equality, which can fail depending on how authorities are loaded (database entities vs SimpleGrantedAuthority instances). SecurityUtils and the stream-based approach check the authority string value, which is reliable regardless of implementation.

**SecurityUtils Benefits:**
- Encapsulates the correct authority checking logic
- Null-safe (handles null authentication and authorities)
- Prevents common bugs from incorrect authority checks
- Provides a consistent API across the codebase
- See `com.muczynski.library.util.SecurityUtils` for implementation

## Authentication Methods

### Form-Based Login
- Client-side SHA-256 password hashing before transmission
  - Avoids BCrypt 72-byte limit
  - Implementation in `frontend/src/utils/auth.ts` `hashPassword()` function (React)
  - Previously in `utils.js` (legacy vanilla JS)
- Hashed passwords stored with BCrypt in database
- Custom success handler returns 200 OK with JSON (no redirect)
- React implementation: LoginPage handles authentication via authStore

### Google OAuth2 SSO
- Dynamically configured via database settings (`GlobalSettings`)
- Falls back to environment variables if not set in database
- OAuth2 flow for Google Photos authorization at `/api/oauth/google/authorize`
- Scopes include full Google Photos Library access

### OAuth Principal Name Handling
- Both OIDC and OAuth2 logins use **database user ID** as the principal name
- `authentication.getName()` returns the database user ID (as a string), NOT the OAuth subject ID
- Custom user classes ensure consistent behavior:
  - `CustomOidcUser` - For OpenID Connect logins (e.g., Google with OIDC)
  - `CustomOAuth2User` - For plain OAuth2 logins (e.g., Google without OIDC)
- Services can directly parse the user ID: `Long userId = Long.parseLong(authentication.getName())`
- OAuth subject ID is still stored in `User.ssoSubjectId` for user lookup during login

## Public Endpoints
- Test data generation endpoints (`/api/test-data/**`)
- Book/author/library listings
- Search functionality

## UI Visibility Rules

### React Component-Based Access Control
The frontend uses React components to control access based on authentication and authorization:

- **`<ProtectedRoute />`**: Wraps routes that require authentication (any authority)
- **`<LibrarianRoute />`**: Wraps routes that require LIBRARIAN authority
- **Conditional rendering**: Components check `isLibrarian` from authStore for conditional UI
  - Example: Bulk delete buttons only shown to librarians
  - Example: "Apply for Library Card" shown only to unauthenticated users

### Legacy CSS Classes (Vanilla JS - Deprecated)
The old vanilla JavaScript implementation used CSS classes:
- **`librarian-only`**: Hidden for non-librarian users
- **`public-item`**: Visible to unauthenticated users
- **`librarian-or-unauthenticated`**: Visible only to LIBRARIAN and unauthenticated users

These classes are no longer used in the React implementation.

## Security Configuration
- All REST controllers use `@RestController` with `/api/*` paths
- `@PreAuthorize("hasAuthority('LIBRARIAN')")` for librarian-only endpoints
- `@PreAuthorize("isAuthenticated()")` for authenticated endpoints (both authorities)
- Global exception handling returns consistent error responses
- CORS configured to allow specific origins
- Credentials allowed for authenticated requests

## Related Files

### Backend
- `SecurityConfig.java` - Spring Security configuration
- `CustomUserDetailsService.java` - User details loading for form-based login
- `CustomOidcUserService.java` - OIDC user service (returns CustomOidcUser)
- `CustomOAuth2UserService.java` - OAuth2 user service (returns CustomOAuth2User)
- `CustomOidcUser.java` - Custom OIDC user with database ID as principal
- `CustomOAuth2User.java` - Custom OAuth2 user with database ID as principal
- `GoogleOAuthController.java` - OAuth endpoints
- `sso.md` - Google OAuth SSO configuration details

### Frontend (React)
- `frontend/src/stores/authStore.ts` - Authentication state management (Zustand)
- `frontend/src/components/auth/ProtectedRoute.tsx` - Authenticated route wrapper
- `frontend/src/components/auth/LibrarianRoute.tsx` - Librarian-only route wrapper
- `frontend/src/pages/LoginPage.tsx` - Login page component
- `frontend/src/utils/auth.ts` - Auth utilities (hashPassword)
- `frontend/src/App.tsx` - Route configuration with protection

### Legacy (Vanilla JS - Deprecated)
- `auth.js` - Old login/logout handling (replaced by authStore.ts)
- `init.js` - Old authentication check (replaced by App.tsx useEffect)
