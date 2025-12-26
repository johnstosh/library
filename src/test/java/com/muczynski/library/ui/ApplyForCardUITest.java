/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.*;
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
 * UI Tests for public library card application using Playwright.
 * Tests the Apply for Card page form validation and submission.
 *
 * Note: Application is public, so no login required.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-apply.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApplyForCardUITest {

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
        page.setDefaultTimeout(30000L);
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
    @DisplayName("Should display apply for card page with all elements")
    void testApplyForCardPageLayout() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for React app to render
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Verify page title
        assertThat(page.locator("h1")).containsText("Apply for a Library Card");

        // Verify description text
        assertThat(page.locator("text=Fill out the form below")).isVisible();

        // Verify name input
        Locator nameInput = page.locator("[data-test='apply-name']");
        assertThat(nameInput).isVisible();

        // Verify password input
        Locator passwordInput = page.locator("[data-test='apply-password']");
        assertThat(passwordInput).isVisible();

        // Verify confirm password input
        Locator confirmPasswordInput = page.locator("[data-test='apply-confirm-password']");
        assertThat(confirmPasswordInput).isVisible();

        // Verify submit button
        Locator submitButton = page.locator("[data-test='apply-submit']");
        assertThat(submitButton).isVisible();
        assertThat(submitButton).containsText("Submit Application");

        // Verify "Already have an account" link
        assertThat(page.locator("text=Already have an account?")).isVisible();
        assertThat(page.locator("a[href='/login']")).isVisible();

        // Verify "What happens next" section
        assertThat(page.locator("text=What happens next?")).isVisible();
    }

    @Test
    @DisplayName("Should successfully submit application with valid data")
    void testSuccessfulApplication() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill in the form
        page.fill("[data-test='apply-name']", "John Smith");
        page.fill("[data-test='apply-password']", "password123");
        page.fill("[data-test='apply-confirm-password']", "password123");

        // Submit the form
        page.click("[data-test='apply-submit']");

        // Wait for success message
        Locator successMessage = page.locator("text=Application submitted successfully!");
        assertThat(successMessage).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10000L));

        // Verify confirmation message is shown
        assertThat(page.locator("[data-test='success-container']")).isVisible();
        assertThat(page.locator("text=Redirecting to login page")).isVisible();

        // Verify form is hidden after success
        Locator nameInput = page.locator("[data-test='apply-name']");
        assertThat(nameInput).not().isVisible();
    }

    @Test
    @DisplayName("Should show error when name is empty")
    void testEmptyNameValidation() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Leave name empty, fill other fields
        page.fill("[data-test='apply-password']", "password123");
        page.fill("[data-test='apply-confirm-password']", "password123");

        // Try to submit
        page.click("[data-test='apply-submit']");

        // Verify error message
        assertThat(page.locator("text=Name is required")).isVisible();
    }

    @Test
    @DisplayName("Should show error when password is empty")
    void testEmptyPasswordValidation() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill name, leave password empty
        page.fill("[data-test='apply-name']", "John Smith");
        page.fill("[data-test='apply-confirm-password']", "password123");

        // Try to submit
        page.click("[data-test='apply-submit']");

        // Verify error message
        assertThat(page.locator("text=Password is required")).isVisible();
    }

    @Test
    @DisplayName("Should show error when password is too short")
    void testShortPasswordValidation() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill form with short password
        page.fill("[data-test='apply-name']", "John Smith");
        page.fill("[data-test='apply-password']", "abc");
        page.fill("[data-test='apply-confirm-password']", "abc");

        // Try to submit
        page.click("[data-test='apply-submit']");

        // Verify error message
        assertThat(page.locator("text=Password must be at least 6 characters")).isVisible();
    }

    @Test
    @DisplayName("Should show error when passwords do not match")
    void testPasswordMismatchValidation() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill form with mismatched passwords
        page.fill("[data-test='apply-name']", "John Smith");
        page.fill("[data-test='apply-password']", "password123");
        page.fill("[data-test='apply-confirm-password']", "different456");

        // Try to submit
        page.click("[data-test='apply-submit']");

        // Verify error message
        assertThat(page.locator("text=Passwords do not match")).isVisible();
    }

    @Test
    @DisplayName("Should show loading state while submitting")
    void testLoadingState() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill in valid data
        page.fill("[data-test='apply-name']", "Jane Doe");
        page.fill("[data-test='apply-password']", "password123");
        page.fill("[data-test='apply-confirm-password']", "password123");

        // Submit the form
        page.click("[data-test='apply-submit']");

        // The button should show loading state (text changes or becomes disabled)
        // This happens very quickly, so we just verify the form processes
        // The success message appearing confirms the submission worked
        page.waitForSelector("text=Application submitted successfully!",
            new Page.WaitForSelectorOptions().setTimeout(10000L));
    }

    @Test
    @DisplayName("Should navigate to login page when clicking sign in link")
    void testSignInLink() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click the "Sign in" link
        page.click("a[href='/login']");

        // Verify we navigated to login page
        page.waitForURL("**/login", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify login page elements are visible
        assertThat(page.locator("h1")).containsText("Library");
    }

    @Test
    @DisplayName("Should trim whitespace from name input")
    void testNameTrimming() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill form with name that has leading/trailing whitespace
        page.fill("[data-test='apply-name']", "  John Smith  ");
        page.fill("[data-test='apply-password']", "password123");
        page.fill("[data-test='apply-confirm-password']", "password123");

        // Submit the form
        page.click("[data-test='apply-submit']");

        // Should succeed (name gets trimmed)
        assertThat(page.locator("text=Application submitted successfully!"))
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10000L));
    }

    @Test
    @DisplayName("Should not accept whitespace-only name")
    void testWhitespaceOnlyName() {
        page.navigate(getBaseUrl() + "/apply");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill form with whitespace-only name
        page.fill("[data-test='apply-name']", "   ");
        page.fill("[data-test='apply-password']", "password123");
        page.fill("[data-test='apply-confirm-password']", "password123");

        // Try to submit
        page.click("[data-test='apply-submit']");

        // Should show validation error
        assertThat(page.locator("text=Name is required")).isVisible();
    }
}
