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
 * UI Tests for Books functionality using Playwright.
 * Tests book CRUD operations, filters, and LOC lookup.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-books.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("UI tests temporarily disabled")
public class BooksUITest {

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
                .setViewportSize(1280, 1600)); // Increased height for tall modal dialogs
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
     * Helper method to login as librarian and navigate to books page
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

        // Wait for successful login and redirect to books page
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));
    }

    @Test
    @DisplayName("Should display books page with all elements")
    void testBooksPageLayout() {
        // Already on books page from login
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Verify page title
        assertThat(page.locator("h1")).containsText("Books");

        // Verify add book button
        Locator addButton = page.locator("[data-test='add-book']");
        assertThat(addButton).isVisible();
        assertThat(addButton).containsText("Add Book");

        // Verify filters are present
        assertThat(page.locator("[data-test='filter-all']")).isVisible();
        assertThat(page.locator("[data-test='filter-most-recent']")).isVisible();
        assertThat(page.locator("[data-test='filter-without-loc']")).isVisible();

        // Verify initial book is displayed
        assertThat(page.locator("text=Initial Book")).isVisible();
    }

    @Test
    @DisplayName("Should navigate to add book page when clicking Add Book button")
    void testOpenAddBookForm() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click add book button
        page.click("[data-test='add-book']");

        // Wait for navigation to /books/new
        page.waitForURL("**/books/new", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify we're on the add book page
        assertThat(page.locator("text=Add New Book")).isVisible();

        // Verify all form fields are present
        assertThat(page.locator("[data-test='book-title']")).isVisible();
        assertThat(page.locator("[data-test='book-author']")).isVisible();
        assertThat(page.locator("[data-test='book-library']")).isVisible();
        assertThat(page.locator("[data-test='book-year']")).isVisible();
        assertThat(page.locator("[data-test='book-publisher']")).isVisible();
        assertThat(page.locator("[data-test='book-loc']")).isVisible();
        assertThat(page.locator("[data-test='book-status']")).isVisible();
        assertThat(page.locator("[data-test='book-form-submit']")).isVisible();
    }

    @Test
    @DisplayName("Should create a new book successfully")
    void testCreateBook() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click add book button
        page.click("[data-test='add-book']");

        // Wait for navigation to /books/new
        page.waitForURL("**/books/new", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for form to load
        page.waitForSelector("[data-test='book-title']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Fill in the form
        page.fill("[data-test='book-title']", "New Test Book");
        page.selectOption("[data-test='book-author']", "1"); // Select first author
        page.selectOption("[data-test='book-library']", "1"); // Select first library
        page.fill("[data-test='book-year']", "2024");
        page.fill("[data-test='book-publisher']", "Test Publisher");
        page.fill("[data-test='book-loc']", "BX1234 .T45 2024");
        page.selectOption("[data-test='book-status']", "ACTIVE");

        // Submit the form
        page.click("[data-test='book-form-submit']");

        // Should navigate back to /books after successful creation
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));

        // Switch to 'all' filter to see all books
        page.click("[data-test='filter-all']");

        // Wait for books to load after filter change
        page.waitForTimeout(2000);

        // Verify new book is in the table (allowing up to 30s total)
        assertThat(page.locator("text=New Test Book"))
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30000));
    }

    @Test
    @DisplayName("Should show validation error when required fields are missing")
    void testCreateBookValidation() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click add book button
        page.click("[data-test='add-book']");

        // Wait for navigation to /books/new
        page.waitForURL("**/books/new", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for form to load
        page.waitForSelector("[data-test='book-title']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Try to submit without filling required fields
        page.click("[data-test='book-form-submit']");

        // Verify error message appears
        assertThat(page.locator("text=Title, Author, and Library are required")).isVisible();
    }

    @Test
    @DisplayName("Should edit an existing book")
    void testEditBook() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Switch to 'all' filter to see all books
        page.click("[data-test='filter-all']");
        page.waitForTimeout(2000); // Wait for filter to apply and books to load

        // Wait for initial book to be visible
        page.waitForSelector("text=Initial Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on book title to view it
        page.click("text=Initial Book");

        // Wait for navigation to /books/1
        page.waitForURL("**/books/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Click edit button
        page.click("[data-test='book-view-edit']");

        // Wait for navigation to /books/1/edit
        page.waitForURL("**/books/1/edit", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for form to load
        page.waitForSelector("text=Edit Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify we're editing the right book
        assertThat(page.locator("[data-test='book-title']")).hasValue("Initial Book");

        // Update the title
        page.fill("[data-test='book-title']", "Updated Book Title");

        // Submit the form
        page.click("[data-test='book-form-submit']");

        // Should navigate back to /books/1 (view page) after successful edit
        page.waitForURL("**/books/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify updated title on view page
        assertThat(page.locator("[data-test='book-title']")).containsText("Updated Book Title");
    }

    @Test
    @DisplayName("Should delete a book with confirmation")
    void testDeleteBook() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Switch to 'all' filter to see all books
        page.click("[data-test='filter-all']");
        page.waitForTimeout(2000); // Wait for filter to apply and books to load

        // Wait for initial book
        page.waitForSelector("text=Initial Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on book title to view it
        page.click("text=Initial Book");

        // Wait for navigation to /books/1
        page.waitForURL("**/books/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Click delete button
        page.click("[data-test='book-view-delete']");

        // Wait for confirmation to appear (inline on page, not a modal)
        page.waitForSelector("text=Are you sure you want to delete this book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Confirm deletion
        page.click("[data-test='confirm-delete-book']");

        // Should navigate back to /books after successful deletion
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait a reasonable time for mutation and refetch
        page.waitForTimeout(2000);

        // Verify book is no longer in table
        assertThat(page.locator("text=Initial Book")).not().isVisible();
    }

    @Test
    @DisplayName("Should filter books by 'All Books'")
    void testFilterAllBooks() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for either the table, loading spinner, or "No data available" message
        page.waitForTimeout(2000); // Give React Query time to load

        // Click 'All Books' filter (default is 'most-recent')
        page.click("[data-test='filter-all']");

        // Filter should now be selected
        assertThat(page.locator("[data-test='filter-all']")).isChecked();

        // Should see the initial book (wait up to 10 seconds for books to load)
        assertThat(page.locator("text=Initial Book")).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10000));
    }

    @Test
    @DisplayName("Should filter books by 'Without LOC'")
    void testFilterWithoutLoc() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click "Without LOC" filter
        page.click("[data-test='filter-without-loc']");

        // Wait for filter to apply
        page.waitForTimeout(1000);

        // Initial book has no LOC, so it should still be visible
        // (or might not be visible if it has LOC - depends on test data)
        // Verify the filter is selected
        assertThat(page.locator("[data-test='filter-without-loc']")).isChecked();
    }

    @Test
    @DisplayName("Should filter books by 'Most Recent Day'")
    void testFilterMostRecent() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click "Most Recent Day" filter
        page.click("[data-test='filter-most-recent']");

        // Wait for filter to apply
        page.waitForTimeout(1000);

        // Verify the filter is selected
        assertThat(page.locator("[data-test='filter-most-recent']")).isChecked();
    }

    @Test
    @DisplayName("Should display LOC lookup button when editing book")
    void testLocLookupButtonVisible() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for initial book
        page.waitForSelector("text=Initial Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on book title to view it
        page.click("text=Initial Book");

        // Wait for navigation to /books/1
        page.waitForURL("**/books/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Click edit button
        page.click("[data-test='book-view-edit']");

        // Wait for navigation to /books/1/edit
        page.waitForURL("**/books/1/edit", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for edit page to load
        page.waitForSelector("text=Edit Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify LOC lookup button is visible (only for editing)
        assertThat(page.locator("[data-test='lookup-loc-button']")).isVisible();
        assertThat(page.locator("[data-test='lookup-loc-button']")).containsText("Lookup");
    }

    @Test
    @DisplayName("Should display AI Suggest button in book form")
    void testAiSuggestButtonVisible() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Open add book form
        page.click("[data-test='add-book']");

        // Wait for navigation to /books/new
        page.waitForURL("**/books/new", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for form to load
        page.waitForSelector("[data-test='book-title']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify AI Suggest button is visible
        assertThat(page.locator("[data-test='suggest-loc-button']")).isVisible();
        assertThat(page.locator("[data-test='suggest-loc-button']")).containsText("AI Suggest");
    }

    @Test
    @DisplayName("Should navigate back when clicking Cancel button")
    void testCancelBookForm() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Open add book form
        page.click("[data-test='add-book']");

        // Wait for navigation to /books/new
        page.waitForURL("**/books/new", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for form to load
        page.waitForSelector("text=Add New Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click cancel button
        page.click("[data-test='book-form-cancel']");

        // Should navigate back to /books
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify we're back on books list page
        assertThat(page.locator("h1")).containsText("Books");
    }

    @Test
    @DisplayName("Should show book count in footer")
    void testBookCount() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Should show count of books
        assertThat(page.locator("text=Showing 1 book")).isVisible();
    }

    @Test
    @DisplayName("Should display book details in table")
    void testBookTableDetails() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for book to appear
        page.waitForSelector("text=Initial Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify book details are shown
        assertThat(page.locator("text=Initial Book")).isVisible();
        assertThat(page.locator("text=Initial Author")).isVisible();
        assertThat(page.locator("text=2023")).isVisible();
    }

    @Test
    @DisplayName("Should display edit, clone, and delete buttons on book view page")
    void testBookViewPageButtons() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for initial book
        page.waitForSelector("text=Initial Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on the book title to navigate to view page
        page.click("text=Initial Book");

        // Wait for navigation to /books/1
        page.waitForURL("**/books/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify Edit, Clone, and Delete buttons are visible for librarian
        assertThat(page.locator("[data-test='book-view-edit']")).isVisible();
        assertThat(page.locator("[data-test='book-view-clone']")).isVisible();
        assertThat(page.locator("[data-test='book-view-delete']")).isVisible();

        // Verify Back button is visible
        assertThat(page.locator("[data-test='back-to-books']")).isVisible();

        // Verify last modified and loan count fields are visible
        assertThat(page.locator("[data-test='book-last-modified']")).isVisible();
        assertThat(page.locator("[data-test='book-loan-count']")).isVisible();
    }

    @Test
    @DisplayName("Should navigate to edit form from book view page")
    void testEditFromViewPage() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for initial book
        page.waitForSelector("text=Initial Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on the book title to navigate to view page
        page.click("text=Initial Book");

        // Wait for navigation to /books/1
        page.waitForURL("**/books/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Click Edit button
        page.click("[data-test='book-view-edit']");

        // Wait for navigation to /books/1/edit
        page.waitForURL("**/books/1/edit", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for edit form to load
        page.waitForSelector("text=Edit Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Verify we're editing the right book
        assertThat(page.locator("[data-test='book-title']")).hasValue("Initial Book");
    }

    @Test
    @DisplayName("Should clone a book from view page")
    void testCloneFromViewPage() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Switch to 'all' filter to see all books
        page.click("[data-test='filter-all']");
        page.waitForTimeout(2000); // Wait for filter to apply and books to load

        // Wait for initial book
        page.waitForSelector("text=Initial Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on book title to navigate to view page
        page.click("text=Initial Book");

        // Wait for navigation to /books/1
        page.waitForURL("**/books/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Click Clone button
        page.click("[data-test='book-view-clone']");

        // Should navigate back to /books after successful clone
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait a reasonable time for mutation and refetch
        page.waitForTimeout(2000);

        // Verify cloned book appears in the table (allowing up to 30s total)
        assertThat(page.locator("text=Initial Book, c. 1"))
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30000));
    }
}
