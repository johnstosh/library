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

## Authentication Methods

### Form-Based Login
- Client-side SHA-256 password hashing before transmission
  - Avoids BCrypt 72-byte limit
  - Implementation in `utils.js` `hashPassword()` function
- Hashed passwords stored with BCrypt in database
- Custom success handler returns 200 OK with JSON (no redirect)
- Failure redirects to `/?error`

### Google OAuth2 SSO
- Dynamically configured via database settings (`GlobalSettings`)
- Falls back to environment variables if not set in database
- OAuth2 flow for Google Photos authorization at `/api/oauth/google/authorize`
- Scopes include full Google Photos Library access

### OAuth Subject ID Handling
- OAuth users are identified by their subject ID (not username) in `authentication.getName()`
- Services must handle lookups by both username and SSO subject ID
- Example: `LoanService.getLoansByUsername()` checks username first, then SSO subject ID

## Public Endpoints
- Test data generation endpoints (`/api/test-data/**`)
- Book/author/library listings
- Search functionality

## UI Visibility Rules

### CSS Classes for Access Control
- **`librarian-only`**: Hidden for non-librarian users (USER authority and unauthenticated)
- **`public-item`**: Visible to unauthenticated users
- **`librarian-or-unauthenticated`**: Visible only to LIBRARIAN and unauthenticated users (hidden from USER)
  - Example: "Apply for Library Card" section - users who already have cards (USER authority) don't need to see it

## Security Configuration
- All REST controllers use `@RestController` with `/api/*` paths
- `@PreAuthorize("hasAuthority('LIBRARIAN')")` for librarian-only endpoints
- `@PreAuthorize("isAuthenticated()")` for authenticated endpoints (both authorities)
- Global exception handling returns consistent error responses
- CORS configured to allow specific origins
- Credentials allowed for authenticated requests

## Related Files
- `SecurityConfig.java` - Spring Security configuration
- `CustomUserDetailsService.java` - User details loading
- `auth.js` - Frontend login/logout handling
- `init.js` - Authentication check on app load
- `sso.md` - Google OAuth SSO configuration details
