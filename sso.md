# Google SSO Integration Design Document

## Overview

This document outlines the design for adding Google Single Sign-On (SSO) authentication to the library application. The implementation will allow users to authenticate using their Google accounts while maintaining backward compatibility with existing username/password authentication.

## Goals

1. Add Google SSO as an authentication option on the login page
2. Store SSO users in the existing `users` table alongside traditional users
3. Allow librarians to manage authorities (roles) for SSO users
4. Display SSO provider information on the Users admin page
5. Maintain existing username/password authentication
6. Reuse existing Google OAuth infrastructure where possible

## Current State

### Authentication System
- **Framework**: Spring Boot 3.5+ with Spring Security
- **Current Auth Method**: Form-based username/password authentication
- **Password Hashing**: SHA-256 on frontend, BCrypt on backend
- **User Details Service**: `CustomUserDetailsService` loads users from database
- **Roles**: Two roles exist - `LIBRARIAN` and `USER`

### Existing Google OAuth Integration
The application already has Google OAuth implemented for Google Photos integration:
- **Controller**: `GoogleOAuthController.java`
- **Purpose**: Access user's Google Photos library for book cover imports
- **OAuth Scopes**: Google Photos scopes
- **Storage**: OAuth tokens stored in `User` entity fields

### Database Schema
**User Table** (`users`):
- `id` (PK)
- `username`
- `password` (BCrypt hash)
- `xaiApiKey`
- `googlePhotosApiKey` (OAuth access token)
- `googlePhotosRefreshToken`
- `googlePhotosTokenExpiry`
- `googleClientSecret`
- `googlePhotosAlbumId`
- `lastPhotoTimestamp`

**Role Table** (`role`):
- `id` (PK)
- `name` (USER or LIBRARIAN)

**Junction Table** (`users_roles`):
- `user_id` (FK)
- `role_id` (FK)

## Proposed Design

### 1. Database Changes

Add new fields to the `User` entity to track SSO authentication:

```java
@Column(name = "sso_provider")
private String ssoProvider;  // "google", "local", or null (legacy)

@Column(name = "sso_subject_id")
private String ssoSubjectId;  // OAuth "sub" claim (unique user ID from provider)

@Column(name = "email")
private String email;  // Email address from SSO provider
```

**Rationale**:
- `ssoProvider`: Distinguishes between local (username/password) and SSO users
- `ssoSubjectId`: Unique identifier from OAuth provider, used for linking accounts
- `email`: Stores the user's email from Google, useful for user management and as a fallback identifier

**Migration Strategy**:
- Existing users will have `ssoProvider = "local"` or `null`
- SSO users will have `ssoProvider = "google"`
- `password` field can be null for SSO users (they don't need passwords)

### 2. Spring Security Configuration

Update `SecurityConfig.java` to support both form-based and OAuth2 authentication:

**Changes Required**:
1. Add `spring-boot-starter-oauth2-client` dependency
2. Configure OAuth2 login in security filter chain
3. Create custom `OAuth2UserService` to handle Google user info
4. Map Google OAuth users to application `User` entities
5. Keep existing form login configuration

**Security Filter Chain**:
```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/users/**").hasAuthority("LIBRARIAN")
        // ... existing rules
    )
    .formLogin(form -> form
        .loginPage("/login")
        .defaultSuccessUrl("/")
        .failureHandler(customAuthenticationFailureHandler())
    )
    .oauth2Login(oauth2 -> oauth2
        .loginPage("/login")
        .defaultSuccessUrl("/")
        .userInfoEndpoint(userInfo -> userInfo
            .userService(customOAuth2UserService())
        )
        .failureHandler(customOAuth2AuthenticationFailureHandler())
    )
```

### 3. OAuth2 User Service

Create `CustomOAuth2UserService` to:
1. Receive Google user information after successful OAuth2 authentication
2. Extract user details (email, name, sub)
3. Check if user exists by `ssoSubjectId`
4. If new user, create account with default `USER` role
5. Return Spring Security user principal

**User Matching Logic**:
- First, try to find user by `ssoProvider = "google"` AND `ssoSubjectId = {Google sub}`
- If not found, create new user with:
  - `username` = email from Google
  - `ssoProvider` = "google"
  - `ssoSubjectId` = Google "sub" claim
  - `email` = email from Google
  - `password` = null (not needed for SSO)
  - Default role: `USER`

### 4. Application Configuration

Add OAuth2 client configuration to `application.properties`:

```properties
# Google SSO Configuration
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_SSO_CLIENT_ID:}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_SSO_CLIENT_SECRET:}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google

spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo
spring.security.oauth2.client.provider.google.user-name-attribute=sub
```

**Note**: This is separate from the existing Google Photos OAuth configuration. The SSO configuration uses:
- Different scopes: `openid`, `profile`, `email` (standard OpenID Connect)
- Different redirect URI: `/login/oauth2/code/google` (Spring Security default)
- Standard Spring Security OAuth2 client autoconfiguration

### 5. Frontend Changes

#### Login Page (`index.html`)

Add Google SSO button to the login form:

```html
<div id="login-form" data-test="login-form" class="col-md-6 offset-md-3" style="display: none;">
    <h2 data-test="login-header">Login</h2>

    <!-- Traditional Login Form -->
    <form id="login" name="login" action="/login" method="post">
        <div class="mb-3">
            <label for="username" class="form-label">Name:</label>
            <input type="text" id="username" name="username" class="form-control" autocomplete="username" required data-test="login-username">
        </div>
        <div class="mb-3">
            <label for="password" class="form-label">Password:</label>
            <input type="password" id="password" name="password" class="form-control" autocomplete="current-password" required data-test="login-password">
        </div>
        <button type="submit" name="login-submit" class="btn btn-primary" data-test="login-submit">Login</button>
    </form>

    <!-- Divider -->
    <div class="text-center my-3">
        <span class="text-muted">or</span>
    </div>

    <!-- Google SSO Button -->
    <div class="d-grid">
        <a href="/oauth2/authorization/google" class="btn btn-outline-primary" data-test="google-sso-button">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-google" viewBox="0 0 16 16">
              <path d="M15.545 6.558a9.4 9.4 0 0 1 .139 1.626c0 2.434-.87 4.492-2.384 5.885h.002C11.978 15.292 10.158 16 8 16A8 8 0 1 1 8 0a7.7 7.7 0 0 1 5.352 2.082l-2.284 2.284A4.35 4.35 0 0 0 8 3.166c-2.087 0-3.86 1.408-4.492 3.304a4.8 4.8 0 0 0 0 3.063h.003c.635 1.893 2.405 3.301 4.492 3.301 1.078 0 2.004-.276 2.722-.764h-.003a3.7 3.7 0 0 0 1.599-2.431H8v-3.08z"/>
            </svg>
            Sign in with Google
        </a>
    </div>

    <div id="login-error" class="alert alert-danger mt-3" style="display: none;" data-test="login-error"></div>
</div>
```

#### Users Admin Page (`index.html` + `users.js`)

Add SSO provider column to the users table:

```html
<table class="table" data-test="user-table">
    <thead>
        <tr>
            <th>Name</th>
            <th>Role</th>
            <th>SSO</th>  <!-- New column -->
            <th>Actions</th>
        </tr>
    </thead>
    <tbody id="user-table-body">
        <!-- Populated by users.js -->
    </tbody>
</table>
```

Update `users.js` to display SSO provider:

```javascript
function displayUsers(users) {
    const tbody = document.getElementById('user-table-body');
    tbody.innerHTML = '';

    users.forEach(user => {
        const row = tbody.insertRow();
        row.innerHTML = `
            <td>${user.username}</td>
            <td>${user.roleName}</td>
            <td>${getSsoProviderBadge(user.ssoProvider)}</td>
            <td>
                <button onclick="editUser(${user.id})" class="btn btn-sm btn-primary">Edit</button>
                <button onclick="deleteUser(${user.id})" class="btn btn-sm btn-danger">Delete</button>
            </td>
        `;
    });
}

function getSsoProviderBadge(ssoProvider) {
    if (!ssoProvider || ssoProvider === 'local') {
        return '<span class="badge bg-secondary">Local</span>';
    }
    if (ssoProvider === 'google') {
        return '<span class="badge bg-primary">Google</span>';
    }
    return `<span class="badge bg-info">${ssoProvider}</span>`;
}
```

### 6. DTO Changes

Update `UserDto` to include SSO fields:

```java
public class UserDto {
    private Long id;
    private String username;
    private String password;  // Optional for SSO users
    private String roleName;
    private String ssoProvider;  // New field
    private String email;  // New field
    // ... getters/setters
}
```

### 7. Service Layer Changes

Update `UserService` to:
1. Handle creation of SSO users (no password required)
2. Prevent password changes for SSO users via admin UI
3. Support finding users by `ssoSubjectId` and `ssoProvider`
4. Add validation: SSO users cannot have their `ssoProvider` changed

### 8. Security Considerations

1. **Account Linking**:
   - Do NOT automatically link Google accounts to existing username/password accounts by email
   - Each authentication method creates separate user records
   - Prevents security issues if email addresses are reused

2. **Password Requirements**:
   - SSO users should have `password = null`
   - Backend validation should allow null passwords only when `ssoProvider != "local"`

3. **OAuth Client Credentials**:
   - Store in environment variables: `GOOGLE_SSO_CLIENT_ID`, `GOOGLE_SSO_CLIENT_SECRET`
   - Document setup instructions in README

4. **Redirect URI**:
   - Must be registered in Google Cloud Console
   - Development: `http://localhost:8080/login/oauth2/code/google`
   - Production: `https://yourdomain.com/login/oauth2/code/google`

5. **Session Management**:
   - Both form login and OAuth2 login use same session mechanism
   - No changes needed to existing session handling

## Implementation Plan

### Phase 1: Database & Domain Layer
1. Add new columns to `User` entity
2. Create database migration (if using Flyway/Liquibase) or update schema
3. Update `UserDto` with new fields
4. Update mappers to handle new fields

### Phase 2: Spring Security & OAuth2
1. Add `spring-boot-starter-oauth2-client` dependency to `build.gradle`
2. Create `CustomOAuth2UserService` class
3. Update `SecurityConfig` to enable OAuth2 login
4. Add OAuth2 configuration to `application.properties`
5. Create error handlers for OAuth2 failures

### Phase 3: Service Layer
1. Update `UserService` to support SSO user creation
2. Add methods for finding users by `ssoSubjectId`
3. Add validation for SSO vs local users

### Phase 4: Frontend
1. Add Google SSO button to login page
2. Add SSO provider column to Users admin table
3. Update `users.js` to display SSO badges
4. Handle OAuth2 errors in UI

### Phase 5: Testing & Documentation
1. Test traditional login (should still work)
2. Test Google SSO login (new user creation)
3. Test Google SSO login (returning user)
4. Test librarian can modify roles for SSO users
5. Update README with Google OAuth setup instructions
6. Test error scenarios (OAuth failure, network issues, etc.)

## Configuration Requirements

To enable Google SSO, administrators must:

1. Create a Google Cloud Project
2. Enable Google+ API
3. Create OAuth 2.0 credentials (Web application)
4. Add authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google` (dev)
   - `https://your-domain.com/login/oauth2/code/google` (prod)
5. Set environment variables:
   - `GOOGLE_SSO_CLIENT_ID`
   - `GOOGLE_SSO_CLIENT_SECRET`

## Backward Compatibility

- Existing users with username/password authentication continue to work
- No changes to existing login behavior
- SSO is an additional option, not a replacement
- Existing `CustomUserDetailsService` remains unchanged
- All existing endpoints and authorization rules remain the same

## Future Enhancements

1. **Multiple SSO Providers**: Design supports adding more providers (Microsoft, GitHub, etc.)
2. **Account Linking**: Allow users to link Google account to existing username/password account
3. **SSO-Only Mode**: Configuration option to disable traditional login
4. **Email Verification**: Send verification emails to SSO users
5. **Profile Picture**: Store and display user's Google profile picture

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| OAuth credentials leaked | Use environment variables, never commit to git |
| User creates multiple accounts | Document that Google SSO creates separate accounts |
| Google API downtime | Traditional login remains available as fallback |
| Redirect URI misconfiguration | Provide clear setup documentation with examples |
| Password field validation breaks | Update validation to allow null passwords for SSO users |

## Acceptance Criteria

- [ ] Users can click "Sign in with Google" on login page
- [ ] Successful Google authentication creates new user account with USER authority
- [ ] SSO users appear in Users admin page with "Google" badge
- [ ] Librarians can change authorities for SSO users
- [ ] Traditional username/password login still works
- [ ] SSO users cannot login with username/password
- [ ] Existing users are not affected by changes
- [ ] Application works with missing OAuth credentials (SSO button hidden)

## Open Questions

1. Should we allow librarians to manually create SSO users via the admin UI?
   - **Recommendation**: No, SSO users should only be created via actual SSO login

2. What happens if a user's Google account is deleted?
   - **Recommendation**: User remains in database but cannot login; librarian can delete if needed

3. Should we sync user information (email, name) on each login?
   - **Recommendation**: Yes, update email on each login to keep data current

4. Should the first Google SSO user be automatically granted LIBRARIAN authority?
   - **Recommendation**: No, require manual authority assignment for security

5. Should we prevent librarians from deleting SSO users?
   - **Recommendation**: No, librarians should be able to delete any user for account management
