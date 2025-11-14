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

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GlobalSettingsUITest {

    @LocalServerPort
    private int port;

    private Browser browser;
    private Page page;

    @BeforeAll
    void launchBrowser() {
        Playwright playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true)); // Headless for CI execution
    }

    @AfterAll
    void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 720));
        page = context.newPage();
        page.setDefaultTimeout(20000L);
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private void login() {
        page.navigate("http://localhost:" + port);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(20000L));
        page.waitForSelector("[data-test='menu-login']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.click("[data-test='menu-login']");
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.waitForSelector("[data-test='menu-authors']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
    }

    private void loginAsRegularUser() {
        page.navigate("http://localhost:" + port);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(20000L));
        page.waitForSelector("[data-test='menu-login']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.click("[data-test='menu-login']");
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "user");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
    }

    private void navigateToGlobalSettings() {
        // Click the menu button for global-settings
        page.click("[data-test='menu-global-settings']");

        // Wait for global-settings section to be visible
        String targetSelector = "#global-settings-section";
        Locator targetSection = page.locator(targetSelector);
        targetSection.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
        assertThat(targetSection).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    void testGlobalSettingsSectionVisibilityForLibrarian() {
        try {
            login();

            // Navigate to global settings section
            navigateToGlobalSettings();

            // Verify section is visible
            Locator globalSettingsSection = page.locator("[data-test='global-settings-section']");
            assertThat(globalSettingsSection).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // Verify header is visible
            Locator header = page.locator("[data-test='global-settings-header']");
            assertThat(header).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(header).hasText("Global Settings", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-global-settings-visibility-test.png")));
            throw e;
        }
    }

    @Test
    void testGlobalSettingsSectionNotVisibleForRegularUser() {
        try {
            loginAsRegularUser();

            // Wait for page to load
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Verify global settings menu item doesn't exist or is hidden for regular users
            // Global settings section should not be visible
            Locator globalSettingsSection = page.locator("[data-test='global-settings-section']");

            // Check if the section exists but is hidden (has display: none via librarian-only class)
            // The librarian-only class applies display:none via CSS
            if (globalSettingsSection.count() > 0) {
                assertThat(globalSettingsSection).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(20000L));
            }

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-global-settings-regular-user-test.png")));
            throw e;
        }
    }

    @Test
    void testGlobalSettingsLoadAndDisplay() {
        try {
            login();
            navigateToGlobalSettings();

            // Wait for NETWORKIDLE to ensure loadGlobalSettings() completes
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Verify Client ID is displayed (should come from application.properties)
            Locator clientIdElement = page.locator("[data-test='global-client-id']");
            clientIdElement.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
            assertThat(clientIdElement).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            // Should not be "(loading...)" anymore
            assertThat(clientIdElement).not().hasText("(loading...)", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

            // Verify Redirect URI is displayed
            Locator redirectUriElement = page.locator("[data-test='global-redirect-uri']");
            redirectUriElement.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
            assertThat(redirectUriElement).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(redirectUriElement).not().hasText("(loading...)", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

            // Verify Client Secret partial is displayed (either "(not configured)" or "...XXXX")
            Locator secretPartialElement = page.locator("[data-test='global-secret-partial']");
            secretPartialElement.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
            assertThat(secretPartialElement).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(secretPartialElement).not().hasText("(loading...)", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

            // Verify validation status is displayed
            Locator validationElement = page.locator("[data-test='global-secret-validation']");
            validationElement.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
            assertThat(validationElement).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // Verify configured badge is displayed
            Locator configuredElement = page.locator("[data-test='global-secret-configured']");
            configuredElement.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
            assertThat(configuredElement).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-global-settings-load-test.png")));
            throw e;
        }
    }

    @Test
    void testUpdateGlobalClientSecret() {
        try {
            login();
            navigateToGlobalSettings();

            // Wait for NETWORKIDLE to ensure loadGlobalSettings() completes
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Find the Client Secret input
            Locator secretInput = page.locator("[data-test='global-client-secret']");
            secretInput.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));

            // Enter a test Client Secret (with GOCSPX- prefix to pass validation)
            String testSecret = "GOCSPX-test-secret-" + System.currentTimeMillis();
            secretInput.fill(testSecret);

            // Handle the confirmation dialog
            page.onDialog(dialog -> dialog.accept());

            // Click the update button
            Locator updateButton = page.locator("[data-test='update-global-secret-btn']");
            updateButton.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
            updateButton.click();

            // Wait for NETWORKIDLE to ensure save completes
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Wait for success message to appear
            Locator successMessage = page.locator("[data-test='global-settings-success']");
            successMessage.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
            assertThat(successMessage).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(successMessage).containsText("successfully", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));

            // Verify the input was cleared
            assertThat(secretInput).hasValue("", new LocatorAssertions.HasValueOptions().setTimeout(20000L));

            // Verify the partial secret was updated (should show last 4 chars of the new secret)
            Locator secretPartialElement = page.locator("[data-test='global-secret-partial']");
            String expectedPartial = "..." + testSecret.substring(testSecret.length() - 4);
            assertThat(secretPartialElement).hasText(expectedPartial, new LocatorAssertions.HasTextOptions().setTimeout(20000L));

            // Verify validation status is "Valid"
            Locator validationElement = page.locator("[data-test='global-secret-validation']");
            assertThat(validationElement).hasText("Valid", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

            // Verify configured badge shows "Configured"
            Locator configuredElement = page.locator("[data-test='global-secret-configured']");
            assertThat(configuredElement).containsText("Configured", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));

            // Verify last updated timestamp is not "(never)"
            Locator lastUpdatedElement = page.locator("[data-test='global-secret-updated-at']");
            assertThat(lastUpdatedElement).not().hasText("(never)", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-update-global-secret-test.png")));
            throw e;
        }
    }

    @Test
    void testUpdateGlobalClientSecretPersistence() {
        try {
            login();
            navigateToGlobalSettings();

            // Wait for NETWORKIDLE to ensure loadGlobalSettings() completes
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Enter a test Client Secret
            String testSecret = "GOCSPX-persistence-test-" + System.currentTimeMillis();
            Locator secretInput = page.locator("[data-test='global-client-secret']");
            secretInput.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
            secretInput.fill(testSecret);

            // Handle the confirmation dialog
            page.onDialog(dialog -> dialog.accept());

            // Click the update button
            Locator updateButton = page.locator("[data-test='update-global-secret-btn']");
            updateButton.click();

            // Wait for NETWORKIDLE
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Wait for success message
            Locator successMessage = page.locator("[data-test='global-settings-success']");
            successMessage.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
            assertThat(successMessage).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // Navigate away and back to verify persistence
            page.click("[data-test='menu-books']");
            Locator booksSection = page.locator("[data-test='books-section']");
            booksSection.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));

            // Navigate back to global settings
            navigateToGlobalSettings();

            // Wait for NETWORKIDLE
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Verify the partial secret persisted (should show last 4 chars)
            Locator secretPartialElement = page.locator("[data-test='global-secret-partial']");
            String expectedPartial = "..." + testSecret.substring(testSecret.length() - 4);
            assertThat(secretPartialElement).hasText(expectedPartial, new LocatorAssertions.HasTextOptions().setTimeout(20000L));

            // Verify validation status is still "Valid"
            Locator validationElement = page.locator("[data-test='global-secret-validation']");
            assertThat(validationElement).hasText("Valid", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-global-secret-persistence-test.png")));
            throw e;
        }
    }

    @Test
    void testEmptyClientSecretValidation() {
        try {
            login();
            navigateToGlobalSettings();

            // Wait for NETWORKIDLE
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Try to submit without entering a Client Secret
            Locator secretInput = page.locator("[data-test='global-client-secret']");
            secretInput.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
            // Leave it empty

            // Click the update button
            Locator updateButton = page.locator("[data-test='update-global-secret-btn']");
            updateButton.click();

            // Wait for NETWORKIDLE
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Should show error message
            Locator errorMessage = page.locator("[data-test='global-settings-error']");
            errorMessage.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
            assertThat(errorMessage).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(errorMessage).containsText("Client Secret", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-empty-secret-validation-test.png")));
            throw e;
        }
    }

    @Test
    void testClientSecretFormatValidation() {
        try {
            login();
            navigateToGlobalSettings();

            // Wait for NETWORKIDLE
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Enter a Client Secret that doesn't start with GOCSPX- (should get warning)
            String invalidSecret = "invalid-secret-format-" + System.currentTimeMillis();
            Locator secretInput = page.locator("[data-test='global-client-secret']");
            secretInput.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
            secretInput.fill(invalidSecret);

            // Handle the confirmation dialog
            page.onDialog(dialog -> dialog.accept());

            // Click the update button
            Locator updateButton = page.locator("[data-test='update-global-secret-btn']");
            updateButton.click();

            // Wait for NETWORKIDLE
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Should still save but show warning in validation
            Locator successMessage = page.locator("[data-test='global-settings-success']");
            successMessage.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
            assertThat(successMessage).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // Verify validation status shows warning
            Locator validationElement = page.locator("[data-test='global-secret-validation']");
            assertThat(validationElement).containsText("Warning", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-format-validation-test.png")));
            throw e;
        }
    }
}
