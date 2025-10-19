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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-authors.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthorsUITest {

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
        // Data is inserted via data-authors.sql in test profile, so no additional setup needed
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
            assertThat(page.locator("[data-test='add-author-btn']")).hasText("Update Author", new LocatorAssertions.HasTextOptions().setTimeout(5000));
            String updatedName = "Updated Author " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-author-name']", updatedName);
            page.click("[data-test='add-author-btn']");
            assertThat(page.locator("[data-test='add-author-btn']")).hasText("Add Author", new LocatorAssertions.HasTextOptions().setTimeout(5000));

            // Wait for the updated item to appear (confirms reload)
            Locator updatedAuthorItem = authorList.filter(new Locator.FilterOptions().setHasText(updatedName));
            updatedAuthorItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(updatedAuthorItem.first()).isVisible();

            // Assert old item is gone (confirms successful reload)
            assertThat(authorList.filter(new Locator.FilterOptions().setHasText(uniqueName))).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(5000));

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
    @Sql(value = "classpath:data-authors-sorting.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testAuthorsAreSortedBySurname() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            ensurePrerequisites();

            navigateToSection("authors");

            page.waitForSelector("[data-test='author-item']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));

            List<String> authorNames = page.locator("[data-test='author-name']").allTextContents();

            List<String> expectedOrder = Arrays.asList(
                    "Jane Austen",
                    "Charlotte Brontë",
                    "Emily Brontë",
                    "Charles Dickens",
                    "George Eliot",
                    "Mary Shelley"
            );

            assertEquals(expectedOrder, authorNames);

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-authors-sorting.png")));
            throw e;
        }
    }
}
