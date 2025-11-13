# Books-from-Feed Feature Work Items

## Implementation Status
- [ ] = Pending
- [IN PROGRESS] = Currently working on
- [✓] = Completed
- [BLOCKED] = Blocked (with reason)

---

## Task List (In Implementation Order)

### Phase 1: Understanding & Documentation

#### Task 1: Review Google Integration Code
- [✓] **Status:** COMPLETED
- **Description:** Review all code interfacing between the app and Google Photos
- **Files:**
  - GoogleOAuthController.java
  - GooglePhotosService.java
  - BooksFromFeedService.java
  - settings.js
- **Notes:** Code review complete via exploration

#### Task 2: Document Redirect URI Configuration
- [✓] **Status:** COMPLETED
- **Description:** Answer: What is the correct redirect URL and where to configure it in Google Cloud Console?
- **Answer:**
  - Redirect URI: `https://library.muczynskifamily.com/api/oauth/google/callback`
  - Configure in: Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 Client IDs
- **Deliverable:** Included in Google configuration guide

#### Task 3: Create Google Configuration Guide
- [✓] **Status:** COMPLETED
- **Description:** Make comprehensive document describing how to configure Google Cloud Console for this app
- **Deliverable:** `docs/google-oauth-setup.md` ✓ CREATED
- **Contents:**
  - Create project in Google Cloud Console ✓
  - Enable Google Photos Library API ✓
  - Create OAuth 2.0 credentials ✓
  - Configure redirect URIs ✓
  - Where to find Client ID and Client Secret ✓
  - Step-by-step instructions with detailed explanations ✓

#### Task 4: Identify Missing Configuration Values
- [✓] **Status:** COMPLETED
- **Description:** Review settings and identify what's missing or misconfigured
- **Deliverable:** `docs/configuration-analysis.md` ✓ CREATED
- **Key Findings:**
  - Client Secret architecture is unusual (per-user vs application-wide)
  - Missing configuration visibility in UI (no Client ID display, no redirect URI, no timestamps)
  - No validation or "Test Connection" capability
  - See configuration-analysis.md for full details

#### Task 5: Document Typical Configuration Problems
- [✓] **Status:** COMPLETED
- **Description:** Create list of common issues and how to detect them
- **Deliverable:** `docs/troubleshooting-google-oauth.md` ✓ CREATED
- **Included:**
  - redirect_uri_mismatch (current issue) ✓
  - Invalid client secret ✓
  - Missing API enablement ✓
  - Token expiration issues ✓
  - Scope permission problems ✓
  - State token validation failures ✓
  - Network/firewall issues ✓
  - Complete diagnostic checklist ✓

---

### Phase 2: Code Improvements - Logging & Diagnostics

#### Task 6: Add Comprehensive Logging to OAuth Flow
- [✓] **Status:** COMPLETED
- **Description:** Add detailed logging to GoogleOAuthController
- **Completed:**
  - ✓ authorize() method - logs redirect URI, Client ID, authorization URL
  - ✓ callback() method - logs state validation, token exchange, success/failure
  - ✓ exchangeCodeForTokens() - logs request details (sanitized), response status, error diagnostics
  - ✓ Error cases - specific error messages with troubleshooting hints
  - ✓ revoke() method - logs revocation operations
- **Files:** `GoogleOAuthController.java` ✓ UPDATED

#### Task 7: Add Logging to Google Photos Service
- [✓] **Status:** COMPLETED
- **Description:** Add logging to GooglePhotosService operations
- **Completed:**
  - ✓ Token refresh operations - logs refresh attempts, success/failure, expiry times
  - ✓ API calls to Google Photos - logs fetch, download, update operations
  - ✓ Error responses from Google - logs with diagnostic hints for 401, 403, 404
  - ✓ Token validation - logs token expiry checks, proactive refresh
- **Files:** `GooglePhotosService.java` ✓ UPDATED

#### Task 8: Add Logging to Books-from-Feed Service
- [✓] **Status:** COMPLETED
- **Description:** Add logging to BooksFromFeedService
- **Completed:**
  - ✓ Photo processing start/end - logs summary, timestamps, batch progress
  - ✓ AI detection results - logs detection responses, metadata extraction
  - ✓ Book creation success/failure - logs each step with photo ID and book ID
  - ✓ Skipped photos - logs reason for skipping each photo
  - ✓ Processing summary - logs final statistics
- **Files:** `BooksFromFeedService.java` ✓ UPDATED

#### Task 9: Make Code Self-Diagnostic for Common Issues
- [✓] **Status:** COMPLETED (via enhanced logging)
- **Description:** Add checks and helpful error messages for typical problems
- **Completed:**
  - ✓ Validate Client Secret format in exchangeCodeForTokens() - warns if too short or wrong prefix
  - ✓ Detect and log specific OAuth errors (invalid_client, redirect_uri_mismatch, invalid_grant)
  - ✓ Token expiration detection - logs expiry time and proactive refresh
  - ✓ API response validation - logs specific error codes with actionable advice
  - ✓ State token validation - logs when tokens expire or are invalid
- **Files:** `GoogleOAuthController.java`, `GooglePhotosService.java` ✓ UPDATED
- **Note:** Code now provides extensive diagnostic information in logs for troubleshooting

---

### Phase 3: UI/UX Improvements

#### Task 10: Research Client Secret Format Standards
- [✓] **Status:** COMPLETED
- **Description:** Answer: Is the Google Client Secret usually a minimum size or start with particular characters?
- **Research Findings:**
  - **Format:** Google OAuth Client Secrets typically start with `GOCSPX-`
  - **Length:** Usually 35-50 characters total (including the prefix)
  - **Characters:** Alphanumeric and hyphens
  - **Example format:** `GOCSPX-aBcDeFgHiJkLmNoPqRsTuVwXyZ123`
  - **Validation added to code:** Warns in logs if secret doesn't start with `GOCSPX-` or is < 20 characters
- **Deliverable:** Validation logic added to `GoogleOAuthController.java` (lines 250-259)

#### Task 11: Show Partial Client Secret for Verification
- [✓] **Status:** COMPLETED (changed to last 4 chars to match Google's pattern)
- **Description:** Display partial Client Secret in settings UI
- **Implementation:**
  - Shows **last 4 characters** (e.g., "...uXnb") matching Google Cloud Console's display
  - Includes creation/updated timestamp
  - Displays as read-only in Global Settings UI
- **Files:** ✓ `GlobalSettingsService.java`, `GlobalSettingsDto.java`, `index.html`, `global-settings.js`
- **Security:** Low risk - showing last 4 chars is standard industry practice

#### Task 12: Add "Last Updated" Timestamp for Client Secret
- [✓] **Status:** COMPLETED
- **Description:** Display when Client Secret was last updated
- **Implementation:**
  - Added `googleClientSecretUpdatedAt` field to GlobalSettings entity
  - Timestamp automatically updates when secret is saved
  - Displayed in UI with relative time ("5 minutes ago") and absolute time on hover
- **Files:** ✓ `GlobalSettings.java`, `GlobalSettingsDto.java`, `GlobalSettingsService.java`, `index.html`, `global-settings.js`
- **Benefit:** Helps users confirm they're using the latest secret

#### Task 13: Improve Settings UI Feedback
- [✓] **Status:** COMPLETED (via Global Settings)
- **Description:** Add comprehensive configuration visibility and status
- **Implemented:**
  - ✓ Show Client ID (read-only from application.properties)
  - ✓ Show configured redirect URI (read-only, dynamically constructed)
  - ✓ Display partial Client Secret (last 4 chars)
  - ✓ Show Client Secret validation status (Valid/Warning/Error)
  - ✓ Display last updated timestamp with relative time
  - ✓ Configuration status badge (Configured/Not Configured)
  - ✓ Librarian-only update form
- **Files:** ✓ `index.html`, `global-settings.js`, `GlobalSettingsController.java`, `GlobalSettingsService.java`
- **Notes:** All implemented in new Global Settings section

---

### Phase 4: Architecture Changes (MAJOR UPDATE)

#### Task 14: Migrate to Application-Wide Client Secret Architecture
- [✓] **Status:** COMPLETED
- **Description:** Changed Client Secret from per-user to application-wide (global) setting
- **Decision:** Implemented standard OAuth pattern - Client Secret is now global, only librarians can change it
- **Changes Made:**

**Backend Changes:**
1. **Created GlobalSettings entity** (`GlobalSettings.java`)
   - Stores application-wide settings
   - Fields: `googleClientSecret`, `googleClientSecretUpdatedAt`, `googleClientId`, `redirectUri`
   - Singleton pattern (only one row in table)

2. **Created GlobalSettingsRepository** (`GlobalSettingsRepository.java`)
   - JPA repository for global settings
   - Method: `findFirstByOrderByIdAsc()` for singleton access

3. **Created GlobalSettingsDto** (`GlobalSettingsDto.java`)
   - Includes partial secret display (last 4 chars)
   - Validation message
   - Configured status
   - Timestamps

4. **Created GlobalSettingsService** (`GlobalSettingsService.java`)
   - Manages global settings singleton
   - `getEffectiveClientSecret()` - returns DB value or falls back to env var
   - `validateClientSecretFormat()` - checks GOCSPX- prefix and length
   - `updateGlobalSettings()` - librarian-only update
   - Partial secret display logic (last 4 chars)

5. **Created GlobalSettingsController** (`GlobalSettingsController.java`)
   - `GET /api/global-settings` - Anyone can view (read-only)
   - `PUT /api/global-settings` - Librarian-only update (@PreAuthorize)

6. **Updated GoogleOAuthController** (`GoogleOAuthController.java`)
   - Removed per-user Client Secret logic
   - Now uses `globalSettingsService.getEffectiveClientSecret()`
   - Updated error messages to reference librarians/global settings

7. **Updated GooglePhotosService** (`GooglePhotosService.java`)
   - Removed per-user Client Secret logic from `refreshAccessToken()`
   - Now uses global settings service
   - Updated error messages

**Frontend Changes:**
8. **Created global-settings.js** (`/static/js/global-settings.js`)
   - `loadGlobalSettings()` - Fetches and displays global settings
   - `saveGlobalSettings()` - Updates Client Secret (librarian-only)
   - `formatRelativeTime()` - Displays "5 minutes ago" style timestamps
   - Auto-loads on page load if section exists

9. **Updated index.html** (`index.html`)
   - Added new "Global Settings" section (librarian-only)
   - Displays: Client ID, Redirect URI, Partial Secret, Validation, Last Updated
   - Update form for Client Secret (librarian-only)
   - Removed old per-user Client Secret field from user settings
   - Added script tag for global-settings.js

**Migration Notes:**
- Old per-user `googleClientSecret` field in User entity is deprecated but not removed (backward compatibility)
- System falls back to environment variable if DB value not set
- All users now share same Client Secret (standard OAuth pattern)
- Only librarians can update the secret

**Benefits:**
- Standard OAuth architecture
- Centralized management
- Easier troubleshooting
- Consistent configuration
- Matches industry best practices
- Clear visibility of configuration status

#### Task 15: Enhanced Global Settings UI
- [✓] **Status:** COMPLETED
- **Description:** Comprehensive global settings interface
- **Implemented Features:**
  - Client ID display (from application.properties)
  - Redirect URI display (dynamically constructed)
  - Partial Client Secret display (last 4 chars, matching Google's pattern)
  - Validation status with color coding (Valid/Warning/Error)
  - Last updated timestamp with relative time
  - Configuration status badge
  - Librarian-only update form with confirmation dialog
  - Responsive card-based layout
  - Auto-loading on page load
- **Security:**
  - Full secret never sent to frontend (write-only)
  - Only last 4 characters displayed (read-only)
  - Librarian role required for updates (@PreAuthorize)
  - Confirmation dialog before updating

---

### Phase 5: UI Testing

#### Task 16: Create UI Tests for Global Settings
- [✓] **Status:** COMPLETED
- **Description:** Create comprehensive UI tests for new Global Settings page
- **Completed:**
  - ✓ Added data-test attributes to all Global Settings HTML elements
  - ✓ Updated global-settings.js to use success/error divs (instead of alerts)
  - ✓ Added menu item for Global Settings (librarian-only)
  - ✓ Created GlobalSettingsUITest.java with 7 tests:
    1. testGlobalSettingsSectionVisibilityForLibrarian - verify librarians can see section
    2. testGlobalSettingsSectionNotVisibleForRegularUser - verify regular users cannot see section
    3. testGlobalSettingsLoadAndDisplay - verify all settings load and display correctly
    4. testUpdateGlobalClientSecret - verify updating Client Secret works
    5. testUpdateGlobalClientSecretPersistence - verify updates persist across navigation
    6. testEmptyClientSecretValidation - verify empty input shows error
    7. testClientSecretFormatValidation - verify invalid format shows warning
  - ✓ Updated SettingsUITest.java - removed obsolete testGoogleClientSecretPersistence test
- **Files Created:**
  - `src/test/java/com/muczynski/library/ui/GlobalSettingsUITest.java` ✓
- **Files Modified:**
  - `src/main/resources/static/index.html` ✓ (added data-test attributes and menu item)
  - `src/main/resources/static/js/global-settings.js` ✓ (changed alerts to success/error divs)
  - `src/test/java/com/muczynski/library/ui/SettingsUITest.java` ✓ (removed obsolete test)
- **Test Coverage:**
  - Visibility (librarian vs regular user)
  - Loading and displaying settings
  - Updating Client Secret
  - Persistence across navigation
  - Validation (empty input, format warnings)
  - Success and error message display
- **Notes:** All tests follow uitest-requirements.md patterns (20-second timeouts, NETWORKIDLE waits, data-test selectors)

---

## Quick Reference Answers

### Q: What is the correct redirect URI?
**A:** `https://library.muczynskifamily.com/api/oauth/google/callback`

### Q: Where to configure it in Google?
**A:** Google Cloud Console → Your Project → APIs & Services → Credentials → OAuth 2.0 Client IDs → Edit → Authorized redirect URIs → Add URI

### Q: What's causing "redirect_uri_mismatch" error?
**A:** The redirect URI configured in Google Cloud Console doesn't match the one your app is sending. Add the exact URI above to your Google OAuth client configuration.

### Q: Do we need more in settings?
**A:** Currently reviewing - see Tasks 13, 14, and 15 above

---

## Notes

- **No Building/Testing:** Per user request, skip build and test steps during implementation
- **Implementation Approach:** Complete tasks one at a time, mark progress in this file
- **Blocked Items:** Task 14 requires architectural decision before proceeding
