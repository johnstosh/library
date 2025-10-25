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
   - Use `PlaywrightAssertions.assertThat()` for visibility (`isVisible()`), text content (`hasText()`), value (`hasValue()`), and count (`hasCount()`). Include timeouts (e.g., `setTimeout(5000L)`) to handle async rendering.
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

## Appendix: Avoiding Playwright Java Serialization Errors with waitForFunction

In Playwright Java, using `page.waitForFunction(jsExpression, new Page.WaitForFunctionOptions().setTimeout(5000))` can lead to a `PlaywrightException: Unsupported type of argument: com.microsoft.playwright.Page$WaitForFunctionOptions@...` error. This occurs because the `WaitForFunctionOptions` object cannot be properly serialized for transmission to the browser during the wait operation.

### How to Avoid This Error
1. **Omit Options from waitForFunction**: Do not pass `WaitForFunctionOptions` to `waitForFunction`. Rely on the default timeout (typically 30 seconds, but you can set a global timeout via `page.setDefaultTimeout(5000L)` after creating the page in `@BeforeEach` to enforce the 5-second limit across all waits). Example:
   ```
   page.waitForFunction(jsExpression);
   ```
   Ensure your JavaScript expression throws an error if the condition is not met, so the wait fails explicitly rather than timing out indefinitely.

2. **Use Alternative Waiting Methods**: Replace `waitForFunction` with more reliable alternatives that support options without serialization issues:
   - `page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE))` for element visibility.
   - `locator.waitFor(new Locator.WaitForOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE))` for locators.
   - For complex JS polling, use `page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L))` or chain multiple simple waits.

3. **Set Global Timeouts**: In your test setup (`@BeforeEach`), after creating the page, configure the page's default timeout to cap all operations at 5 seconds:
   ```
   page = context.newPage();
   page.setDefaultTimeout(5000L);  // Applies to all waits and actions on this page
   ```
   This ensures compliance with the 5-second timeout rule without per-call options. The `setDefaultTimeout` method sets the default timeout for actions, navigation, and waits on the page.

4. **Debugging Tip**: If the error persists, verify your Playwright Java version (use the latest stable, e.g., 1.47.0+). Test the JS expression in the browser console first to ensure it evaluates correctly. For serialization issues, simplify the expression to avoid complex objects.

### Additional Note on Context Timeouts
When setting timeouts on `Browser.NewContextOptions`, use `long` values (e.g., `5000L`) to match the API signature, which expects milliseconds as a `long`. Using `int` (e.g., `5000`) will cause a compilation error: "cannot find symbol setTimeout(int)". Always append `L` for long literals in timeout configurations to ensure compatibility. This applies to all timeout options in Playwright Java APIs, such as `WaitForSelectorOptions`, `WaitForLoadStateOptions`, and `Locator.WaitForOptions`, to prevent similar compilation issues. Instead of setting timeouts on `NewContextOptions` (which may not be supported in all versions), use `page.setDefaultTimeout(5000L)` after creating the page to enforce the 5-second limit across test interactions.

By following these guidelines, tests remain reliable and adhere to the project's timeout constraints while avoiding Playwright's Java-specific serialization pitfalls.
