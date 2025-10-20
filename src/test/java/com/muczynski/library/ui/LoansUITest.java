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
        BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private void login() {
        page.navigate("http://localhost:" + port);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.click("[data-test='menu-login']");
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
    }

    private void navigateToSection(String section) {
        // Click the menu button for the section
        page.click("[data-test='menu-" + section + "']");

        // Wait for target section to be visible and assert it
        String targetSelector = "#" + section + "-section";
        Locator targetSection = page.locator(targetSelector);
        targetSection.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        assertThat(targetSection).isVisible();

        // Assert all non-target sections are hidden to test exclusivity
        List<String> allSections = Arrays.asList("authors", "books", "libraries", "loans", "users", "search");
        List<String> hiddenSections = allSections.stream()
                .filter(s -> !s.equals(section) && !s.equals("search"))
                .collect(Collectors.toList());
        if (!hiddenSections.isEmpty()) {
            for (String hiddenSection : hiddenSections) {
                assertThat(page.locator("#" + hiddenSection + "-section")).isHidden();
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
        try {
            page.navigate("http://localhost:" + port);
            login();
            ensurePrerequisites();
            page.onDialog(dialog -> dialog.accept());

            // Navigate to loans section and assert visibility
            navigateToSection("loans");

            // Wait for loan section to be interactable, focusing on form and table
            page.waitForSelector("[data-test='loan-book']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
            page.waitForSelector("[data-test='loan-table']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));

            // Assert initial loan exists from SQL data and due date is shown (not returned)
            Locator initialLoanList = page.locator("[data-test='loan-item']");
            initialLoanList.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(initialLoanList).hasCount(1);
            String initialDetails = initialLoanList.first().locator("[data-test='loan-details']").innerText();
            assertTrue(initialDetails.contains("Initial Book") && initialDetails.contains("testuser"));
            String initialDueDate = initialLoanList.first().locator("[data-test='loan-due-date']").innerText();
            assertFalse(initialDueDate.isEmpty());
            assertTrue(initialDueDate.contains("/"));

            // Return the initial loan to test due date hiding
            initialLoanList.first().locator("[data-test='return-book-btn']").click();
            // Wait for return button to disappear
            page.waitForSelector("[data-test='return-book-btn']", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.DETACHED));
            initialLoanList = page.locator("[data-test='loan-item']");
            String returnedDueDate = initialLoanList.first().locator("[data-test='loan-due-date']").innerText();
            assertTrue(returnedDueDate.isEmpty());
            String returnedDetails = initialLoanList.first().locator("[data-test='loan-details']").innerText();
            assertTrue(returnedDetails.contains("returned"));

            // Delete the returned loan for clean state
            initialLoanList.first().locator("[data-test='delete-loan-btn']").click();
            initialLoanList.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(5000));
            assertThat(page.locator("[data-test='loan-item']")).hasCount(0);

            // Create a new loan with custom due date
            page.selectOption("[data-test='loan-book']", "1");
            page.selectOption("[data-test='loan-user']", "2");
            page.fill("[data-test='loan-date']", "2023-01-01");
            page.fill("[data-test='due-date']", "2023-01-15");
            page.click("[data-test='checkout-btn']");

            // Read: Wait for new loan item to appear
            Locator loanList = page.locator("[data-test='loan-item']");
            loanList.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(loanList).hasCount(1);
            String detailsText = loanList.first().locator("[data-test='loan-details']").innerText();
            assertTrue(detailsText.contains("Initial Book") && detailsText.contains("loaned to testuser on 01/01/2023"));
            String dueDateText = loanList.first().locator("[data-test='loan-due-date']").innerText();
            assertEquals("01/15/2023", dueDateText);

            // Update: Edit due date
            loanList.first().locator("[data-test='edit-loan-btn']").click();
            assertThat(page.locator("[data-test='checkout-btn']")).hasText("Update Loan", new LocatorAssertions.HasTextOptions().setTimeout(5000));
            page.fill("[data-test='due-date']", "2023-02-01");
            page.click("[data-test='checkout-btn']");

            // Wait for button to reset to "Checkout Book", confirming the update operation completed successfully
            assertThat(page.locator("[data-test='checkout-btn']")).hasText("Checkout Book", new LocatorAssertions.HasTextOptions().setTimeout(5000));

            // Wait for the updated item to appear (confirms reload)
            loanList = page.locator("[data-test='loan-item']");
            loanList.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(loanList).hasCount(1);
            String updatedDueDate = loanList.first().locator("[data-test='loan-due-date']").innerText();
            assertEquals("02/01/2023", updatedDueDate);

            // Delete
            loanList.first().locator("[data-test='delete-loan-btn']").click();
            loanList.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(5000));
            assertThat(page.locator("[data-test='loan-item']")).hasCount(0);

        } catch (Exception e) {
            // Screenshot on failure for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loans-crud.png")));
            throw e;
        }
    }
}
