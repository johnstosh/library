# User Settings Endpoints

## GET /api/user-settings
Returns the current user's settings and profile information.

**Authentication:** Authenticated users only (`isAuthenticated()`)

**Response:** UserDto with full user settings including API keys and library card design

```json
{
  "id": 1,
  "username": "johndoe",
  "authorities": ["USER"],
  "xaiApiKey": "xai-key-123",
  "googlePhotosApiKey": "google-key-456",
  "googleClientSecret": "secret-789",
  "googlePhotosAlbumId": "album-123",
  "lastPhotoTimestamp": "2025-01-01T12:00:00",
  "libraryCardDesign": "CLASSICAL_DEVOTION",
  "email": "john@example.com",
  "activeLoansCount": 2,
  "ssoProvider": "google",
  "ssoSubjectId": "123456789",
  "lastModified": "2025-01-01T12:00:00"
}
```

---

## PUT /api/user-settings
Updates the current user's settings.

**Authentication:** Authenticated users only (`isAuthenticated()`)

**Request Body:** UserSettingsDto (all fields optional)
```json
{
  "username": "newusername",
  "currentPassword": "sha256-hashed-current-password",
  "password": "sha256-hashed-new-password",
  "xaiApiKey": "new-xai-key",
  "googlePhotosApiKey": "new-google-key",
  "googleClientSecret": "new-secret",
  "googlePhotosAlbumId": "new-album-id",
  "lastPhotoTimestamp": "2025-01-02T12:00:00",
  "libraryCardDesign": "COUNTRYSIDE_YOUTH"
}
```

**Response:** Updated UserDto

**Notes:**
- All fields are optional - only provide fields you want to update
- Password changes require both `currentPassword` and `password` fields
- `currentPassword` is verified before allowing password change
- Passwords must be SHA-256 hashed on client side before sending
- Username changes check for uniqueness
- API keys can be set to empty string to clear them

**Error Responses:**
- 400: Invalid password format (not SHA-256 hash)
- 400: Current password is incorrect
- 400: Username already taken

---

## DELETE /api/user-settings
Deletes the current user's account.

**Authentication:** Authenticated users only (`isAuthenticated()`)

**Response:** 204 No Content

**Warning:** This permanently deletes the user account. Cannot be undone.

---

**Related:** UserSettingsController.java, UserSettingsService.java, UserSettingsDto.java
