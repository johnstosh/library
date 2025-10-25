package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-search.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchUITest {

    @LocalServerPort
    private int port;

    private Browser browser;
    private Page page;

    @BeforeAll
    void launchBrowser() {
        Playwright playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
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

    @Test
    void testSearchFunctionality() {
        try {
            login();
            page.click("[data-test='menu-search']");
            page.waitForSelector("#search-section", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));

            page.fill("[data-test='search-input']", "Initial");
            page.waitForSelector("[data-test='search-btn']", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));
            page.click("[data-test='search-btn']");

            // Wait for network idle and results header
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000L));
            page.waitForSelector("[data-test='search-results'] h3", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));

            // Assert that the search results are not empty
            Locator resultsLocator = page.locator("[data-test='search-results']");
            resultsLocator.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(resultsLocator).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));
            assertThat(resultsLocator.locator("p:has-text('No results found')")).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(5000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-search-functionality.png")));
            throw e;
        }
    }

    @Test
    void testSearchExecutesOnEnterKey() {
        try {
            login();
            page.click("[data-test='menu-search']");
            page.waitForSelector("#search-section", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));

            page.fill("[data-test='search-input']", "Initial");
            page.press("[data-test='search-input']", "Enter");

            // Wait for network idle and results header
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000L));
            page.waitForSelector("[data-test='search-results'] h3", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));

            // Assert that the search results are not empty
            Locator resultsLocator = page.locator("[data-test='search-results']");
            resultsLocator.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(resultsLocator).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));
            assertThat(resultsLocator.locator("p:has-text('No results found')")).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(5000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-search-enter-key.png")));
            throw e;
        }
    }

    @Test
    void testSearchForShortQuery() {
        try {
            login();
            page.click("[data-test='menu-search']");
            page.waitForSelector("#search-section", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));

            page.fill("[data-test='search-input']", "in");
            page.press("[data-test='search-input']", "Enter");

            // Wait for network idle and results header
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000L));
            page.waitForSelector("[data-test='search-results'] h3", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000L));

            // Assert that the search results are not empty
            Locator resultsLocator = page.locator("[data-test='search-results']");
            resultsLocator.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(resultsLocator).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5000L));
            assertThat(resultsLocator.locator("p:has-text('No results found')")).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(5000L));

            // Assert that the search results contain the expected book and author
            Locator bookItem = page.locator("[data-test='search-book-item'] td:nth-child(2)");
            bookItem.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(bookItem).hasText("Initial Book", new LocatorAssertions.HasTextOptions().setTimeout(5000L));

            Locator authorItem = page.locator("[data-test='search-author-item'] span");
            authorItem.waitFor(new Locator.WaitForOptions().setTimeout(5000L));
            assertThat(authorItem).hasText("1. Initial Author", new LocatorAssertions.HasTextOptions().setTimeout(5000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-search-short-query.png")));
            throw e;
        }
    }
}
