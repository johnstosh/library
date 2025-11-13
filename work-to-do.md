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
- [ ] **Status:** PENDING
- **Description:** Display first 4-6 characters of Client Secret in settings UI
- **Analysis:**
  - **Recommendation:** YES, showing first 6-8 characters (e.g., `GOCSPX-`) is beneficial and common practice
  - **Security:** Low risk - the prefix `GOCSPX-` is predictable anyway, showing a few more chars helps verification
  - **Industry practice:** AWS, Stripe, GitHub all show partial keys/secrets for verification
  - **Benefit:** Helps users verify they copied the right secret without showing the sensitive part
  - **Alternative considered:** Last 4 characters - less useful since users can't match with source
- **Implementation:** Show first 8 characters (includes full prefix plus 1-2 random chars)
- **Files:** `index.html`, `settings.js`, `UserSettingsController.java`
- **Decision:** YES, implement this feature

#### Task 12: Add "Last Updated" Timestamp for Client Secret
- [ ] **Status:** PENDING
- **Description:** Display when Client Secret was last updated
- **Analysis:**
  - **Recommendation:** YES, very helpful for troubleshooting
  - **Use cases:**
    - User can verify they just updated the secret
    - Helps troubleshoot "why is my old secret not working" issues
    - Useful audit trail
  - **Implementation:**
    - Add `googleClientSecretUpdatedAt` field to User entity (Instant or LocalDateTime)
    - Update timestamp whenever secret is saved (even if same value)
    - Display in settings UI as relative time ("Updated 5 minutes ago") or absolute time
- **Files:** `User.java`, `UserSettingsDto.java`, `index.html`, `settings.js`, `UserSettingsService.java`
- **Benefit:** Significant - helps users confirm they're using the latest secret
- **Decision:** YES, implement this feature

#### Task 13: Improve Settings UI Feedback
- [ ] **Status:** PENDING
- **Description:** Add more configuration status and validation feedback
- **Improvements:**
  - Show Client ID in settings (read-only, since it's in application.properties)
  - Show configured redirect URI
  - Add "Test Connection" button
  - Show last successful OAuth time
  - Display token expiration time
- **Files:** `index.html`, `settings.js`, `UserSettingsController.java`

---

### Phase 4: Architecture Review

#### Task 14: Review Client Secret Storage Architecture
- [BLOCKED] **Status:** BLOCKED - Need decision on architecture
- **Description:** Determine if Client Secret should be per-user or application-wide
- **Current State:** Stored per-user in database (unusual pattern)
- **Standard Pattern:** Client Secret is application-wide, same for all users
- **Decision Needed:**
  - Keep per-user (allows different Google projects per user)?
  - Change to application-wide (standard OAuth pattern)?
  - Document rationale either way
- **Blocker:** Need user decision before implementing changes

#### Task 15: Review Settings Requirements
- [ ] **Status:** PENDING
- **Description:** Answer: Do we need more settings items?
- **Current Settings:**
  - Google OAuth Client Secret (per-user)
  - Authorize/Revoke buttons
- **Potential Additions:**
  - Show Client ID (read-only)
  - Show redirect URI (read-only)
  - Processing options (e.g., auto-process interval)
  - AI confidence threshold
  - Photo date range limits
- **Decision:** Determine what's actually needed

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
