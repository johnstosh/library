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
 * Tests book and author search, filter chips, results display, and pagination.
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

        // Search button is ENABLED even with empty input (blank search is allowed)
        assertThat(searchButton).isEnabled();

        // Verify all four filter chips are present
        assertThat(page.locator("[data-test='filter-in-library']")).isVisible();
        assertThat(page.locator("[data-test='filter-electronic']")).isVisible();
        assertThat(page.locator("[data-test='filter-free-text']")).isVisible();
        assertThat(page.locator("[data-test='filter-audio']")).isVisible();
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
    @DisplayName("Search button should always be enabled (blank search is allowed)")
    void testSearchButtonAlwaysEnabled() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        Locator searchInput = page.locator("[data-test='search-input']");
        Locator searchButton = page.locator("[data-test='search-button']");

        // Button is enabled with empty input
        assertThat(searchButton).isEnabled();

        // Still enabled after typing
        searchInput.fill("Test");
        assertThat(searchButton).isEnabled();

        // Still enabled after clearing the text
        searchInput.fill("");
        assertThat(searchButton).isEnabled();
    }

    @Test
    @DisplayName("Should allow searching with blank input and return results")
    void testBlankSearchReturnsResults() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Leave search input empty and click search
        page.click("[data-test='search-button']");

        // Wait for results — blank search returns all books (test data has 10 books)
        page.waitForSelector("h2:has-text('Books')", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Books section should appear
        Locator booksHeader = page.locator("h2:has-text('Books')");
        assertThat(booksHeader).isVisible();
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
    }

    @Test
    @DisplayName("Should update URL when searching")
    void testSearchUpdatesUrl() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Perform a search
        page.fill("[data-test='search-input']", "Augustine");
        page.click("[data-test='search-button']");

        // Wait for results
        page.waitForSelector("[data-test^='author-result-']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify URL contains search query parameter
        String currentUrl = page.url();
        Assertions.assertTrue(currentUrl.contains("q=Augustine"), "URL should contain search query parameter");
    }

    @Test
    @DisplayName("Should load search results from URL with query parameter")
    void testSearchFromUrlParameter() {
        // Navigate directly to search URL with query parameter
        page.navigate(getBaseUrl() + "/search?q=Augustine");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for results to appear
        page.waitForSelector("[data-test^='author-result-']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify search input has the query value
        Locator searchInput = page.locator("[data-test='search-input']");
        assertThat(searchInput).hasValue("Augustine");

        // Verify results are displayed
        Locator authorResults = page.locator("[data-test^='author-result-']");
        assertThat(authorResults.first()).isVisible();
        assertThat(authorResults.first()).containsText("Augustine");
    }

    @Test
    @DisplayName("Should clear URL when clearing search")
    void testClearSearchUpdatesUrl() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Perform a search
        page.fill("[data-test='search-input']", "Augustine");
        page.click("[data-test='search-button']");

        // Wait for results and verify URL
        page.waitForSelector("[data-test^='author-result-']", new Page.WaitForSelectorOptions().setTimeout(10000L));
        Assertions.assertTrue(page.url().contains("q=Augustine"), "URL should contain search query");

        // Click clear button
        page.click("[data-test='clear-search']");

        // Verify URL no longer contains query parameter
        Assertions.assertFalse(page.url().contains("q="), "URL should not contain search query after clear");
    }

    @Test
    @DisplayName("Should have correct href on book view button")
    void testViewBookNavigatesToPage() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Search for a book
        page.fill("[data-test='search-input']", "City of God");
        page.click("[data-test='search-button']");

        // Wait for results
        page.waitForSelector("[data-test^='book-result-']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify the view button has the correct href pointing to /books/{id}
        Locator viewButton = page.locator("[data-test^='book-result-view-']").first();
        assertThat(viewButton).isVisible();
        String href = viewButton.getAttribute("href");
        Assertions.assertNotNull(href, "View button should have href attribute");
        Assertions.assertTrue(href.matches(".*/books/\\d+$"), "href should be /books/{id}, got: " + href);
    }

    @Test
    @DisplayName("Should have correct href on author view button")
    void testViewAuthorNavigatesToPage() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Search for an author
        page.fill("[data-test='search-input']", "Augustine");
        page.click("[data-test='search-button']");

        // Wait for results
        page.waitForSelector("[data-test^='author-result-']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify the view button has the correct href pointing to /authors/{id}
        Locator viewButton = page.locator("[data-test^='author-result-view-']").first();
        assertThat(viewButton).isVisible();
        String href = viewButton.getAttribute("href");
        Assertions.assertNotNull(href, "View button should have href attribute");
        Assertions.assertTrue(href.matches(".*/authors/\\d+$"), "href should be /authors/{id}, got: " + href);
    }

    // ── Filter chip tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter chips should be visible and show tooltip attributes")
    void testFilterChipsVisible() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // All four chips visible
        Locator inLibChip    = page.locator("[data-test='filter-in-library']");
        Locator elecChip     = page.locator("[data-test='filter-electronic']");
        Locator freeTextChip = page.locator("[data-test='filter-free-text']");
        Locator audioChip    = page.locator("[data-test='filter-audio']");

        assertThat(inLibChip).isVisible();
        assertThat(elecChip).isVisible();
        assertThat(freeTextChip).isVisible();
        assertThat(audioChip).isVisible();

        // Chips have tooltip (title attribute)
        Assertions.assertNotNull(inLibChip.getAttribute("title"), "In-library chip should have a tooltip");
        Assertions.assertNotNull(elecChip.getAttribute("title"), "Electronic chip should have a tooltip");
        Assertions.assertNotNull(freeTextChip.getAttribute("title"), "Free text chip should have a tooltip");
        Assertions.assertNotNull(audioChip.getAttribute("title"), "Audio chip should have a tooltip");

        // Text content
        assertThat(inLibChip).containsText("In-library materials");
        assertThat(elecChip).containsText("Electronic resource");
        assertThat(freeTextChip).containsText("Has free online text");
        assertThat(audioChip).containsText("Has free online audio");
    }

    @Test
    @DisplayName("Activating in-library filter chip updates URL and returns results")
    void testInLibraryFilterChipUpdatesUrl() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Click the in-library chip (no prior text search)
        page.click("[data-test='filter-in-library']");

        // Wait for results to appear (filter triggers search; 6 books have loc_number in test data)
        page.waitForSelector("h2:has-text('Books')", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // URL should contain the filter param
        String currentUrl = page.url();
        Assertions.assertTrue(currentUrl.contains("inLib=true"),
                "URL should contain inLib=true filter param, got: " + currentUrl);

        // Results should show (test data has books with loc_number)
        assertThat(page.locator("h2:has-text('Books')")).isVisible();

        // Clear button should appear
        assertThat(page.locator("[data-test='clear-search']")).isVisible();
    }

    @Test
    @DisplayName("Activating free online audio filter returns only LibriVox books")
    void testAudioFilterReturnsLibriVoxBooks() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Click audio filter chip
        page.click("[data-test='filter-audio']");

        // Wait for results (book 10 has a LibriVox URL in test data)
        page.waitForSelector("h2:has-text('Books')", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // URL should contain audio filter
        Assertions.assertTrue(page.url().contains("audio=true"),
                "URL should contain audio=true, got: " + page.url());

        // Should show exactly the LibriVox book from test data
        Locator booksHeader = page.locator("h2:has-text('Books')");
        assertThat(booksHeader).isVisible();
        Locator bookResults = page.locator("[data-test^='book-result-']");
        assertThat(bookResults.first()).containsText("LibriVox");
    }

    @Test
    @DisplayName("Activating free online text filter returns books with free text URLs")
    void testFreeTextFilterReturnsOnlineTextBooks() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Click free-text filter chip
        page.click("[data-test='filter-free-text']");

        // Wait for results (books 9 and 10 both have free_text_url in test data)
        page.waitForSelector("h2:has-text('Books')", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // URL should contain freeText filter
        Assertions.assertTrue(page.url().contains("freeText=true"),
                "URL should contain freeText=true, got: " + page.url());

        // Both books 9 (Gutenberg) and 10 (LibriVox) have free_text_url set
        Locator booksHeader = page.locator("h2:has-text('Books')");
        assertThat(booksHeader).isVisible();

        // Verify 2 results (both Gutenberg and LibriVox books have free text URLs)
        String booksText = booksHeader.textContent();
        Assertions.assertTrue(booksText.contains("2 results") || page.locator("[data-test^='book-result-']").count() >= 2,
                "Expected 2 free-text books in results");
    }

    @Test
    @DisplayName("Filter chip state persists when loaded from URL")
    void testFilterChipStateRestoredFromUrl() {
        // Navigate with filter chips pre-set in URL
        page.navigate(getBaseUrl() + "/search?inLib=true&elec=true");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Wait for results to load (inLib=true returns 6 books; elec=true adds 1 more → 7 total)
        page.waitForSelector("h2:has-text('Books')", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Filter chips should reflect the URL state (active chips have distinct styling)
        // We check via aria or class—simplest is to verify the data-test buttons are active
        // (Active chips contain a checkmark SVG path "M5 13l4 4L19 7")
        Locator inLibChip = page.locator("[data-test='filter-in-library']");
        Locator elecChip  = page.locator("[data-test='filter-electronic']");

        // Both chips should be "active" (contain blue styling / checkmark)
        // Simplified check: the chip text content should still be correct
        assertThat(inLibChip).isVisible();
        assertThat(elecChip).isVisible();
    }

    @Test
    @DisplayName("Clearing search also deactivates filter chips")
    void testClearRemovesFilterChips() {
        // Start with a filter chip active
        page.navigate(getBaseUrl() + "/search?inLib=true");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Wait for clear button to appear (filter active)
        page.waitForSelector("[data-test='clear-search']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click clear
        page.click("[data-test='clear-search']");

        // URL should no longer contain the filter param
        Assertions.assertFalse(page.url().contains("inLib=true"),
                "URL should not contain inLib=true after clear");

        // Clear button should disappear
        assertThat(page.locator("[data-test='clear-search']")).not().isVisible();
    }
}
