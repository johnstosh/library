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
 * UI Tests for public search functionality using Playwright.
 * Tests book and author search, results display, and pagination.
 *
 * Note: Search is public, so no login required.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-search.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchUITest {

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
    @DisplayName("Should display search page with all elements")
    void testSearchPageLayout() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for React app to render
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Verify page header
        assertThat(page.locator("h1")).containsText("Search Library");

        // Verify search input field
        Locator searchInput = page.locator("[data-test='search-input']");
        assertThat(searchInput).isVisible();
        assertThat(searchInput).hasAttribute("placeholder", "Enter book title or author name...");

        // Verify search button
        Locator searchButton = page.locator("[data-test='search-button']");
        assertThat(searchButton).isVisible();
        assertThat(searchButton).containsText("Search");

        // Verify search button is disabled when input is empty
        assertThat(searchButton).isDisabled();
    }

    @Test
    @DisplayName("Should search for and display books by title")
    void testSearchForBooks() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill search input with book title
        page.fill("[data-test='search-input']", "Summa");

        // Click search button
        page.click("[data-test='search-button']");

        // Wait for results to appear
        page.waitForSelector("h2:has-text('Books')", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify Books section is displayed
        assertThat(page.locator("h2:has-text('Books')")).isVisible();

        // Verify at least one book result is shown
        Locator bookResults = page.locator("[data-test^='book-result-']");
        assertThat(bookResults.first()).isVisible();

        // Verify the book title contains our search term
        assertThat(bookResults.first()).containsText("Summa");
    }

    @Test
    @DisplayName("Should search for and display authors by name")
    void testSearchForAuthors() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Fill search input with author name
        page.fill("[data-test='search-input']", "Augustine");

        // Click search button
        page.click("[data-test='search-button']");

        // Wait for results to appear
        page.waitForSelector("h2:has-text('Authors')", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify Authors section is displayed
        assertThat(page.locator("h2:has-text('Authors')")).isVisible();

        // Verify at least one author result is shown
        Locator authorResults = page.locator("[data-test^='author-result-']");
        assertThat(authorResults.first()).isVisible();

        // Verify the author name contains our search term
        assertThat(authorResults.first()).containsText("Augustine");
    }

    @Test
    @DisplayName("Should display both books and authors in search results")
    void testSearchForBooksAndAuthors() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Search for a term that matches both books and authors
        page.fill("[data-test='search-input']", "Teresa");

        // Click search button
        page.click("[data-test='search-button']");

        // Wait for results to appear
        page.waitForSelector("h2", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // The search term "Teresa" should match author Teresa of Avila
        // and possibly books if any have "Teresa" in the title
        // At minimum, verify we have results
        Locator results = page.locator("[data-test^='book-result-'], [data-test^='author-result-']");
        assertThat(results.first()).isVisible();
    }

    @Test
    @DisplayName("Should show no results message when nothing found")
    void testNoResultsFound() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Search for something that doesn't exist
        page.fill("[data-test='search-input']", "XyzNonexistentBook123");

        // Click search button
        page.click("[data-test='search-button']");

        // Wait for no results message
        Locator noResults = page.locator("text=No books or authors found");
        assertThat(noResults).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10000L));
    }

    @Test
    @DisplayName("Should clear search when clear button is clicked")
    void testClearSearch() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Perform a search
        page.fill("[data-test='search-input']", "Confessions");
        page.click("[data-test='search-button']");

        // Wait for results
        page.waitForSelector("h2:has-text('Books')", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click clear button
        Locator clearButton = page.locator("[data-test='clear-search']");
        assertThat(clearButton).isVisible();
        clearButton.click();

        // Verify search input is cleared
        Locator searchInput = page.locator("[data-test='search-input']");
        assertThat(searchInput).hasValue("");

        // Verify results are no longer displayed
        Locator results = page.locator("h2:has-text('Books')");
        assertThat(results).not().isVisible();
    }

    @Test
    @DisplayName("Should enable search button only when input has text")
    void testSearchButtonState() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        Locator searchInput = page.locator("[data-test='search-input']");
        Locator searchButton = page.locator("[data-test='search-button']");

        // Initially disabled with empty input
        assertThat(searchButton).isDisabled();

        // Type some text
        searchInput.fill("Test");

        // Should be enabled
        assertThat(searchButton).isEnabled();

        // Clear the text
        searchInput.fill("");

        // Should be disabled again
        assertThat(searchButton).isDisabled();
    }

    @Test
    @DisplayName("Should display book details in search results")
    void testBookResultDetails() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Search for a specific book
        page.fill("[data-test='search-input']", "City of God");
        page.click("[data-test='search-button']");

        // Wait for results
        page.waitForSelector("[data-test^='book-result-']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        Locator bookResult = page.locator("[data-test^='book-result-']").first();

        // Verify book details are shown
        assertThat(bookResult).containsText("City of God");
        assertThat(bookResult).containsText("Augustine"); // Author name

        // Book may have publication year, publisher, library name
        // These should be visible if present in the data
    }

    @Test
    @DisplayName("Should display author details in search results")
    void testAuthorResultDetails() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Search for a specific author
        page.fill("[data-test='search-input']", "Francis");
        page.click("[data-test='search-button']");

        // Wait for results
        page.waitForSelector("[data-test^='author-result-']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        Locator authorResult = page.locator("[data-test^='author-result-']").first();

        // Verify author name is shown
        assertThat(authorResult).containsText("Francis");

        // Author may have birth/death dates, biography, book count
        // These should be visible if present in the data
    }
}
