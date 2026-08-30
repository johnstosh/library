/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * UI Tests for Navigation component using Playwright.
 * Tests visibility of navigation menu items based on authentication state.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-login.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NavigationUITest {

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

    /**
     * Helper method to login as librarian
     */
    private void loginAsLibrarian() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for React to render
        page.waitForSelector("[data-test='login-username']",
                new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Fill login form
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");

        // Submit
        page.click("[data-test='login-submit']");

        // Wait for successful login
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));
    }

    private void openAccountMenu() {
        Locator userMenu = page.locator("[data-test='nav-user-menu']");
        userMenu.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        PlaywrightException lastError = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            userMenu.click();
            try {
                page.locator("[data-test='nav-logout']").waitFor(
                        new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(3000L));
                return;
            } catch (PlaywrightException e) {
                lastError = e;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new AssertionError("Account menu did not open");
    }

    @Test
    @DisplayName("My Card menu item should NOT be visible when user is logged out")
    void testMyCardNotVisibleWhenLoggedOut() {
        // Navigate to a public page (search) without logging in
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for React app to render
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Wait for navigation to be visible
        page.waitForSelector("[data-test='navigation']",
                new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

        assertThat(page.locator("[data-test='nav-user-menu']")).not().isVisible();
        assertThat(page.locator("[data-test='nav-my-card']")).not().isVisible();
    }

    @Test
    @DisplayName("My Card menu item should be visible in the account menu when user is logged in - Desktop")
    void testMyCardVisibleWhenLoggedInDesktop() {
        // Login as librarian
        loginAsLibrarian();

        // Navigate to books page (or any page after login)
        page.navigate(getBaseUrl() + "/books");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for navigation to be visible
        page.waitForSelector("[data-test='navigation']",
                new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

        // My Card lives in the account menu, not the primary nav
        assertThat(page.locator("[data-test='nav-loans']")).isVisible();
        assertThat(page.locator("[data-test='nav-my-card']")).not().isVisible();

        openAccountMenu();

        Locator myCardLink = page.locator("[data-test='nav-my-card']");
        assertThat(myCardLink).isVisible();
        assertThat(myCardLink).containsText("My Card");
        assertThat(myCardLink).hasAttribute("href", "/my-card");
    }

    @Test
    @DisplayName("My Card menu item should be visible in the account menu when user is logged in - Mobile")
    void testMyCardVisibleWhenLoggedInMobile() {
        // Set mobile viewport
        BrowserContext mobileContext = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(375, 667)); // iPhone size
        Page mobilePage = mobileContext.newPage();
        mobilePage.setDefaultTimeout(30000L);

        try {
            // Login as librarian using the mobile page
            mobilePage.navigate(getBaseUrl() + "/login");
            mobilePage.waitForLoadState(LoadState.NETWORKIDLE);
            mobilePage.waitForSelector("[data-test='login-username']",
                    new Page.WaitForSelectorOptions().setTimeout(30000L));
            mobilePage.fill("[data-test='login-username']", "librarian");
            mobilePage.fill("[data-test='login-password']", "password");
            mobilePage.click("[data-test='login-submit']");
            mobilePage.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));

            // Navigate to books page
            mobilePage.navigate(getBaseUrl() + "/books");
            mobilePage.waitForLoadState(LoadState.NETWORKIDLE);

            // Wait for navigation to be visible
            mobilePage.waitForSelector("[data-test='navigation']",
                    new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

            // Open the mobile hamburger and confirm My Card is not in the primary panel
            mobilePage.click("[data-test='mobile-menu-button']");
            assertThat(mobilePage.locator("[data-test='nav-loans-mobile']")).isVisible();
            assertThat(mobilePage.locator("[data-test='nav-my-card']")).not().isVisible();

            // My Card lives in the account menu
            Locator mobileUserMenu = mobilePage.locator("[data-test='nav-user-menu']");
            mobileUserMenu.click();
            mobilePage.locator("[data-test='nav-logout']").waitFor(
                    new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000L));

            Locator myCardLink = mobilePage.locator("[data-test='nav-my-card']");
            assertThat(myCardLink).isVisible();
            assertThat(myCardLink).containsText("My Card");
            assertThat(myCardLink).hasAttribute("href", "/my-card");
        } finally {
            // Close mobile context; do not update page field to avoid double-close in @AfterEach
            mobileContext.close();
        }
    }

    @Test
    @DisplayName("Account menu should contain Settings, My Card, and Logout when logged in")
    void testAuthenticatedNavigationItemsVisible() {
        // Login as librarian
        loginAsLibrarian();

        // Navigate to books page
        page.navigate(getBaseUrl() + "/books");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for navigation to be visible
        page.waitForSelector("[data-test='navigation']",
                new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

        assertThat(page.locator("[data-test='nav-loans']")).isVisible();
        assertThat(page.locator("[data-test='nav-user-menu']")).isVisible();

        openAccountMenu();

        assertThat(page.locator("[data-test='nav-settings']")).isVisible();
        assertThat(page.locator("[data-test='nav-my-card']")).isVisible();
        assertThat(page.locator("[data-test='nav-logout']")).isVisible();
        assertThat(page.locator("[data-test='nav-settings']")).hasAttribute("href", "/settings");
    }

    @Test
    @DisplayName("Authenticated navigation items should NOT be visible when logged out")
    void testAuthenticatedNavigationItemsNotVisible() {
        // Navigate to a public page without logging in
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for React app to render
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Wait for navigation to be visible
        page.waitForSelector("[data-test='navigation']",
                new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

        // Verify authenticated navigation items are NOT visible
        assertThat(page.locator("[data-test='nav-loans']")).not().isVisible();
        assertThat(page.locator("[data-test='nav-user-menu']")).not().isVisible();
        assertThat(page.locator("[data-test='nav-settings']")).not().isVisible();
        assertThat(page.locator("[data-test='nav-my-card']")).not().isVisible();
        assertThat(page.locator("[data-test='nav-logout']")).not().isVisible();
    }

    @Test
    @DisplayName("Public navigation items should always be visible")
    void testPublicNavigationItemsAlwaysVisible() {
        // Navigate to search page without logging in
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for navigation to be visible
        page.waitForSelector("[data-test='navigation']",
                new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

        // Search is always visible (public route). Books and Authors require authentication.
        assertThat(page.locator("[data-test='nav-search']")).isVisible();

        // Login as librarian
        loginAsLibrarian();

        // Navigate to books page
        page.navigate(getBaseUrl() + "/books");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for navigation to be visible
        page.waitForSelector("[data-test='navigation']",
                new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

        // When logged in, all nav items should be visible
        assertThat(page.locator("[data-test='nav-books']")).isVisible();
        assertThat(page.locator("[data-test='nav-authors']")).isVisible();
        assertThat(page.locator("[data-test='nav-search']")).isVisible();
    }
}
