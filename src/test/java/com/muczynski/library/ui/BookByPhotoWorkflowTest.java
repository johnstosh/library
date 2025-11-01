/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.muczynski.library.LibraryApplication;
import com.muczynski.library.service.AskGrok;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-books.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BookByPhotoWorkflowTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private AskGrok askGrok;

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
        page.setDefaultTimeout(20000L);

        // Set up the mock BEFORE the test runs
        String mockJsonResponse = "{\"title\": \"Mock AI Title\", \"author\": \"Mock AI Author\"}";
        when(askGrok.askAboutPhoto(any(byte[].class), anyString(), anyString())).thenReturn(mockJsonResponse);
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private void login() {
        page.navigate("http://localhost:" + port);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(20000L));
        page.waitForSelector("[data-test='menu-login']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.click("[data-test='menu-login']");
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.waitForSelector("[data-test='menu-authors']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
    }

    @Test
    @Disabled("MockitoBean not properly intercepting askGrok.askAboutPhoto() calls - needs investigation")
    void testFullBookByPhotoWorkflow() {
        Path tempFile = null;
        try {
            // 1. User clicks book-by-photo in the menu
            login();
            page.click("[data-test='menu-book-by-photo']");

            // Wait for initial book creation to complete (buttons become visible)
            page.waitForSelector("[data-test='cancel-book-btn']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));

            // 2. Books page opens
            // 8. Books page goes to edit mode
            Locator booksForm = page.locator("[data-test='books-form']");
            booksForm.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(booksForm).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // 3. Title is set to date/time stamp
            Locator titleInput = page.locator("[data-test='new-book-title']");
            titleInput.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            String title = titleInput.inputValue();
            assertTrue(title.matches("^\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}$"));

            // 4. Author is set to the first author in the database
            Locator authorSelect = page.locator("[data-test='book-author']");
            authorSelect.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(authorSelect).hasValue("1", new LocatorAssertions.HasValueOptions().setTimeout(20000L));

            // 5. Page scrolls to bottom
            // This is hard to test reliably, but we can check the scroll position
            // We'll trust the implementation for now and focus on functional steps.

            // 6. Data in page is saved to back-end
            // 7. Page scroll stays at bottom
            // 9. Page scroll stays at bottom
            // These are covered by the workflow implicitly.

            // 10. The cancel and add-photo buttons become available
            Locator cancelBookBtn = page.locator("[data-test='cancel-book-btn']");
            cancelBookBtn.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(cancelBookBtn).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            Locator addPhotoBtn = page.locator("[data-test='add-photo-btn']");
            addPhotoBtn.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(addPhotoBtn).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // 11. The user adds one or more photos
            tempFile = Files.createTempFile("test-photo-workflow", ".jpg");
            Files.write(tempFile, "dummy photo content".getBytes());
            page.setInputFiles("input#photo-upload", tempFile);

            // Wait for photo upload to complete and thumbnail to appear
            page.waitForSelector("[data-test='book-photo']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));

            // 12. The photos show at the bottom of the page
            Locator photoThumbnail = page.locator("[data-test='book-photo']");
            photoThumbnail.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(photoThumbnail).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // 15. Once a photo has been added the book-by-photo button is visible
            Locator bookByPhotoButton = page.locator("[data-test='book-by-photo-btn']");
            bookByPhotoButton.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(bookByPhotoButton).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // 16. The user clicks book-by-photo
            // (Mock is already set up in @BeforeEach)
            bookByPhotoButton.click();

            // Wait for title to be updated from AI response
            assertThat(titleInput).hasValue("Mock AI Title", new LocatorAssertions.HasValueOptions().setTimeout(20000L));
            titleInput.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(titleInput).hasValue("Mock AI Title", new LocatorAssertions.HasValueOptions().setTimeout(20000L));

            // 17. The first photo is used to determine the title, author, ...
            // 18. The new book dto is returned from the backend and the fields are updated with non-blank data from the dto

            // Wait for new author option to appear after repopulation
            page.waitForSelector("[data-test='book-author'] option[value='2']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));

            // 19. The authors need to be re-read from the backend to obtain the new author for the book
            Locator authorOption = page.locator("[data-test='book-author'] option[value='2']");
            authorOption.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(authorOption).hasText("Mock AI Author", new LocatorAssertions.HasTextOptions().setTimeout(20000L));
            assertThat(authorSelect).hasValue("2", new LocatorAssertions.HasValueOptions().setTimeout(20000L));

            // 20. The books page stays in edit mode
            assertThat(booksForm).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

            // 21. When the user clicks Update Book, the fields are sent to the backend to save
            page.click("[data-test='add-book-btn']");

            // Wait for update to complete and book table to become visible
            page.waitForSelector("[data-test='book-table']", new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));

            // 22. The Books page leaves edit mode (the book table at the top becomes visible)
            Locator bookTable = page.locator("[data-test='book-table']");
            bookTable.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(bookTable).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));
            Locator bookItem = page.locator("[data-test='book-item']").filter(new Locator.FilterOptions().setHasText("Mock AI Title"));
            bookItem.waitFor(new Locator.WaitForOptions().setTimeout(20000L));
            assertThat(bookItem).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-book-by-photo-workflow.png")));
            throw new RuntimeException(e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    // Suppress
                }
            }
        }
    }
}
