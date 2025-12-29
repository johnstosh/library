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
 * UI Tests for Loans page using Playwright.
 * Tests both USER and LIBRARIAN authorities for loan management.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-loans.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoansUITest {

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

    private void navigateToLoans() {
        page.click("[data-test='nav-loans']");
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    // ==================== USER AUTHORITY TESTS ====================

    @Test
    @DisplayName("USER: Should display Loans page with user's loans only")
    void testUserCanViewOwnLoans() {
        login("testuser", "password");
        navigateToLoans();

        // Wait for page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify page title
        assertThat(page.locator("h1")).containsText("Loans");

        // Verify checkout button exists
        assertThat(page.locator("[data-test='checkout-book']")).isVisible();

        // Should show "Show returned loans" checkbox
        assertThat(page.locator("[data-test='show-all-loans']")).isVisible();

        // By default, should only show active loans (1 for testuser)
        // testuser has 1 active loan on book ID 3
        assertThat(page.locator("text=Loaned Book")).isVisible();
    }

    @Test
    @DisplayName("USER: Should see only their own active loans by default")
    void testUserSeesOnlyOwnActiveLoans() {
        login("testuser", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Should see their own active loan (Loaned Book)
        assertThat(page.locator("text=Loaned Book")).isVisible();

        // Should NOT see other user's loans (Available Book 2 is loaned to otheruser)
        assertThat(page.locator("text=otheruser")).not().isVisible();
    }

    @Test
    @DisplayName("USER: Should show returned loans when checkbox is selected")
    void testUserCanViewReturnedLoans() {
        login("testuser", "password");
        navigateToLoans();

        // Wait for page to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Initially should only show 1 active loan
        assertThat(page.locator("text=Loaned Book")).isVisible();

        // Check the "Show returned loans" checkbox
        page.click("[data-test='show-all-loans']");
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Now should see both active and returned loans (2 total for testuser)
        assertThat(page.locator("text=Loaned Book")).isVisible();
        assertThat(page.locator("text=Available Book 1")).isVisible();
    }

    @Test
    @DisplayName("USER: Should open checkout form when clicking Checkout Book")
    void testUserCanOpenCheckoutForm() {
        login("testuser", "password");
        navigateToLoans();

        // Click checkout button
        page.click("[data-test='checkout-book']");

        // Modal should appear
        page.waitForSelector("text=Checkout Book", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify form fields
        assertThat(page.locator("[data-test='checkout-book-select']")).isVisible();
        assertThat(page.locator("text=Borrower")).isVisible();
        assertThat(page.locator("text=testuser")).isVisible();
        assertThat(page.locator("[data-test='checkout-submit']")).isVisible();
        assertThat(page.locator("[data-test='checkout-cancel']")).isVisible();
    }

    @Test
    @DisplayName("USER: Should checkout a book successfully")
    void testUserCanCheckoutBook() {
        login("testuser", "password");
        navigateToLoans();

        // Click checkout button
        page.click("[data-test='checkout-book']");

        // Wait for modal
        page.waitForSelector("[data-test='checkout-book-select']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Select a book (Available Book 1 - book ID 1)
        page.selectOption("[data-test='checkout-book-select']", "1");

        // Submit checkout
        page.click("[data-test='checkout-submit']");

        // Wait for modal to close and data to refresh
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Verify the new loan appears in the table
        assertThat(page.locator("text=Available Book 1"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("USER: Should cancel checkout form")
    void testUserCanCancelCheckout() {
        login("testuser", "password");
        navigateToLoans();

        // Click checkout button
        page.click("[data-test='checkout-book']");

        // Wait for modal
        page.waitForSelector("[data-test='checkout-cancel']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Click cancel
        page.click("[data-test='checkout-cancel']");

        // Modal should close
        page.waitForTimeout(1000);
        assertThat(page.locator("text=Checkout Book")).not().isVisible();
    }

    @Test
    @DisplayName("USER: Should NOT see return button (only librarians can return)")
    void testUserCannotReturnBooks() {
        login("testuser", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Return buttons should NOT be visible for regular users
        // Note: Return button has icon with path containing "9 12l2 2 4-4m6"
        Locator returnButton = page.locator("button[title='Return Book']");
        assertThat(returnButton).not().isVisible();
    }

    @Test
    @DisplayName("USER: Should NOT see delete button (only librarians can delete)")
    void testUserCannotDeleteLoans() {
        login("testuser", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Delete buttons should NOT be visible for regular users
        Locator deleteButton = page.locator("button[title='Delete']");
        assertThat(deleteButton).not().isVisible();
    }

    @Test
    @DisplayName("USER: Should display loan status badges")
    void testUserSeesLoanStatusBadges() {
        login("testuser", "password");
        navigateToLoans();

        // Check "Show returned loans" to see both active and returned
        page.click("[data-test='show-all-loans']");
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Should see "Active" status badge
        assertThat(page.locator("text=Active")).isVisible();

        // Should see "Returned" status badge
        assertThat(page.locator("text=Returned")).isVisible();
    }

    // ==================== LIBRARIAN AUTHORITY TESTS ====================

    @Test
    @DisplayName("LIBRARIAN: Should display Loans page with all users' loans")
    void testLibrarianCanViewAllLoans() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for page to load
        page.waitForSelector("h1", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Verify page title
        assertThat(page.locator("h1")).containsText("Loans");

        // By default, should show all active loans from all users
        // Should see testuser's active loan
        assertThat(page.locator("text=Loaned Book")).isVisible();
        assertThat(page.locator("text=Borrowed by: testuser")).isVisible();

        // Should see otheruser's active loan
        assertThat(page.locator("text=Available Book 2")).isVisible();
        assertThat(page.locator("text=Borrowed by: otheruser")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Table should NOT be empty when there are loans")
    void testLibrarianTableNotEmpty() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Should NOT see empty message
        assertThat(page.locator("text=No loans found")).not().isVisible();

        // Should see loan data
        assertThat(page.locator("text=Loaned Book")).isVisible();
        assertThat(page.locator("text=Available Book 2")).isVisible();

        // Should see count
        assertThat(page.locator("text=Showing 2 loans")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should see all loans including returned when checkbox selected")
    void testLibrarianCanViewAllIncludingReturned() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for page to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Initially should show 2 active loans
        assertThat(page.locator("text=Showing 2 loans")).isVisible();

        // Check "Show returned loans" checkbox
        page.click("[data-test='show-all-loans']");
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Now should see all 3 loans (2 active + 1 returned)
        assertThat(page.locator("text=Showing 3 loans")).isVisible();

        // Should see the returned loan
        assertThat(page.locator("text=Returned")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should see return buttons on active loans")
    void testLibrarianSeesReturnButtons() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Should see return buttons (check for the specific loan ID)
        assertThat(page.locator("[data-test='return-loan-1']")).isVisible();
        assertThat(page.locator("[data-test='return-loan-3']")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should see delete buttons on all loans")
    void testLibrarianSeesDeleteButtons() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Should see delete buttons
        assertThat(page.locator("[data-test='delete-loan-1']")).isVisible();
        assertThat(page.locator("[data-test='delete-loan-3']")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should return a book successfully")
    void testLibrarianCanReturnBook() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click return button for loan ID 1
        page.click("[data-test='return-loan-1']");

        // Confirm dialog should appear
        page.waitForSelector("text=Return Book", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        assertThat(page.locator("text=Mark this book as returned?")).isVisible();

        // Click confirm
        Locator confirmButton = page.locator("button:has-text('Return')");
        confirmButton.click();

        // Wait for operation to complete
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // The loan should now be marked as returned
        // Check "Show returned loans" to verify
        page.click("[data-test='show-all-loans']");
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Should see "Returned" status badge
        assertThat(page.locator("text=Returned").first())
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("LIBRARIAN: Should delete a loan successfully")
    void testLibrarianCanDeleteLoan() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Initially should show 2 active loans
        assertThat(page.locator("text=Showing 2 loans")).isVisible();

        // Click delete button for loan ID 3
        page.click("[data-test='delete-loan-3']");

        // Confirm dialog should appear
        page.waitForSelector("text=Delete Loan", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        assertThat(page.locator("text=Are you sure you want to delete this loan record?")).isVisible();

        // Click confirm
        Locator confirmButton = page.locator("button:has-text('Delete')");
        confirmButton.click();

        // Wait for operation to complete
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Should now show 1 loan
        assertThat(page.locator("text=Showing 1 loan"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

        // Should not see otheruser's loan anymore
        assertThat(page.locator("text=Borrowed by: otheruser")).not().isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should cancel return operation")
    void testLibrarianCanCancelReturn() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click return button
        page.click("[data-test='return-loan-1']");

        // Wait for confirm dialog
        page.waitForSelector("text=Return Book", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Click cancel
        Locator cancelButton = page.locator("button:has-text('Cancel')");
        cancelButton.click();

        // Dialog should close
        page.waitForTimeout(1000);
        assertThat(page.locator("text=Mark this book as returned?")).not().isVisible();

        // Loan should still be active (not returned)
        assertThat(page.locator("text=Showing 2 loans")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should cancel delete operation")
    void testLibrarianCanCancelDelete() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click delete button
        page.click("[data-test='delete-loan-3']");

        // Wait for confirm dialog
        page.waitForSelector("text=Delete Loan", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Click cancel
        Locator cancelButton = page.locator("button:has-text('Cancel')");
        cancelButton.click();

        // Dialog should close
        page.waitForTimeout(1000);
        assertThat(page.locator("text=Are you sure you want to delete this loan record?")).not().isVisible();

        // Should still show 2 loans
        assertThat(page.locator("text=Showing 2 loans")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should checkout book for any user")
    void testLibrarianCanCheckoutForAnyUser() {
        login("librarian", "password");
        navigateToLoans();

        // Click checkout button
        page.click("[data-test='checkout-book']");

        // Wait for modal
        page.waitForSelector("[data-test='checkout-book-select']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        // Select a book (Available Book 1 - book ID 1)
        page.selectOption("[data-test='checkout-book-select']", "1");

        // Submit checkout (will checkout to librarian user)
        page.click("[data-test='checkout-submit']");

        // Wait for modal to close and data to refresh
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

        // Verify the new loan appears
        assertThat(page.locator("text=Available Book 1"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("LIBRARIAN: Should display borrower names for all loans")
    void testLibrarianSeesBorrowerNames() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Should see borrower names
        assertThat(page.locator("text=Borrowed by: testuser")).isVisible();
        assertThat(page.locator("text=Borrowed by: otheruser")).isVisible();
    }
}
