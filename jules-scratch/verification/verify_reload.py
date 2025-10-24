from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    page.goto("http://localhost:8080")

    # Login
    page.locator('[data-test="menu-login"]').click()
    page.locator('[data-test="login-username"]').fill("librarian")
    page.locator('[data-test="login-password"]').fill("divinemercy")
    page.locator('[data-test="login-submit"]').click()

    # Go to authors page
    page.locator('[data-test="menu-authors"]').click()

    # Add an author
    page.locator('[data-test="new-author-name"]').fill("Test Author")
    page.locator('[data-test="add-author-btn"]').click()

    # Click edit on the first author
    # Need to wait for the page to load the authors
    expect(page.locator('[data-test="author-item"]').first).to_be_visible()
    page.locator('[data-test="edit-author-btn"]').first.click()

    # Expect the author list to be hidden
    expect(page.locator('[data-test="author-table"]')).to_be_hidden()

    # Click the authors menu item again
    page.locator('[data-test="menu-authors"]').click()

    # Expect the author list to be visible again
    expect(page.locator('[data-test="author-table"]')).to_be_visible()

    page.screenshot(path="jules-scratch/verification/verification.png")

    browser.close()

with sync_playwright() as playwright:
    run(playwright)