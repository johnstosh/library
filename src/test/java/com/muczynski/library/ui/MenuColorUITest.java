package com.muczynski.library.ui;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.LoadState;
import com.muczynski.library.LibraryApplication;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(value = "classpath:data-menucolor.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MenuColorUITest {

    @LocalServerPort
    private int port;

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
    }

    @AfterEach
    void closeContext() {
        if (page != null) {
            page.context().close();
        }
    }

    private void login() {
        page.navigate("http://localhost:" + port);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.click("[data-test='menu-login']");
        page.waitForSelector("[data-test='login-form']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
        page.fill("[data-test='login-username']", "librarian");
        page.fill("[data-test='login-password']", "password");
        page.click("[data-test='login-submit']");
        page.waitForSelector("[data-test='main-content']", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
    }

    @Test
    void testMenuBarColor() {
        try {
            login();

            // Wait for the menu to be visible
            page.waitForSelector("#section-menu", new Page.WaitForSelectorOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));

            // Wait a bit for the page to fully load
            page.waitForTimeout(1000);

            // Scroll to the top of the page before capturing
            page.evaluate("window.scrollTo(0, 0);");

            // Take a screenshot of the page
            byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));

            // Load the screenshot into a BufferedImage
            BufferedImage image;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(screenshotBytes)) {
                image = ImageIO.read(bais);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read screenshot", e);
            }

            // Get the color at pixel (5, 5)
            int rgb = image.getRGB(5, 5);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            String colorString = String.format("rgb(%d, %d, %d)", red, green, blue);
            System.out.println("Pixel (5, 5) color: " + colorString);

            // Assert that the color is green based on CSS --bs-primary: #2E7D32 which is rgb(46, 125, 50)
            // Adjust tolerance if needed, but for exact match
            // If it's blue (e.g., high blue value like around 100+ for blue, low green), the test will fail
            assertTrue(green > 100 && blue < 100 && red < 100, 
                       "Pixel (5, 5) should be green (high green, low blue), but was: " + colorString);
        } catch (Exception e) {
            // Screenshot on failure for debugging
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("failure-menu-color.png")));
            throw e;
        }
    }
}
