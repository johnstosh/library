package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

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
        browser = playwright.chromium().launch();
    }

    @AfterAll
    void closeBrowser() {
        browser.close();
    }

    @BeforeEach
    void createContextAndPage() {
        BrowserContext context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        page.context().close();
    }

    private void login() {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(10000));
        page.waitForSelector("[data-test='login-username']");
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.waitForSelector("[data-test='login-submit']");
        page.click("[data-test='login-submit']");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForSelector("[data-test='author-list'] li", new Page.WaitForSelectorOptions().setTimeout(10000));
    }

    @Test
    void testAuthorsCRUD() {
        page.navigate("http://localhost:" + port);
        login();

        page.click("[data-test='menu-authors']");

        // Create
        page.waitForSelector("[data-test='new-author-name']");
        page.fill("[data-test='new-author-name']", "Test Author");
        page.click("[data-test='add-author-btn']");
        Locator authorItem = page.locator("[data-test='author-item']:has([data-test='author-name']:has-text('Test Author'))");
        authorItem.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertThat(authorItem).isVisible();

        // Read
        assertThat(authorItem).hasCount(1);

        // Update
        authorItem.locator("[data-test='edit-author-btn']").click();
        page.waitForSelector("[data-test='new-author-name']");
        page.fill("[data-test='new-author-name']", "Updated Test Author");
        page.click("[data-test='add-author-btn']");
        Locator updatedAuthorItem = page.locator("[data-test='author-item']:has([data-test='author-name']:has-text('Updated Test Author'))");
        updatedAuthorItem.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertThat(updatedAuthorItem).isVisible();

        // Delete
        page.onDialog(dialog -> dialog.accept());
        updatedAuthorItem.locator("[data-test='delete-author-btn']").click();
        assertThat(updatedAuthorItem).isHidden();
    }

    @Test
    void testBooksCRUD() {
        page.navigate("http://localhost:" + port);
        login();

        page.click("[data-test='menu-books']");

        // Create
        page.waitForSelector("[data-test='book-author']");
        page.waitForSelector("#book-author option[value='1']", new Page.WaitForSelectorOptions().setTimeout(5000));
        page.waitForSelector("#book-library option[value='1']", new Page.WaitForSelectorOptions().setTimeout(5000));
        page.selectOption("[data-test='book-author']", "1");
        page.selectOption("[data-test='book-library']", "1");
        page.fill("[data-test='new-book-title']", "Test Book");
        page.fill("[data-test='new-book-year']", "2023");
        page.click("[data-test='add-book-btn']");
        Locator bookItem = page.locator("[data-test='book-item']:has([data-test='book-title']:has-text('Test Book'))");
        bookItem.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertThat(bookItem).isVisible();

        // Read
        assertThat(bookItem).hasCount(1);

        // Update
        bookItem.locator("[data-test='edit-book-btn']").click();
        page.waitForSelector("[data-test='new-book-title']");
        page.fill("[data-test='new-book-title']", "Updated Test Book");
        page.click("[data-test='add-book-btn']");
        Locator updatedBookItem = page.locator("[data-test='book-item']:has([data-test='book-title']:has-text('Updated Test Book'))");
        updatedBookItem.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertThat(updatedBookItem).isVisible();

        // Delete
        page.onDialog(dialog -> dialog.accept());
        updatedBookItem.locator("[data-test='delete-book-btn']").click();
        assertThat(updatedBookItem).isHidden();
    }

    @Test
    void testLoansCRUD() {
        page.navigate("http://localhost:" + port);
        login();

        page.click("[data-test='menu-loans']");

        // Create
        page.waitForSelector("[data-test='loan-book']");
        page.waitForSelector("#loan-book option[value='1']", new Page.WaitForSelectorOptions().setTimeout(5000));
        page.waitForSelector("#loan-user option[value='1']", new Page.WaitForSelectorOptions().setTimeout(5000));
        page.selectOption("[data-test='loan-book']", "1");
        page.selectOption("[data-test='loan-user']", "1");
        page.click("[data-test='checkout-btn']");
        Locator loanItem = page.locator("[data-test='loan-item']:has([data-test='loan-details']:has-text('loaned to librarian'))");
        loanItem.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertThat(loanItem).isVisible();

        // Read
        assertThat(loanItem).hasCount(1);

        // Update
        loanItem.locator("[data-test='edit-loan-btn']").click();
        page.waitForSelector("[data-test='loan-user']");
        page.selectOption("[data-test='loan-user']", "2");
        page.click("[data-test='checkout-btn']");
        Locator updatedLoanItem = page.locator("[data-test='loan-item']:has([data-test='loan-details']:has-text('loaned to testuser'))");
        updatedLoanItem.waitFor(new Locator.WaitForOptions().setTimeout(5000));
        assertThat(updatedLoanItem).isVisible();

        // Delete
        page.onDialog(dialog -> dialog.accept());
        updatedLoanItem.locator("[data-test='delete-loan-btn']").click();
        assertThat(updatedLoanItem).isHidden();
    }
}
