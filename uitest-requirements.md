# UI Test Requirements

## Overview
The requirements in this file are requirements for this project/application.

These tests demonstrate robust Playwright usage: launching a browser, navigating pages, interacting with elements (e.g., adding books, searching inventory), handling forms, and asserting UI states. They are designed for a full-stack Java/Spring application with dynamic SPA-like behavior.

## Required Patterns in the UITests
The tests must follow consistent patterns reflecting best practices in Playwright-based UI testing:

1. **Browser Automation and Setup/Teardown**:
   - Launch a headless Chromium browser (`playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))`).
   - Lifecycle management: `@BeforeAll` for browser launch, `@BeforeEach` for new context/page creation, `@AfterEach` for cleanup, and `@AfterAll` for browser close. This ensures isolated test environments.
   - Viewport sizing (e.g., `setViewportSize(1280, 720)`) to simulate desktop views and avoid layout issues.

2. **Navigation and Interaction Patterns**:
   - Page navigation (`page.navigate("http://localhost:" + port)`) and waiting for load states (`page.waitForLoadState(LoadState.DOMCONTENTLOADED)`).
   - Element interactions: `page.click()`, `page.fill()`, `page.selectOption()`, `page.setInputFiles()` for uploads, and handling dialogs (`page.onDialog(dialog -> dialog.accept())`).
   - Custom helper methods like `login()` and `navigateToSection()` for reusable flows.

3. **Assertions and Selectors**:
   - Use `PlaywrightAssertions.assertThat()` for visibility (`isVisible()`), text content (`hasText()`), value (`hasValue()`), and count (`hasCount()`). Include timeouts (e.g., `setTimeout(5000)`) to handle async rendering.
   - Selectors prioritize stable, test-friendly attributes like `[data-test='element-id']`. Use `filter(new Locator.FilterOptions().setHasText(...))` for dynamic lists.
   - Error handling: Screenshots on failure (`page.screenshot()`) and exception throwing for debugging.

4. **Test Structure for Features**:
   - CRUD-focused: Create (fill form, click add), read (wait for row, assert visibility/count), update (edit click, modify, assert change), delete (confirm dialog, wait for detach).
   - Authentication: Handle login flows, role-based visibility (e.g., librarian-only sections), and session persistence.
   - Data-Driven: Use `@Sql` for DB setup (e.g., `data-books.sql`); ensure reproducible state.

5. **Async and Reliability Handling**:
   - Explicit waits (`waitForSelector()`, `waitForFunction()`) over sleeps to handle AJAX/SPA updates.
   - UUIDs for unique test data (e.g., `UUID.randomUUID()`) to avoid conflicts.

6. Timeouts are never to exceed 5 seconds. It is likewise not allowed to remove timeouts so that a larger default timeout is used. 

These patterns make the tests a robust foundation for full-stack UI testing.

## Requirements for UI Testing
UI testing a Spring Boot + Playwright app like this library system requires balancing reliability, maintainability, and coverage. The following outlines essential requirements:

1. **Stable and Maintainable Selectors**:
   - Use `data-test` attributes consistently to decouple tests from UI implementation. Avoid CSS classes/IDs that change with styling. Ensure frontend developers add them for all interactive elements.

2. **Robust Waiting and Async Handling**:
   - Always use explicit waits (`waitForSelector()`, `waitForFunction()`) over sleeps, as the app has async API calls (e.g., `fetchData()`). For SPA-like behavior (e.g., section switching), poll with JS to confirm state changes. Set global timeouts (e.g., 10s) and retry failed locators to prevent flakiness.

3. **Test Data Management**:
   - Leverage `@Sql` scripts for clean, repeatable DB state (e.g., inserting test books/authors). Use `@DirtiesContext` to reset between tests. Keep SQL minimal and version-controlled; combine with mocks (`@MockitoBean`) for external services (e.g., AI API).

4. **Isolation and Environment Setup**:
   - Run on random ports (`webEnvironment = RANDOM_PORT`) to avoid conflicts. Use profiles (`@ActiveProfiles("test")`) for H2 in-memory DB. Enable headless mode (`setHeadless(true)`) for CI compatibility (e.g., GitHub Actions). Integrate with Gradle/Maven for `./gradlew test`; add `--info` for verbose logs.

5. **Coverage and Edge Cases**:
   - Cover happy paths (CRUD) plus errors (e.g., invalid login, no API key). Test role-based access (public vs. librarian). Aim for 80%+ UI coverage on critical flows (login, search, forms); use tools like Playwright Trace Viewer for debugging.

6. **Error Handling and Debugging**:
   - Include screenshots/videos on failure (extend to `page.video()` for recordings). Handle network errors (e.g., 401 redirects to login). In CI, use artifacts for failure outputs. Target <5% flakiness with retry mechanisms.

