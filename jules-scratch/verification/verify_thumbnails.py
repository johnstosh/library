from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    # Log in as librarian
    page.goto("http://localhost:8080")
    page.locator('[data-test="menu-login"]').click()
    page.locator('[data-test="login-username"]').fill("librarian")
    page.locator('[data-test="login-password"]').fill("divinemercy")
    page.locator('[data-test="login-submit"]').click()

    # Navigate to books and authors pages and take screenshots
    page.locator('[data-test="menu-books"]').click()
    expect(page.locator('[data-test="book-table"]')).to_be_visible()
    page.screenshot(path="jules-scratch/verification/books-page.png")

    page.locator('button:has-text("Authors")').click()
    expect(page.locator('[data-test="author-table"]')).to_be_visible()
    page.screenshot(path="jules-scratch/verification/authors-page.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)