# UI Tests - Execution Guide

## Overview

This project includes Playwright-based UI tests for testing the web interface end-to-end. These tests are located in `src/test/java/com/muczynski/library/ui/`.

## Test Coverage

### SettingsUITest (23 test cases)

Comprehensive tests for Settings pages covering both USER and LIBRARIAN authorities:

**USER Authority Tests (9 tests):**
- View account information
- View and select library card designs (5 design options)
- Change library card design with success validation
- View and submit password change form
- Password validation (mismatch detection)
- Verify XAI Configuration section is NOT visible
- Verify Google Photos Integration section is NOT visible
- Verify redirect when accessing Global Settings (unauthorized)

**LIBRARIAN Authority Tests (14 tests):**
- User Settings:
  - View all sections including librarian-only features
  - Verify LIBRARIAN authority display
  - Update XAI API Key
  - View and configure Google Photos Integration
  - Update Google Photos Album ID
  - Cancel password changes

- Global Settings:
  - Access Global Settings page
  - View all sections (SSO, Photos API, Redirect URI)
  - Update SSO and OAuth credentials
  - View redirect URI information
  - Cancel settings changes

### Other UI Tests

- **LoginUITest**: Authentication and login flow tests
- **BooksUITest**: Book management CRUD operations
- **SearchUITest**: Search functionality tests
- **ApplyForCardUITest**: Library card application tests
- **NavigationUITest**: Navigation and routing tests

## System Requirements

### Required Dependencies

UI tests require Playwright browser dependencies to be installed on the system:

```bash
# On Ubuntu/Debian
sudo apt-get install -y libgbm1

# Or using Playwright's installer
sudo mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install-deps"
```

### Java Requirements

- Java 17 or later
- Gradle 8.x

## Running UI Tests

### Run All UI Tests

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew test --tests "com.muczynski.library.ui.*"
```

### Run Specific Test Class

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew test --tests "com.muczynski.library.ui.SettingsUITest"
```

### Run Single Test Method

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew test --tests "com.muczynski.library.ui.SettingsUITest.testUserCanViewAccountInformation"
```

### Run Non-UI Tests Only (Fast)

```bash
./fast-tests.sh
```

This script runs all unit and integration tests but excludes UI tests for faster execution.

## Test Data

Each UI test uses dedicated SQL scripts for test data initialization:

- `data-settings.sql`: Users for Settings tests (testuser, librarian)
- `data-login.sql`: User for login tests
- `data-books.sql`: Books and authors for book management tests
- `data-search.sql`: Data for search functionality tests

Test data is loaded before each test method using `@Sql` annotation and cleaned up after with `@DirtiesContext`.

## CI/CD Integration

UI tests are designed to run in automated CI/CD pipelines. The test execution environment should have:

1. **Browser Dependencies**: Install libgbm1 and other Chromium dependencies
2. **Headless Mode**: Tests run in headless mode (no display required)
3. **Timeouts**: 20-second timeouts for all operations to handle network delays
4. **Test Isolation**: Each test gets a fresh database state via `@DirtiesContext`

## Troubleshooting

### Missing libgbm.so.1 Error

```
error while loading shared libraries: libgbm.so.1: cannot open shared object file
```

**Solution**: Install browser dependencies:
```bash
sudo apt-get install -y libgbm1
```

### Java Version Errors

```
Dependency requires at least JVM runtime version 17
```

**Solution**: Set JAVA_HOME to Java 17:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### Tests Timing Out

If tests timeout waiting for elements:
1. Check that the Spring Boot application starts successfully
2. Verify database connections are working
3. Check browser console logs in test output
4. Review screenshots saved to `/tmp/` on failure

## Test Patterns and Best Practices

### Selectors

All tests use `data-test` attributes for stable, reliable element selection:

```java
page.click("[data-test='save-settings']")
```

### Waits

Tests use explicit waits with `LoadState.NETWORKIDLE` after CRUD operations:

```java
page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));
```

### Assertions

Playwright assertions with appropriate timeouts:

```java
assertThat(page.locator("text=Success")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
```

### Login Helper

Reusable login method for authenticated test scenarios:

```java
private void login(String username, String password) {
    page.navigate(getBaseUrl() + "/login");
    page.waitForLoadState(LoadState.NETWORKIDLE);
    page.fill("[data-test='login-username']", username);
    page.fill("[data-test='login-password']", password);
    page.click("[data-test='login-submit']");
    page.waitForLoadState(LoadState.NETWORKIDLE);
}
```

## Environment Limitations

**Note**: UI tests require Playwright browser dependencies (libgbm1, etc.) which need system-level installation. On systems without sudo access or in restricted environments, UI tests may not be executable locally. In such cases:

1. Tests are verified to compile successfully
2. Code follows established patterns from working UI tests
3. Tests can be run in CI/CD environments with proper dependencies
4. Use `./fast-tests.sh` to run non-UI tests during development

## Test Maintenance

When modifying frontend components:

1. **Add `data-test` attributes** to all interactive elements
2. **Update tests** when UI structure changes
3. **Run specific test class** during development
4. **Verify all UI tests** before merging changes

## For More Information

See `uitest-requirements.md` for detailed testing patterns, requirements, and common issues.
