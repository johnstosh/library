/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.LoadState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI Test for thumbnail rendered visibility in the books table.
 * Unlike ThumbnailPersistenceUITest which checks element count,
 * this test checks actual bounding box dimensions and image naturalWidth
 * to detect thumbnails that are in the DOM but render invisibly
 * (e.g., revoked blob URLs, zero-dimension images, elements removed from layout).
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-thumbnail-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ThumbnailVisibilityUITest {

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    // 56x80 solid red PNG — sized to match the w-14 h-20 thumbnail display area
    private static final byte[] RED_PNG_BYTES;
    static {
        RED_PNG_BYTES = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAADgAAABQCAIAAADDQyF+AAAAWUlEQVR4nO3OAQkAMBAD" +
                "sfdvepNRDgIRkHt3CfuBqKioqKioqKioaMB+ICoqKioqKioqKhqwH4iKioqKioqKiooG" +
                "7AeioqKioqKioqKiAfuBqKioqKhoMvoBgnZvgBp1bJkAAAAASUVORK5CYII=");
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

        // Intercept all thumbnail requests and return a red PNG
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

    /**
     * Asserts that all thumbnail images are truly visible:
     * - Element is in layout (non-null bounding box with non-zero height)
     * - Image data is loaded (naturalWidth > 0 — catches revoked blob URLs)
     * Returns the number of thumbnails verified.
     */
    private int assertThumbnailsVisible(Locator thumbnails, String phase) {
        int count = thumbnails.count();
        assertTrue(count >= 5, phase + ": Expected at least 5 thumbnail-img elements, found " + count);

        for (int i = 0; i < count; i++) {
            Locator thumb = thumbnails.nth(i);

            // Check the element is in layout (bounding box exists with non-zero height)
            BoundingBox box = thumb.boundingBox();
            assertNotNull(box,
                    phase + ": Thumbnail " + i + " has null bounding box (not rendered)");
            assertTrue(box.height > 0,
                    phase + ": Thumbnail " + i + " has zero height (w=" + box.width + ", h=" + box.height + ")");

            // Check the image data is actually loaded (naturalWidth > 0).
            // A revoked blob URL or broken src will have naturalWidth = 0.
            int naturalWidth = (int) thumb.evaluate("el => el.naturalWidth");
            assertTrue(naturalWidth > 0,
                    phase + ": Thumbnail " + i + " has naturalWidth=" + naturalWidth +
                    " (image data not loaded — likely revoked blob URL or broken src)");
        }
        return count;
    }

    @Test
    @DisplayName("Thumbnails should remain visually rendered (non-zero bounding box) after background refetch")
    void testThumbnailsRemainVisibleAfterRefetch() {
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Click "All Books" filter to see all 5 test books
        page.click("[data-test='filter-all']");

        // Wait for books to load
        page.locator("text=Thumbnail Book One").waitFor(
                new Locator.WaitForOptions().setTimeout(20000));

        // Poll until 5 thumbnail-img elements appear in the DOM
        Locator thumbnails = page.locator("[data-test='thumbnail-img']");
        for (int attempt = 0; attempt < 40; attempt++) {
            page.waitForTimeout(500);
            if (thumbnails.count() >= 5) {
                break;
            }
        }

        // Phase 1: Assert all thumbnails are rendered and have loaded image data
        int initialCount = assertThumbnailsVisible(thumbnails, "Before refetch");

        // Trigger background refetch via online event.
        // TanStack Query's refetchOnReconnect fires for all stale queries
        // (summaries has staleTime: 0, so it's always stale).
        // This is the most common trigger for the thumbnail disappearance bug.
        page.evaluate("window.dispatchEvent(new Event('online'))");

        // Wait for the refetch cascade to settle
        page.waitForTimeout(8000);

        // Phase 2: Assert all thumbnails still have loaded image data.
        // This is where the test should fail if the disappearing-thumbnail bug is present —
        // blob URLs get revoked during refetch, causing naturalWidth to drop to 0.
        int finalCount = assertThumbnailsVisible(thumbnails, "After refetch");

        assertTrue(finalCount >= initialCount,
                "Thumbnail count decreased from " + initialCount + " to " + finalCount +
                " after refetch, suggesting thumbnails were lost.");
    }
}
