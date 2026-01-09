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
 * UI Tests for Settings pages using Playwright.
 * Tests both User Settings and Global Settings for USER and LIBRARIAN authorities.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-settings.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("UI tests temporarily disabled")
public class SettingsUITest {

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
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
        context = browser.newContext(new Browser.NewContextOptions()
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

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private void login(String username, String password) {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        page.fill("[data-test='login-username']", username);
        page.fill("[data-test='login-password']", password);
        page.click("[data-test='login-submit']");

        // Wait for navigation to complete
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    private void navigateToSettings() {
        page.click("[data-test='nav-settings']");
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    // ==================== USER AUTHORITY TESTS ====================

    @Test
    @DisplayName("USER: Should display User Settings page with account information")
    void testUserCanViewAccountInformation() {
        login("testuser", "password");
        navigateToSettings();

        // Wait for settings page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify page title
        assertThat(page.locator("h1")).containsText("User Settings");

        // Verify account information section
        assertThat(page.locator("text=Account Information")).isVisible();
        assertThat(page.locator("text=testuser")).isVisible();
        assertThat(page.locator("text=USER")).isVisible();
    }

    @Test
    @DisplayName("USER: Should display library card design options")
    void testUserCanViewLibraryCardDesigns() {
        login("testuser", "password");
        navigateToSettings();

        // Wait for settings page to load
        page.waitForSelector("h2:has-text('Library Card Design')", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify all 5 library card design options are present
        assertThat(page.locator("[data-test='library-card-design-CLASSICAL_DEVOTION']")).isVisible();
        assertThat(page.locator("[data-test='library-card-design-COUNTRYSIDE_YOUTH']")).isVisible();
        assertThat(page.locator("[data-test='library-card-design-SACRED_HEART_PORTRAIT']")).isVisible();
        assertThat(page.locator("[data-test='library-card-design-RADIANT_BLESSING']")).isVisible();
        assertThat(page.locator("[data-test='library-card-design-PATRON_OF_CREATURES']")).isVisible();

        // Verify design names are displayed
        assertThat(page.locator("text=Classical Devotion")).isVisible();
        assertThat(page.locator("text=Countryside Youth")).isVisible();
        assertThat(page.locator("text=Sacred Heart Portrait")).isVisible();
        assertThat(page.locator("text=Radiant Blessing")).isVisible();
        assertThat(page.locator("text=Patron of Creatures")).isVisible();
    }

    @Test
    @DisplayName("USER: Should change library card design")
    void testUserCanChangeLibraryCardDesign() {
        login("testuser", "password");
        navigateToSettings();

        // Wait for settings page to load
        page.waitForSelector("[data-test='library-card-design-COUNTRYSIDE_YOUTH']",
                new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Click on a different design
        page.click("[data-test='library-card-design-COUNTRYSIDE_YOUTH']");

        // Wait for network to be idle (API call completes)
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Verify success message appears
        assertThat(page.locator("text=Library card design updated successfully"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("USER: Should display password change form")
    void testUserCanViewPasswordChangeForm() {
        login("testuser", "password");
        navigateToSettings();

        // Wait for settings page to load
        page.waitForSelector("h2:has-text('Change Password')", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify password change form fields
        assertThat(page.locator("[data-test='current-password']")).isVisible();
        assertThat(page.locator("[data-test='new-password']")).isVisible();
        assertThat(page.locator("[data-test='confirm-password']")).isVisible();
        assertThat(page.locator("[data-test='change-password-submit']")).isVisible();
        assertThat(page.locator("[data-test='cancel-password-change']")).isVisible();
    }

    @Test
    @DisplayName("USER: Should change password successfully")
    void testUserCanChangePassword() {
        login("testuser", "password");
        navigateToSettings();

        // Wait for password form to be visible
        page.waitForSelector("[data-test='current-password']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Fill in password change form
        page.fill("[data-test='current-password']", "password");
        page.fill("[data-test='new-password']", "newpassword123");
        page.fill("[data-test='confirm-password']", "newpassword123");

        // Submit form
        page.click("[data-test='change-password-submit']");

        // Wait for network to be idle
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Verify success message
        assertThat(page.locator("text=Password changed successfully"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

        // Verify form is cleared
        assertThat(page.locator("[data-test='current-password']")).hasValue("");
        assertThat(page.locator("[data-test='new-password']")).hasValue("");
        assertThat(page.locator("[data-test='confirm-password']")).hasValue("");
    }

    @Test
    @DisplayName("USER: Should show error for password mismatch")
    void testUserPasswordMismatchError() {
        login("testuser", "password");
        navigateToSettings();

        // Wait for password form to be visible
        page.waitForSelector("[data-test='current-password']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Fill in password change form with mismatched passwords
        page.fill("[data-test='current-password']", "password");
        page.fill("[data-test='new-password']", "newpassword123");
        page.fill("[data-test='confirm-password']", "differentpassword");

        // Submit form
        page.click("[data-test='change-password-submit']");

        // Verify error message appears
        assertThat(page.locator("text=New passwords do not match"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("USER: Should NOT see XAI Configuration section")
    void testUserCannotSeeXaiConfig() {
        login("testuser", "password");
        navigateToSettings();

        // Wait for settings page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // XAI Configuration should NOT be visible for regular users
        assertThat(page.locator("h2:has-text('XAI Configuration')")).not().isVisible();
        assertThat(page.locator("[data-test='xai-api-key-input']")).not().isVisible();
    }

    @Test
    @DisplayName("USER: Should NOT see Google Photos Integration section")
    void testUserCannotSeeGooglePhotosConfig() {
        login("testuser", "password");
        navigateToSettings();

        // Wait for settings page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Google Photos Integration should NOT be visible for regular users
        assertThat(page.locator("h2:has-text('Google Photos Integration')")).not().isVisible();
        assertThat(page.locator("[data-test='authorize-google-photos-button']")).not().isVisible();
    }

    @Test
    @DisplayName("USER: Should NOT have access to Global Settings")
    void testUserCannotAccessGlobalSettings() {
        login("testuser", "password");

        // Try to navigate directly to global settings
        page.navigate(getBaseUrl() + "/global-settings");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Should be redirected to /books (LibrarianRoute redirects non-librarians)
        assertThat(page).hasURL(getBaseUrl() + "/books");

        // Should not see Global Settings content
        Locator globalSettingsHeader = page.locator("h1:has-text('Global Settings')");
        assertThat(globalSettingsHeader).not().isVisible();
    }

    // ==================== LIBRARIAN AUTHORITY TESTS ====================

    @Test
    @DisplayName("LIBRARIAN: Should display User Settings page with all sections")
    void testLibrarianCanViewUserSettings() {
        login("librarian", "password");
        navigateToSettings();

        // Wait for settings page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify page title
        assertThat(page.locator("h1")).containsText("User Settings");

        // Verify all sections are present
        assertThat(page.locator("text=Account Information")).isVisible();
        assertThat(page.locator("h2:has-text('Library Card Design')")).isVisible();
        assertThat(page.locator("h2:has-text('Change Password')")).isVisible();

        // Librarian should see these additional sections
        assertThat(page.locator("h2:has-text('XAI Configuration')")).isVisible();
        assertThat(page.locator("h2:has-text('Google Photos Integration')")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should see authority as LIBRARIAN")
    void testLibrarianAuthorityDisplay() {
        login("librarian", "password");
        navigateToSettings();

        // Wait for settings page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify authority is displayed as LIBRARIAN
        assertThat(page.locator("text=LIBRARIAN")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should display XAI Configuration section")
    void testLibrarianCanSeeXaiConfig() {
        login("librarian", "password");
        navigateToSettings();

        // Wait for XAI section to load
        page.waitForSelector("h2:has-text('XAI Configuration')", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify XAI Configuration elements
        assertThat(page.locator("[data-test='xai-api-key-input']")).isVisible();
        assertThat(page.locator("[data-test='save-xai-api-key-button']")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should update XAI API Key")
    void testLibrarianCanUpdateXaiApiKey() {
        login("librarian", "password");
        navigateToSettings();

        // Wait for XAI section to load
        page.waitForSelector("[data-test='xai-api-key-input']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Enter API key
        page.fill("[data-test='xai-api-key-input']", "xai-test-key-12345");

        // Click save button
        page.click("[data-test='save-xai-api-key-button']");

        // Wait for network to be idle
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Verify success message
        assertThat(page.locator("text=XAI API Key updated successfully"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("LIBRARIAN: Should display Google Photos Integration section")
    void testLibrarianCanSeeGooglePhotosConfig() {
        login("librarian", "password");
        navigateToSettings();

        // Wait for Google Photos section to load
        page.waitForSelector("h2:has-text('Google Photos Integration')", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify Google Photos elements
        assertThat(page.locator("text=Authorization Status")).isVisible();
        assertThat(page.locator("text=Not Authorized")).isVisible();
        assertThat(page.locator("[data-test='authorize-google-photos-button']")).isVisible();
        assertThat(page.locator("[data-test='google-photos-album-id-input']")).isVisible();
        assertThat(page.locator("[data-test='save-album-id-button']")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should update Google Photos Album ID")
    void testLibrarianCanUpdateGooglePhotosAlbumId() {
        login("librarian", "password");
        navigateToSettings();

        // Wait for Google Photos section to load
        page.waitForSelector("[data-test='google-photos-album-id-input']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Enter album ID
        page.fill("[data-test='google-photos-album-id-input']", "album-id-test-12345");

        // Click save button
        page.click("[data-test='save-album-id-button']");

        // Wait for network to be idle
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Verify success message
        assertThat(page.locator("text=Google Photos Album ID updated successfully"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("LIBRARIAN: Should have access to Global Settings")
    void testLibrarianCanAccessGlobalSettings() {
        login("librarian", "password");

        // Navigate to global settings
        page.navigate(getBaseUrl() + "/global-settings");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify we're on Global Settings page
        assertThat(page.locator("h1")).containsText("Global Settings");
    }

    @Test
    @DisplayName("LIBRARIAN: Should display Global Settings page with all sections")
    void testLibrarianCanViewGlobalSettings() {
        login("librarian", "password");

        // Navigate to global settings
        page.navigate(getBaseUrl() + "/global-settings");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify all sections are present
        assertThat(page.locator("h2:has-text('Google SSO (User Authentication)')")).isVisible();
        assertThat(page.locator("h2:has-text('Google Photos API')")).isVisible();
        assertThat(page.locator("h2:has-text('OAuth Redirect URI')")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should display Global Settings form fields")
    void testLibrarianCanViewGlobalSettingsFormFields() {
        login("librarian", "password");

        // Navigate to global settings
        page.navigate(getBaseUrl() + "/global-settings");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for form fields to load
        page.waitForSelector("[data-test='sso-client-id']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify Google SSO fields
        assertThat(page.locator("[data-test='sso-client-id']")).isVisible();
        assertThat(page.locator("[data-test='sso-client-secret']")).isVisible();

        // Verify Google Photos API fields
        assertThat(page.locator("[data-test='photos-client-id']")).isVisible();
        assertThat(page.locator("[data-test='photos-client-secret']")).isVisible();

        // Verify action buttons
        assertThat(page.locator("[data-test='save-settings']")).isVisible();
        assertThat(page.locator("[data-test='cancel-settings']")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should update Global Settings")
    void testLibrarianCanUpdateGlobalSettings() {
        login("librarian", "password");

        // Navigate to global settings
        page.navigate(getBaseUrl() + "/global-settings");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for form to load
        page.waitForSelector("[data-test='sso-client-id']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Fill in SSO Client ID
        page.fill("[data-test='sso-client-id']", "test-sso-client-id-12345");

        // Fill in SSO Client Secret
        page.fill("[data-test='sso-client-secret']", "test-sso-secret-67890");

        // Click save button
        page.click("[data-test='save-settings']");

        // Wait for network to be idle
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Verify success message
        assertThat(page.locator("text=Settings updated successfully"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("LIBRARIAN: Should display redirect URI in Global Settings")
    void testLibrarianCanViewRedirectUri() {
        login("librarian", "password");

        // Navigate to global settings
        page.navigate(getBaseUrl() + "/global-settings");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for page to load
        page.waitForSelector("h2:has-text('OAuth Redirect URI')", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify redirect URI section exists
        assertThat(page.locator("text=Configured Redirect URI:")).isVisible();
        assertThat(page.locator("text=Use this URI when configuring OAuth apps")).isVisible();

        // Verify redirect URI value is displayed (using data-test attribute)
        Locator redirectUriElement = page.locator("[data-test='global-redirect-uri']");
        redirectUriElement.waitFor(new Locator.WaitForOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        assertThat(redirectUriElement).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

        // Verify it contains the expected redirect URI (for Google Photos OAuth)
        assertThat(redirectUriElement).containsText("/api/oauth/google/callback");
        assertThat(redirectUriElement).not().hasText("Not configured", new LocatorAssertions.HasTextOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("LIBRARIAN: Should cancel password change")
    void testLibrarianCanCancelPasswordChange() {
        login("librarian", "password");
        navigateToSettings();

        // Wait for password form to be visible
        page.waitForSelector("[data-test='current-password']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Fill in password change form
        page.fill("[data-test='current-password']", "password");
        page.fill("[data-test='new-password']", "newpassword123");
        page.fill("[data-test='confirm-password']", "newpassword123");

        // Click cancel button
        page.click("[data-test='cancel-password-change']");

        // Verify form is cleared
        assertThat(page.locator("[data-test='current-password']")).hasValue("");
        assertThat(page.locator("[data-test='new-password']")).hasValue("");
        assertThat(page.locator("[data-test='confirm-password']")).hasValue("");
    }

    @Test
    @DisplayName("LIBRARIAN: Should cancel Global Settings changes")
    void testLibrarianCanCancelGlobalSettingsChanges() {
        login("librarian", "password");

        // Navigate to global settings
        page.navigate(getBaseUrl() + "/global-settings");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for form to load
        page.waitForSelector("[data-test='sso-client-id']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Get initial value
        String initialValue = page.locator("[data-test='sso-client-id']").inputValue();

        // Fill in SSO Client ID with new value
        page.fill("[data-test='sso-client-id']", "test-changed-value");

        // Click cancel button
        page.click("[data-test='cancel-settings']");

        // Verify form is reset to original value
        assertThat(page.locator("[data-test='sso-client-id']")).hasValue(initialValue);
    }

    @Test
    @DisplayName("LIBRARIAN: Should update Google Photos Client ID")
    void testLibrarianCanUpdatePhotosClientId() {
        login("librarian", "password");

        // Navigate to global settings
        page.navigate(getBaseUrl() + "/global-settings");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for form to load
        page.waitForSelector("[data-test='photos-client-id']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify Client ID field is NOT disabled (editable)
        Locator clientIdInput = page.locator("[data-test='photos-client-id']");
        assertThat(clientIdInput).isEditable(new LocatorAssertions.IsEditableOptions().setTimeout(20000L));

        // Fill in new Client ID
        String newClientId = "test-photos-client-id-" + System.currentTimeMillis();
        page.fill("[data-test='photos-client-id']", newClientId);

        // Click save button
        page.click("[data-test='save-settings']");

        // Wait for network to be idle
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Verify success message
        assertThat(page.locator("text=Settings updated successfully"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

        // Reload page to verify persistence
        page.reload();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for form to load again
        page.waitForSelector("[data-test='photos-client-id']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify the help text contains the new Client ID (it shows "Current: <value>")
        Locator helpText = page.locator("[data-test='photos-client-id']").locator("xpath=following-sibling::*[contains(@class, 'text-sm')]");
        assertThat(helpText).containsText(newClientId, new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));
    }
}
