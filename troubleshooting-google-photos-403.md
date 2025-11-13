# Troubleshooting Google Photos 403 "Insufficient Authentication Scopes"

## Error
```
403 Forbidden: Request had insufficient authentication scopes.
PERMISSION_DENIED
```

## Root Cause
The OAuth access token doesn't include the `photoslibrary.readonly` scope needed to access Google Photos.

## Solution Steps

### Step 1: Verify Google Cloud Console Configuration

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project
3. Navigate to **APIs & Services** > **Library**
4. Search for **"Photos Library API"**
5. Click on it and verify it says **"API Enabled"**
   - If not, click **"Enable"**

### Step 2: Verify OAuth Consent Screen

1. Go to **APIs & Services** > **OAuth consent screen**
2. Scroll to **"Scopes for Google APIs"**
3. Click **"Edit App"**
4. Go to **"Scopes"** step
5. Click **"Add or Remove Scopes"**
6. Search for **"Photos Library API"**
7. Check the box for **".../auth/photoslibrary.readonly"**
   - Scope: `https://www.googleapis.com/auth/photoslibrary.readonly`
   - Description: "View your Google Photos library"
8. Click **"Update"** then **"Save and Continue"**

### Step 3: Verify OAuth Client Configuration

1. Go to **APIs & Services** > **Credentials**
2. Find your OAuth 2.0 Client ID
3. Click the pencil icon to edit
4. Verify **Authorized redirect URIs** includes:
   ```
   http://localhost:8080/api/oauth/google/callback
   ```
   (or your production URL if deployed)

### Step 4: Revoke and Re-Authorize in the App

**IMPORTANT**: You must revoke the old token and get a new one with the correct scope.

1. In the application, go to **Settings** (User Settings section)
2. Scroll to **"Google Photos Access"**
3. Click **"Revoke Access"** button
4. Wait for confirmation message
5. Click **"Authorize Google Photos"** button
6. You will be redirected to Google's consent screen
7. **IMPORTANT**: Look at the permissions being requested
   - It should say: "View your Google Photos library"
   - If it doesn't show this permission, STOP and check Steps 1-2 again
8. Click **"Allow"** to grant permissions
9. You should be redirected back with a success message

### Step 5: Test the Feature

1. Go to **Books-from-Feed** section
2. Click **"Process Photos"**
3. If it still fails with 403, check the server logs for:
   ```
   Using scope: https://www.googleapis.com/auth/photoslibrary.readonly
   ```

## Common Issues

### Issue: "View your Google Photos library" not shown in consent screen
- **Cause**: Photos Library API scope not added in OAuth consent screen
- **Fix**: Complete Step 2 above

### Issue: Photos Library API not enabled
- **Cause**: API not enabled in Google Cloud Console
- **Fix**: Complete Step 1 above

### Issue: Still getting 403 after re-authorizing
- **Cause**: Old token cached or consent screen didn't show Photos permission
- **Fix**:
  1. Revoke access again
  2. Clear browser cookies/cache
  3. Check Google account's connected apps: https://myaccount.google.com/permissions
  4. Remove the app if it's listed
  5. Re-authorize again

### Issue: "Access blocked: This app's request is invalid"
- **Cause**: Redirect URI mismatch
- **Fix**: Complete Step 3 above

## Verification

After re-authorizing, check the server logs when clicking "Process Photos":
```
Using scope: https://www.googleapis.com/auth/photoslibrary.readonly
Redirecting user to Google consent screen
Token exchange successful for user: [username]
Successfully obtained access token for user: [username]
```

If you see these logs but still get 403:
- The scope might not have been granted
- Check https://myaccount.google.com/permissions to see what permissions were granted
- The app should show: "View your Google Photos library"

## Still Not Working?

Check the application properties file has the correct scope:
```properties
google.oauth.scope=https://www.googleapis.com/auth/photoslibrary.readonly
```

File location: `src/main/resources/application.properties`
