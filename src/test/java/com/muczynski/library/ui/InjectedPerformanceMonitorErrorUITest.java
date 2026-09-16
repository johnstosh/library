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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Confirms the SPA swallows Chromium's injected reportAllChanges TypeError
 * (GitHub issue 322) without hiding real application errors.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-login.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InjectedPerformanceMonitorErrorUITest {

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

    @Test
    @DisplayName("Should swallow Chromium reportAllChanges startTime TypeError and not swallow app errors")
    void testInjectedPerformanceMonitorErrorIsPrevented() {
        page.navigate("http://localhost:" + port + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector("[data-test='login-username']", new Page.WaitForSelectorOptions()
                .setTimeout(20000L)
                .setState(WaitForSelectorState.VISIBLE));

        Boolean injectedPrevented = (Boolean) page.evaluate("""
                () => {
                  const message = "Cannot read properties of undefined (reading 'startTime')";
                  const error = new Error(message);
                  error.stack = [
                    'TypeError: ' + message,
                    '    at et.reportAllChanges (<anonymous>:2:19429)',
                    '    at <anonymous>:2:13070',
                  ].join('\\n');
                  const event = new ErrorEvent('error', {
                    message: 'Uncaught TypeError: ' + message,
                    filename: '',
                    lineno: 2,
                    colno: 19429,
                    error,
                    cancelable: true,
                  });
                  window.dispatchEvent(event);
                  return event.defaultPrevented;
                }
                """);
        assertEquals(Boolean.TRUE, injectedPrevented, "Chromium monitor TypeError should be prevented");

        Boolean appPrevented = (Boolean) page.evaluate("""
                () => {
                  const message = "Cannot read properties of undefined (reading 'title')";
                  const error = new Error(message);
                  error.stack = message + '\\n    at BookViewPage (http://localhost/assets/BookViewPage.js:40:12)';
                  const event = new ErrorEvent('error', {
                    message,
                    filename: 'http://localhost/assets/BookViewPage.js',
                    lineno: 40,
                    colno: 12,
                    error,
                    cancelable: true,
                  });
                  window.dispatchEvent(event);
                  return event.defaultPrevented;
                }
                """);
        assertEquals(Boolean.FALSE, appPrevented, "Application TypeErrors must still surface");
    }
}
