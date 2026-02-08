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
import org.springframework.test.context.jdbc.Sql;

import java.util.Base64;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * UI Test for thumbnail persistence in the books table.
 * Verifies that books with photos show thumbnail images and that
 * book data remains stable after loading (no flickering or disappearing).
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-thumbnail-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ThumbnailPersistenceUITest {

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private static final byte[] RED_PNG_BYTES;
    static {
        RED_PNG_BYTES = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAOCAYAAAAWo42rAAAAEElEQVQYV2P8z8Dwn4EI" +
                "AAAz9AH9UhZjvAAAAABJRU5ErkJggg==");
    }

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
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 1600));
        page = context.newPage();
        page.setDefaultTimeout(30000L);

        // Intercept all thumbnail requests and return a small red PNG
        page.route("**/api/photos/*/thumbnail**", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("image/png")
                    .setBodyBytes(RED_PNG_BYTES));
        });

        loginAsLibrarian();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
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
    @DisplayName("Books with photos should display thumbnail images that persist")
    void testThumbnailsPersistAfterLoading() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click "All Books" filter to see all 5 test books
        page.click("[data-test='filter-all']");

        // Wait for books to load - verify book titles appear
        assertThat(page.locator("text=Thumbnail Book One")).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(20000));

        // Verify all 5 books are displayed
        assertThat(page.locator("text=Thumbnail Book Two")).isVisible();
        assertThat(page.locator("text=Thumbnail Book Three")).isVisible();
        assertThat(page.locator("text=Thumbnail Book Four")).isVisible();
        assertThat(page.locator("text=Thumbnail Book Five")).isVisible();

        // Wait for thumbnail images to load through the throttled queue
        Locator thumbnails = page.locator("[data-test='thumbnail-img']");
        int thumbnailCount = 0;
        for (int attempt = 0; attempt < 40; attempt++) {
            page.waitForTimeout(500);
            thumbnailCount = thumbnails.count();
            if (thumbnailCount >= 5) {
                break;
            }
        }
        Assertions.assertTrue(thumbnailCount >= 3,
                "Expected at least 3 thumbnail images, but found " + thumbnailCount);

        // Verify showing correct book count
        assertThat(page.locator("text=Showing 5 books")).isVisible();

        // Wait for any background refetches to settle
        page.waitForTimeout(3000);

        // After waiting, verify books and thumbnails are still present (not replaced by spinner)
        assertThat(page.locator("text=Thumbnail Book One")).isVisible();
        assertThat(page.locator("text=Thumbnail Book Five")).isVisible();

        int finalThumbnailCount = thumbnails.count();
        Assertions.assertTrue(finalThumbnailCount >= thumbnailCount,
                "Thumbnail count decreased from " + thumbnailCount + " to " + finalThumbnailCount +
                " after waiting, suggesting the table was re-rendered and thumbnails were lost.");
    }
}
