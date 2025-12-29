# User Lookup Bug Checklist

## Overview

**Bug**: Google Photos picker returns "User not found" error
**Root Cause**: 12 files treat `authentication.getName()` as username, but it contains user ID
**Impact**: Breaks Google Photos picker and potentially other features requiring user lookup
**Scope**: 12 code files + ~20 test files + 4-5 documentation files
**Severity**: Critical (blocks core functionality)

## Execution Order

1. Read this entire checklist first ✓ (You are here)
2. Review all 12 buggy files to understand the pattern
3. Fix code files (see "Files That Need Fixing" section)
4. Update tests (see "Definition of Done" section)
5. Update documentation (see "Definition of Done" section)
6. Run fast-tests.sh to verify
7. Manual testing (especially Google Photos picker)
8. Git commit and push to dev

---

## Root Cause
**CRITICAL BUG**: Many services and controllers are treating `authentication.getName()` as a **username**, but it actually contains the **database user ID** (as a string).

From `AuthController.java:72`:
```java
// Create authentication token using user ID as principal (not username)
UsernamePasswordAuthenticationToken authToken =
    new UsernamePasswordAuthenticationToken(
        user.getId().toString(),  // <-- USER ID, NOT USERNAME!
        ...
```

This means any code calling `authentication.getName()` gets the user ID string (e.g., "42"), but many services incorrectly try to look up users by username using this value, causing "User not found" errors.

## Files That Need Fixing

### 1. GooglePhotosService.java ✗ CRITICAL - CAUSES PICKER BUG
**Location**: `src/main/java/com/muczynski/library/service/GooglePhotosService.java`

**Problem**: All methods use `authentication.getName()` and treat it as username
- Line 78: `downloadPhoto()` - calls `getValidAccessToken(username)` where username is actually user ID
- Line 139: `updatePhotoDescription()` - same issue
- Line 435: `fetchPickerMediaItems()` - same issue
- Line 512: `createPickerSession()` - **THIS IS THE PICKER BUG!**
- Line 563: `getPickerSessionStatus()` - same issue

**Current code pattern**:
```java
String username = authentication.getName();  // WRONG - this is user ID!
String apiKey = getValidAccessToken(username);  // Looks up by username, fails!
```

**Fix**: Parse as Long user ID and look up by ID
```java
Long userId = Long.parseLong(authentication.getName());
User user = userRepository.findById(userId)
    .orElseThrow(() -> new LibraryException("User not found"));
String apiKey = getValidAccessToken(user);
```

**Methods to fix**:
- `downloadPhoto()` - line 78
- `updatePhotoDescription()` - line 139
- `fetchPickerMediaItems()` - line 435
- `createPickerSession()` - line 512 **← PICKER BUG**
- `getPickerSessionStatus()` - line 563
- Refactor `getValidAccessToken()` to accept `User` object instead of username

**Tests to update**:
- `src/test/java/com/muczynski/library/service/GooglePhotosServiceTest.java`
- `src/test/java/com/muczynski/library/controller/BooksFromFeedControllerTest.java` (uses picker session)
- `src/test/java/com/muczynski/library/ui/BookByPhotoWorkflowTest.java` (UI test for picker)

**Documentation**: None specific to this file

---

### 2. UserSettingsService.java ✗
**Location**: `src/main/java/com/muczynski/library/service/UserSettingsService.java`

**Problem**: All public methods accept `currentUsername` parameter which is actually user ID
- Line 49: `getUserSettings(String currentUsername)` - treats as username
- Line 54: `updateUserSettings(String currentUsername, ...)` - treats as username
- Line 121: `deleteUser(String currentUsername)` - treats as username

**Fix**: Change parameter type to `Long userId` and look up by ID

**Tests to update**:
- `src/test/java/com/muczynski/library/service/UserSettingsServiceTest.java`

**Documentation**: None specific to this file

---

### 3. UserSettingsController.java ✗
**Location**: `src/main/java/com/muczynski/library/controller/UserSettingsController.java`

**Problem**: `extractUsername()` method treats principal name as username
- Line 30-57: `extractUsername()` returns principal.getName() which is user ID
- Line 62: `getUserSettings()` - passes to service as username
- Line 69: `updateUserSettings()` - passes to service as username
- Line 77: `deleteUser()` - passes to service as username

**Fix**: Rename to `extractUserId()` and parse as Long

**Tests to update**:
- `src/test/java/com/muczynski/library/controller/UserSettingsControllerTest.java`
- `src/test/java/com/muczynski/library/ui/SettingsUITest.java` (UI test)

**Documentation**: None specific to this file

---

### 4. GoogleOAuthController.java ✗
**Location**: `src/main/java/com/muczynski/library/controller/GoogleOAuthController.java`

**Problem**: Uses `authentication.getName()` as username
- Line 80: `authorize()` - `String username = authentication.getName()`
- Line 231: `revoke()` - `String username = authentication.getName()`

**Current code**:
```java
String username = authentication.getName();
User user = findUserByUsername(username);  // WRONG - username is actually user ID!
```

**Fix**: Parse as user ID
```java
Long userId = Long.parseLong(authentication.getName());
User user = userRepository.findById(userId)
    .orElseThrow(() -> new LibraryException("User not found"));
```

**Methods to fix**:
- `authorize()` - line 80
- `callback()` - receives username from stateToken storage (needs investigation)
- `revoke()` - line 231
- Remove `findUserByUsername()` helper method if no longer needed

**Tests to update**:
- No dedicated test file (manual testing required)
- `src/test/java/com/muczynski/library/ui/SettingsUITest.java` (UI test for OAuth flow)

**Documentation**:
- `sso.md` - Update if OAuth flow changes
- `feature-design-security.md` - Update if authentication flow changes

---

### 5. GooglePhotosDiagnosticController.java ✗
**Location**: `src/main/java/com/muczynski/library/controller/GooglePhotosDiagnosticController.java`

**Problem**: All test methods use `authentication.getName()` as username
- Line 44: `testToken()` - treats as username
- Line 85: `testListAlbums()` - treats as username
- Line 131: `testSearchSimple()` - treats as username
- Line 182: `testSearchWithDateFilter()` - treats as username

**Fix**: Parse as user ID in all methods

**Tests to update**:
- This IS a diagnostic controller (for manual testing), no unit tests exist
- Manual testing required after fix

**Documentation**: None specific to this file

---

### 6. PhotoExportService.java ✗
**Location**: `src/main/java/com/muczynski/library/service/PhotoExportService.java`

**Problem**: Uses `authentication.getName()` as username
- Line 559: `getPhotoExportInfo()` - treats as username
- Line 577: `startPhotoExport()` - treats as username
- Line 703: `verifyPhotoExport()` - treats as username

**Current pattern**:
```java
String username = authentication.getName();
Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);
```

**Fix**: Parse as user ID

**Tests to update**:
- `src/test/java/com/muczynski/library/controller/PhotoExportControllerTest.java`

**Documentation**:
- `feature-design-photos.md` - Verify photo export documentation is accurate
- `feature-design-import-export.md` - Update if export flow changes

---

### 7. AskGrok.java ✗
**Location**: `src/main/java/com/muczynski/library/service/AskGrok.java`

**Problem**: Uses `authentication.getName()` as username
- Line 39: `askAboutBook()` - treats as username
- Line 108: `askAboutAuthor()` - treats as username

**Fix**: Parse as user ID and look up user

**Tests to update**:
- No dedicated test file exists for AskGrok.java
- Consider adding unit tests

**Documentation**: None specific to this file

---

### 8. BooksFromFeedService.java ✗
**Location**: `src/main/java/com/muczynski/library/service/BooksFromFeedService.java`

**Problem**: Uses `authentication.getName()` and passes to `UserSettingsService`
- Needs line numbers (file was truncated in read)

**Fix**: Parse as user ID

**Tests to update**:
- `src/test/java/com/muczynski/library/service/BooksFromFeedServiceTest.java`
- `src/test/java/com/muczynski/library/controller/BooksFromFeedControllerTest.java`
- `src/test/java/com/muczynski/library/ui/BookByPhotoWorkflowTest.java` (UI test)

**Documentation**: None specific to this file

---

### 9. LibraryCardController.java ✗
**Location**: `src/main/java/com/muczynski/library/controller/LibraryCardController.java`

**Problem**: Line 53 treats `authentication.getName()` as username
```java
String username = authentication.getName();
User user = userRepository.findByUsernameIgnoreCase(username)
    .orElseThrow(() -> new LibraryException("User not found"));
```

**Fix**: Parse as user ID

**Tests to update**:
- `src/test/java/com/muczynski/library/controller/LibraryCardControllerTest.java`
- `src/test/java/com/muczynski/library/service/LibraryCardPdfServiceTest.java`
- `src/test/java/com/muczynski/library/ui/ApplyForCardUITest.java` (UI test)

**Documentation**:
- `feature-design-library-cards.md` - Verify library card documentation is accurate

---

### 10. AuthorController.java ✗
**Location**: `src/main/java/com/muczynski/library/controller/AuthorController.java`

**Problem**: Line 151 treats `authentication.getName()` as username

**Fix**: Parse as user ID

**Tests to update**:
- `src/test/java/com/muczynski/library/controller/AuthorControllerTest.java`
- `src/test/java/com/muczynski/library/service/AuthorServiceTest.java`

**Documentation**: None specific to this file

---

### 11. BookController.java ✗
**Location**: `src/main/java/com/muczynski/library/controller/BookController.java`

**Problem**: Line 191 treats `authentication.getName()` as username

**Fix**: Parse as user ID

**Tests to update**:
- `src/test/java/com/muczynski/library/controller/BookControllerTest.java`
- `src/test/java/com/muczynski/library/controller/BookCacheIntegrationTest.java`
- `src/test/java/com/muczynski/library/ui/BooksUITest.java` (UI test)

**Documentation**: None specific to this file

---

### 12. LoanController.java ✗
**Location**: `src/main/java/com/muczynski/library/controller/LoanController.java`

**Problem**:
- Line 42: treats `authentication.getName()` as username
- Line 77: treats `authentication.getName()` as username

**Fix**: Parse as user ID in both locations

**Tests to update**:
- `src/test/java/com/muczynski/library/controller/LoanControllerTest.java`
- `src/test/java/com/muczynski/library/controller/LoanControllerIntegrationTest.java`
- `src/test/java/com/muczynski/library/ui/LoansUITest.java` (UI test)

**Documentation**: None specific to this file

---

## Files That Are CORRECT ✓

### AuthController.java ✓
**Location**: `src/main/java/com/muczynski/library/controller/AuthController.java`

**Correct usage** (line 121):
```java
Long userId = Long.parseLong(authentication.getName());
User user = userRepository.findById(userId)
    .orElseThrow(() -> new RuntimeException("User not found"));
```

This is the **correct pattern** that all other files should follow!

---

## Related Issues to Investigate

### OAuth Callback State Token Storage
**File**: `GoogleOAuthController.java`

The OAuth callback flow stores username in the state token:
```java
// Line 100: During authorize
stateTokens.put(state, username + ":" + origin);

// Line 171: During callback
String username = parts[0];
User user = findUserByUsername(username);
```

**Question**: When the OAuth flow starts from an authenticated session:
1. Line 95: `String username = authentication.getName();` - is this user ID or username?
2. If it's user ID, the state token will store "42:origin" instead of "username:origin"
3. The callback will then try to look up user by ID string "42" as if it were a username

**Investigation needed**: Test OAuth flow to see if it's broken. May need to:
- Store user ID in state token instead of username
- Or look up user first to get real username before storing in state token

---

## Common Patterns

### ❌ WRONG (Current pattern in most files)
```java
String username = authentication.getName();
User user = userRepository.findByUsernameIgnoreCase(username)
    .orElseThrow(() -> new LibraryException("User not found"));
```

### ✅ CORRECT (Pattern to use)
```java
Long userId = Long.parseLong(authentication.getName());
User user = userRepository.findById(userId)
    .orElseThrow(() -> new LibraryException("User not found"));
```

---

## Testing Strategy

After fixing all bugs:

1. **Test Google Photos Picker** - should work now
2. **Test User Settings** - update settings, change password
3. **Test OAuth Flow** - authorize Google Photos
4. **Test Photo Export** - all three endpoints
5. **Test Ask Grok** - book and author questions
6. **Test Library Cards** - PDF generation
7. **Test Loans** - create/manage loans
8. **Test Books From Feed** - picker session creation

---

## Summary

**Total files with bugs**: 12
**Most critical**: GooglePhotosService.java (breaks picker)
**Pattern**: All use `authentication.getName()` as username instead of user ID
**Fix**: Parse as `Long.parseLong()` and use `userRepository.findById()`

---

## Definition of Done

For this bug fix to be considered complete, ALL of these steps must be accomplished:

### 1. Checkout dev branch
```bash
git checkout dev
git pull
```

### 2. Complete the code fixes
- [ ] Fix all 12 files listed above
- [ ] Ensure all files have copyright headers: `// (c) Copyright 2025 by Muczynski`
- [ ] Follow the correct pattern: `Long userId = Long.parseLong(authentication.getName())`
- [ ] Use `userRepository.findById(userId)` instead of `findByUsernameIgnoreCase(username)`

### 3. Update documentation
- [ ] Update `.md` files if architecture or APIs changed
  - [ ] Review `sso.md` if OAuth flow changes
  - [ ] Review `feature-design-security.md` if authentication flow changes
  - [ ] Review `feature-design-photos.md` for photo export accuracy
  - [ ] Review `feature-design-import-export.md` if export flow changes
  - [ ] Review `feature-design-library-cards.md` for accuracy
- [ ] **DO NOT update CLAUDE.md** (it's for AI context only)
- [ ] Update inline code comments and JavaDoc where appropriate
- [ ] Ensure copyright headers exist at top of every modified source file

### 4. Update ALL tests - Always check and update all three test types:

#### Unit Tests (JUnit)
Test individual classes and methods in isolation:
- [ ] `GooglePhotosServiceTest.java`
- [ ] `UserSettingsServiceTest.java`
- [ ] `UserSettingsControllerTest.java`
- [ ] `PhotoExportControllerTest.java`
- [ ] `AuthorControllerTest.java`
- [ ] `AuthorServiceTest.java`
- [ ] `BookControllerTest.java`
- [ ] `BookCacheIntegrationTest.java`
- [ ] `LoanControllerTest.java`
- [ ] Consider adding: `AskGrokTest.java` (currently missing)

#### Integration Tests
Test API endpoints and service interactions:
- [ ] `BooksFromFeedControllerTest.java`
- [ ] `BooksFromFeedServiceTest.java`
- [ ] `LibraryCardControllerTest.java`
- [ ] `LibraryCardPdfServiceTest.java`
- [ ] `LoanControllerIntegrationTest.java`

#### UI Tests (Playwright)
Test end-to-end user flows:
- [ ] `BookByPhotoWorkflowTest.java` (Google Photos picker)
- [ ] `SettingsUITest.java` (User settings and OAuth flow)
- [ ] `BooksUITest.java` (Book management)
- [ ] `LoansUITest.java` (Loan management)
- [ ] `ApplyForCardUITest.java` (Library card generation)

**Why all three?** Other programmers make errors that need to be caught at multiple levels.

### 5. Run verifier
Run the fast-tests.sh script (runs all tests except UI tests - UI tests are too slow):
```bash
./fast-tests.sh
```

**What this does**: Runs all JUnit unit tests and integration tests, but skips UI tests.

**If tests fail**: Fix the failing tests before proceeding. Common issues:
- Tests may be using hardcoded usernames instead of user IDs
- Test mocks may need to return user IDs instead of usernames
- Authentication mocks may need to return `user.getId().toString()` instead of `user.getUsername()`

### 6. Manual testing
After automated tests pass:
- [ ] Test Google Photos Picker - should work now (CRITICAL)
- [ ] Test User Settings - update settings, change password
- [ ] Test OAuth Flow - authorize/revoke Google Photos
- [ ] Test Photo Export - all three endpoints
- [ ] Test Ask Grok - book and author questions
- [ ] Test Library Cards - PDF generation
- [ ] Test Loans - create/manage loans
- [ ] Test Books From Feed - picker session creation
- [ ] Test Diagnostic Controller - all endpoints (manual only)

### 7. Push to dev
```bash
git add .
git commit -m "Fix user lookup bug: use user ID instead of username

- Fixed 12 files that incorrectly treated authentication.getName() as username
- Updated all unit tests, integration tests, and UI tests
- Critical fix: Google Photos picker now works
- Pattern: Changed from findByUsernameIgnoreCase() to findById()

🤖 Generated with Claude Code (https://claude.com/claude-code)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

git push origin dev
```

---

## Test Categories Summary

| Category | Test Count | Location |
|----------|------------|----------|
| Unit Tests | 10+ | `src/test/java/com/muczynski/library/controller/` and `.../service/` |
| Integration Tests | 5 | `src/test/java/com/muczynski/library/controller/` |
| UI Tests | 5 | `src/test/java/com/muczynski/library/ui/` |
| Manual Tests | 9 features | Manual testing required after automated tests |

**Total test files to update**: ~20 files

---

## Quick Reference: Files to Modify

### Code Files (12)
1. GooglePhotosService.java ⚠️ CRITICAL
2. UserSettingsService.java
3. UserSettingsController.java
4. GoogleOAuthController.java
5. GooglePhotosDiagnosticController.java
6. PhotoExportService.java
7. AskGrok.java
8. BooksFromFeedService.java
9. LibraryCardController.java
10. AuthorController.java
11. BookController.java
12. LoanController.java

### Test Files (~20)
See "Update ALL tests" section above for complete list

### Documentation Files (4-5)
1. sso.md (if OAuth flow changes)
2. feature-design-security.md (if auth flow changes)
3. feature-design-photos.md (verify accuracy)
4. feature-design-import-export.md (if export changes)
5. feature-design-library-cards.md (verify accuracy)
