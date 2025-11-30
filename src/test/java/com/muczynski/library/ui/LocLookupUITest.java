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
@Sql(value = "classpath:data-loc-lookup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LocLookupUITest {

    @LocalServerPort
    private int port;

    private Browser browser;
    private Page page;

    @BeforeAll
    void launchBrowser() {
        Playwright playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
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
        // Password must be SHA-256 hashed before submission (client-side hashing in auth.js)
        // SHA-256("password") = 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8
        page.fill("[data-test='login-password']", "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8");
        page.click("[data-test='login-submit']");
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.waitForSelector("[data-test='menu-authors']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
    }

    private void navigateToLocLookup() {
        page.click("[data-test='menu-loc-bulk-lookup']");
        Locator locLookupSection = page.locator("[data-test='loc-bulk-lookup-section']");
        locLookupSection.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
        assertThat(locLookupSection).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    void testNavigateToLocLookupSection() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            navigateToLocLookup();

            // Verify section header is visible
            Locator header = page.locator("[data-test='loc-bulk-lookup-header']");
            assertThat(header).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(header).hasText("LOC Lookup", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

            // Verify buttons are visible
            assertThat(page.locator("[data-test='view-all-books-btn']")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(page.locator("[data-test='view-missing-loc-btn']")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(page.locator("[data-test='lookup-table-missing-btn']")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(page.locator("[data-test='lookup-all-missing-btn']")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loc-lookup-navigate.png")));
            throw e;
        }
    }

    @Test
    void testViewAllBooks() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            navigateToLocLookup();

            // Click "View All Books" button
            page.click("[data-test='view-all-books-btn']");

            // Wait for network to be idle after loading books
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Verify books are loaded in the table
            Locator bookRows = page.locator("[data-test='loc-lookup-book-row']");
            bookRows.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
            assertThat(bookRows).hasCount(3, new LocatorAssertions.HasCountOptions().setTimeout(20000L)); // 3 books from data-loc-lookup.sql

            // Verify success message
            Locator successDiv = page.locator("[data-test='loc-lookup-results']");
            assertThat(successDiv).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(successDiv).containsText("Loaded 3 book(s)", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loc-lookup-view-all.png")));
            throw e;
        }
    }

    @Test
    void testViewBooksMissingLoc() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            navigateToLocLookup();

            // Click "View Books Missing LOC" button
            page.click("[data-test='view-missing-loc-btn']");

            // Wait for network to be idle after loading books
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Verify only books without LOC numbers are loaded
            Locator bookRows = page.locator("[data-test='loc-lookup-book-row']");
            bookRows.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
            assertThat(bookRows).hasCount(2, new LocatorAssertions.HasCountOptions().setTimeout(20000L)); // 2 books missing LOC

            // Verify all rows show "Not set" for LOC number
            Locator locCells = page.locator("[data-test='loc-number']");
            for (int i = 0; i < 2; i++) {
                assertThat(locCells.nth(i)).containsText("Not set", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));
            }

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loc-lookup-view-missing.png")));
            throw e;
        }
    }

    @Test
    void testSelectAllCheckbox() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            navigateToLocLookup();

            // Load books first
            page.click("[data-test='view-all-books-btn']");
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));
            page.locator("[data-test='loc-lookup-book-row']").first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));

            // Click select-all checkbox
            Locator selectAllCheckbox = page.locator("[data-test='select-all-loc-lookup']");
            selectAllCheckbox.check();

            // Verify all book checkboxes are checked
            Locator bookCheckboxes = page.locator("[data-test='book-checkbox']");
            for (int i = 0; i < 3; i++) {
                assertThat(bookCheckboxes.nth(i)).isChecked(new LocatorAssertions.IsCheckedOptions().setTimeout(20000L));
            }

            // Uncheck select-all
            selectAllCheckbox.uncheck();

            // Verify all book checkboxes are unchecked
            for (int i = 0; i < 3; i++) {
                assertThat(bookCheckboxes.nth(i)).not().isChecked(new LocatorAssertions.IsCheckedOptions().setTimeout(20000L));
            }

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loc-lookup-select-all.png")));
            throw e;
        }
    }

    @Test
    void testViewEditDeleteButtons() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            navigateToLocLookup();

            // Load books
            page.click("[data-test='view-all-books-btn']");
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            Locator firstRow = page.locator("[data-test='loc-lookup-book-row']").first();
            firstRow.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));

            // Verify action buttons exist in first row
            assertThat(firstRow.locator("[data-test='lookup-single-btn']")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(firstRow.locator("[data-test='view-loc-book-btn']")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(firstRow.locator("[data-test='edit-loc-book-btn']")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(firstRow.locator("[data-test='delete-loc-book-btn']")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loc-lookup-action-buttons.png")));
            throw e;
        }
    }

    @Test
    void testBookTitleAndAuthorDisplay() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            navigateToLocLookup();

            // Load books
            page.click("[data-test='view-all-books-btn']");
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            Locator firstRow = page.locator("[data-test='loc-lookup-book-row']").first();
            firstRow.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));

            // Verify book title is displayed
            Locator titleCell = firstRow.locator("[data-test='book-title']");
            assertThat(titleCell).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(titleCell).containsText("Book with LOC", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));

            // Verify author is displayed (as a sub-text)
            assertThat(titleCell).containsText("Test Author", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loc-lookup-book-display.png")));
            throw e;
        }
    }

    @Test
    void testLocNumberDisplay() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            navigateToLocLookup();

            // Load all books
            page.click("[data-test='view-all-books-btn']");
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Find the book with LOC number
            Locator bookWithLoc = page.locator("[data-test='loc-lookup-book-row']")
                    .filter(new Locator.FilterOptions().setHasText("Book with LOC"));
            bookWithLoc.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));

            // Verify LOC number is displayed
            Locator locCell = bookWithLoc.first().locator("[data-test='loc-number']");
            assertThat(locCell).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(locCell).containsText("PS3505", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loc-lookup-loc-display.png")));
            throw e;
        }
    }
}
