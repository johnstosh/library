/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
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
 * UI Tests for Navigation component using Playwright.
 * Tests visibility of navigation menu items based on authentication state.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-login.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("UI tests temporarily disabled")
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

        // Desktop: My Card link should NOT be visible
        Locator myCardLink = page.locator("[data-test='nav-my-card']");
        assertThat(myCardLink).not().isVisible();

        // Mobile: My Card link should NOT be visible
        Locator myCardLinkMobile = page.locator("[data-test='nav-my-card-mobile']");
        assertThat(myCardLinkMobile).not().isVisible();
    }

    @Test
    @DisplayName("My Card menu item should be visible when user is logged in - Desktop")
    void testMyCardVisibleWhenLoggedInDesktop() {
        // Login as librarian
        loginAsLibrarian();

        // Navigate to books page (or any page after login)
        page.navigate(getBaseUrl() + "/books");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for navigation to be visible
        page.waitForSelector("[data-test='navigation']",
                new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

        // Desktop: My Card link should be visible
        Locator myCardLink = page.locator("[data-test='nav-my-card']");
        assertThat(myCardLink).isVisible();

        // Verify it has correct text and link
        assertThat(myCardLink).containsText("My Card");
        assertThat(myCardLink).hasAttribute("href", "/my-card");
    }

    @Test
    @DisplayName("My Card menu item should be visible when user is logged in - Mobile")
    void testMyCardVisibleWhenLoggedInMobile() {
        // Set mobile viewport
        BrowserContext mobileContext = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(375, 667)); // iPhone size
        page = mobileContext.newPage();
        page.setDefaultTimeout(30000L);

        // Login as librarian
        loginAsLibrarian();

        // Navigate to books page
        page.navigate(getBaseUrl() + "/books");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for navigation to be visible
        page.waitForSelector("[data-test='navigation']",
                new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

        // Mobile: My Card link should be visible
        Locator myCardLinkMobile = page.locator("[data-test='nav-my-card-mobile']");
        assertThat(myCardLinkMobile).isVisible();

        // Verify it has correct text and link
        assertThat(myCardLinkMobile).containsText("My Card");
        assertThat(myCardLinkMobile).hasAttribute("href", "/my-card");

        // Close mobile context
        mobileContext.close();
    }

    @Test
    @DisplayName("Authenticated navigation items should be visible when logged in")
    void testAuthenticatedNavigationItemsVisible() {
        // Login as librarian
        loginAsLibrarian();

        // Navigate to books page
        page.navigate(getBaseUrl() + "/books");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for navigation to be visible
        page.waitForSelector("[data-test='navigation']",
                new Page.WaitForSelectorOptions().setTimeout(10000L).setState(WaitForSelectorState.VISIBLE));

        // Verify all authenticated navigation items are visible
        assertThat(page.locator("[data-test='nav-loans']")).isVisible();
        assertThat(page.locator("[data-test='nav-settings']")).isVisible();
        assertThat(page.locator("[data-test='nav-my-card']")).isVisible();
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
        assertThat(page.locator("[data-test='nav-settings']")).not().isVisible();
        assertThat(page.locator("[data-test='nav-my-card']")).not().isVisible();
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

        // Public items should be visible when logged out
        assertThat(page.locator("[data-test='nav-books']")).isVisible();
        assertThat(page.locator("[data-test='nav-authors']")).isVisible();
        assertThat(page.locator("[data-test='nav-search']")).isVisible();

        // Login as librarian
        loginAsLibrarian();

        // Navigate to books page
        page.navigate(getBaseUrl() + "/books");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Public items should still be visible when logged in
        assertThat(page.locator("[data-test='nav-books']")).isVisible();
        assertThat(page.locator("[data-test='nav-authors']")).isVisible();
        assertThat(page.locator("[data-test='nav-search']")).isVisible();
    }
}
