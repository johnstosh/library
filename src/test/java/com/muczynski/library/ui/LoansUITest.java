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
    @DisplayName("USER: Should navigate to checkout form when clicking Checkout Book")
    void testUserCanOpenCheckoutForm() {
        login("testuser", "password");
        navigateToLoans();

        // Click checkout button
        page.click("[data-test='checkout-book']");

        // Wait for navigation to /loans/new
        page.waitForURL("**/loans/new", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify we're on the checkout page
        assertThat(page.locator("text=Checkout Book")).isVisible();

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

        // Wait for navigation to /loans/new
        page.waitForURL("**/loans/new", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for form to load
        page.waitForSelector("[data-test='checkout-book-select']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Select a book (Available Book 1 - book ID 1)
        page.selectOption("[data-test='checkout-book-select']", "1");

        // Submit checkout
        page.click("[data-test='checkout-submit']");

        // Should navigate back to /loans after successful checkout
        page.waitForURL("**/loans", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for data to refresh
        page.waitForTimeout(2000);

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

        // Wait for navigation to /loans/new
        page.waitForURL("**/loans/new", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for cancel button to be visible
        page.waitForSelector("[data-test='checkout-cancel']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click cancel
        page.click("[data-test='checkout-cancel']");

        // Should navigate back to /loans
        page.waitForURL("**/loans", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify we're back on loans list page
        assertThat(page.locator("h1")).containsText("Loans");
    }

    @Test
    @DisplayName("USER: Should navigate to loan view page when clicking on loan")
    void testUserCanViewOwnLoan() {
        login("testuser", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for loan to be visible
        page.waitForSelector("text=Loaned Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on loan to view it
        page.click("text=Loaned Book");

        // Wait for navigation to /loans/:id
        page.waitForURL("**/loans/3", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify loan details are visible
        assertThat(page.locator("text=Loan Details")).isVisible();
        assertThat(page.locator("text=Loaned Book")).isVisible();

        // Should see back button
        assertThat(page.locator("[data-test='back-to-loans']")).isVisible();
    }

    @Test
    @DisplayName("USER: Should NOT see return button on view page (only librarians can return)")
    void testUserCannotReturnBooks() {
        login("testuser", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for loan to be visible
        page.waitForSelector("text=Loaned Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on loan to view it
        page.click("text=Loaned Book");

        // Wait for navigation to /loans/3
        page.waitForURL("**/loans/3", new Page.WaitForURLOptions().setTimeout(10000L));

        // Return button should NOT be visible for regular users
        assertThat(page.locator("[data-test='loan-view-return']")).not().isVisible();
    }

    @Test
    @DisplayName("USER: Should NOT see delete button on view page (only librarians can delete)")
    void testUserCannotDeleteLoans() {
        login("testuser", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for loan to be visible
        page.waitForSelector("text=Loaned Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on loan to view it
        page.click("text=Loaned Book");

        // Wait for navigation to /loans/3
        page.waitForURL("**/loans/3", new Page.WaitForURLOptions().setTimeout(10000L));

        // Delete button should NOT be visible for regular users
        assertThat(page.locator("[data-test='loan-view-delete']")).not().isVisible();
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
    @DisplayName("LIBRARIAN: Should navigate to loan view page when clicking on loan")
    void testLibrarianCanViewLoan() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for loan to be visible
        page.waitForSelector("text=Loaned Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on loan to view it
        page.click("text=Loaned Book");

        // Wait for navigation to /loans/1
        page.waitForURL("**/loans/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Verify loan details are visible
        assertThat(page.locator("text=Loan Details")).isVisible();
        assertThat(page.locator("text=Loaned Book")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should see return and delete buttons on loan view page")
    void testLibrarianSeesActionButtonsOnViewPage() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for loan to be visible
        page.waitForSelector("text=Loaned Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on loan to view it
        page.click("text=Loaned Book");

        // Wait for navigation to /loans/1
        page.waitForURL("**/loans/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Should see return and delete buttons on view page
        assertThat(page.locator("[data-test='loan-view-return']")).isVisible();
        assertThat(page.locator("[data-test='loan-view-delete']")).isVisible();

        // Should also see back button
        assertThat(page.locator("[data-test='back-to-loans']")).isVisible();
    }

    @Test
    @DisplayName("LIBRARIAN: Should return a book successfully")
    void testLibrarianCanReturnBook() {
        login("librarian", "password");
        navigateToLoans();

        // Wait for table to load
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for loan to be visible
        page.waitForSelector("text=Loaned Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on loan to view it
        page.click("text=Loaned Book");

        // Wait for navigation to /loans/1
        page.waitForURL("**/loans/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Click return button
        page.click("[data-test='loan-view-return']");

        // Confirm dialog should appear (inline on page)
        page.waitForSelector("text=Mark this book as returned?", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click confirm
        page.click("[data-test='confirm-return-loan']");

        // Should navigate back to /loans after successful return
        page.waitForURL("**/loans", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for data to refresh
        page.waitForTimeout(2000);

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

        // Wait for loan to be visible
        page.waitForSelector("text=Available Book 2", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on loan to view it (loan ID 3)
        page.click("text=Available Book 2");

        // Wait for navigation to /loans/3
        page.waitForURL("**/loans/3", new Page.WaitForURLOptions().setTimeout(10000L));

        // Click delete button
        page.click("[data-test='loan-view-delete']");

        // Confirm dialog should appear (inline on page)
        page.waitForSelector("text=Are you sure you want to delete this loan", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click confirm
        page.click("[data-test='confirm-delete-loan']");

        // Should navigate back to /loans after successful deletion
        page.waitForURL("**/loans", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for data to refresh
        page.waitForTimeout(2000);

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

        // Wait for loan to be visible
        page.waitForSelector("text=Loaned Book", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on loan to view it
        page.click("text=Loaned Book");

        // Wait for navigation to /loans/1
        page.waitForURL("**/loans/1", new Page.WaitForURLOptions().setTimeout(10000L));

        // Click return button
        page.click("[data-test='loan-view-return']");

        // Wait for confirm dialog (inline on page)
        page.waitForSelector("text=Mark this book as returned?", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click cancel
        page.click("[data-test='cancel-return-loan']");

        // Confirmation should disappear
        page.waitForTimeout(1000);
        assertThat(page.locator("text=Mark this book as returned?")).not().isVisible();

        // Navigate back to loans page
        page.click("[data-test='back-to-loans']");

        // Wait for navigation to /loans
        page.waitForURL("**/loans", new Page.WaitForURLOptions().setTimeout(10000L));

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

        // Wait for loan to be visible
        page.waitForSelector("text=Available Book 2", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click on loan to view it
        page.click("text=Available Book 2");

        // Wait for navigation to /loans/3
        page.waitForURL("**/loans/3", new Page.WaitForURLOptions().setTimeout(10000L));

        // Click delete button
        page.click("[data-test='loan-view-delete']");

        // Wait for confirm dialog (inline on page)
        page.waitForSelector("text=Are you sure you want to delete this loan", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Click cancel
        page.click("[data-test='cancel-delete-loan']");

        // Confirmation should disappear
        page.waitForTimeout(1000);
        assertThat(page.locator("text=Are you sure you want to delete this loan")).not().isVisible();

        // Navigate back to loans page
        page.click("[data-test='back-to-loans']");

        // Wait for navigation to /loans
        page.waitForURL("**/loans", new Page.WaitForURLOptions().setTimeout(10000L));

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

        // Wait for navigation to /loans/new
        page.waitForURL("**/loans/new", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for form to load
        page.waitForSelector("[data-test='checkout-book-select']", new Page.WaitForSelectorOptions().setTimeout(10000L));

        // Select a book (Available Book 1 - book ID 1)
        page.selectOption("[data-test='checkout-book-select']", "1");

        // Submit checkout (will checkout to librarian user)
        page.click("[data-test='checkout-submit']");

        // Should navigate back to /loans after successful checkout
        page.waitForURL("**/loans", new Page.WaitForURLOptions().setTimeout(10000L));

        // Wait for data to refresh
        page.waitForTimeout(2000);

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
