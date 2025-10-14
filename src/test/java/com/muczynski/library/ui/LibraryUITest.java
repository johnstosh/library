package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

    @Test
    void testAuthorsCRUD() {
        page.navigate("http://localhost:" + port);

        page.click("text=Authors");

        // Create
        page.click("text=Add Author");
        page.fill("input[name='name']", "Test Author");
        page.click("button[type='submit']");
        assertThat(page.locator("text=Test Author")).isVisible();

        // Read
        assertThat(page.locator("text=Test Author")).hasCount(1);

        // Update
        page.click("text=Test Author >> .. >> button:has-text('Edit')");
        page.fill("input[name='name']", "Updated Test Author");
        page.click("button[type='submit']");
        assertThat(page.locator("text=Updated Test Author")).isVisible();

        // Delete
        page.click("text=Updated Test Author >> .. >> button:has-text('Delete')");
        assertThat(page.locator("text=Updated Test Author")).isHidden();
    }

    @Test
    void testBooksCRUD() {
        page.navigate("http://localhost:" + port);

        page.click("text=Books");

        // Create
        page.click("text=Add Book");
        page.fill("input[name='title']", "Test Book");
        page.fill("input[name='author']", "Test Author");
        page.fill("input[name='publicationYear']", "2023");
        page.click("button[type='submit']");
        assertThat(page.locator("text=Test Book")).isVisible();

        // Read
        assertThat(page.locator("text=Test Book")).hasCount(1);

        // Update
        page.click("text=Test Book >> .. >> button:has-text('Edit')");
        page.fill("input[name='title']", "Updated Test Book");
        page.click("button[type='submit']");
        assertThat(page.locator("text=Updated Test Book")).isVisible();

        // Delete
        page.click("text=Updated Test Book >> .. >> button:has-text('Delete')");
        assertThat(page.locator("text=Updated Test Book")).isHidden();
    }

    @Test
    void testLoansCRUD() {
        page.navigate("http://localhost:" + port);

        page.click("text=Loans");

        // Create
        page.click("text=Add Loan");
        page.fill("input[name='bookId']", "1");
        page.fill("input[name='borrowerName']", "Test Borrower");
        page.click("button[type='submit']");
        assertThat(page.locator("text=Test Borrower")).isVisible();

        // Read
        assertThat(page.locator("text=Test Borrower")).hasCount(1);

        // Update
        page.click("text=Test Borrower >> .. >> button:has-text('Edit')");
        page.fill("input[name='borrowerName']", "Updated Test Borrower");
        page.click("button[type='submit']");
        assertThat(page.locator("text=Updated Test Borrower")).isVisible();

        // Delete
        page.click("text=Updated Test Borrower >> .. >> button:has-text('Delete')");
        assertThat(page.locator("text=Updated Test Borrower")).isHidden();
    }
}