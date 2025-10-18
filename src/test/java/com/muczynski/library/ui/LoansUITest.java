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

            // Set dialog handler once for the entire test
            page.onDialog(dialog -> dialog.accept());

            // Navigate to loans section and assert visibility
            navigateToSection("loans");

            // Delete any existing loans to ensure clean state
            Locator loanList = page.locator("[data-test='loan-item']");
            if (loanList.count() > 0) {
                loanList.first().locator("[data-test='delete-loan-btn']").click();
                assertThat(loanList).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(5000));
            }

            // Wait for loan section and dropdown options
            page.waitForSelector("[data-test='loan-book']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='loan-book']", "1");
            page.waitForSelector("[data-test='loan-user']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='loan-user']", "2"); // Assuming testuser has ID 2

            // Create
            page.click("[data-test='checkout-btn']");

            // Read: Match on text content flexibly - adjust for actual username
            loanList = page.locator("[data-test='loan-item']");
            Locator loanItem = loanList.filter(new Locator.FilterOptions().setHasText("loaned to testuser"));
            loanItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(loanItem.first()).isVisible();
            assertThat(loanItem).hasCount(1);

            // Update: Change loan date instead of user (backend doesn't support user update)
            loanItem.first().locator("[data-test='edit-loan-btn']").click();
            page.selectOption("[data-test='loan-book']", "1");
            page.selectOption("[data-test='loan-user']", "2");
            page.fill("[data-test='loan-date']", "2023-01-01");
            page.click("[data-test='checkout-btn']");
            assertThat(page.locator("[data-test='checkout-btn']")).hasText("Checkout Book", new LocatorAssertions.HasTextOptions().setTimeout(5000));

            // Wait for the updated item to appear (confirms reload)
            Locator updatedLoanItem = loanList.filter(new Locator.FilterOptions().setHasText("loaned to testuser on 01/01/2023"));
            updatedLoanItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(updatedLoanItem.first()).isVisible();

            // Assert old item is gone (confirms successful reload)
            assertThat(loanList.filter(new Locator.FilterOptions().setHasText("loaned to testuser on"))).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(5000)); // Only the updated one remains

            // Delete
            Locator toDeleteLoan = loanList.filter(new Locator.FilterOptions().setHasText("loaned to testuser on 01/01/2023"));
            toDeleteLoan.first().locator("[data-test='delete-loan-btn']").click();
            toDeleteLoan.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(5000));
            assertThat(loanList.filter(new Locator.FilterOptions().setHasText("loaned to testuser on 01/01/2023"))).hasCount(0);

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loans-crud.png")));
            throw e;
        }
    }
}
