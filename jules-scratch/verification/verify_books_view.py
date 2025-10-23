from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # Log in as librarian
    page.goto("http://localhost:8080")
    page.wait_for_timeout(30000) # Wait for 30 seconds
    page.get_by_test_id("menu-login").click()
    page.get_by_test_id("login-username").fill("librarian")
    page.get_by_test_id("login-password").fill("divinemercy")
    page.get_by_test_id("login-submit").click()

    # Go to books page
    page.get_by_test_id("menu-books").click()

    # Expect the book list to be visible and the form to be hidden
    expect(page.get_by_test_id("book-table")).to_be_visible()
    expect(page.get_by_test_id("books-form")).to_be_hidden()

    # Click edit on the first book
    page.get_by_test_id("edit-book-btn").first.click()

    # Expect the book list to be hidden and the form to be visible
    expect(page.get_by_test_id("book-table")).to_be_hidden()
    expect(page.get_by_test_id("books-form")).to_be_visible()

    # Take a screenshot
    page.screenshot(path="jules-scratch/verification/edit-book-view.png")

    # Click the cancel button
    page.get_by_test_id("cancel-book-btn").click()

    # Expect the book list to be visible and the form to be hidden again
    expect(page.get_by_test_id("book-table")).to_be_visible()
    expect(page.get_by_test_id("books-form")).to_be_hidden()

    # Take a screenshot
    page.screenshot(path="jules-scratch/verification/book-list-view.png")

    context.close()
    browser.close()

with sync_playwright() as playwright:
    run(playwright)