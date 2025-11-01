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
   - Use `PlaywrightAssertions.assertThat()` for visibility (`isVisible()`), text content (`hasText()`), value (`hasValue()`), and count (`hasCount()`). Include timeouts (e.g., `setTimeout(20000L)`) to handle async rendering.
   - Selectors prioritize stable, test-friendly attributes like `[data-test='element-id']`. For form inputs, use more specific selectors like `[data-test-form='input-name']` to avoid collisions with table cells that may use the same `data-test` value.
   - When the same `data-test` value appears in multiple contexts (e.g., `<td data-test="loan-date">` vs `<input data-test="loan-date">`), use scoped selectors (`#section-id [data-test='...']`) or additional specific attributes (`data-test-form`) to disambiguate.
   - Use `filter(new Locator.FilterOptions().setHasText(...))` for dynamic lists.
   - Error handling: Screenshots on failure (`page.screenshot()`) and exception throwing for debugging.

4. **Test Structure for Features**:
   - CRUD-focused: Create (fill form, click add), read (wait for row, assert visibility/count), update (edit click, modify, assert change), delete (confirm dialog, wait for detach).
   - Authentication: Handle login flows, role-based visibility (e.g., librarian-only sections), and session persistence.
   - Data-Driven: Use `@Sql` for DB setup (e.g., `data-books.sql`); ensure reproducible state.

5. **Async and Reliability Handling**:
   - Explicit waits (`waitForSelector()`, `waitForFunction()`) over sleeps to handle AJAX/SPA updates.
   - Use `LoadState.NETWORKIDLE` after CRUD operations to ensure both the API call and subsequent data refresh complete before continuing.
   - For elements that depend on async data loading (dropdowns, date inputs), wait for the data to be populated, not just for the element to be visible. Use `waitForFunction()` to check for options or values.
   - UUIDs for unique test data (e.g., `UUID.randomUUID()`) to avoid conflicts.

6. **Timeout Requirements**:
   - Maximum timeout allowed: **20 seconds (20000L)**.
   - Always set `page.setDefaultTimeout(20000L)` in `@BeforeEach` to ensure consistent timeout behavior.
   - For CRUD operations involving network calls and DOM updates, use `LoadState.NETWORKIDLE` waits with 20-second timeouts.
   - For simple element visibility checks, 20-second timeouts are still required due to potential database transaction delays and DOM rendering time.
   - When waiting for elements to appear after state changes, always specify the expected state: `.setState(WaitForSelectorState.VISIBLE)`.
   - When waiting for elements to disappear, use `hasCount(0)` assertions with timeouts rather than `waitFor()` with DETACHED state. 

These patterns make the tests a robust foundation for full-stack UI testing.

## Requirements for UI Testing
UI testing a Spring Boot + Playwright app like this library system requires balancing reliability, maintainability, and coverage. The following outlines essential requirements:

1. **Stable and Maintainable Selectors**:
   - Use `data-test` attributes consistently to decouple tests from UI implementation. Avoid CSS classes/IDs that change with styling. Ensure frontend developers add them for all interactive elements.

2. **Robust Waiting and Async Handling**:
   - Always use explicit waits (`waitForSelector()`, `waitForFunction()`) over sleeps, as the app has async API calls (e.g., `fetchData()`). For SPA-like behavior (e.g., section switching), poll with JS to confirm state changes. Set global timeouts to 20 seconds (`page.setDefaultTimeout(20000L)`) and retry failed locators to prevent flakiness.
   - **Critical:** After any CRUD operation (Create, Update, Delete), use `page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L))` to ensure the POST/PUT/DELETE request AND the subsequent GET request to refresh the list have both completed.
   - **Dropdown/Form Population:** When navigating to a section with form inputs (especially loans), the section must call a function to populate dropdowns and set default values. Check that the section's load function (e.g., `loadLoansSection()`) properly initializes all form elements. Wait for form elements to contain data, not just to be visible.

3. **Test Data Management**:
   - Leverage `@Sql` scripts for clean, repeatable DB state (e.g., inserting test books/authors). Use `@DirtiesContext` to reset between tests. Keep SQL minimal and version-controlled; combine with mocks (`@MockitoBean`) for external services (e.g., AI API).

4. **Isolation and Environment Setup**:
   - Run on random ports (`webEnvironment = RANDOM_PORT`) to avoid conflicts. Use profiles (`@ActiveProfiles("test")`) for H2 in-memory DB. Enable headless mode (`setHeadless(true)`) for CI compatibility (e.g., GitHub Actions). Integrate with Gradle/Maven for `./gradlew test`; add `--info` for verbose logs.

5. **Coverage and Edge Cases**:
   - Cover happy paths (CRUD) plus errors (e.g., invalid login, no API key). Test role-based access (public vs. librarian). Aim for 80%+ UI coverage on critical flows (login, search, forms); use tools like Playwright Trace Viewer for debugging.

6. **Error Handling and Debugging**:
   - Include screenshots/videos on failure (extend to `page.video()` for recordings). Handle network errors (e.g., 401 redirects to login). In CI, use artifacts for failure outputs. Target <5% flakiness with retry mechanisms.

7. **Production Code Requirements for Testability**:
   - **Logging:** Add console logging to key async functions (e.g., `populateLoanDropdowns()`, section load functions) to help diagnose test failures. Use prefixed logs like `[Loans]`, `[Sections]` for easy filtering.
   - **Section Load Functions:** Each section that has forms must have a dedicated load function (e.g., `loadLoansSection()`) that:
     - Calls the data loading function (e.g., `loadLoans()`)
     - Calls any initialization functions (e.g., `populateLoanDropdowns()` for form dropdowns and default values)
     - Logs start and completion for debugging
   - **Selector Uniqueness:** When elements in tables and forms share the same semantic meaning (e.g., "loan-date"), use different selector attributes:
     - Table cells: `data-test="loan-date"` (for display)
     - Form inputs: `data-test-form="loan-date-input"` (for interaction)
     - This prevents Playwright's strict mode violations when selectors resolve to multiple elements.
   - **Form Initialization:** Forms with date inputs or dropdowns must be populated during section load, not just when data changes. Default values should be set immediately when showing the section.

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
When setting timeouts on `Browser.NewContextOptions`, use `long` values (e.g., `20000L`) to match the API signature, which expects milliseconds as a `long`. Using `int` (e.g., `20000`) will cause a compilation error: "cannot find symbol setTimeout(int)". Always append `L` for long literals in timeout configurations to ensure compatibility. This applies to all timeout options in Playwright Java APIs, such as `WaitForSelectorOptions`, `WaitForLoadStateOptions`, and `Locator.WaitForOptions`, to prevent similar compilation issues. Instead of setting timeouts on `NewContextOptions` (which may not be supported in all versions), use `page.setDefaultTimeout(20000L)` after creating the page to enforce the 20-second limit across test interactions.

By following these guidelines, tests remain reliable and adhere to the project's timeout constraints while avoiding Playwright's Java-specific serialization pitfalls.

## Common UI Test Issues and Solutions

### Issue 1: Dropdowns/Inputs Not Populated When Section Loads
**Symptom:** Test fails with timeout waiting for dropdown options or date inputs to be populated.

**Root Cause:** The section's load function only loads data for display (e.g., `loadLoans()`) but doesn't initialize form elements.

**Solution:**
1. Create a wrapper function (e.g., `loadLoansSection()`) that calls both data loading and form initialization
2. Update `sections.js` sectionConfig to use the wrapper function
3. Add logging to verify the function is called
4. In tests, use `waitForFunction()` to check for populated data:
   ```java
   page.waitForFunction("() => document.querySelector('[data-test-form=\"loan-book-select\"]').options.length > 0",
       null, new Page.WaitForFunctionOptions().setTimeout(20000L));
   ```

### Issue 2: Selector Resolves to Multiple Elements (Strict Mode Violation)
**Symptom:** `Error: strict mode violation: locator("[data-test='loan-date']") resolved to 2 elements`

**Root Cause:** The same `data-test` value is used in both table cells (`<td>`) and form inputs (`<input>`).

**Solution:**
1. Add distinct attributes: `data-test` for table cells, `data-test-form` for form inputs
2. Update HTML to include both attributes on form inputs
3. Update tests to use the specific selector (`data-test-form` for form interactions)

### Issue 3: CRUD Operations Timeout Waiting for List Updates
**Symptom:** Test creates/updates/deletes an item successfully, but times out waiting for the list to refresh.

**Root Cause:** Test doesn't wait for the network to be idle after the operation. The POST/PUT/DELETE completes but the subsequent GET to refresh the list may still be in progress.

**Solution:**
1. Replace `page.waitForLoadState(LoadState.DOMCONTENTLOADED, ...)` with `page.waitForLoadState(LoadState.NETWORKIDLE, ...)`
2. Use 20-second timeouts for CRUD operations
3. Always specify expected state when waiting: `.setState(WaitForSelectorState.VISIBLE)`

### Issue 4: Elements Exist But Test Times Out
**Symptom:** Manual testing shows the element exists, but the test times out finding it.

**Root Cause:**
- Insufficient timeout (was 5s, needed 20s due to DB transactions + network + DOM rendering)
- Not waiting for the element to be in the expected state (visible, attached, etc.)

**Solution:**
1. Increase all timeouts to 20 seconds
2. Always specify the expected state: `.setState(WaitForSelectorState.VISIBLE)`
3. For disappearing elements, use `hasCount(0)` assertions instead of `waitFor(DETACHED)`

### Debugging Checklist
When a UI test fails:
1. ✅ Check browser console logs (look for `[Sections]`, `[Loans]`, etc. prefixes)
2. ✅ Verify the element selector matches only one element (check for strict mode violations)
3. ✅ Confirm async operations complete (check NETWORKIDLE waits after CRUD)
4. ✅ Review screenshots generated on failure
5. ✅ Check if the production code calls initialization functions when loading the section
6. ✅ Verify form elements are populated with data, not just visible
7. ✅ Ensure timeouts are set to 20 seconds for all operations
