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
            assertThat(table).isVisible();

            // Wait for library section to be interactable, focusing on form
            page.waitForSelector("[data-test='new-library-name']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));

            // Create with unique name to avoid conflict
            String uniqueName = "Test Library " + UUID.randomUUID().toString().substring(0, 8);
            String uniqueHostname = "test-" + UUID.randomUUID().toString().substring(0, 8) + ".local";
            page.fill("[data-test='new-library-name']", uniqueName);
            page.fill("[data-test='new-library-hostname']", uniqueHostname);
            page.click("[data-test='add-library-btn']");

            // Read: Use filter for flexible matching and assert name in specific cell
            Locator libraryList = page.locator("[data-test='library-item']");
            Locator libraryItem = libraryList.filter(new Locator.FilterOptions().setHasText(uniqueName));
            libraryItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(libraryItem.first()).isVisible();
            assertThat(libraryItem).hasCount(1);
            // Assert the name is in the library-name span
            assertThat(libraryItem.first().locator("[data-test='library-name']")).hasText(uniqueName + " (" + uniqueHostname + ")");

            // Update
            libraryItem.first().locator("[data-test='edit-library-btn']").click();
            String updatedName = "Updated Library " + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-library-name']", updatedName);
            page.click("[data-test='add-library-btn']");

            // Wait for button to reset to "Add Library", confirming the update operation completed successfully
            Locator addButton = page.locator("[data-test='add-library-btn']");
            assertThat(addButton).hasText("Add Library", new LocatorAssertions.HasTextOptions().setTimeout(5000));

            // Wait for the updated item to appear (confirms reload)
            Locator updatedLibraryItem = libraryList.filter(new Locator.FilterOptions().setHasText(updatedName));
            updatedLibraryItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(updatedLibraryItem.first()).isVisible();
            // Assert the updated name is in the library-name span
            assertThat(updatedLibraryItem.first().locator("[data-test='library-name']")).hasText(updatedName + " (" + uniqueHostname + ")");

            // Assert old item is gone (confirms successful reload)
            assertThat(libraryList.filter(new Locator.FilterOptions().setHasText(uniqueName))).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(5000));

            // Delete
            Locator toDelete = libraryList.filter(new Locator.FilterOptions().setHasText(updatedName));
            page.onDialog(dialog -> dialog.accept());
            toDelete.first().locator("[data-test='delete-library-btn']").click();
            toDelete.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(5000));
            assertThat(libraryList.filter(new Locator.FilterOptions().setHasText(updatedName))).hasCount(0);

        } catch (Exception e) {
            // Screenshot on failure for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-libraries-crud.png")));
            throw e;
        }
    }
}
