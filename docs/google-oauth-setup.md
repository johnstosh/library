# Google OAuth Setup Guide

## Overview

This guide walks you through configuring Google Cloud Console to enable Google Photos integration for the Books-from-Feed feature.

## Prerequisites

- A Google account
- Access to [Google Cloud Console](https://console.cloud.google.com/)
- Your application URL: `https://library.muczynskifamily.com`

---

## Step-by-Step Configuration

### Step 1: Create or Select a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click the project dropdown at the top of the page
3. Either:
   - **Create new project:**
     - Click "New Project"
     - Enter project name (e.g., "Library Books Feed")
     - Click "Create"
   - **Use existing project:**
     - Select your existing project from the list

### Step 2: Enable Required APIs

You must enable **both** APIs for the Picker to work:

#### Enable Google Photos Library API

1. In the Google Cloud Console, open the navigation menu (☰)
2. Go to **APIs & Services** → **Library**
3. Search for **"Photos Library API"**
4. Click on "Photos Library API"
5. Click the **"Enable"** button
6. Wait for the API to be enabled (may take a few seconds)

#### Enable Google Picker API

1. Still in **APIs & Services** → **Library**
2. Search for **"Google Picker API"**
3. Click on "Google Picker API"
4. Click the **"Enable"** button
5. Wait for the API to be enabled (may take a few seconds)

**CRITICAL:** If you skip the Picker API, you will get a **403 error** when trying to open the photo picker. If you skip the Photos Library API, OAuth will succeed but photo downloads will fail.

**Verify:** Go to **APIs & Services** → **Enabled APIs & services** and confirm you see:
- ✅ Google Photos Library API
- ✅ Google Picker API

### Step 3: Configure OAuth Consent Screen

1. In the navigation menu, go to **APIs & Services** → **OAuth consent screen**
2. Select **External** user type (unless you have a Google Workspace organization)
3. Click **"Create"**

4. Fill in the required fields:

   **App Information:**
   - **App name:** `Library Books Feed` (or your preferred name)
   - **User support email:** Your email address
   - **App logo:** (Optional) Upload an app icon

   **App Domain:**
   - **Application home page:** `https://library.muczynskifamily.com`
   - **Application privacy policy link:** (Optional, but recommended)
   - **Application terms of service link:** (Optional)

   **Authorized Domains:**
   - Add: `muczynskifamily.com`

   **Developer Contact Information:**
   - **Email addresses:** Your email address

5. Click **"Save and Continue"**

### Step 4: Configure OAuth Scopes

1. On the "Scopes" page, click **"Add or Remove Scopes"**
2. Search for and select **Google Photos Library API** scopes:
   - `https://www.googleapis.com/auth/photoslibrary`
   - `https://www.googleapis.com/auth/photoslibrary.readonly`
   - `https://www.googleapis.com/auth/photoslibrary.readonly.originals`
   - `https://www.googleapis.com/auth/photoslibrary.edit.appcreateddata`
   - `https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata`
   - `https://www.googleapis.com/auth/photospicker.mediaitems.readonly`
   - `https://www.googleapis.com/auth/photoslibrary.appendonly`
   - `https://www.googleapis.com/auth/photoslibrary.sharing`

   **Note:** The application currently uses the Google Photos Picker API which requires these scopes. These scopes allow the app to:
   - Display the Google Photos Picker UI
   - Access photos selected by the user via the Picker
   - Download selected photos for AI processing
   - Add to your Google Photos library (if needed)
   - Manage and add to shared albums (if needed)

3. Click **"Update"**
4. Click **"Save and Continue"**

### Step 5: Add Test Users (for External Apps)

If your app is in "Testing" mode (not published):

1. On the "Test users" page, click **"Add Users"**
2. Enter the email addresses of users who should have access
   - Include your own email: `johnstosh@gmail.com`
   - Add any other users who will test the feature
3. Click **"Save and Continue"**
4. Click **"Back to Dashboard"**

**Note:** In testing mode, only these users can authorize the app. To allow all Google users, you'll need to publish the app (see Step 8).

### Step 6: Create OAuth 2.0 Credentials

1. Go to **APIs & Services** → **Credentials**
2. Click **"+ Create Credentials"** at the top
3. Select **"OAuth client ID"**

4. Configure the OAuth client:
   - **Application type:** Select **"Web application"**
   - **Name:** `Library Web Client` (or your preferred name)

5. **Authorized JavaScript origins:**
   - Click **"+ Add URI"**
   - Add: `https://library.muczynskifamily.com`

6. **Authorized redirect URIs:**
   - Click **"+ Add URI"**
   - Add: `https://library.muczynskifamily.com/api/oauth/google/callback`

   **⚠️ CRITICAL:** This exact URI must match what your app sends. The error "redirect_uri_mismatch" means this doesn't match.

7. Click **"Create"**

### Step 7: Save Your Credentials

After creation, a dialog will appear with your credentials:

1. **Copy the Client ID** (looks like: `422211234280-abss0eud25flhodvgm4cuid7cr4ts4qd.apps.googleusercontent.com`)
   - This is already configured in your `application.properties` file
   - Verify it matches: `google.oauth.client-id=...`

2. **Copy the Client Secret** (looks like: `GOCSPX-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)
   - ⚠️ **Keep this secret!** Anyone with this can impersonate your app
   - You'll enter this in your app's Settings page
   - Store it securely (password manager, secure notes, etc.)

3. Click **"OK"** to close the dialog

**Lost your secret?** You can view it again:
- Go to **APIs & Services** → **Credentials**
- Click on your OAuth 2.0 Client ID name
- The Client Secret will be shown (click the eye icon to reveal it)

---

## Step 8: Publishing Your App (Optional)

If you want any Google user to access your app (not just test users):

1. Go to **APIs & Services** → **OAuth consent screen**
2. Click **"Publish App"**
3. Click **"Confirm"**

**Note:** Google may require verification if you're requesting sensitive scopes. For `photoslibrary.readonly`, verification requirements are minimal.

---

## Step 9: Configure Your Application

### Update Application Properties (if needed)

Check your `src/main/resources/application.properties` file:

```properties
google.oauth.client-id=422211234280-abss0eud25flhodvgm4cuid7cr4ts4qd.apps.googleusercontent.com
google.oauth.auth-uri=https://accounts.google.com/o/oauth2/auth
google.oauth.token-uri=https://oauth2.googleapis.com/token
google.oauth.scope=https://www.googleapis.com/auth/photoslibrary https://www.googleapis.com/auth/photoslibrary.readonly https://www.googleapis.com/auth/photoslibrary.readonly.originals https://www.googleapis.com/auth/photoslibrary.edit.appcreateddata https://www.googleapis.com/auth/photoslibrary.readonly.appcreateddata https://www.googleapis.com/auth/photospicker.mediaitems.readonly https://www.googleapis.com/auth/photoslibrary.appendonly https://www.googleapis.com/auth/photoslibrary.sharing
```

**Verify:**
- The `client-id` matches the one from Google Cloud Console
- The URIs are correct (these are standard Google OAuth endpoints)
- The `scope` includes all 8 Google Photos scopes (space-separated)

### Enter Client Secret in Global Settings

**Note:** The Client Secret is now an application-wide setting, not per-user.

1. Open your app: `https://library.muczynskifamily.com`
2. Log in with a **Librarian** account
3. Go to **Global Settings** tab
4. In the "Google OAuth Configuration" section:
   - **Google OAuth Client Secret:** Paste the Client Secret you copied earlier
   - Click **"Save Global Settings"**
5. All users will now use this shared Client Secret for Google Photos authorization

### Authorize Google Photos Access

Each user must authorize their own Google Photos access:

1. Go to **Settings** tab (User Settings section)
2. Click **"Authorize Google Photos"**
3. You'll be redirected to Google's consent screen
4. Review the permissions requested:
   - "See, upload, and organize items in your Google Photos library"
   - These permissions are required for the Google Photos Picker API
5. Click **"Allow"**
6. You'll be redirected back to your app with a success message

---

## Verification

After setup, verify everything works:

1. In **Settings**, you should see:
   - ✅ **Google Photos: Authorized**
   - Green "Revoke" button (meaning you're connected)

2. Go to the **Books from Feed** section
3. Click **"Process Photos"**
4. The **Google Photos Picker** should open (official Google UI)
5. Select one or more photos of books from your library
6. Click **"Select"** in the Picker
7. The system will:
   - Download the selected photos
   - Use AI to detect if they are book covers
   - Extract book title and author information
   - Create book entries in your library
8. View the processing results displayed on the page

---

## Important Security Notes

### Client Secret Security

- **Never commit the Client Secret to version control**
- Store it securely (password manager, environment variables, secure vault)
- Your app stores it in the database as a global setting (application-wide, not per-user)
- Only Librarians can update the global Client Secret

### Access Tokens

- OAuth access tokens are stored in your database
- They expire after 1 hour (automatically refreshed by the app)
- Refresh tokens allow long-term access without re-authorization
- Users can revoke access anytime via the "Revoke" button or [Google Account Settings](https://myaccount.google.com/permissions)

### Redirect URI Security

- The redirect URI must match exactly between Google Cloud Console and your app
- The app dynamically constructs it as: `origin + "/api/oauth/google/callback"`
- Where `origin` is `https://library.muczynskifamily.com`
- Never use HTTP in production (only HTTPS)

---

## Troubleshooting

See [troubleshooting-google-oauth.md](./troubleshooting-google-oauth.md) for common issues and solutions.

### Quick Fixes

**"redirect_uri_mismatch" error:**
- Verify the redirect URI in Google Cloud Console exactly matches: `https://library.muczynskifamily.com/api/oauth/google/callback`
- Check for typos, extra slashes, HTTP vs HTTPS

**"Access blocked: This app's request is invalid":**
- Usually a redirect URI mismatch (see above)
- Ensure the Client ID in `application.properties` matches Google Cloud Console

**"API not enabled" errors:**
- Go to Google Cloud Console and enable "Photos Library API"

**Can't authorize (stuck in testing mode):**
- Add your email as a test user in OAuth consent screen settings

---

## Additional Resources

- [Google Photos Library API Documentation](https://developers.google.com/photos/library/guides/get-started)
- [Google OAuth 2.0 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Google Cloud Console](https://console.cloud.google.com/)
