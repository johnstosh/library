/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-books.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FavoritesUITest {

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 720));
        page = context.newPage();
        page.setDefaultTimeout(20000L);
        loginAsLibrarian();
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private void loginAsLibrarian() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("[data-test='login-username']",
                new Page.WaitForSelectorOptions().setTimeout(30000L));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));
    }

    @Test
    @DisplayName("Librarian can favorite a book immediately from the star modal")
    void testFavoriteStarOpensModalAndSaves() {
        page.waitForSelector("[data-test='favorite-star-book-1']",
                new Page.WaitForSelectorOptions().setTimeout(10000L));
        assertThat(page.locator("[data-test='favorite-star-book-1-outline']")).isVisible();

        page.click("[data-test='favorite-star-book-1']");
        page.waitForSelector("[data-test='favorite-modal']",
                new Page.WaitForSelectorOptions().setTimeout(10000L));
        assertThat(page.locator("[data-test='favorite-list-have-read']")).isVisible();
        assertThat(page.locator("[data-test='favorite-list-want-to-read']")).isVisible();
        assertThat(page.locator("[data-test='favorite-list-want-to-recommend']")).isVisible();
        assertThat(page.locator("[data-test='favorite-list-needs-review']")).isVisible();
        assertThat(page.locator("[data-test='favorite-list-need-to-locate']")).isVisible();
        assertThat(page.locator("[data-test='favorite-add-list']")).isVisible();

        page.click("[data-test='favorite-list-have-read']");
        page.waitForSelector("[data-test='favorite-star-book-1-filled']",
                new Page.WaitForSelectorOptions().setTimeout(10000L));

        page.fill("[data-test='favorite-add-list-name']", "Nightstand");
        page.click("[data-test='favorite-add-list']");
        assertThat(page.locator("[data-test='favorite-list-nightstand']")).isChecked();

        page.click("[data-test='modal-close']");
        assertThat(page.locator("[data-test='favorite-modal']")).not().isVisible();
        assertThat(page.locator("[data-test='favorite-star-book-1-filled']")).isVisible();

        assertThat(page.locator("[data-test='favorite-filter-have-read']")).isVisible();
        assertThat(page.locator("[data-test='favorite-filter-have-read']")).containsText("1 Have Read");
        page.click("[data-test='favorite-filter-have-read']");
        page.waitForURL(url -> url.contains("favoriteLists="),
                new Page.WaitForURLOptions().setTimeout(10000L));
    }
}
