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
import com.microsoft.playwright.options.WaitForSelectorState;
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

/**
 * Unauthenticated visitors can view a book or author, but cannot use list or edit pages.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-books.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PublicCatalogUITest {

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

    @Test
    @DisplayName("Guest can view a book but not edit it")
    void testGuestCanViewBook() {
        page.navigate(getBaseUrl() + "/books/1");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("[data-test='book-title']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        assertThat(page.locator("[data-test='book-title']")).containsText("Initial Book");
        assertThat(page.locator("[data-test='book-view-edit']")).not().isVisible();
        assertThat(page.locator("[data-test='book-view-clone']")).not().isVisible();
        assertThat(page.locator("[data-test='book-view-delete']")).not().isVisible();
        assertThat(page.locator("[data-test='upload-photo-button']")).not().isVisible();
        assertThat(page.locator("[data-test='nav-books']")).not().isVisible();
        assertThat(page.locator("[data-test='back-to-search']")).isVisible();
        assertThat(page.locator("[data-test='book-view-ydl-lookup']")).isVisible();
        assertThat(page.locator("[data-test='book-view-emu-lookup']")).isVisible();
        assertThat(page.locator("[data-test='ydl-last-checked']")).containsText("never");
        assertThat(page.locator("[data-test='emu-last-checked']")).containsText("never");
        assertThat(page.locator("[data-test='ydl-audio-status']")).containsText("Unknown");
        assertThat(page.locator("[data-test='emu-audio-status']")).containsText("Unknown");
    }

    @Test
    @DisplayName("Guest can view an author but not edit it")
    void testGuestCanViewAuthor() {
        page.navigate(getBaseUrl() + "/authors/1");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("[data-test='author-name']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        assertThat(page.locator("[data-test='author-name']")).containsText("Initial Author");
        assertThat(page.locator("[data-test='author-view-edit']")).not().isVisible();
        assertThat(page.locator("[data-test='author-view-delete']")).not().isVisible();
        assertThat(page.locator("[data-test='upload-photo-button']")).not().isVisible();
        assertThat(page.locator("[data-test='author-book-edit-1']")).not().isVisible();
        assertThat(page.locator("[data-test='author-book-view-1']")).isVisible();
        assertThat(page.locator("[data-test='nav-authors']")).not().isVisible();
        assertThat(page.locator("[data-test='back-to-search']")).isVisible();
    }

    @Test
    @DisplayName("Guest is sent to login instead of the books list")
    void testGuestCannotUseBooksList() {
        page.navigate(getBaseUrl() + "/books");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForURL("**/login", new Page.WaitForURLOptions().setTimeout(10000L));
        assertThat(page.locator("[data-test='login-username']")).isVisible();
    }
}
