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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for thumbnail visibility on the Books page.
 * Verifies that thumbnails remain visible after loading on the
 * default "Most Recent Day" view without any user interaction.
 * Guards against the bug where thumbnails would disappear after
 * a few seconds due to blob URL lifecycle or refetch cascade issues.
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

    // 56x80 solid red PNG
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

    @Test
    @DisplayName("Thumbnails should still be visible after waiting 10 seconds on the default Most Recent Day view")
    void testThumbnailsRemainVisibleAfterWaiting() {
        // Capture all console messages and network requests for diagnostics
        List<String> consoleLogs = new ArrayList<>();
        List<String> networkRequests = new ArrayList<>();
        page.onConsoleMessage(msg -> consoleLogs.add("[" + msg.type() + "] " + msg.text()));
        page.onRequest(req -> {
            if (req.url().contains("/api/")) {
                networkRequests.add(req.method() + " " + req.url());
            }
        });

        // Default filter is "Most Recent Day" — all 5 books were added today.
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for the book titles to appear
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
        int initialCount = thumbnails.count();
        assertTrue(initialCount >= 5,
                "Expected 5 thumbnail images to initially appear, found " + initialCount);

        // Snapshot network activity so far
        int networkCountBefore = networkRequests.size();

        // Now just wait 10 seconds — don't click anything.
        page.waitForTimeout(10000);

        // Diagnostics: check what happened during the wait
        int networkCountAfter = networkRequests.size();
        System.out.println("=== DIAGNOSTIC: Network requests during 10s wait ===");
        for (int i = networkCountBefore; i < networkCountAfter; i++) {
            System.out.println("  " + networkRequests.get(i));
        }
        System.out.println("=== DIAGNOSTIC: Console messages ===");
        for (String log : consoleLogs) {
            System.out.println("  " + log);
        }

        // Check thumbnail count and naturalWidth after waiting
        int finalCount = thumbnails.count();
        System.out.println("=== DIAGNOSTIC: Thumbnail count: before=" + initialCount + " after=" + finalCount + " ===");

        if (finalCount > 0) {
            for (int i = 0; i < finalCount; i++) {
                Locator thumb = thumbnails.nth(i);
                int naturalWidth = (int) thumb.evaluate("el => el.naturalWidth");
                String src = (String) thumb.evaluate("el => el.src");
                System.out.println("  Thumbnail " + i + ": naturalWidth=" + naturalWidth + " src=" + src.substring(0, Math.min(80, src.length())));
            }
        }

        // Check if loading spinner appeared (would indicate data refetch caused remount)
        boolean spinnerVisible = page.locator(".animate-spin").isVisible();
        System.out.println("=== DIAGNOSTIC: Spinner visible=" + spinnerVisible + " ===");

        // The actual assertion: thumbnails should still be showing
        assertTrue(finalCount >= 5,
                "After waiting 10 seconds, thumbnail count dropped from " + initialCount + " to " + finalCount +
                " — thumbnails disappeared.");

        for (int i = 0; i < finalCount; i++) {
            Locator thumb = thumbnails.nth(i);
            int naturalWidth = (int) thumb.evaluate("el => el.naturalWidth");
            assertTrue(naturalWidth > 0,
                    "Thumbnail " + i + " has naturalWidth=" + naturalWidth +
                    " after waiting — image data is gone.");
        }
    }
}
