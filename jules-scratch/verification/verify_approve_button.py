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

    # Go to applications page
    page.locator('[data-test="menu-applied"]').click()

    # Wait for the table to load
    expect(page.locator('[data-test="applied-table"]')).to_be_visible()

    # Take a screenshot
    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)