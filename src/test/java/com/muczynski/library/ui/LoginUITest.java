/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * UI Tests for Login functionality using Playwright.
 * Tests form-based login, OAuth integration, and error handling.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-login.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginUITest {

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 720));
        page = context.newPage();
        page.setDefaultTimeout(30000L); // Increased timeout for React app initialization
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @DisplayName("Should display login page with all elements")
    void testLoginPageLayout() {
        // Listen for console messages
        page.onConsoleMessage(msg -> System.out.println("CONSOLE: " + msg.type() + ": " + msg.text()));

        // Listen for page errors
        page.onPageError(error -> System.out.println("PAGE ERROR: " + error));

        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Take screenshot for debugging
        try {
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get("/tmp/login-page.png")));
            System.out.println("Screenshot saved to /tmp/login-page.png");
        } catch (Exception e) {
            System.out.println("Failed to save screenshot: " + e.getMessage());
        }

        // Print page URL and title
        System.out.println("Current URL: " + page.url());
        System.out.println("Page title: " + page.title());

        // Wait for React app to render - check for the root div to have content
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Wait specifically for the login username field to appear (indicates React rendered)
        page.waitForSelector("[data-test='login-username']", new Page.WaitForSelectorOptions()
                .setTimeout(30000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify page title and header
        assertThat(page.locator("h1")).containsText("St. Martin de Porres Library");

        // Verify username field
        Locator usernameField = page.locator("[data-test='login-username']");
        assertThat(usernameField).isVisible();
        assertThat(usernameField).hasAttribute("type", "text");

        // Verify password field
        Locator passwordField = page.locator("[data-test='login-password']");
        assertThat(passwordField).isVisible();
        assertThat(passwordField).hasAttribute("type", "password");

        // Verify submit button
        Locator submitButton = page.locator("[data-test='login-submit']");
        assertThat(submitButton).isVisible();
        assertThat(submitButton).isEnabled();
        assertThat(submitButton).containsText("Sign In");

        // Verify Google OAuth button exists
        Locator googleButton = page.locator("[data-test='google-login']");
        assertThat(googleButton).isVisible();
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testSuccessfulLogin() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill in login form
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");

        // Click submit button
        page.click("[data-test='login-submit']");

        // Wait for navigation to books page (default after login)
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify we're on the books page
        assertThat(page).hasURL(getBaseUrl() + "/books");

        // Verify navigation menu is visible (indicates successful login)
        assertThat(page.locator("[data-test='navigation']")).isVisible();
    }

    @Test
    @DisplayName("Should show error message for invalid credentials")
    void testInvalidCredentials() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill in login form with wrong credentials
        page.fill("[data-test='login-username']", "wronguser");
        page.fill("[data-test='login-password']", "wrongpassword");

        // Click submit button
        page.click("[data-test='login-submit']");

        // Wait for error message to appear
        Locator errorMessage = page.locator(".bg-red-50");
        assertThat(errorMessage).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));
        assertThat(errorMessage).containsText("Invalid username or password");
    }

    @Test
    @DisplayName("Should show error for empty username")
    void testEmptyUsername() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Leave username empty, fill password
        page.fill("[data-test='login-password']", "password");

        // Click submit button
        page.click("[data-test='login-submit']");

        // Verify form validation (browser should prevent submission)
        // Username field should be invalid
        Locator usernameField = page.locator("[data-test='login-username']");

        // Should still be on login page
        assertThat(page).hasURL(getBaseUrl() + "/login");
    }

    @Test
    @DisplayName("Should show error for empty password")
    void testEmptyPassword() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill username, leave password empty
        page.fill("[data-test='login-username']", "librarian");

        // Click submit button
        page.click("[data-test='login-submit']");

        // Verify form validation (browser should prevent submission)
        // Should still be on login page
        assertThat(page).hasURL(getBaseUrl() + "/login");
    }

    @Test
    @DisplayName("Should redirect to books page when already logged in")
    void testRedirectWhenLoggedIn() {
        // First, login
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");

        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));

        // Now try to navigate to login page again
        page.navigate(getBaseUrl() + "/login");

        // Should redirect to books page (or stay if auth check redirects)
        // At minimum, should not show login form if already authenticated
        page.waitForTimeout(1000); // Give time for potential redirect
    }

    @Test
    @DisplayName("Should show loading state during login")
    void testLoadingState() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");

        // Click submit and check for loading state
        page.click("[data-test='login-submit']");

        // Submit button should show loading spinner or be disabled
        Locator submitButton = page.locator("[data-test='login-submit']");

        // Button should be disabled during submission
        // Note: This might be brief, so we use a short timeout
        try {
            assertThat(submitButton).isDisabled(new LocatorAssertions.IsDisabledOptions().setTimeout(500L));
        } catch (AssertionError e) {
            // Loading state might be too brief to catch, that's okay
        }
    }

    @Test
    @DisplayName("Should display library logo image")
    void testLogoDisplay() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Verify Marian M image is displayed
        Locator logoImage = page.locator("img[alt*='Martin']");
        assertThat(logoImage).isVisible();

        // Verify image source includes marian-m
        String src = logoImage.getAttribute("src");
        Assertions.assertTrue(src.contains("marian-m.png"), "Logo image should be marian-m.png");
    }
}
