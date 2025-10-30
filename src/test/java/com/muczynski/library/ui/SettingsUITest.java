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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SettingsUITest {

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
        List<String> allSections = Arrays.asList("authors", "books", "libraries", "loans", "users", "search", "settings");
        List<String> hiddenSections = allSections.stream()
                .filter(s -> !s.equals(section) && !s.equals("search"))
                .collect(Collectors.toList());
        if (!hiddenSections.isEmpty()) {
            for (String hiddenSection : hiddenSections) {
                Locator hiddenLocator = page.locator("#" + hiddenSection + "-section");
                if (hiddenLocator.count() > 0) {
                    assertThat(hiddenLocator).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(5000L));
                }
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
    void testSettingsLoadAndSave() {
        try {
            login();
            ensurePrerequisites();

            // Navigate to settings section and assert visibility
            navigateToSection("settings");

            // Wait for settings section to be interactable, focusing on input
            Locator apiKeyInput = page.locator("[data-test='xai-api-key']");
            apiKeyInput.waitFor(new Locator.WaitForOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));

            // Load: Assert initial values
            Locator nameInput = page.locator("#settings-section [data-test='user-name']");
            nameInput.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(nameInput).hasValue("librarian", new LocatorAssertions.HasValueOptions().setTimeout(5000L));
            apiKeyInput.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(apiKeyInput).hasValue("", new LocatorAssertions.HasValueOptions().setTimeout(5000L));

            // Save: Enter a test key, then save (do not change username)
            String testApiKey = "sk-test-key-" + System.currentTimeMillis() + "1234567"; // Ensure >=32 chars
            apiKeyInput.fill(testApiKey);
            page.click("[data-test='save-settings-btn']");

            // Wait for success message to confirm save completed
            Locator successLocator = page.locator("[data-test='settings-success']");
            successLocator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));
            assertThat(successLocator).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));

            // Reload the page to verify persistence
            page.reload();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(5000L));
            // Wait for main content since already authenticated via session
            page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(5000L).setState(WaitForSelectorState.VISIBLE));
            navigateToSection("settings");

            // Wait for loadSettings to complete and set the value
            apiKeyInput = page.locator("[data-test='xai-api-key']");
            apiKeyInput.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            nameInput = page.locator("#settings-section [data-test='user-name']");
            nameInput.waitFor(new Locator.WaitForOptions().setTimeout(5000L));

            // Verify: Check that the inputs now have the saved values
            assertThat(nameInput).hasValue("librarian", new LocatorAssertions.HasValueOptions().setTimeout(5000L));
            assertThat(apiKeyInput).hasValue(testApiKey, new LocatorAssertions.HasValueOptions().setTimeout(5000L));

        } catch (Exception e) {
            // Screenshot on failure for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-settings-test.png")));
            throw e;
        }
    }
}
