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

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-libraries.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LibrariesUITest {

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
        page.setDefaultTimeout(5000L);
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private void login() {
        page.navigate("http://localhost:" + port);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L));
        page.waitForSelector("[data-test='menu-login']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
        page.click("[data-test='menu-login']");
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
        page.waitForSelector("[data-test='menu-authors']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
    }

    private void navigateToSection(String section) {
        // Click the menu button for the section
        page.click("[data-test='menu-" + section + "']");

        // Wait for target section to be visible and assert it
        String targetSelector = "#" + section + "-section";
        Locator targetSection = page.locator(targetSelector);
        targetSection.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));
        assertThat(targetSection).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));

        // Assert all non-target sections are hidden to test exclusivity
        List<String> allSections = Arrays.asList("authors", "books", "libraries", "loans", "users", "search");
        List<String> hiddenSections = allSections.stream()
                .filter(s -> !s.equals(section) && !s.equals("search"))
                .collect(Collectors.toList());
        if (!hiddenSections.isEmpty()) {
            for (String hiddenSection : hiddenSections) {
                assertThat(page.locator("#" + hiddenSection + "-section")).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(5000L));
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
        // Data is inserted via data-libraries.sql in test profile, so no additional setup needed
    }

    @Test
    void testLibrariesCRUD() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            ensurePrerequisites();

            // Navigate to libraries section and assert visibility
            navigateToSection("libraries");

            // Assert the table structure is present
            Locator table = page.locator("[data-test='library-table']");
            table.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(table).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));

            // Wait for library section to be interactable, focusing on form
            page.waitForSelector("[data-test='new-library-name']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));

            // Create with unique name to avoid conflict
            String uniqueName = "St. Martin de Porres " + UUID.randomUUID().toString().substring(0, 8);
            String uniqueHostname = "library-" + UUID.randomUUID().toString().substring(0, 8) + ".muczynskifamily.com";
            page.fill("[data-test='new-library-name']", uniqueName);
            page.fill("[data-test='new-library-hostname']", uniqueHostname);
            page.click("[data-test='add-library-btn']");

            // Wait for the operation to complete
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L));

            // Wait for button to reset to "Add Library" after creation
            Locator addButton = page.locator("[data-test='add-library-btn']");
            assertThat(addButton).hasText("Add Library", new LocatorAssertions.HasTextOptions().setTimeout(5000L));

            // Read: Use filter for flexible matching and assert name in specific cell
            Locator libraryList = page.locator("[data-test='library-item']");
            Locator libraryItem = libraryList.filter(new Locator.FilterOptions().setHasText(uniqueName));
            libraryItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(libraryItem.first()).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));
            assertThat(libraryItem).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(5000L)); // Only new
            assertThat(libraryList).hasCount(2, new LocatorAssertions.HasCountOptions().setTimeout(5000L)); // Initial + new
            // Assert the name is in the library-name span
            assertThat(libraryItem.first().locator("[data-test='library-name']")).hasText(uniqueName + " (" + uniqueHostname + ")", new LocatorAssertions.HasTextOptions().setTimeout(5000L));

            // Update
            libraryItem.first().locator("[data-test='edit-library-btn']").click();

            // Wait for the form to be in update mode
            addButton.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(addButton).hasText("Update Library", new LocatorAssertions.HasTextOptions().setTimeout(5000L));

            String updatedName = "Updated Library " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-library-name']", updatedName);
            page.click("[data-test='add-library-btn']");

            // Wait for the operation to complete
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L));

            // Wait for button to reset to "Add Library", confirming the update operation completed successfully
            assertThat(addButton).hasText("Add Library", new LocatorAssertions.HasTextOptions().setTimeout(5000L));
            addButton.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(addButton).hasText("Add Library", new LocatorAssertions.HasTextOptions().setTimeout(5000L));

            // Wait for the updated item to appear (confirms reload)
            Locator updatedLibraryItem = libraryList.filter(new Locator.FilterOptions().setHasText(updatedName));
            updatedLibraryItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(updatedLibraryItem.first()).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));
            // Assert the updated name is in the library-name span
            assertThat(updatedLibraryItem.first().locator("[data-test='library-name']")).hasText(updatedName + " (" + uniqueHostname + ")", new LocatorAssertions.HasTextOptions().setTimeout(5000L));

            // Assert old item is gone (confirms successful reload)
            Locator oldLibraryItem = libraryList.filter(new Locator.FilterOptions().setHasText(uniqueName));
            oldLibraryItem.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(oldLibraryItem).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(5000L)); // Test item removed

            // Delete
            Locator toDelete = libraryList.filter(new Locator.FilterOptions().setHasText(updatedName));
            page.onDialog(dialog -> dialog.accept());
            toDelete.first().locator("[data-test='delete-library-btn']").click();

            // Wait for the operation to complete
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L));

            toDelete.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(5000L));
            Locator deletedCheck = libraryList.filter(new Locator.FilterOptions().setHasText(updatedName));
            deletedCheck.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(deletedCheck).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(5000L));
            assertThat(libraryList).hasCount(1, new LocatorAssertions.HasCountOptions().setTimeout(5000L));

        } catch (Exception e) {
            // Screenshot on failure for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-libraries-crud.png")));
            throw e;
        }
    }
}
