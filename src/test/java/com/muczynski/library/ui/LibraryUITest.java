package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Paths;
import java.util.UUID;

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
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(10000).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(10000).setState(WaitForSelectorState.VISIBLE));
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

            page.click("[data-test='menu-authors']");
            page.waitForTimeout(2000); // Allow JS to settle panels

            // Wait for author section to be interactable, focusing on form
            page.waitForSelector("[data-test='new-author-name']", new Page.WaitForSelectorOptions().setTimeout(10000).setState(WaitForSelectorState.VISIBLE));

            // Create with unique name to avoid conflict
            String uniqueName = "Test Author " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-author-name']", uniqueName);
            page.click("[data-test='add-author-btn']");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(1000);

            // Read: Use filter for flexible matching
            Locator authorList = page.locator("[data-test='author-item']");
            Locator authorItem = authorList.filter(new Locator.FilterOptions().setHasText(uniqueName));
            authorItem.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
            assertThat(authorItem.first()).isVisible();
            assertThat(authorItem).hasCount(1);

            // Update
            authorItem.first().locator("[data-test='edit-author-btn']").click();
            page.waitForTimeout(500);
            page.waitForSelector("[data-test='new-author-name']", new Page.WaitForSelectorOptions().setTimeout(10000).setState(WaitForSelectorState.VISIBLE));
            String updatedName = uniqueName + " Updated";
            page.fill("[data-test='new-author-name']", updatedName);
            page.click("[data-test='add-author-btn']");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(1000);

            Locator updatedAuthorItem = authorList.filter(new Locator.FilterOptions().setHasText(updatedName));
            updatedAuthorItem.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
            assertThat(updatedAuthorItem.first()).isVisible();

            // Delete
            page.onDialog(dialog -> dialog.accept());
            updatedAuthorItem.first().locator("[data-test='delete-author-btn']").click();
            page.waitForTimeout(1000);
            assertThat(updatedAuthorItem.first()).isHidden();

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

            page.click("[data-test='menu-books']");
            page.waitForTimeout(2000);

            // Wait for book section and dropdown options to load
            page.waitForSelector("[data-test='book-author']", new Page.WaitForSelectorOptions().setTimeout(10000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='book-author']", "1");
            page.waitForSelector("[data-test='book-library']", new Page.WaitForSelectorOptions().setTimeout(10000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='book-library']", "1");

            // Create
            String uniqueTitle = "Test Book " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-book-title']", uniqueTitle);
            page.fill("[data-test='new-book-year']", "2023");
            page.click("[data-test='add-book-btn']");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(1000);

            // Read
            Locator bookList = page.locator("[data-test='book-item']");
            Locator bookItem = bookList.filter(new Locator.FilterOptions().setHasText(uniqueTitle));
            bookItem.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
            assertThat(bookItem.first()).isVisible();
            assertThat(bookItem).hasCount(1);

            // Update
            bookItem.first().locator("[data-test='edit-book-btn']").click();
            page.waitForTimeout(500);
            String updatedTitle = uniqueTitle + " Updated";
            page.fill("[data-test='new-book-title']", updatedTitle);
            page.click("[data-test='add-book-btn']");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(1000);

            Locator updatedBookItem = bookList.filter(new Locator.FilterOptions().setHasText(updatedTitle));
            updatedBookItem.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
            assertThat(updatedBookItem.first()).isVisible();

            // Delete
            page.onDialog(dialog -> dialog.accept());
            updatedBookItem.first().locator("[data-test='delete-book-btn']").click();
            page.waitForTimeout(1000);
            assertThat(updatedBookItem.first()).isHidden();

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

            page.click("[data-test='menu-loans']");
            page.waitForTimeout(2000);

            // Wait for loan section and dropdown options
            page.waitForSelector("[data-test='loan-book']", new Page.WaitForSelectorOptions().setTimeout(10000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='loan-book']", "1");
            page.waitForSelector("[data-test='loan-user']", new Page.WaitForSelectorOptions().setTimeout(10000).setState(WaitForSelectorState.VISIBLE));
            page.selectOption("[data-test='loan-user']", "2"); // Assuming testuser has ID 2

            // Create
            page.click("[data-test='checkout-btn']");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(1000);

            // Read: Match on text content flexibly - adjust for actual username
            Locator loanList = page.locator("[data-test='loan-item']");
            Locator loanItem = loanList.filter(new Locator.FilterOptions().setHasText("loaned to testuser"));
            loanItem.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
            assertThat(loanItem.first()).isVisible();
            assertThat(loanItem).hasCount(1);

            // Update
            loanItem.first().locator("[data-test='edit-loan-btn']").click();
            page.waitForTimeout(500);
            page.selectOption("[data-test='loan-user']", "1"); // Change to librarian
            page.click("[data-test='checkout-btn']");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(1000);

            Locator updatedLoanItem = loanList.filter(new Locator.FilterOptions().setHasText("loaned to librarian"));
            updatedLoanItem.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
            assertThat(updatedLoanItem.first()).isVisible();

            // Delete
            page.onDialog(dialog -> dialog.accept());
            updatedLoanItem.first().locator("[data-test='delete-loan-btn']").click();
            page.waitForTimeout(1000);
            assertThat(updatedLoanItem.first()).isHidden();

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-loans-crud.png")));
            throw e;
        }
    }
}
