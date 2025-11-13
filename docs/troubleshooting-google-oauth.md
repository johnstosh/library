# Troubleshooting Google OAuth Issues

## Overview

This guide covers common issues with Google Photos OAuth integration and how to diagnose and fix them.

---

## Common Issues and Solutions

### 1. Error 400: redirect_uri_mismatch

**Symptoms:**
- After clicking "Authorize Google Photos", you see:
  ```
  Error 400: redirect_uri_mismatch
  Access blocked: This app's request is invalid
  ```
- The error page may show the redirect URI your app sent vs. what Google expected

**Root Cause:**
The redirect URI configured in Google Cloud Console doesn't exactly match the URI your application is sending in the OAuth request.

**How to Diagnose:**
1. Check the error page - Google often shows both URIs and highlights the mismatch
2. Look at the URL in your browser when the error appears
3. Check browser developer console (F12) → Network tab → Look at the redirect to Google

**What Your App Sends:**
- The app dynamically constructs: `origin + "/api/oauth/google/callback"`
- For your site: `https://library.muczynskifamily.com/api/oauth/google/callback`

**How to Fix:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to: **APIs & Services** → **Credentials**
3. Click on your OAuth 2.0 Client ID
4. Under "Authorized redirect URIs", add (or fix):
   ```
   https://library.muczynskifamily.com/api/oauth/google/callback
   ```
5. Click **"Save"**
6. Wait 1-2 minutes for changes to propagate
7. Try authorizing again

**Common Mistakes:**
- ❌ `http://` instead of `https://`
- ❌ Extra trailing slash: `.../callback/`
- ❌ Missing `/api/` in the path
- ❌ Wrong domain or subdomain
- ❌ Port number in URL when not needed

**Verification:**
The redirect URI must match EXACTLY:
- Protocol (https://)
- Domain (library.muczynskifamily.com)
- Path (/api/oauth/google/callback)
- No extra slashes, parameters, or fragments

---

### 2. Invalid Client Secret

**Symptoms:**
- OAuth authorization succeeds (redirects to Google)
- But after clicking "Allow", you get an error
- Error message: "oauth_error=token_exchange_failed"
- Or: Returns to home page but authorization status still shows "Not Authorized"

**Root Cause:**
The Client Secret entered in your app's settings is incorrect, missing, or from a different OAuth client.

**How to Diagnose:**
1. Check the URL after failed authorization: Look for `?oauth_error=...`
2. Check browser console for errors
3. Check server logs for "Invalid client secret" or "401 Unauthorized" from Google
4. Verify the Client Secret in Settings matches the one in Google Cloud Console

**How to Fix:**

**Option A: Retrieve the correct Client Secret**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to: **APIs & Services** → **Credentials**
3. Find your OAuth 2.0 Client ID
4. Click the **pencil icon** (Edit) or the **client name**
5. The Client Secret is shown (may need to click eye icon to reveal)
6. Copy the secret

**Option B: Reset the Client Secret**
1. In Google Cloud Console → Credentials → Your OAuth Client
2. Click **"Reset Secret"**
3. Confirm the reset
4. Copy the new secret

**Update in Your App:**
1. Open Settings in your app
2. Paste the Client Secret into "Google OAuth Client Secret" field
3. Click "Save Settings"
4. Try authorizing again

**Security Note:**
- If you reset the secret, the old one immediately stops working
- Any users who were authorized will need to re-authorize
- Store the secret securely (password manager, not in code)

---

### 3. Google Photos Library API Not Enabled

**Symptoms:**
- OAuth authorization succeeds
- Settings shows "Authorized" status
- But when you click "Process Photos from Feed", you get an error
- Error: "Photos Library API has not been used..." or "API not enabled"

**Root Cause:**
The Photos Library API is not enabled for your Google Cloud project.

**How to Diagnose:**
1. Try processing photos - if it fails immediately with API error, this is the issue
2. Check server logs for "403 Forbidden" or "API not enabled" errors
3. Check the error response from Google Photos API calls

**How to Fix:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to: **APIs & Services** → **Library**
3. Search for: "Photos Library API"
4. Click on "Photos Library API"
5. Click **"Enable"**
6. Wait 30-60 seconds for the API to fully enable
7. Try processing photos again

**Verification:**
- Go to **APIs & Services** → **Enabled APIs & services**
- Confirm "Photos Library API" is in the list

---

### 4. OAuth Consent Screen Issues

**Symptoms:**
- Can't authorize - error says "app is in testing mode"
- Or: "This app is blocked" or "unverified app" warnings
- User email not in test users list can't authorize

**Root Cause:**
The OAuth consent screen is in "Testing" mode and the user isn't added as a test user.

**How to Diagnose:**
1. Check if you get "app is in testing mode" error
2. Verify your email is the one used for Google Cloud Console project
3. Check if the user trying to authorize is in the test users list

**How to Fix:**

**Option A: Add Test Users**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to: **APIs & Services** → **OAuth consent screen**
3. Under "Test users", click **"Add Users"**
4. Enter email addresses (e.g., `johnstosh@gmail.com`)
5. Click "Save"
6. Users should receive an email (may go to spam)
7. Try authorizing again

**Option B: Publish the App**
1. In OAuth consent screen settings
2. Click **"Publish App"**
3. Confirm the warning
4. Your app will be available to all Google users

**Note:** Publishing with `photoslibrary.readonly` scope usually doesn't require Google verification.

---

### 5. Token Expiration / Refresh Failures

**Symptoms:**
- Authorization worked initially
- But after some time (hours/days), requests start failing
- Error: "Invalid credentials" or "unauthorized"
- Settings still shows "Authorized" but API calls fail

**Root Cause:**
- Access tokens expire after 1 hour
- Refresh token is missing, invalid, or expired
- Refresh token request fails

**How to Diagnose:**
1. Check server logs for "token expired" or "401 Unauthorized" messages
2. Check if `googlePhotosTokenExpiry` timestamp is in the past
3. Check if `googlePhotosRefreshToken` exists in database
4. Try to trigger token refresh and check logs

**How to Fix:**

**Short-term:** Re-authorize
1. Go to Settings
2. Click "Revoke Google Photos" (to clean up old tokens)
3. Click "Authorize Google Photos" again
4. Complete the authorization flow

**Long-term:** Check refresh logic
1. Verify `GooglePhotosService.refreshAccessToken()` is working
2. Check that refresh token is being saved during initial authorization
3. Add logging to see when/why refresh fails
4. Ensure offline access is requested (should be by default)

**Prevention:**
- The app should automatically refresh tokens when needed
- `GooglePhotosService.getValidAccessToken()` handles this
- If refresh fails repeatedly, user needs to re-authorize

---

### 6. Wrong Client ID

**Symptoms:**
- Error: "The OAuth client was not found"
- Or: "Client ID does not match"
- Authorization fails immediately

**Root Cause:**
The Client ID in `application.properties` doesn't match the one from Google Cloud Console.

**How to Diagnose:**
1. Check error message - often says "client not found"
2. Compare Client IDs:
   - In `src/main/resources/application.properties`: `google.oauth.client-id=...`
   - In Google Cloud Console: **APIs & Services** → **Credentials**

**How to Fix:**
1. Get the correct Client ID from Google Cloud Console
2. Update `application.properties`:
   ```properties
   google.oauth.client-id=YOUR-ACTUAL-CLIENT-ID.apps.googleusercontent.com
   ```
3. Restart the application
4. Try authorizing again

**Note:** Client ID is public (not secret) and is the same for all users.

---

### 7. State Token Validation Failures

**Symptoms:**
- Error: "Invalid state parameter"
- Authorization redirects back but fails
- URL shows `?oauth_error=invalid_state`

**Root Cause:**
- State token expired or not found (stored in memory, lost on restart)
- CSRF attack attempt
- Browser cookies disabled
- Callback URL opened in different browser session

**How to Diagnose:**
1. Check if error message mentions "state"
2. Check server logs for "State parameter not found" or "State validation failed"
3. Check if the app was restarted between authorization start and callback

**How to Fix:**

**If app was restarted:**
- Simply try authorizing again
- State tokens are stored in memory and lost on restart

**If cookies disabled:**
- Enable cookies for your domain
- Check browser privacy settings

**If using different browser:**
- Complete the OAuth flow in the same browser window
- Don't copy the callback URL to a different browser

**Persistent issues:**
- Check server logs for details
- Verify clock sync (time-based tokens might fail if system time is wrong)

---

### 8. Scope Permission Issues

**Symptoms:**
- Authorization succeeds but can't access photos
- Error: "Insufficient permissions" or "Access denied"
- App requests wrong permissions

**Root Cause:**
- Requested scope doesn't match configured scope
- User denied permissions during authorization
- Scope changed after initial authorization

**How to Diagnose:**
1. Check what permissions are shown on Google's consent screen
2. Verify `application.properties` has correct scope:
   ```properties
   google.oauth.scope=https://www.googleapis.com/auth/photoslibrary.readonly
   ```
3. Check if user clicked "Deny" instead of "Allow"

**How to Fix:**

**If scope is wrong:**
1. Update `application.properties` with correct scope
2. Restart application
3. Revoke and re-authorize

**If user denied:**
1. Try authorizing again
2. Make sure to click "Allow" on the consent screen
3. Review what permissions are being requested

**If scope changed:**
1. User must re-authorize with new scope
2. Click "Revoke" then "Authorize" again
3. Or revoke from [Google Account Permissions](https://myaccount.google.com/permissions)

---

### 9. Network / Firewall Issues

**Symptoms:**
- Timeout errors
- "Connection refused" or "Unable to reach Google servers"
- Authorization starts but never completes

**Root Cause:**
- Firewall blocking outbound HTTPS to Google
- DNS resolution failures
- Network connectivity issues

**How to Diagnose:**
1. Check server logs for timeout or connection errors
2. Try accessing Google OAuth URLs directly from the server:
   ```bash
   curl https://accounts.google.com/o/oauth2/auth
   curl https://oauth2.googleapis.com/token
   ```
3. Check if server can resolve Google domains:
   ```bash
   nslookup accounts.google.com
   ```

**How to Fix:**
- Ensure outbound HTTPS (port 443) is allowed to:
  - `accounts.google.com`
  - `oauth2.googleapis.com`
  - `photoslibrary.googleapis.com`
- Check DNS configuration
- Verify proxy settings (if applicable)

---

## Diagnostic Checklist

When troubleshooting OAuth issues, check these in order:

1. **Redirect URI Configuration**
   - [ ] Exact match in Google Cloud Console
   - [ ] HTTPS protocol (not HTTP)
   - [ ] Correct domain and path
   - [ ] No typos or extra characters

2. **API Enablement**
   - [ ] Photos Library API is enabled in Google Cloud Console
   - [ ] Under correct project

3. **Credentials**
   - [ ] Client ID matches between app and Google Cloud Console
   - [ ] Client Secret is correct and not expired
   - [ ] Client Secret saved in app settings

4. **OAuth Consent Screen**
   - [ ] Properly configured with all required fields
   - [ ] Test users added (if in testing mode)
   - [ ] Or app is published

5. **Scopes**
   - [ ] Correct scope requested: `https://www.googleapis.com/auth/photoslibrary.readonly`
   - [ ] User approved permissions

6. **Network**
   - [ ] Server can reach Google OAuth endpoints
   - [ ] No firewall blocking
   - [ ] DNS resolution working

7. **Tokens**
   - [ ] Access token not expired (or refresh working)
   - [ ] Refresh token present in database
   - [ ] Token refresh logic functioning

---

## Getting More Information

### Enable Detailed Logging

To see more diagnostic information, check your application logs for:

- OAuth authorization requests and responses
- Token exchange details (sanitized)
- API call errors from Google
- Token refresh attempts

**Log locations:**
- Development: Console output
- Production: Application log files

### Browser Developer Tools

1. Open Developer Tools (F12)
2. Go to **Network** tab
3. Try the authorization flow
4. Look for:
   - Redirect to Google (check parameters)
   - Callback from Google (check parameters)
   - Any error responses

### Google Cloud Console Logs

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to **Logging** → **Logs Explorer**
3. Filter by your OAuth client
4. Look for error messages or failed requests

---

## Still Having Issues?

If you've tried all the above and still have problems:

1. **Verify the basics:**
   - Client ID and Secret are from the same OAuth client
   - Redirect URI is exactly correct
   - Photos Library API is enabled
   - Your email is in test users (if testing mode)

2. **Check logs:**
   - Application logs for errors
   - Browser console for JavaScript errors
   - Network tab for failed requests

3. **Try a fresh start:**
   - Create a new OAuth client in Google Cloud Console
   - Update Client ID and Secret in your app
   - Try authorizing again

4. **Test with curl:**
   - Manually test the OAuth flow with curl commands
   - Verify Google endpoints are reachable
   - Check response codes and error messages

---

## Additional Resources

- [Google OAuth 2.0 Troubleshooting](https://developers.google.com/identity/protocols/oauth2/web-server#handlingresponse)
- [Google Photos Library API Errors](https://developers.google.com/photos/library/guides/api-limits-quotas)
- [OAuth 2.0 Error Codes](https://www.oauth.com/oauth2-servers/server-side-apps/possible-errors/)
