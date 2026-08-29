/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
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

/**
 * UI tests for librarian review of library card applications.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-applications.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApplicationsUITest {

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

    private void loginAsLibrarian() {
        page.navigate(getBaseUrl() + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("[data-test='login-username']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(10000L));
    }

    @Test
    @DisplayName("Should show the existing-user error when approving an application")
    void testApproveDuplicateUsernameShowsError() {
        loginAsLibrarian();

        page.click("[data-test='nav-applications']");
        page.waitForURL("**/applications", new Page.WaitForURLOptions().setTimeout(10000L));
        page.waitForLoadState(LoadState.NETWORKIDLE);

        page.waitForSelector("[data-test='approve-application-1']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));
        assertThat(page.locator("text=Existing Applicant").first()).isVisible();

        page.click("[data-test='approve-application-1']");
        page.waitForSelector("[data-test='confirm-dialog-confirm']", new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE));
        page.click("[data-test='confirm-dialog-confirm']");

        Locator dialogError = page.locator("[data-test='confirm-dialog-error']");
        assertThat(dialogError).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10000L));
        assertThat(dialogError).containsText("already exists");
        assertThat(dialogError).containsText("Existing Applicant");

        Locator pageError = page.locator("[data-test='application-action-error']");
        assertThat(pageError).isVisible();
        assertThat(pageError).containsText("already exists");

        assertThat(page.locator("[data-test='approve-application-1']")).isVisible();
    }
}
