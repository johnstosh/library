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
@Sql(value = "classpath:data-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UsersUITest {

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
        // Data is inserted via data-users.sql in test profile, so no additional setup needed
    }

    @Test
    void testUsersCRUD() {
        try {
            page.navigate("http://localhost:" + port);
            login();
            ensurePrerequisites();

            // Navigate to users section and assert visibility
            navigateToSection("users");

            // Wait for user section to be interactable, focusing on form
            page.waitForSelector("[data-test='new-user-username']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));

            // Create with unique username to avoid conflict
            String uniqueUsername = "testuser" + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-user-username']", uniqueUsername);
            page.fill("[data-test='new-user-password']", "password123");
            page.selectOption("[data-test='new-user-role']", "USER");
            page.click("[data-test='add-user-btn']");

            // Read: Use filter for flexible matching
            Locator userList = page.locator("[data-test='user-item']");
            Locator userItem = userList.filter(new Locator.FilterOptions().setHasText(uniqueUsername));
            userItem.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            assertThat(userItem.first()).isVisible();
            assertThat(userItem).hasCount(1);

            // Update
            userItem.first().locator("[data-test='edit-user-btn']").click();
            String updatedUsername = "updateduser" + UUID.randomUUID().toString().substring(0, 8);
            page.fill("[data-test='new-user-username']", updatedUsername);
            page.fill("[data-test='new-user-password']", "newpassword123");
            page.selectOption("[data-test='new-user-role']", "USER");
            page.click("[data-test='add-user-btn']");

            // Wait for button to reset to "Add User", confirming the update operation completed successfully
            Locator addButton = page.locator("[data-test='add-user-btn']");
            assertThat(addButton).hasText("Add User", new LocatorAssertions.HasTextOptions().setTimeout(5000));

            // Wait for the updated item to appear (confirms reload)
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            Locator updatedUserItem = userList.filter(new Locator.FilterOptions().setHasText(updatedUsername));
            updatedUserItem.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
            assertThat(updatedUserItem.first()).isVisible();

            // Assert old item is gone (confirms successful reload without detach wait)
            assertThat(userList.filter(new Locator.FilterOptions().setHasText(uniqueUsername))).hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(5000));

            // Delete
            Locator toDelete = userList.filter(new Locator.FilterOptions().setHasText(updatedUsername));
            int initialCount = userList.count();
            page.onDialog(dialog -> dialog.accept());
            toDelete.first().locator("[data-test='delete-user-btn']").click();

            // Wait for the user count to decrease
            page.waitForFunction("() => document.querySelectorAll(\"[data-test='user-item']\").length < " + initialCount);

            assertThat(userList.filter(new Locator.FilterOptions().setHasText(updatedUsername))).hasCount(0);

        } catch (Exception e) {
            // Screenshot on failure for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-users-crud.png")));
            throw e;
        }
    }
}
