# Global Settings Endpoints

## GET /api/global-settings
Returns application-wide global settings including OAuth credentials configuration.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Response:** GlobalSettingsDto with OAuth configuration information

```json
{
  "googleClientId": "123456789.apps.googleusercontent.com",
  "redirectUri": "https://library.example.com/api/oauth/google/callback",
  "googleClientSecretPartial": "...uXnb",
  "googleClientSecretConfigured": true,
  "googleClientSecretValidation": "Valid",
  "googleClientSecretUpdatedAt": "2025-01-15T10:30:00Z",
  "googleSsoClientId": "987654321.apps.googleusercontent.com",
  "googleSsoClientSecretPartial": "...Xyz9",
  "googleSsoClientSecretConfigured": true,
  "googleSsoClientIdConfigured": true,
  "googleSsoCredentialsUpdatedAt": "2025-01-15T10:30:00Z",
  "lastUpdated": "2025-01-15T10:30:00Z"
}
```

**Security Notes:**
- Full Client Secrets are NEVER returned in responses - only partial display (last 4 characters)
- `googleClientSecret` and `googleSsoClientSecret` fields will always be null in responses
- Regular users (USER authority) will receive 403 Forbidden

---

## PUT /api/global-settings
Updates application-wide global settings.

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** GlobalSettingsDto (all fields optional)

```json
{
  "googleClientSecret": "GOCSPX-newSecretValue123456789",
  "googleClientId": "123456789.apps.googleusercontent.com",
  "redirectUri": "https://library.example.com/api/oauth/google/callback",
  "googleSsoClientId": "987654321.apps.googleusercontent.com",
  "googleSsoClientSecret": "GOCSPX-newSsoSecret123456789"
}
```

**Response:** Updated GlobalSettingsDto (same format as GET endpoint)

**Notes:**
- All fields are optional - only provide fields you want to update
- Empty or null values will NOT update existing values (existing values are preserved)
- Google Photos API credentials:
  - `googleClientId`: OAuth Client ID for Google Photos API
  - `googleClientSecret`: OAuth Client Secret for Google Photos API (write-only)
  - `redirectUri`: OAuth redirect URI
- Google SSO credentials (separate from Photos API):
  - `googleSsoClientId`: OAuth Client ID for user authentication via Google SSO
  - `googleSsoClientSecret`: OAuth Client Secret for SSO (write-only)
- Secret validation checks for proper format (GOCSPX- prefix, minimum length)
- Warnings are returned in `googleClientSecretValidation` field if format is suspicious

**Validation:**
- Client Secrets are validated for GOCSPX- prefix (Google OAuth standard)
- Secrets shorter than 20 characters trigger a warning
- Invalid formats are accepted but validation warnings are returned

**Error Responses:**
- 403: User does not have LIBRARIAN authority
- 401: User is not authenticated

**Fallback Behavior:**
- Settings can be configured via database (this endpoint), environment variables, or application.properties
- Priority order: Database > Environment Variable > Config File
- Use `getEffectiveClientId()` and `getEffectiveClientSecret()` to get the active value from any source

---

## GET /api/global-settings/sso-status
Check if Google SSO is configured and available for login.

**Authentication:** None (public endpoint)

**Response:** SsoStatusDto

```json
{
  "ssoConfigured": true
}
```

**Notes:**
- This endpoint is intentionally public (no authentication required)
- Used by the login page to determine whether to show the "Sign in with Google" button
- Returns true only if BOTH `googleSsoClientId` and `googleSsoClientSecret` are configured

**Use Case:**
Frontend login page calls this endpoint on page load to show/hide SSO login button:
```javascript
const response = await fetch('/api/global-settings/sso-status');
const { ssoConfigured } = await response.json();
if (ssoConfigured) {
  // Show "Sign in with Google" button
}
```

---

**Related Files:**
- `GlobalSettingsController.java` - REST endpoint controller
- `GlobalSettingsService.java` - Business logic and credential management
- `GlobalSettingsDto.java` - Data transfer object
- `GlobalSettings.java` - JPA entity (database model)
- `GlobalSettingsMapper.java` - MapStruct mapper for entity-DTO conversion
- `GlobalSettingsRepository.java` - JPA repository
