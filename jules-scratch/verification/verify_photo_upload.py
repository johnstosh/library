from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    page.goto("http://localhost:8080")
    page.wait_for_load_state("networkidle")

    # Login
    page.get_by_test_id("menu-login").click()
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("password")
    page.get_by_test_id("login-submit").click()

    # Go to libraries section and create a library
    page.get_by_test_id("menu-libraries").click()
    page.get_by_test_id("new-library-name").fill("Test Library")
    page.get_by_test_id("add-library-btn").click()

    # Go to authors section and create an author
    page.get_by_test_id("menu-authors").click()
    page.get_by_test_id("new-author-name").fill("Test Author")
    page.get_by_test_id("add-author-btn").click()

    # Go to books section
    page.get_by_test_id("menu-books").click()

    # Create a new book
    page.get_by_test_id("new-book-title").fill("Test Book for Photo")
    page.get_by_test_id("book-author").select_option(label="Test Author")
    page.get_by_test_id("book-library").select_option(label="Test Library")
    page.get_by_test_id("add-book-btn").click()

    # Edit the new book
    page.locator('[data-test="book-item"]:has-text("Test Book for Photo") [data-test="edit-book-btn"]').click()

    # Add a photo
    page.set_input_files('input[type="file"]', 'README.md')

    page.wait_for_timeout(1000)

    # Take screenshot
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()