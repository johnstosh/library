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

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * UI test for the "Book from Title and Author" workflow:
 * - User logs in as librarian
 * - User navigates to book edit page
 * - User clicks "Book from Title & Author" to invoke AI lookup
 * - AskGrok is mocked to return a predictable response
 * - Test verifies the form updates with AI-returned title
 * - User saves and the book view page reflects the new title
 */
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

    private Playwright playwright;
    private Browser browser;
    private Page page;

    // Proper nested JSON format expected by BookService.getBookFromTitleAuthor()
    private static final String MOCK_AI_RESPONSE =
        "{\"author\": {\"name\": \"Mock AI Author\", \"dateOfBirth\": null, \"dateOfDeath\": null, " +
        "\"religiousAffiliation\": \"Unknown\", \"birthCountry\": \"USA\", \"nationality\": \"American\", " +
        "\"briefBiography\": \"A test author for automated testing.\"}, " +
        "\"book\": {\"title\": \"Mock AI Book Title\", \"publicationYear\": 2024, \"publisher\": \"Test Press\", " +
        "\"locNumber\": null, \"plotSummary\": \"A test summary.\", \"relatedWorks\": \"None\", " +
        "\"detailedDescription\": \"A detailed test description.\"}}";

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
    void createContextAndPageAndSetupMocks() {
        // Set up mocks before each test so they are active for the current Spring context
        when(askGrok.askQuestion(anyString())).thenReturn(MOCK_AI_RESPONSE);

        BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        page = context.newPage();
        page.setDefaultTimeout(20000L);
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private void loginAsLibrarian() {
        page.navigate("http://localhost:" + port + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));
        page.waitForSelector("[data-test='login-username']",
            new Page.WaitForSelectorOptions().setTimeout(20000L).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForURL("**/books", new Page.WaitForURLOptions().setTimeout(20000L));
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));
    }

    @Test
    @DisplayName("Book from Title & Author: AI fills in book metadata from mocked Grok response")
    void testBookFromTitleAndAuthorWorkflow() {
        try {
            // Step 1: Login as librarian
            loginAsLibrarian();

            // Step 2: Navigate to book 1 edit page
            page.navigate("http://localhost:" + port + "/books/1/edit");
            page.waitForURL("**/books/1/edit", new Page.WaitForURLOptions().setTimeout(20000L));
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Step 3: Wait for the edit form to load with the existing title
            Locator titleInput = page.locator("[data-test='book-title']");
            titleInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(20000L));
            assertThat(titleInput).hasValue("Initial Book", new LocatorAssertions.HasValueOptions().setTimeout(20000L));

            // Step 4: Click "Book from Title & Author" to invoke AI lookup
            Locator bookFromTitleAuthorBtn = page.locator("[data-test='book-operation-book-from-title-author']");
            bookFromTitleAuthorBtn.scrollIntoViewIfNeeded();
            bookFromTitleAuthorBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(20000L));
            bookFromTitleAuthorBtn.click();

            // Step 5: Wait for network idle to ensure the AI response has been processed
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Step 6: Verify the title was updated from the mock AI response
            assertThat(titleInput).hasValue("Mock AI Book Title", new LocatorAssertions.HasValueOptions().setTimeout(20000L));

            // Step 7: Verify the author combobox input shows the new AI-generated author name
            Locator authorInput = page.locator("[data-test='book-author-input']");
            authorInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(20000L));
            assertThat(authorInput).hasValue("Mock AI Author", new LocatorAssertions.HasValueOptions().setTimeout(20000L));

            // Step 8: Click Save to persist the AI-generated data
            Locator saveBtn = page.locator("[data-test='book-form-submit']");
            saveBtn.scrollIntoViewIfNeeded();
            saveBtn.click();

            // Step 9: Wait for navigation back to the book view page after save
            page.waitForURL("**/books/1", new Page.WaitForURLOptions().setTimeout(20000L));
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(20000L));

            // Step 10: Verify the book view page shows the updated title from AI
            Locator bookTitle = page.locator("[data-test='book-title']");
            bookTitle.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(20000L));
            assertThat(bookTitle).containsText("Mock AI Book Title", new LocatorAssertions.ContainsTextOptions().setTimeout(20000L));

        } catch (Exception e) {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-book-by-photo-workflow.png")));
            throw new RuntimeException(e);
        }
    }
}
