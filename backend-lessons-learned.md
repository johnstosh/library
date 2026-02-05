# Development Requirements

## Lessons Learned

### Spring Security Authorization Annotations

**Issue**: Authorization failures when using `@PreAuthorize("hasRole('LIBRARIAN')")` despite users having the `LIBRARIAN` role.

**Root Cause**: Spring Security's `hasRole()` method expects roles to be prefixed with `ROLE_`. When checking `hasRole('LIBRARIAN')`, Spring Security looks for `ROLE_LIBRARIAN` in the authorities collection, not `LIBRARIAN`.

**Solution**: Use `hasAuthority()` instead of `hasRole()` when your role names in the database don't have the `ROLE_` prefix.

**Example**:

```java
// INCORRECT - This looks for "ROLE_LIBRARIAN"
@PreAuthorize("hasRole('LIBRARIAN')")

// CORRECT - This looks for "LIBRARIAN"
@PreAuthorize("hasAuthority('LIBRARIAN')")
```

**Best Practice**:
- Maintain consistency across all controllers in the application
- If using `hasAuthority()`, use it everywhere
- If using `hasRole()`, ensure all role names in the database are prefixed with `ROLE_`

**Reference**:
- File: `src/main/java/com/muczynski/library/controller/BooksFromFeedController.java:16`
- All other controllers in the application use `hasAuthority()` pattern

---

### Database Unique Constraint Warnings with Hibernate ddl-auto=update

These "constraint does not exist, skipping" warnings have **two distinct causes**. They look identical in the logs but arise from different problems.

#### Cause 1: Dual Constraint Definitions (fixed)

**Symptom**: Warnings with auto-generated constraint names like `ukhl8cmkyvgqsgelu76p0x79wjb`.

**Root Cause**: Using both `@Column(unique = true)` on a field AND `@UniqueConstraint` at the `@Table` level creates two competing constraint definitions. Hibernate generates an auto-named constraint from `@Column(unique = true)` and a named constraint from `@UniqueConstraint`. During schema update, Hibernate tries to drop constraints by specific names that may not match what actually exists in the database.

**What Didn't Work**:
- Having both `@Column(unique = true)` and `@UniqueConstraint` on the same column — creates redundant constraints with unpredictable names
- Using only `@Column(unique = true)` — constraint name is auto-generated and uncontrollable, making it impossible to reference or manage

**Solution**: Use ONLY `@UniqueConstraint` at the `@Table` level with an explicit name. Remove `@Column(unique = true)` from the field. This gives Hibernate a single, predictable constraint name that matches across environments.

```java
// INCORRECT - Dual constraint, causes warnings on any database
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_author_name", columnNames = "name")
})
public class Author {
    @Column(unique = true)  // BAD: creates second auto-named constraint
    private String name;
}

// CORRECT - Single named constraint
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_author_name", columnNames = "name")
})
public class Author {
    private String name;  // No @Column(unique = true)
}
```

**Best Practice**:
- Always use `@UniqueConstraint` at `@Table` level with explicit `name` parameter
- Never combine `@Column(unique = true)` with `@UniqueConstraint` on the same column
- Use descriptive constraint names like `uk_entity_field` for easy debugging

**Status**: All entities now use `@UniqueConstraint` only. `PhotoUploadSession` was the last entity fixed (had `@Column(unique = true)` without a table-level constraint, producing auto-named constraint `uk9mm1kvw0ep5gboxxky2e21pbj`).

#### Cause 2: Fresh Database with ddl-auto=update (cosmetic, harmless)

**Symptom**: Warnings with properly named constraints like `uk_author_name`, `uk_book_title`, `uk_applied_name`, etc. — appearing on Docker test runs or any fresh Testcontainers database.

**Root Cause**: This is unrelated to dual constraints. Hibernate's `ddl-auto=update` strategy always runs `ALTER TABLE ... DROP CONSTRAINT IF EXISTS "uk_xyz"` before adding each unique constraint. On a **fresh database** (like Testcontainers creates for every test run, or `docker-test.sh` with a new container), the constraints don't exist yet because the tables were just created. PostgreSQL responds with a NOTICE that the constraint doesn't exist, which Hibernate logs as a WARN.

**Why it only appears on fresh databases**: On an existing database (like the dev Docker volume or production Cloud SQL), the constraints already exist from a previous run, so the DROP succeeds silently and no warning is logged.

**This is expected behavior** — the constraints are still created correctly after the failed drop. The warnings are cosmetic.

**Options to suppress** (if desired):
- Use `ddl-auto=create` instead of `update` for Docker test environments — creates schema from scratch without the drop-then-recreate dance
- Set log level `logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=ERROR` in test properties
- Accept them as harmless (current approach)

---

### Form Field Persistence Issue

**Issue**: User settings fields (like Google Photos API key) were not being saved or would disappear after saving.

**Root Cause**: Inconsistent handling of optional fields between frontend and backend:
1. Frontend was conditionally including `googlePhotosApiKey` in the request payload only if it had a value, while always including `xaiApiKey`
2. Backend was only updating fields if they contained text using `StringUtils.hasText()`, which prevented clearing values

**Solution**:
1. **Frontend**: Always include all optional fields in the payload, even if empty (for consistency)
2. **Backend**: Check for `null` instead of using `StringUtils.hasText()` for fields that users should be able to clear

**Example**:

```javascript
// INCORRECT - Conditional inclusion
const payload = { username, xaiApiKey };
if (googlePhotosApiKey) {
    payload.googlePhotosApiKey = googlePhotosApiKey;
}

// CORRECT - Always include
const payload = {
    username,
    xaiApiKey,
    googlePhotosApiKey
};
```

```java
// INCORRECT - Prevents clearing values
if (StringUtils.hasText(dto.getGooglePhotosApiKey())) {
    user.setGooglePhotosApiKey(dto.getGooglePhotosApiKey());
}

// CORRECT - Allows clearing and updating
if (dto.getGooglePhotosApiKey() != null) {
    user.setGooglePhotosApiKey(dto.getGooglePhotosApiKey());
}
```

**Best Practice**:
- Be consistent in how optional fields are handled across the codebase
- Always include fields in request payloads to ensure they can be cleared
- Use `null` checks instead of `hasText()` for clearable fields
- Use `hasText()` only for required fields or validation

**Reference**:
- Files: `src/main/resources/static/js/settings.js:32-36`, `src/main/java/com/muczynski/library/service/UserSettingsService.java:49-55`

---

### DTO Mapper Missing Fields

**Issue**: User settings fields (like Google Photos API key) were being saved to the database but not returned when retrieving user settings.

**Root Cause**: The `UserMapper.toDto()` method was missing mappings for newly added fields (`googlePhotosApiKey`, `lastPhotoTimestamp`). While the Entity had these fields and they were being saved correctly, the mapper wasn't transferring them to the DTO when loading data.

**Solution**: Always update all mapper classes when adding new fields to domain entities. Ensure every field in the DTO has a corresponding setter call in the `toDto()` method.

**Example**:

```java
// INCORRECT - Missing new fields
public UserDto toDto(User user) {
    UserDto dto = new UserDto();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setXaiApiKey(user.getXaiApiKey());
    // Missing: googlePhotosApiKey, lastPhotoTimestamp
    return dto;
}

// CORRECT - All fields mapped
public UserDto toDto(User user) {
    UserDto dto = new UserDto();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    dto.setXaiApiKey(user.getXaiApiKey());
    dto.setGooglePhotosApiKey(user.getGooglePhotosApiKey());
    dto.setLastPhotoTimestamp(user.getLastPhotoTimestamp());
    return dto;
}
```

**Best Practice**:
- When adding new fields to an Entity, update ALL related code:
  1. Entity class (domain model)
  2. DTO class (data transfer object)
  3. Mapper class (toDto and fromDto methods)
  4. Service class (save/update logic)
  5. Test data SQL files (INSERT statements)
- Write both API tests (backend) and UI tests (end-to-end) to catch mapper issues
- API tests verify backend logic works correctly
- UI tests verify the full workflow including frontend-backend integration

**Reference**:
- File: `src/main/java/com/muczynski/library/mapper/UserMapper.java:21-22`
- Test: `src/test/java/com/muczynski/library/service/UserSettingsServiceTest.java`
- Test: `src/test/java/com/muczynski/library/ui/SettingsUITest.java:testGooglePhotosApiKeyPersistenceAcrossNavigation()`

---

### Google Photos OAuth 2.0 Integration

**Overview**: The application implements OAuth 2.0 authorization code flow with automatic token refresh for Google Photos integration. Users authorize the app through their Google account, and the app securely manages access tokens and refresh tokens.

---

#### Environment Setup

**Required Environment Variable**:

```bash
export GOOGLE_CLIENT_SECRET="your_google_client_secret_here"
```

This environment variable MUST be set before running the application. The OAuth flow will fail without it.

**For Docker/Production**:
```bash
# Add to your environment or docker-compose.yml
GOOGLE_CLIENT_SECRET=your_google_client_secret_here
```

**For Local Development**:
```bash
# Add to your shell profile (~/.bashrc, ~/.zshrc, etc.)
export GOOGLE_CLIENT_SECRET="your_google_client_secret_here"
```

---

#### Google Cloud Console Configuration

1. **Enable Google Photos Library API**:
   - Go to [Google Cloud Console](https://console.cloud.google.com)
   - Navigate to "APIs & Services" > "Library"
   - Search for "Photos Library API"
   - Click "Enable"

2. **Configure OAuth 2.0 Credentials**:
   - Go to "APIs & Services" > "Credentials"
   - Find your OAuth 2.0 Client ID: `422211234280-glqprc2nvm2nkr6t2olv0sf4jegqqq0h.apps.googleusercontent.com`
   - Click "Edit" on the OAuth 2.0 Client

3. **Add Authorized Redirect URIs**:
   - For local development: `http://localhost:8080/api/oauth/google/callback`
   - For production: `https://your-domain.com/api/oauth/google/callback`
   - Click "Save"

4. **Verify Scopes**:
   - The application requests: `https://www.googleapis.com/auth/photoslibrary.readonly`
   - This provides read-only access to the user's Google Photos

---

#### How to Use OAuth Flow (User Perspective)

1. **Login to Application**:
   - User must be authenticated first (logged in)

2. **Navigate to Settings**:
   - Click on "Settings" in the navigation
   - Scroll to "Google Photos Access" section

3. **Authorize Google Photos**:
   - Click "Authorize Google Photos" button
   - Browser redirects to Google's consent screen
   - Select Google account to use
   - Review permissions (read-only access to Google Photos)
   - Click "Allow"

4. **Automatic Redirect**:
   - Google redirects back to the application
   - Success message appears: "Google Photos authorized successfully!"
   - Status badge changes from "Not Authorized" to "Authorized"
   - Tokens are securely stored in the database

5. **Use Books-from-Feed Feature**:
   - Navigate to "Books from Feed"
   - Click "Process Feed"
   - Application will automatically use stored OAuth tokens
   - Tokens auto-refresh if expired (no re-authorization needed)

6. **Revoke Access** (Optional):
   - Click "Revoke Access" button in Settings
   - Tokens are cleared from the database
   - Status returns to "Not Authorized"

---

#### Architecture and Implementation

**OAuth Flow Components**:

1. **Controller**: `GoogleOAuthController.java`
   - `/api/oauth/google/authorize` - Initiates OAuth flow, redirects to Google
   - `/api/oauth/google/callback` - Handles Google's callback with authorization code
   - `/api/oauth/google/revoke` - Clears stored tokens

2. **Service**: `GooglePhotosService.java`
   - `getValidAccessToken()` - Returns valid access token, auto-refreshes if needed
   - `refreshAccessToken()` - Uses refresh token to get new access token
   - All API methods use `getValidAccessToken()` for automatic token management

3. **Database Storage** (User entity):
   - `googlePhotosApiKey` - Current OAuth access token
   - `googlePhotosRefreshToken` - Long-lived refresh token (never expires)
   - `googlePhotosTokenExpiry` - ISO 8601 timestamp when access token expires

4. **Frontend**: `settings.js` and `index.html`
   - OAuth status badges (Authorized/Not Authorized)
   - Authorize and Revoke buttons
   - Handles OAuth callback messages (success/error)

**Security Features**:

- **CSRF Protection**: Uses state tokens to prevent CSRF attacks
- **Secure Storage**: Tokens stored in database, never exposed to client
- **Auto-Refresh**: Access tokens refresh automatically when within 5 minutes of expiry
- **Offline Access**: Uses `access_type=offline` to get refresh token
- **Forced Consent**: Uses `prompt=consent` to always get refresh token

**Token Lifecycle**:

1. **Initial Authorization**:
   - User clicks "Authorize"
   - App redirects to Google with state token
   - User consents, Google returns authorization code
   - App exchanges code for access token + refresh token
   - Tokens stored in database

2. **Using Tokens**:
   - When API call is made, `getValidAccessToken()` checks expiry
   - If token expires in < 5 minutes, automatically refreshes
   - New access token stored, refresh token remains unchanged
   - API call proceeds with fresh token

3. **Token Refresh**:
   - Uses refresh token to get new access token
   - No user interaction required
   - Refresh token is long-lived (typically doesn't expire)
   - If refresh fails, user must re-authorize

**Error Handling**:

- **401 Unauthorized**: Token invalid or expired, auto-refresh attempted
- **No refresh token**: User shown error, must re-authorize
- **OAuth callback error**: Error message shown in Settings with details
- **Invalid state token**: CSRF protection triggered, authorization rejected

---

#### Testing the OAuth Flow

**Manual Testing Steps**:

1. **Start Application**:
   ```bash
   export GOOGLE_CLIENT_SECRET="your_secret"
   ./docker-test.sh
   ```

2. **Access Application**:
   - Navigate to `http://localhost:8080`
   - Login as a librarian user

3. **Test Authorization**:
   - Go to Settings
   - Verify "Not Authorized" badge is shown
   - Click "Authorize Google Photos"
   - Complete Google OAuth consent
   - Verify redirect back to app with success message
   - Verify "Authorized" badge is now shown

4. **Test Token Persistence**:
   - Navigate away from Settings
   - Return to Settings
   - Verify "Authorized" badge still shows

5. **Test API Usage**:
   - Go to "Books from Feed"
   - Click "Process Feed"
   - Verify no 401 errors in browser console
   - Should see successful API calls to Google Photos

6. **Test Revoke**:
   - Go to Settings
   - Click "Revoke Access"
   - Confirm revocation
   - Verify "Not Authorized" badge shows

**Automated Testing**:

Currently no automated tests exist for OAuth flow. Future tests should cover:
- OAuth authorization redirect
- Callback handling with valid code
- Token storage in database
- Automatic token refresh
- Revoke functionality

---

#### Configuration Reference

**application.properties**:
```properties
google.oauth.client-id=422211234280-glqprc2nvm2nkr6t2olv0sf4jegqqq0h.apps.googleusercontent.com
google.oauth.auth-uri=https://accounts.google.com/o/oauth2/auth
google.oauth.token-uri=https://oauth2.googleapis.com/token
google.oauth.redirect-uri=${BASE_URL:http://localhost:8080}/api/oauth/google/callback
google.oauth.scope=https://www.googleapis.com/auth/photoslibrary.readonly
```

**Environment Variables**:
- `GOOGLE_CLIENT_SECRET` (required) - OAuth client secret from Google Cloud Console
- `BASE_URL` (optional) - Base URL for redirect URI, defaults to `http://localhost:8080`

---

#### Common Issues and Solutions

**Issue**: "GOOGLE_CLIENT_SECRET environment variable not set"
- **Solution**: Set the environment variable before starting the application
- Check with: `echo $GOOGLE_CLIENT_SECRET`

**Issue**: "OAuth error: redirect_uri_mismatch"
- **Solution**: Add the exact redirect URI to Google Cloud Console authorized redirect URIs
- Local: `http://localhost:8080/api/oauth/google/callback`
- Production: Update `BASE_URL` environment variable

**Issue**: "No refresh token available. Please re-authorize"
- **Solution**: User needs to re-authorize through Settings
- This happens if refresh token was somehow lost or invalidated

**Issue**: "Failed to refresh access token"
- **Solution**: Check GOOGLE_CLIENT_SECRET is correct
- Check Google Cloud Console credentials are still valid
- User may need to re-authorize

---

#### Files Modified for OAuth Implementation

- `src/main/java/com/muczynski/library/domain/User.java` - Added OAuth token fields
- `src/main/java/com/muczynski/library/dto/UserDto.java` - Added OAuth token fields to DTO
- `src/main/java/com/muczynski/library/dto/UserSettingsDto.java` - Added OAuth fields
- `src/main/java/com/muczynski/library/controller/GoogleOAuthController.java` - NEW: OAuth endpoints
- `src/main/java/com/muczynski/library/service/GooglePhotosService.java` - Added auto-refresh logic
- `src/main/java/com/muczynski/library/service/UserSettingsService.java` - Handle OAuth token updates
- `src/main/resources/application.properties` - Added OAuth configuration
- `src/main/resources/static/index.html` - Added OAuth UI components
- `src/main/resources/static/js/settings.js` - Added OAuth flow functions
- All `src/test/resources/data-*.sql` files - Added OAuth token columns

**Reference**:
- Google Photos Library API: https://developers.google.com/photos/library/guides/get-started
- OAuth 2.0 Authorization Code Flow: https://developers.google.com/identity/protocols/oauth2/web-server

### Data Integrity: Duplicate Entity Prevention

**Issue**: `NonUniqueResultException: Query did not return a unique result` during JSON import when duplicate entities existed in the database.

**Root Cause**: Optional-returning repository methods (`findByName()`, `findByUsername()`, etc.) throw when multiple rows match. Duplicates accumulated over time from imports without uniqueness checks.

**Solution**: Multi-layered defense:
1. **Database unique constraints** on all natural keys (catches anything that slips through)
2. **Service-layer checks** before `save()` for key operations
3. **findOrCreate methods** for import and creation paths
4. **List-based lookups** (`findAllBy*OrderByIdAsc()`) that handle duplicates gracefully by selecting the oldest entity
5. **`DuplicateEntityException`** for enriched 409 CONFLICT responses

**Key Lesson**: Never use Optional-returning repository methods for natural-key lookups. Always use list-based queries with `OrderByIdAsc` and take the first result. Mark Optional methods as `@Deprecated`.

**Reference**: See `feature-design-data-integrity.md` for full details.
