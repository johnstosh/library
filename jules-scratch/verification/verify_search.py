from playwright.sync_api import sync_playwright

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    try:
        page.goto("http://localhost:8080")

        # Login
        page.locator("[data-test='menu-login']").click()
        page.locator("[data-test='login-username']").fill("librarian")
        page.locator("[data-test='login-password']").fill("librarian")
        page.locator("[data-test='login-submit']").click()
        page.wait_for_selector("[data-test='main-content']")

        # Navigate to authors and add some
        page.locator("[data-test='menu-authors']").click()
        page.locator("[data-test='bulk-authors']").fill('[{"name": "Author 1"}, {"name": "Author 2"}]')
        page.locator("[data-test='bulk-import-authors-btn']").click()

        # Navigate to search
        page.locator("[data-test='menu-search']").click()

        # Perform search
        page.locator("[data-test='search-input']").fill("Author")
        page.locator("[data-test='search-btn']").click()

        page.wait_for_selector("[data-test='search-authors-list']")

        # Take screenshot
        page.screenshot(path="jules-scratch/verification/verification.png")

    finally:
        browser.close()

with sync_playwright() as playwright:
    run(playwright)