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

### Google Photos API Authentication

**Important**: The "Google Photos API Key" field in Settings actually requires an **OAuth 2.0 access token**, not a simple API key.

**Setup Instructions**:

1. **Enable Google Photos Library API** in Google Cloud Console
2. **Create OAuth 2.0 Credentials**:
   - Go to APIs & Services > Credentials
   - Create OAuth 2.0 Client ID
   - Add authorized redirect URIs for your application
3. **Get an Access Token**:
   - Use OAuth 2.0 Playground or implement OAuth flow
   - Request scopes: `https://www.googleapis.com/auth/photoslibrary.readonly`
   - Copy the access token
4. **Enter Token in Settings**:
   - Go to Settings page
   - Paste the access token in "Google Photos API Key" field
   - Save settings

**Note**: OAuth access tokens expire (typically after 1 hour). You'll need to refresh the token or re-authenticate when it expires.

**Error Symptoms**:
- `401 Unauthorized` from Google Photos API
- Error message: "Failed to fetch photos from Google Photos: 401 Unauthorized"

**Reference**:
- File: `src/main/java/com/muczynski/library/service/GooglePhotosService.java:66` (uses `setBearerAuth()`)
- Google Photos Library API: https://developers.google.com/photos/library/guides/get-started
