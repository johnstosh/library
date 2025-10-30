/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApplyForCardUITest {

    @LocalServerPort
    private int port;
    private String appUrl;

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
        page.setDefaultTimeout(5000L);
        appUrl = "http://localhost:" + port;
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    @Test
    public void testApplyForLibraryCard() {
        page.navigate(appUrl);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        page.click("text=Library Card");

        page.fill("#new-applicant-name", "John");
        page.fill("#new-applicant-password", "John");
        page.click("button:has-text('Apply')");

        Locator errorLocator = page.locator("[data-test='apply-error']");
        assertThat(errorLocator).isVisible();
        assertThat(errorLocator).hasText("Password is not complex enough.");

        page.fill("#new-applicant-password", "divinemercy");
        page.click("button:has-text('Apply')");

        Locator successLocator = page.locator("[data-test='apply-success']");
        assertThat(successLocator).isVisible();
        assertThat(successLocator).hasText("Library card application successful.");
    }
}