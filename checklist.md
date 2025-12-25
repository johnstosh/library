# UI Test Implementation Checklist

## Rules
1. **Make a *UITest class to test a feature. Stop when you're done.**
2. **All testing must pass to consider the work done.**
3. **Don't disable any tests.**
4. **Don't commit.**
5. **Other than the login test, bypass the login sequence when testing other features.**

## Progress Tracking

### Step zero
- [✓] Fix failing tests like RandomBookTest > testRandomBookCreation()
  - **Status**: Complete - Fixed SQL initialization conflicts
  - **Solution**: Created data-base.sql with roles only, renamed data.sql to data-default.sql, fixed ImportControllerIntegrationTest to use existing roles

### Public Pages
- [🚧] LoginUITest - Login page with form-based and OAuth login
  - **Status**: Backend authentication API implemented, ready for testing
  - **Completed**:
    - ✅ Frontend builds successfully (no TypeScript errors)
    - ✅ Created AuthController with /api/auth/login, /api/auth/logout, /api/auth/me endpoints
    - ✅ Added CurrentUserDto and LoginRequestDto
    - ✅ Updated User class with getHighestAuthority() and getAuthorities() methods
    - ✅ Fixed SecurityConfig to permit /assets/** for Vite build files
    - ✅ Updated data-login.sql with correct BCrypt(SHA-256("password")) hash
    - ✅ All backend tests (215 tests) passing
  - **Next Steps**: Run LoginUITest to verify frontend-backend integration (8 tests, ~4 min runtime)
- [ ] SearchUITest - Public search functionality
- [ ] ApplyForCardUITest - Public library card application

### Core Features (Authenticated)
- [ ] BooksUITest - Book CRUD, filters, photos, LOC lookup
- [ ] AuthorsUITest - Author CRUD, filters, photos
- [ ] LoansUITest - Loan checkout, return, overdue tracking
- [ ] LibrariesUITest - Library CRUD operations

### Photo & Import Features (Librarian)
- [ ] BooksFromFeedUITest - Google Photos import with AI processing
- [ ] DataManagementUITest - JSON export/import, photo export

### User Management (Librarian)
- [ ] UsersUITest - User CRUD operations

### Library Cards (Mixed Access)
- [ ] MyLibraryCardUITest - User's library card view and PDF
- [ ] ApplicationsUITest - Librarian card application approvals

### Utility Features (Librarian)
- [ ] LabelsUITest - PDF label generation for books
- [ ] TestDataUITest - Test data generation

### Settings
- [ ] UserSettingsUITest - User profile and settings
- [ ] GlobalSettingsUITest - OAuth configuration (Librarian)

## Notes
- Use Playwright for all UI tests
- All tests should use `data-test` attributes for element selection
- Bypass login for non-login tests (use direct session setup or API calls)
- Verify CRUD operations with assertions
- Test error handling and validation
