import time
from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # Give server time to start
    time.sleep(30)

    page.goto("http://localhost:8080")

    # Login
    page.click("[data-test='menu-login']")
    page.fill("[data-test='login-username']", "librarian")
    page.fill("[data-test='login-password']", "password")
    page.click("[data-test='login-submit']")

    # Wait for page to reload and authentication to complete
    time.sleep(5)

    # Wait for the logout button to become visible. This is a reliable
    # signal that the login was successful and the UI has updated.
    logout_button = page.locator("[data-test='menu-logout']")
    expect(logout_button).to_be_visible(timeout=15000)

    # Now that we're logged in, the main content should be visible.
    main_content = page.locator("[data-test='main-content']")
    expect(main_content).to_be_visible()

    # Navigate to books section
    page.click("[data-test='menu-books']")

    # Wait for the table to appear in the books section
    book_table = page.locator("[data-test='book-table']")
    expect(book_table).to_be_visible()

    # Wait for at least one book item to be rendered to ensure async loading is complete.
    first_book_item = page.locator("[data-test='book-item']").first
    expect(first_book_item).to_be_visible()

    # Take screenshot
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)