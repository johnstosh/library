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
  "lastUpdated": "2025-01-15T10:30:00Z",
  "emailMethod": "DISABLED",
  "emailFromAddress": "library@example.com",
  "emailFromName": "Library",
  "emailNotifyLibrariansOnPending": true,
  "emailNotifyApplicantOnPending": false,
  "emailLibrarianRecipients": "librarian@example.com",
  "emailIncludeLibrarianUserEmails": true,
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "library@example.com",
  "smtpPasswordPartial": "...word",
  "smtpPasswordConfigured": true,
  "smtpStartTls": true,
  "smtpSsl": false,
  "sendGridApiKeyPartial": "(not configured)",
  "sendGridApiKeyConfigured": false,
  "webhookUrl": "",
  "webhookBearerTokenPartial": "(not configured)",
  "webhookBearerTokenConfigured": false,
  "emailMethodConfigured": true,
  "emailMethodStatus": "Disabled — no email will be sent"
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
  "googleSsoClientSecret": "GOCSPX-newSsoSecret123456789",
  "emailMethod": "SMTP",
  "emailFromAddress": "library@example.com",
  "emailFromName": "Library",
  "emailNotifyLibrariansOnPending": true,
  "emailNotifyApplicantOnPending": false,
  "emailLibrarianRecipients": "librarian@example.com",
  "emailIncludeLibrarianUserEmails": true,
  "smtpHost": "smtp.gmail.com",
  "smtpPort": 587,
  "smtpUsername": "library@example.com",
  "smtpPassword": "app-password",
  "smtpStartTls": true,
  "smtpSsl": false
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

---

## POST /api/global-settings/test-email
Send a test message using the **currently saved** email method (not unsaved form values).

**Authentication:** Librarian only (`hasAuthority('LIBRARIAN')`)

**Request Body:** optional
```json
{
  "to": "you@example.com"
}
```

If `to` is omitted, the configured librarian recipients are used.

**Response:** TestEmailResultDto
```json
{
  "sent": true,
  "method": "LOG",
  "message": "Test email sent via LOG to [librarian@example.com]",
  "recipients": ["librarian@example.com"]
}
```

`sent` is false when the method is DISABLED, the transport is missing required fields, or the provider returns an error. HTTP status is still 200 so the UI can show the reason.

---

## Email methods

Configured on `GlobalSettings.emailMethod`:

| Method | What it does | When to use |
|--------|----------------|-------------|
| `DISABLED` | No email is sent (default) | Fresh install / until a transport is chosen |
| `LOG` | Writes subject/body/recipients to application logs | Verify the pending-application flow in Cloud Run or local logs |
| `SMTP` | Jakarta Mail SMTP at send time from these settings | Gmail app password, Fastmail, Mailgun SMTP. Use port 587 or 465; Cloud Run blocks 25 |
| `SENDGRID` | HTTPS POST to `https://api.sendgrid.com/v3/mail/send` | Cloud Run-friendly transactional email |
| `WEBHOOK` | HTTPS POST of JSON (`event`, `to`, `subject`, `text`, `html`, `payload`) | Zapier, n8n, Make, Apps Script, or a Cloud Function |

Librarian pending-application mail is sent after a public register saves an `Applied` row with status `PENDING`. Delivery failures are logged and never roll back the application.

**Related Files:**
- `GlobalSettingsController.java` - REST endpoint controller
- `GlobalSettingsService.java` - Business logic and credential management
- `ApplicationEmailService.java` - Pending-application and test-email dispatch
- `email/` - Transport implementations (`LogEmailSender`, `SmtpEmailSender`, `SendGridEmailSender`, `WebhookEmailSender`)
- `GlobalSettingsDto.java` - Data transfer object
- `GlobalSettings.java` - JPA entity (database model)
- `GlobalSettingsMapper.java` - MapStruct mapper for entity-DTO conversion
- `GlobalSettingsRepository.java` - JPA repository
