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

            // Set dialog handler once for the entire test
            page.onDialog(dialog -> dialog.accept());

            // Navigate to loans section and assert visibility
            navigateToSection("loans");

            // Assert that the table is visible
            assertThat(page.locator("[data-test='loan-table']")).isVisible();

            // Delete any existing loans to ensure clean state
            Locator loanList = page.locator("[data-test='loan-item']");
            while (loanList.count() > 0) {
                int currentCount = loanList.count();
                loanList.first().locator("[data-test='delete-loan-btn']").click();
                page.waitForFunction("count => document.querySelectorAll('[data-test=\"loan-item\"]').length < count", currentCount);
                loanList = page.locator("[data-test='loan-item']");
            }
            assertThat(loanList).hasCount(0);

            // Wait for loan section and dropdown options
            page.waitForSelector("[data-test='loan-book']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='loan-book']", "1");
            page.waitForSelector("[data-test='loan-user']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='loan-user']", "2"); // Assuming testuser has ID 2

            // Create
            page.click("[data-test='checkout-btn']");

            // Wait for new loan item to appear by count
            page.waitForFunction("() => document.querySelectorAll('[data-test=\"loan-item\"]').length === 1");

            loanList = page.locator("[data-test='loan-item']");
            assertThat(loanList).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(5000));

            // Assert details text
            String detailsText = loanList.first().locator("[data-test='loan-details']").innerText();
            assertTrue(detailsText.contains("Initial Book"));
            assertTrue(detailsText.contains("loaned to testuser"));

            // Assert due date is present and formatted
            String dueText = loanList.first().locator("[data-test='loan-due-date']").innerText();
            assertFalse(dueText.isEmpty());
            assertTrue(dueText.contains("/"));

            // Update: Change loan date
            loanList.first().locator("[data-test='edit-loan-btn']").click();
            page.selectOption("[data-test='loan-book']", "1");
            page.selectOption("[data-test='loan-user']", "2");
            page.fill("[data-test='loan-date']", "2023-01-01");
            page.click("[data-test='checkout-btn']");
            page.waitForTimeout(500); // Allow JS to settle

            // Wait for button to reset to "Checkout Book"
            assertThat(page.locator("[data-test='checkout-btn']")).hasText("Checkout Book", new LocatorAssertions.HasTextOptions().setTimeout(5000));

            // Wait for list to update (still 1 item)
            loanList = page.locator("[data-test='loan-item']");
            assertThat(loanList).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(5000));

            // Assert updated details text
            String updatedDetails = loanList.first().locator("[data-test='loan-details']").innerText();
            assertTrue(updatedDetails.contains("on 01/01/2023"));

            // Delete
            loanList.first().locator("[data-test='delete-loan-btn']").click();

            // Wait for item to detach
            loanList.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(5000));
            loanList = page.locator("[data-test='loan-item']");
            assertThat(loanList).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(5000));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loans-crud.png")));
            throw e;
        }
    }
}
