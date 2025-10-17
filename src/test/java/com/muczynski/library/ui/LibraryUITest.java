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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LibraryUITest {

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
        List<String> allSections = Arrays.asList("authors", "books", "libraries", "loans", "users");
        List<String> hiddenSections = allSections.stream()
                .filter(s -> !s.equals(section))
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
        // Data is inserted via data.sql in test profile, so no additional setup needed
    }

    @Test
    void testAuthorsCRUD() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            ensurePrerequisites();

            // Navigate to authors section and assert visibility
            navigateToSection("authors");

            // Wait for author section to be interactable, focusing on form
            page.waitForSelector("[data-test='new-author-name']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));

            // Create with unique name to avoid conflict
            String uniqueName = "Test Author " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-author-name']", uniqueName);
            page.click("[data-test='add-author-btn']");

            // Read: Use filter for flexible matching
            Locator authorList = page.locator("[data-test='author-item']");
            Locator authorItem = authorList.filter(new Locator.FilterOptions().setHasText(uniqueName));
            authorItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(authorItem.first()).isVisible();
            assertThat(authorItem).hasCount(1);

            // Update
            authorItem.first().locator("[data-test='edit-author-btn']").click();
            String updatedName = uniqueName + " Updated";
            page.fill("[data-test='new-author-name']", updatedName);
            page.click("[data-test='add-author-btn']");

            Locator updatedAuthorItem = authorList.filter(new Locator.FilterOptions().setHasText(updatedName));
            updatedAuthorItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(updatedAuthorItem.first()).isVisible();

            // Delete
            Locator toDelete = authorList.filter(new Locator.FilterOptions().setHasText(updatedName));
            page.onDialog(dialog -> dialog.accept());
            toDelete.first().locator("[data-test='delete-author-btn']").click();
            toDelete.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(5000));
            assertThat(authorList.filter(new Locator.FilterOptions().setHasText(updatedName))).hasCount(0);

        } catch (Exception e) {
            // Screenshot on failure for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-authors-crud.png")));
            throw e;
        }
    }

    @Test
    void testBooksCRUD() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            ensurePrerequisites();

            // Navigate to books section and assert visibility
            navigateToSection("books");

            // Wait for book section and dropdown options to load
            page.waitForSelector("[data-test='book-author']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='book-author']", "1");
            page.waitForSelector("[data-test='book-library']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='book-library']", "1");

            // Create
            String uniqueTitle = "Test Book " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-book-title']", uniqueTitle);
            page.fill("[data-test='new-book-year']", "2023");
            page.click("[data-test='add-book-btn']");

            // Read
            Locator bookList = page.locator("[data-test='book-item']");
            Locator bookItem = bookList.filter(new Locator.FilterOptions().setHasText(uniqueTitle));
            bookItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(bookItem.first()).isVisible();
            assertThat(bookItem).hasCount(1);

            // Update
            bookItem.first().locator("[data-test='edit-book-btn']").click();
            assertThat(page.locator("[data-test='add-book-btn']")).hasText("Update Book", new LocatorAssertions.HasTextOptions().setTimeout(5000));
            String updatedTitle = uniqueTitle + " Updated";
            page.fill("[data-test='new-book-title']", updatedTitle);
            page.click("[data-test='add-book-btn']");
            assertThat(page.locator("[data-test='add-book-btn']")).hasText("Add Book", new LocatorAssertions.HasTextOptions().setTimeout(5000));

            // Wait for list refresh by checking total count (initial 1 + test 1 = 2, after update back to 2 but old gone)
            Locator allBooks = page.locator("[data-test='book-item']");
            assertThat(allBooks).hasCount(2, new LocatorAssertions.HasCountOptions().setTimeout(5000));

            // Confirm the item matching the old title pattern is actually the updated one
            Locator oldFiltered = bookList.filter(new Locator.FilterOptions().setHasText(uniqueTitle));
            assertThat(oldFiltered).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(5000));
            assertThat(oldFiltered.first().locator("[data-test='book-title']")).hasText(updatedTitle);

            Locator updatedBookItem = bookList.filter(new Locator.FilterOptions().setHasText(updatedTitle));
            updatedBookItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(updatedBookItem.first()).isVisible();

            // Delete
            Locator toDeleteBook = bookList.filter(new Locator.FilterOptions().setHasText(updatedTitle));
            page.onDialog(dialog -> dialog.accept());
            toDeleteBook.first().locator("[data-test='delete-book-btn']").click();
            toDeleteBook.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(5000));
            assertThat(bookList.filter(new Locator.FilterOptions().setHasText(updatedTitle))).hasCount(0);

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-books-crud.png")));
            throw e;
        }
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

            Locator updatedLoanItem = loanList.filter(new Locator.FilterOptions().setHasText("loaned to testuser on 01/01/2023"));
            updatedLoanItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(updatedLoanItem.first()).isVisible();

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
