/*
 * (c) Copyright 2025 by Muczynski
 */
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
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 720));
        page = context.newPage();
        page.setDefaultTimeout(20000L);
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private void login() {
        page.navigate("http://localhost:" + port);
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));
        page.waitForSelector("[data-test='menu-login']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.click("[data-test='menu-login']");
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.waitForSelector("[data-test='menu-authors']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
    }

    private void navigateToSection(String section) {
        // Click the menu button for the section
        page.click("[data-test='menu-" + section + "']");

        // Wait for target section to be visible and assert it
        String targetSelector = "#" + section + "-section";
        Locator targetSection = page.locator(targetSelector);
        targetSection.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
        assertThat(targetSection).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

        // Assert all non-target sections are hidden to test exclusivity
        List<String> allSections = Arrays.asList("authors", "books", "libraries", "loans", "users", "search");
        List<String> hiddenSections = allSections.stream()
                .filter(s -> !s.equals(section) && !s.equals("search"))
                .collect(Collectors.toList());
        if (!hiddenSections.isEmpty()) {
            for (String hiddenSection : hiddenSections) {
                assertThat(page.locator("#" + hiddenSection + "-section")).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(20000L));
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
            page.waitForSelector("[data-test='new-author-name']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));

            // Create with unique name to avoid conflict
            String uniqueName = "Test Author " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-author-name']", uniqueName);
            page.click("[data-test='add-author-btn']");

            // Wait for the operation to complete - network idle ensures POST + GET /api/authors completes
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Wait for button to reset to "Add Author" after creation
            Locator addButton = page.locator("[data-test='add-author-btn']");
            assertThat(addButton).hasText("Add Author", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

            // Read: Use filter for flexible matching
            Locator authorList = page.locator("[data-test='author-item']");
            Locator authorRow = authorList.filter(new Locator.FilterOptions().setHasText(uniqueName));
            authorRow.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
            assertThat(authorRow.first()).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            assertThat(authorRow).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(20000L)); // Only new
            assertThat(authorList).hasCount(2, new LocatorAssertions.HasCountOptions().setTimeout(20000L)); // Initial + new

            // Update
            authorRow.first().scrollIntoViewIfNeeded();
            authorRow.first().locator("[data-test='edit-author-btn']").click();
            addButton.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(addButton).hasText("Update Author", new LocatorAssertions.HasTextOptions().setTimeout(20000L));
            String updatedName = "Updated Author " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-author-name']", updatedName);
            page.click("[data-test='add-author-btn']");

            // Wait for the operation to complete
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Wait for the button to reset to "Add Author" after update
            assertThat(addButton).hasText("Add Author", new LocatorAssertions.HasTextOptions().setTimeout(20000L));
            addButton.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(addButton).hasText("Add Author", new LocatorAssertions.HasTextOptions().setTimeout(20000L));

            // Wait for the updated item to appear (confirms reload)
            Locator updatedAuthorRow = authorList.filter(new Locator.FilterOptions().setHasText(updatedName));
            updatedAuthorRow.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000L));
            assertThat(updatedAuthorRow.first()).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // Assert old item is gone (confirms successful reload)
            Locator oldAuthorRow = authorList.filter(new Locator.FilterOptions().setHasText(uniqueName));
            assertThat(oldAuthorRow).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(20000L)); // Test item removed

            // Delete
            Locator toDelete = authorList.filter(new Locator.FilterOptions().setHasText(updatedName));
            page.onDialog(dialog -> dialog.accept());
            toDelete.first().locator("[data-test='delete-author-btn']").click();

            // Wait for the operation to complete
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            toDelete.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(20000L));
            Locator deletedRowCheck = authorList.filter(new Locator.FilterOptions().setHasText(updatedName));
            assertThat(deletedRowCheck).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(20000L));
            assertThat(authorList).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(20000L));

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

            // Wait for author section to be interactable, focusing on list
            page.waitForSelector("[data-test='author-item']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));

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
