/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * UI tests for the Data Management page Books Availability chips.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-login.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataManagementUITest {

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

    private void loginAsLibrarian() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("[data-test='login-username']",
                new Page.WaitForSelectorOptions().setTimeout(20000L)
                        .setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("Phone availability chips stack in one column with full labels")
    void testAvailabilityChipsOneColumnOnPhone() {
        loginAsLibrarian();

        Locator dataNav = page.locator("[data-test='nav-data']");
        dataNav.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        dataNav.click();
        page.waitForURL("**/data-management", new Page.WaitForURLOptions().setTimeout(20000L));
        page.waitForLoadState(LoadState.NETWORKIDLE);

        Locator grid = page.locator("[data-test='availability-stats-grid']");
        grid.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        page.setViewportSize(375, 667);

        Locator inLibrary = page.locator("[data-test='availability-count-in-library']");
        Locator electronic = page.locator("[data-test='availability-count-electronic']");
        inLibrary.scrollIntoViewIfNeeded();
        inLibrary.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        electronic.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        assertThat(inLibrary).containsText("In-library materials");

        BoundingBox first = inLibrary.boundingBox();
        BoundingBox second = electronic.boundingBox();
        Assertions.assertNotNull(first, "In-library chip should have a bounding box");
        Assertions.assertNotNull(second, "Electronic chip should have a bounding box");
        Assertions.assertTrue(second.y >= first.y + first.height - 1,
                "Availability chips should stack vertically on phone, first.bottom="
                        + (first.y + first.height) + " second.y=" + second.y);
        Assertions.assertTrue(Math.abs(second.x - first.x) < 20,
                "Availability chips should share a column on phone, first.x="
                        + first.x + " second.x=" + second.x);
    }
}
