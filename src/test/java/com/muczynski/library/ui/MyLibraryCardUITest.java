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
 * UI Tests for the My Library Card page, including the Card Design section.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-settings.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MyLibraryCardUITest {

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

        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    private void navigateToMyCard() {
        page.click("[data-test='nav-my-card']");
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Test
    @DisplayName("USER: Should view Card Design section on My Card page")
    void testUserCanViewCardDesignSectionOnMyCardPage() {
        login("testuser", "password");
        navigateToMyCard();

        // Wait for page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify page title
        assertThat(page.locator("h1")).containsText("My Library Card");

        // Verify Card Design section heading
        assertThat(page.locator("h2:has-text('Card Design')")).isVisible();

        // Verify the design preview image is visible
        page.waitForSelector("[data-test='library-card-design-preview']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));
        assertThat(page.locator("[data-test='library-card-design-preview']")).isVisible();

        // Verify the src attribute contains the expected path
        String src = page.locator("[data-test='library-card-design-preview']").getAttribute("src");
        assert src != null && src.contains("/images/library-cards/") :
                "Expected preview src to contain '/images/library-cards/' but was: " + src;
    }

    @Test
    @DisplayName("USER: Should change card design from My Card page")
    void testUserCanChangeDesignFromMyCardPage() {
        login("testuser", "password");
        navigateToMyCard();

        // Wait for the design picker to load
        page.waitForSelector("[data-test='library-card-design-COUNTRYSIDE_YOUTH']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Click on the COUNTRYSIDE_YOUTH design
        page.click("[data-test='library-card-design-COUNTRYSIDE_YOUTH']");

        // Wait for network to be idle (API call completes)
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Verify success message appears
        assertThat(page.locator("text=Library card design updated successfully"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }
}
