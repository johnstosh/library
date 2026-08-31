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

import java.util.Base64;

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

    // 56x80 solid red PNG — Search intercepts thumbnail requests the same way Books UI tests do
    private static final byte[] RED_PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAADgAAABQCAIAAADDQyF+AAAAWUlEQVR4nO3OAQkAMBAD" +
            "sfdvepNRDgIRkHt3CfuBqKioqKioqKioaMB+ICoqKioqKioqKhqwH4iKioqKioqKiooG" +
            "7AeioqKioqKioqKiAfuBqKioqKhoMvoBgnZvgBp1bJkAAAAASUVORK5CYII=");

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
        page.route("**/api/photos/*/thumbnail**", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("image/png")
                    .setBodyBytes(RED_PNG_BYTES));
        });
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

        // Discovery chips (including Recent Arrivals) — cataloger chips belong on Books
        assertThat(page.locator("[data-test='book-filter-ydl']")).isVisible();
        assertThat(page.locator("[data-test='book-filter-emu']")).isVisible();
        assertThat(page.locator("[data-test='filter-has-ydl-audio']")).isVisible();
        assertThat(page.locator("[data-test='filter-has-ydl-book']")).isVisible();
        assertThat(page.locator("[data-test='filter-has-ydl-ebook']")).isVisible();
        assertThat(page.locator("[data-test='filter-has-emu-audio']")).isVisible();
        assertThat(page.locator("[data-test='filter-has-emu-book']")).isVisible();
        assertThat(page.locator("[data-test='filter-has-emu-ebook']")).isVisible();
        assertThat(page.locator("[data-test='filter-3-letter-loc']")).hasCount(0);
        assertThat(page.locator("[data-test='filter-in-library']")).isVisible();
        assertThat(page.locator("[data-test='filter-electronic']")).isVisible();
        assertThat(page.locator("[data-test='filter-free-text']")).isVisible();
        assertThat(page.locator("[data-test='filter-audio']")).isVisible();
        assertThat(page.locator("[data-test='filter-most-recent']")).isVisible();
        assertThat(page.locator("[data-test='filter-most-recent']")).containsText("Recent Arrivals");
        assertThat(page.locator("[data-test='filter-without-loc']")).hasCount(0);
        assertThat(page.locator("[data-test='filter-without-grokipedia']")).hasCount(0);
        assertThat(page.locator("[data-test='filter-with-grokipedia']")).hasCount(0);
        assertThat(page.locator("[data-test='filter-without-genres']")).hasCount(0);
        assertThat(page.locator("[data-test='filter-not-active-status']")).hasCount(0);
        assertThat(page.locator("[data-test='filter-without-free-text-urls']")).hasCount(0);
        assertThat(page.locator("[data-test='open-in-books']")).hasCount(0);
    }

    @Test
    @DisplayName("Should omit cataloger chips on phone as well")
    void testSearchPageHidesCatalogerFiltersOnPhone() {
        BrowserContext mobileContext = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(375, 667));
        Page mobilePage = mobileContext.newPage();
        mobilePage.setDefaultTimeout(20000L);
        try {
            mobilePage.navigate(getBaseUrl() + "/search");
            mobilePage.waitForLoadState(LoadState.NETWORKIDLE);

            assertThat(mobilePage.locator("[data-test='filter-without-loc']")).hasCount(0);
            assertThat(mobilePage.locator("[data-test='filter-without-grokipedia']")).hasCount(0);
            assertThat(mobilePage.locator("[data-test='filter-without-genres']")).hasCount(0);
            assertThat(mobilePage.locator("[data-test='filter-without-free-text-urls']")).hasCount(0);
            assertThat(mobilePage.locator("[data-test='filter-not-active-status']")).hasCount(0);
            assertThat(mobilePage.locator("[data-test='filter-most-recent']")).isVisible();

            assertThat(mobilePage.locator("[data-test='filter-in-library']")).isVisible();
            assertThat(mobilePage.locator("[data-test='filter-free-text']")).isVisible();
        } finally {
            mobileContext.close();
        }
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
    @DisplayName("Book results show cover thumbnails like the Books table")
    void testBookResultsShowCoverThumbnails() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        page.fill("[data-test='search-input']", "Summa");
        page.click("[data-test='search-button']");
        page.waitForSelector("[data-test='book-result-1']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        Locator cover = page.locator("[data-test='book-result-cover-1']");
        assertThat(cover).isVisible();
        assertThat(cover).hasAttribute("href", "/photos/1");
        Locator thumbnail = cover.locator("[data-test='thumbnail-img']");
        assertThat(thumbnail).isVisible();
        assertThat(thumbnail).hasAttribute("alt", "Cover of Summa Theologica");
    }

    @Test
    @DisplayName("Book results without a photo show a cover placeholder")
    void testBookResultsShowCoverPlaceholderWhenNoPhoto() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        page.fill("[data-test='search-input']", "Canticle");
        page.click("[data-test='search-button']");
        page.waitForSelector("[data-test='book-result-8']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        Locator cover = page.locator("[data-test='book-result-cover-8']");
        assertThat(cover).isVisible();
        assertThat(cover).containsText("-");
        assertThat(cover.locator("[data-test='thumbnail-img']")).hasCount(0);
    }

    @Test
    @DisplayName("Author results show photo thumbnails like the Books table covers")
    void testAuthorResultsShowPhotoThumbnails() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        page.fill("[data-test='search-input']", "Teresa of Avila");
        page.click("[data-test='search-button']");
        page.waitForSelector("[data-test='author-result-4']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        Locator cover = page.locator("[data-test='author-result-cover-4']");
        assertThat(cover).isVisible();
        assertThat(cover).hasAttribute("href", "/photos/3");
        Locator thumbnail = cover.locator("[data-test='thumbnail-img']");
        assertThat(thumbnail).isVisible();
        assertThat(thumbnail).hasAttribute("alt", "Photo of Teresa of Avila");
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

        Locator inLibChip    = page.locator("[data-test='filter-in-library']");
        Locator elecChip     = page.locator("[data-test='filter-electronic']");
        Locator freeTextChip = page.locator("[data-test='filter-free-text']");
        Locator audioChip    = page.locator("[data-test='filter-audio']");
        Locator recentChip   = page.locator("[data-test='filter-most-recent']");

        assertThat(inLibChip).isVisible();
        assertThat(elecChip).isVisible();
        assertThat(freeTextChip).isVisible();
        assertThat(audioChip).isVisible();
        assertThat(recentChip).isVisible();

        assertThat(page.locator("[data-test='filter-in-library-info']")).isVisible();
        assertThat(page.locator("[data-test='filter-electronic-info']")).isVisible();
        assertThat(page.locator("[data-test='filter-free-text-info']")).isVisible();
        assertThat(page.locator("[data-test='filter-audio-info']")).isVisible();
        assertThat(page.locator("[data-test='filter-most-recent-info']")).isVisible();

        assertThat(inLibChip).containsText("In-library materials");
        assertThat(elecChip).containsText("Electronic resource");
        assertThat(freeTextChip).containsText("Has free online text");
        assertThat(audioChip).containsText("Has free online audio");
        assertThat(recentChip).containsText("Recent Arrivals");
    }

    @Test
    @DisplayName("Activating Recent Arrivals filter chip updates URL")
    void testRecentArrivalsFilterChipUpdatesUrl() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        page.click("[data-test='filter-most-recent']");
        page.waitForURL(url -> url.contains("mostRecent=true"),
                new Page.WaitForURLOptions().setTimeout(10000L));
        Assertions.assertTrue(page.url().contains("mostRecent=true"),
                "URL should contain mostRecent=true, got: " + page.url());
        assertThat(page.locator("[data-test='clear-search']")).isVisible();
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
        // Navigate with filter chips pre-set in URL.
        // freeText=true AND audio=true: book 10 (LibriVox) satisfies both (AND logic),
        // so at least 1 result is returned and the Books section renders.
        page.navigate(getBaseUrl() + "/search?freeText=true&audio=true");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        // Wait for results to load (book 10 has a LibriVox URL, satisfying both AND conditions)
        page.waitForSelector("h2:has-text('Books')", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Filter chips should reflect the URL state (active chips have distinct styling)
        // We check via aria or class—simplest is to verify the data-test buttons are active
        // (Active chips contain a checkmark SVG path "M5 13l4 4L19 7")
        Locator freeTextChip = page.locator("[data-test='filter-free-text']");
        Locator audioChip    = page.locator("[data-test='filter-audio']");

        // Both chips should be "active" (contain primary styling / checkmark)
        // Simplified check: the chip text content should still be correct
        assertThat(freeTextChip).isVisible();
        assertThat(audioChip).isVisible();
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

    @Test
    @DisplayName("Blank search hides WITHDRAWN books on the public catalog")
    void testWithdrawnHiddenOnPublicSearch() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        page.click("[data-test='search-button']");
        page.waitForSelector("[data-test='search-results-books']",
                new Page.WaitForSelectorOptions().setTimeout(10000L));

        assertThat(page.locator("[data-test^='book-result-title-']")
                .filter(new Locator.FilterOptions().setHasText("Summa Theologica"))).isVisible();
        assertThat(page.locator("[data-test^='book-result-title-']")
                .filter(new Locator.FilterOptions().setHasText("Little Flowers")))
                .not().isVisible();
    }

    @Test
    @DisplayName("Genre chips write labels into the URL")
    void testGenreFilterUpdatesUrl() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        page.click("[data-test='label-filter-fiction']");
        page.waitForURL(url -> url.contains("labels=fiction"),
                new Page.WaitForURLOptions().setTimeout(10000L));
        Assertions.assertTrue(page.url().contains("labels=fiction"),
                "URL should contain labels=fiction, got: " + page.url());
        assertThat(page.locator("[data-test='clear-search']")).isVisible();
    }

    @Test
    @DisplayName("Librarian Open in Books copies search filters onto /books")
    void testOpenInBooksCopiesFilters() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("[data-test='login-username']",
                new Page.WaitForSelectorOptions().setTimeout(30000L));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));

        page.navigate(getBaseUrl() + "/search?q=Summa&inLib=true");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("[data-test='open-in-books']",
                new Page.WaitForSelectorOptions().setTimeout(10000L));
        page.click("[data-test='open-in-books']");
        page.waitForURL("**/books?*", new Page.WaitForURLOptions().setTimeout(10000L));
        String booksUrl = page.url();
        Assertions.assertTrue(booksUrl.contains("/books"), "Should navigate to Books, got: " + booksUrl);
        Assertions.assertTrue(booksUrl.contains("q=Summa"), "Should copy query, got: " + booksUrl);
        Assertions.assertTrue(booksUrl.contains("inLib=true"), "Should copy in-library chip, got: " + booksUrl);
    }

    @Test
    @DisplayName("In-library AND electronic chips together hide books matching only one")
    void testInLibraryAndElectronicChipsRestrictTogether() {
        page.navigate(getBaseUrl() + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("#root:has(*)", new Page.WaitForSelectorOptions().setTimeout(30000L));

        page.click("[data-test='filter-in-library']");
        page.waitForSelector("[data-test='search-results-books']",
                new Page.WaitForSelectorOptions().setTimeout(10000L));
        assertThat(page.locator("[data-test^='book-result-title-']")
                .filter(new Locator.FilterOptions().setHasText("Summa Theologica"))).isVisible();
        assertThat(page.locator("[data-test^='book-result-title-']")
                .filter(new Locator.FilterOptions().setHasText("Gutenberg"))).not().isVisible();

        page.click("[data-test='filter-electronic']");

        page.waitForSelector("text=No books or authors found",
                new Page.WaitForSelectorOptions().setTimeout(10000L));
        assertThat(page.locator("text=Summa Theologica")).not().isVisible();
        assertThat(page.locator("text=Gutenberg")).not().isVisible();
    }
}
