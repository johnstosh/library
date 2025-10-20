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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-books.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BooksUITest {

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
        // Data is inserted via data-books.sql in test profile, so no additional setup needed
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
            Locator bookRow = page.locator("[data-test='book-item']").filter(new Locator.FilterOptions().setHasText(uniqueTitle));
            bookRow.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(bookRow.first()).isVisible();
            assertThat(bookRow).hasCount(1);

            // Update
            bookRow.first().locator("[data-test='edit-book-btn']").click();
            assertThat(page.locator("[data-test='add-book-btn']")).hasText("Update Book", new LocatorAssertions.HasTextOptions().setTimeout(5000));
            String updatedTitle = "Updated Book " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-book-title']", updatedTitle);
            page.click("[data-test='add-book-btn']");
            assertThat(page.locator("[data-test='add-book-btn']")).hasText("Add Book", new LocatorAssertions.HasTextOptions().setTimeout(5000));

            // Wait for list refresh by checking total count
            Locator allBookRows = page.locator("[data-test='book-item']");
            assertThat(allBookRows).hasCount(2, new LocatorAssertions.HasCountOptions().setTimeout(5000));

            Locator updatedBookRow = page.locator("[data-test='book-item']").filter(new Locator.FilterOptions().setHasText(updatedTitle));
            updatedBookRow.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(updatedBookRow.first()).isVisible();

            // Assert old item is gone
            assertThat(page.locator("[data-test='book-item']").filter(new Locator.FilterOptions().setHasText(uniqueTitle))).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(5000));

            // Delete
            Locator toDeleteRow = page.locator("[data-test='book-item']").filter(new Locator.FilterOptions().setHasText(updatedTitle));
            page.onDialog(dialog -> dialog.accept());
            toDeleteRow.first().locator("[data-test='delete-book-btn']").click();
            toDeleteRow.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(5000));
            assertThat(page.locator("[data-test='book-item']").filter(new Locator.FilterOptions().setHasText(updatedTitle))).hasCount(0);
            assertThat(page.locator("[data-test='book-item']")).hasCount(1);

        } catch (Exception e) {
            // Screenshot on failure for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-books-crud.png")));
            throw e;
        }
    }

    @Test
    @Sql(value = "classpath:data-books-sorting.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testBookListIsSortedAlphabetically() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            navigateToSection("books");

            page.waitForSelector("[data-test='book-item']", new Page.WaitForSelectorOptions().setTimeout(5000));

            List<String> titles = page.locator("tbody#book-list-body tr[data-test='book-item'] td:first-child span[data-test='book-title']").allTextContents();

            List<String> expectedTitles = Arrays.asList(
                    "Animal Farm",
                    "Brave New World",
                    "The Color Purple",
                    "The Great Gatsby"
            );

            assertEquals(expectedTitles, titles, "Books are not sorted correctly.");

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-books-sorting.png")));
            throw e;
        }
    }
}
