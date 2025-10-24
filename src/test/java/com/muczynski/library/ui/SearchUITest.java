package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

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
        page.waitForSelector("[data-test='menu-login']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
        page.click("[data-test='menu-login']");
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
    }

    @Test
    void testSearchFunctionality() {
        login();
        page.click("[data-test='menu-search']");
        page.waitForSelector("#search-section", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        page.fill("[data-test='search-input']", "Test");
        page.waitForSelector("[data-test='search-btn']", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        page.click("[data-test='search-btn']");

        // Wait for the search results to be visible
        page.waitForSelector("[data-test='search-results']", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // Assert that the search results are not empty
        assertThat(page.locator("[data-test='search-results']")).not().isEmpty();
    }

    @Test
    void testSearchExecutesOnEnterKey() {
        login();
        page.click("[data-test='menu-search']");
        page.waitForSelector("#search-section", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        page.fill("[data-test='search-input']", "Test");
        page.press("[data-test='search-input']", "Enter");

        // Wait for the search results to be visible
        page.waitForSelector("[data-test='search-results']", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // Assert that the search results are not empty
        assertThat(page.locator("[data-test='search-results']")).not().isEmpty();
    }

    @Test
    @Disabled
    void testSearchForShortQuery() {
        login();
        page.click("[data-test='menu-search']");
        page.waitForSelector("#search-section", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        page.fill("[data-test='search-input']", "in");
        page.press("[data-test='search-input']", "Enter");

        // Wait for the search results to be visible
        page.waitForSelector("[data-test='search-results']", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // Assert that the search results are not empty
        assertThat(page.locator("[data-test='search-results']")).not().isEmpty();

        // Wait for the search results to be visible
        page.waitForSelector("[data-test='search-results']", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        // Assert that the search results contain the expected book and author
        assertThat(page.locator("[data-test='search-book-item'] td:nth-child(2)")).hasText("Initial Book");
        assertThat(page.locator("[data-test='search-author-item'] span")).hasText("1. Initial Author");
    }
}
