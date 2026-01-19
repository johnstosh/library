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
 * UI Tests for Authors functionality using Playwright.
 * Tests author view and edit pages with books table display.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-books.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("UI tests temporarily disabled")
public class AuthorsUITest {

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
                .setViewportSize(1280, 1600));
        page = context.newPage();
        page.setDefaultTimeout(30000L);

        // Login as librarian before each test
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

    /**
     * Helper method to login as librarian and navigate to authors page
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

        // Wait for successful login and redirect
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));

        // Navigate to authors page
        page.click("a[href='/authors']");
        page.waitForURL("**/authors");
    }

    @Test
    @DisplayName("Should display books table in author view page")
    void shouldDisplayBooksTableInAuthorView() {
        // Click on first author to view details
        page.click("tr[data-row-id]");

        // Wait for view page to load
        page.waitForSelector("[data-test='author-name']",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // Verify books section heading is visible
        Locator booksHeading = page.locator("[data-test='author-books-heading']");
        assertThat(booksHeading).isVisible();

        // Verify books table is visible or empty message is shown
        Locator booksTable = page.locator("[data-test='author-books-table']");
        Locator emptyMessage = page.locator("[data-test='author-books-empty']");

        // At least one should be visible
        if (booksTable.count() > 0) {
            assertThat(booksTable).isVisible();
        } else {
            assertThat(emptyMessage).isVisible();
        }
    }

    @Test
    @DisplayName("Should display books table in author edit page")
    void shouldDisplayBooksTableInAuthorEdit() {
        // Click on first author to view details
        page.click("tr[data-row-id]");

        // Wait for view page to load
        page.waitForSelector("[data-test='author-name']",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // Click edit button
        page.click("[data-test='author-view-edit']");

        // Wait for edit page to load
        page.waitForURL("**/edit");

        // Verify books section heading is visible
        Locator booksHeading = page.locator("[data-test='author-books-heading']");
        assertThat(booksHeading).isVisible();

        // Verify books table is visible or empty message is shown
        Locator booksTable = page.locator("[data-test='author-books-table']");
        Locator emptyMessage = page.locator("[data-test='author-books-empty']");

        // At least one should be visible
        if (booksTable.count() > 0) {
            assertThat(booksTable).isVisible();
        } else {
            assertThat(emptyMessage).isVisible();
        }
    }

    @Test
    @DisplayName("Should navigate to book when clicking on book row in author view")
    void shouldNavigateToBookFromAuthorView() {
        // Click on first author to view details
        page.click("tr[data-row-id]");

        // Wait for view page to load
        page.waitForSelector("[data-test='author-name']",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // Check if books table has rows
        Locator booksTable = page.locator("[data-test='author-books-table']");
        if (booksTable.count() > 0) {
            // Click on first book row
            Locator firstBookRow = booksTable.locator("tr[data-row-id]").first();
            if (firstBookRow.count() > 0) {
                firstBookRow.click();

                // Verify navigation to book view page
                page.waitForURL("**/books/**");
            }
        }
    }

    @Test
    @DisplayName("Should filter authors without Grokipedia URL")
    void shouldFilterAuthorsWithoutGrokipediaUrl() {
        // Click on "Without Grokipedia URL" filter radio button
        page.click("[data-test='filter-without-grokipedia']");

        // Wait for filter to be applied and list to reload
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Verify the filter is selected
        Locator filterRadio = page.locator("[data-test='filter-without-grokipedia']");
        assertThat(filterRadio).isChecked();

        // The authors table should be visible (may have rows or be empty)
        Locator authorsTable = page.locator("table");
        assertThat(authorsTable).isVisible();
    }
}
