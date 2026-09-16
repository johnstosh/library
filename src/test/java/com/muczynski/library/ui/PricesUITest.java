/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-books.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PricesUITest {

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
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 1600));
        page = context.newPage();
        page.setDefaultTimeout(20000L);
        loginAsLibrarian();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private void loginAsLibrarian() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("[data-test='login-username']",
                new Page.WaitForSelectorOptions().setTimeout(30000L));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));
    }

    @Test
    @DisplayName("Librarian can open the Prices page from nav")
    void testPricesNavAndEmptyState() {
        assertThat(page.locator("[data-test='nav-prices']")).isVisible();
        page.click("[data-test='nav-prices']");
        page.waitForURL("**/prices", new Page.WaitForURLOptions().setTimeout(10000L));
        assertThat(page.locator("h1")).containsText("Prices");
        assertThat(page.locator("[data-test='book-price-filters']")).isVisible();
        assertThat(page.locator("[data-test='book-price-filters']")).containsText("Pricing");
        assertThat(page.locator("[data-test='prices-max-total']")).isVisible();
        assertThat(page.locator("[data-test='filter-with-prices']")).isVisible();
        assertThat(page.locator("[data-test='filter-with-prices']")).containsText("Books with Pricing");
        assertThat(page.locator("[data-test='filter-no-prices']")).isVisible();
        assertThat(page.locator("[data-test='filter-no-prices']")).containsText("Books without Pricing");
        assertThat(page.locator("[data-test='filter-price-older']")).isVisible();
        assertThat(page.locator("[data-test='filter-price-hardcover']")).isVisible();
        assertThat(page.locator("[data-test='filter-price-softcover']")).isVisible();
        assertThat(page.locator("[data-test='filter-price-other-unknown']")).isVisible();
        assertThat(page.locator("[data-test='filter-lookup-errors']")).isVisible();
        assertThat(page.locator("[data-test='filter-lookup-errors']")).containsText("Lookup Errors");
        assertThat(page.locator("[data-test='filter-price-recent-hours']")).isVisible();
        assertThat(page.locator("[data-test='desire-to-purchase-filters']")).isVisible();
        assertThat(page.locator("[data-test='desire-to-purchase-filters']")).containsText("Desire to Purchase");
        assertThat(page.locator("[data-test='desire-to-purchase-filter-unset']")).containsText("Unset");
        assertThat(page.locator("[data-test='open-in-books']")).isVisible();
        assertThat(page.locator("[data-test='open-in-books']")).containsText("Open in Books");
        assertThat(page.locator("[data-test='open-in-books']")).hasAttribute("href",
                java.util.regex.Pattern.compile(".*/books.*"));
        assertThat(page.locator("[data-test='prices-search-button']")).containsText("Search");
        assertThat(page.locator("[data-test='table-stats-placeholder']")).isVisible();
        assertThat(page.locator("[data-test='table-count']")).isVisible();
        assertThat(page.locator("[data-test='database-count']")).isVisible();
        assertThat(page.locator("[data-test='price-row-count']")).isVisible();
        assertThat(page.locator("text=No prices match the current filters.")).isVisible();
    }

    @Test
    @DisplayName("Direct load of /prices serves the SPA instead of a static-resource error")
    void testDirectPricesUrlServesSpa() {
        page.navigate(getBaseUrl() + "/prices");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(page.locator("h1")).containsText("Prices");
        assertThat(page.locator("body")).not().containsText("No static resource");
        assertThat(page.locator("body")).not().containsText("INTERNAL_ERROR");
        assertThat(page.locator("[data-test='prices-search-button']")).containsText("Search");
    }

    @Test
    @DisplayName("Open in Prices copies the current Books filters")
    void testOpenInPricesCopiesFilters() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.fill("[data-test='books-title-filter']", "Initial");
        page.click("[data-test='books-search-button']");
        assertThat(page.locator("[data-test='open-in-prices']")).isVisible();
        assertThat(page.locator("[data-test='open-in-prices']")).hasAttribute("href",
                java.util.regex.Pattern.compile(".*/prices\\?q=Initial.*"));
        page.click("[data-test='open-in-prices']");
        page.waitForURL("**/prices**", new Page.WaitForURLOptions().setTimeout(10000L));
        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*[?&]q=Initial.*"));
        assertThat(page.locator("h1")).containsText("Prices");
        assertThat(page.locator("[data-test='prices-title-filter']")).hasValue("Initial");
    }

    @Test
    @DisplayName("Open in Books copies the current Prices filters")
    void testOpenInBooksCopiesFilters() {
        page.click("[data-test='nav-prices']");
        page.waitForURL("**/prices", new Page.WaitForURLOptions().setTimeout(10000L));
        page.fill("[data-test='prices-title-filter']", "Initial");
        page.click("[data-test='prices-search-button']");
        assertThat(page.locator("[data-test='open-in-books']")).isVisible();
        assertThat(page.locator("[data-test='open-in-books']")).hasAttribute("href",
                java.util.regex.Pattern.compile(".*/books\\?q=Initial.*"));
        page.click("[data-test='open-in-books']");
        page.waitForURL("**/books**", new Page.WaitForURLOptions().setTimeout(10000L));
        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*[?&]q=Initial.*"));
        assertThat(page.locator("h1")).containsText("Books");
        assertThat(page.locator("[data-test='books-title-filter']")).hasValue("Initial");
    }

    @Test
    @DisplayName("Lookup Prices appears in the books bulk-action carousel")
    void testBulkLookupPricesButtonVisible() {
        page.waitForSelector("text=Initial Book", new Page.WaitForSelectorOptions().setTimeout(10000L));
        page.click("[data-test='select-checkbox-1']");
        assertThat(page.locator("[data-test='bulk-lookup-prices']")).isVisible();
        assertThat(page.locator("[data-test='bulk-lookup-prices']")).containsText("Lookup Prices");
    }
}
