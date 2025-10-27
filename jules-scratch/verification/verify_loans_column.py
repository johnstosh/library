from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")

    # Login to generate test data
    page.locator('[data-test="menu-login"]').click()
    page.locator('[data-test="login-username"]').fill("librarian")
    page.locator('[data-test="login-password"]').fill("divinemercy")
    page.locator('[data-test="login-submit"]').click()

    # Wait for main content to load
    expect(page.locator('[data-test="main-content"]')).to_be_visible()

    # Go to test data page and generate data
    page.locator('[data-test="menu-test-data"]').click()
    page.locator('[data-test="num-books"]').fill("1")
    page.locator('[data-test="generate-test-data-btn"]').click()
    page.locator('[data-test="generate-test-loans-btn"]').click()

    # Go to books page and create a withdrawn book
    page.locator('[data-test="menu-books"]').click()
    page.locator('[data-test="new-book-title"]').fill("Withdrawn Book")
    page.locator('[data-test="new-book-status"]').select_option("WITHDRAWN")
    page.locator('[data-test="add-book-btn"]').click()
    # Wait for the book to be added
    expect(page.locator('[data-test="book-title"]:has-text("Withdrawn Book")').first).to_be_visible()


    # Create a lost book
    page.locator('[data-test="new-book-title"]').fill("Lost Book")
    page.locator('[data-test="new-book-status"]').select_option("LOST")
    page.locator('[data-test="add-book-btn"]').click()
    # Wait for the book to be added
    expect(page.locator('[data-test="book-title"]:has-text("Lost Book")').first).to_be_visible()

    # Logout
    page.locator('[data-test="menu-logout"]').click()

    # Wait for the search page to appear after logout
    expect(page.locator('[data-test="search-section"]')).to_be_visible()

    # Go to books page as a public user
    page.locator('[data-test="menu-books"]').click()

    # Wait for the table to be visible
    book_table = page.locator('[data-test="book-table"]')
    expect(book_table).to_be_visible()

    page.wait_for_timeout(1000) # 1 second wait

    # Take a screenshot
    page.screenshot(path="loans-column.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)