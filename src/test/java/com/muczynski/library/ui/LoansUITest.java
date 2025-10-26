// (c) Copyright 2025 by Muczynski
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
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-loans.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoansUITest {

    @LocalServerPort
    private int port;

    private Browser browser;
    private Page page;

    @BeforeAll
    void launchBrowser() {
        Playwright playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true)); // Headless for CI execution
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
        page.setDefaultTimeout(5000L);
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private void login() {
        page.navigate("http://localhost:" + port);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L));
        page.waitForSelector("[data-test='menu-login']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
        page.click("[data-test='menu-login']");
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
        page.waitForSelector("[data-test='menu-authors']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
    }

    private void navigateToSection(String section) {
        // Click the menu button for the section
        page.click("[data-test='menu-" + section + "']");

        // Wait for target section to be visible and assert it
        String targetSelector = "#" + section + "-section";
        Locator targetSection = page.locator(targetSelector);
        targetSection.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));
        assertThat(targetSection).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));

        // Assert all non-target sections are hidden to test exclusivity
        List<String> allSections = Arrays.asList("authors", "books", "libraries", "loans", "users", "search");
        List<String> hiddenSections = allSections.stream()
                .filter(s -> !s.equals(section) && !s.equals("search"))
                .collect(Collectors.toList());
        if (!hiddenSections.isEmpty()) {
            for (String hiddenSection : hiddenSections) {
                assertThat(page.locator("#" + hiddenSection + "-section")).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(5000L));
            }
        }

        // Additional JS poll for display style to confirm non-target sections are hidden
        String jsExpression = "(function() { " +
                "document.querySelectorAll('.section').forEach(s => { " +
                "  if (s.id !== '" + section + "-section' && s.id.endsWith('-section')) { " +
                "    if (window.getComputedStyle(s).display !== 'none') { " +
                "      throw new Error('Non-target section is visible'); " +
                "    } " +
                "  } " +
                "}); " +
                "return true; " +
                "})()";
        page.waitForFunction(jsExpression);
    }

    private void ensurePrerequisites() {
        // Data is inserted via data-loans.sql in test profile, so no additional setup needed
    }

    @Test
    void testLoansCRUD() {
        page.onDialog(dialog -> dialog.accept());
        try {
            page.navigate("http://localhost:" + port);
            login();
            ensurePrerequisites();

            // Navigate to loans section and assert visibility
            navigateToSection("loans");

            // Wait for loan section to be interactable, focusing on form and table
            page.waitForSelector("[data-test='loan-book']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
            page.waitForSelector("[data-test='loan-table']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));

            // Assert initial loan exists from SQL data and due date is shown (not returned)
            Locator initialLoanList = page.locator("[data-test='loan-item']");
            initialLoanList.first().waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(initialLoanList).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(5000L));
            String initialBookTitle = initialLoanList.first().locator("[data-test='loan-book-title']").innerText();
            assertTrue(initialBookTitle.contains("Initial Book"));
            String initialUser = initialLoanList.first().locator("[data-test='loan-user']").innerText();
            assertTrue(initialUser.contains("testuser"));
            String initialDueDate = initialLoanList.first().locator("[data-test='loan-due-date']").innerText();
            assertFalse(initialDueDate.isEmpty());
            String initialReturnDate = initialLoanList.first().locator("[data-test='loan-return-date']").innerText();
            assertEquals("Not returned", initialReturnDate);


            // Return the initial loan
            initialLoanList.first().locator("[data-test='return-book-btn']").click();

            // Wait for the loan to disappear from the active loans list (as it's now returned)
            assertThat(initialLoanList).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(10000L));

            // Show returned loans to verify the change
            page.check("[data-test='show-returned-loans-checkbox']");

            // The loan should reappear in the list now
            Locator returnedLoanList = page.locator("[data-test='loan-item']");
            assertThat(returnedLoanList).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(10000L));

            // Assert that the return date is now populated correctly
            Locator returnDateCell = returnedLoanList.first().locator("[data-test='loan-return-date']");
            assertThat(returnDateCell).not().hasText("Not returned", new LocatorAssertions.HasTextOptions().setTimeout(10000L));
            String returnedDate = returnDateCell.innerText();
            assertFalse(returnedDate.isEmpty());

            // Delete the returned loan for clean state
            initialLoanList.first().locator("[data-test='delete-loan-btn']").click();

            // Wait for the operation to complete
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L));

            // Re-query and assert count 0
            Locator loanListAfterDelete = page.locator("[data-test='loan-item']");
            assertThat(loanListAfterDelete).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(5000L));

            // Create a new loan with custom due date
            page.selectOption("[data-test='loan-book']", "1");
            page.selectOption("[data-test='loan-user']", "2");
            page.fill("[data-test='loan-date']", "2023-01-01");
            page.fill("[data-test='due-date']", "2023-01-15");
            page.click("[data-test='checkout-btn']");

            // Wait for the operation to complete
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L));

            // Wait for button to reset to "Checkout Book" after creation
            Locator checkoutBtn = page.locator("[data-test='checkout-btn']");
            assertThat(checkoutBtn).hasText("Checkout Book", new LocatorAssertions.HasTextOptions().setTimeout(5000L));

            // Read: Wait for new loan item to appear
            Locator loanList = page.locator("[data-test='loan-item']");
            loanList.first().waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(loanList).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(5000L));
            String bookTitle = loanList.first().locator("[data-test='loan-book-title']").innerText();
            assertTrue(bookTitle.contains("Initial Book"));
            String user = loanList.first().locator("[data-test='loan-user']").innerText();
            assertTrue(user.contains("testuser"));
            String loanDate = loanList.first().locator("[data-test='loan-date']").innerText();
            assertEquals("01/01/2023", loanDate);
            String dueDateText = loanList.first().locator("[data-test='loan-due-date']").innerText();
            assertEquals("01/15/2023", dueDateText);

            // Update: Edit due date
            loanList.first().locator("[data-test='edit-loan-btn']").click();
            checkoutBtn.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(checkoutBtn).hasText("Update Loan", new LocatorAssertions.HasTextOptions().setTimeout(5000L));
            page.fill("[data-test='due-date']", "2023-02-01");
            page.click("[data-test='checkout-btn']");

            // Wait for the operation to complete
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L));

            // Wait for button to reset to "Checkout Book", confirming the update operation completed successfully
            assertThat(checkoutBtn).hasText("Checkout Book", new LocatorAssertions.HasTextOptions().setTimeout(5000L));
            checkoutBtn.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(checkoutBtn).hasText("Checkout Book", new LocatorAssertions.HasTextOptions().setTimeout(5000L));

            // Wait for the updated item to appear (confirms reload)
            loanList = page.locator("[data-test='loan-item']");
            loanList.first().waitFor(new Locator.WaitForOptions().setTimeout(10000L));
            assertThat(loanList).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(10000L));
            String updatedDueDate = loanList.first().locator("[data-test='loan-due-date']").innerText();
            assertEquals("02/01/2023", updatedDueDate);

            // Delete
            Locator firstLoanItem = loanList.first();
            firstLoanItem.locator("[data-test='delete-loan-btn']").click();

            // Wait for the row to be detached from the DOM
            firstLoanItem.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(10000L));

            // Final assertion: ensure no loan items are visible
            assertThat(page.locator("[data-test='loan-item']")).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(10000L));

        } catch (Exception e) {
            // Screenshot on failure for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loans-crud.png")));
            throw e;
        }
    }

    @Test
    void testDefaultLoanDates() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            navigateToSection("loans");

            page.waitForSelector("[data-test='loan-date']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));

            String loanDateValue = page.locator("[data-test='loans-form'] [data-test='loan-date']").inputValue();
            String dueDateValue = page.locator("[data-test='loans-form'] [data-test='due-date']").inputValue();

            LocalDate today = LocalDate.now();
            LocalDate twoWeeksFromNow = today.plusDays(14);

            assertEquals(today.toString(), loanDateValue);
            assertEquals(twoWeeksFromNow.toString(), dueDateValue);

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-default-dates.png")));
            throw e;
        }
    }
}
